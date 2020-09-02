package NG.Graph;

import NG.Core.AbstractGameLoop;
import NG.Core.Main;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.TimeObserver;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Math.log;

/**
 * @author Geert van Ieperen created on 2-8-2020.
 */
public class SpringLayout extends AbstractGameLoop implements ToolElement {
    public static final float EDGE_HANDLE_FORCE_FACTOR = 10f;
    public static final float EDGE_HANDLE_DISTANCE = 1f;
    private static final int NUM_THREADS = 5;
    private static final float MAX_NODE_MOVEMENT = 10f;

    public final TimeObserver timer = new TimeObserver(10, false);
    private final ExecutorService executor;
    private final List<Runnable> updateListeners = new ArrayList<>();
    private float natLength = 1f;
    private float repulsion = 5f;
    private float attraction = 5f; // 1/100th of what LTSGraph uses
    private float speed = 0;
    private float edgeRepulsion = 0.5f;

    private Graph graph;
    private boolean allow3D = true;

    public SpringLayout() {
        super("layout", 200);
        executor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @Override
    public void init(Main root) throws Exception {
        graph = root.graph();
    }

    public synchronized void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    protected void update(float deltaTime) throws Exception {
        if (speed == 0) return;
        timer.startNewLoop();

        synchronized (this) {
            List<NodeMesh.Node> nodes = graph.getNodeMesh().nodeList();
            List<EdgeMesh.Edge> edges = graph.getEdgeMesh().edgeList();

            int batchSize = (nodes.size() / NUM_THREADS) + 1;
            List<Future<Vector3f[]>> futureResults = new ArrayList<>();
            Map<NodeMesh.Node, Vector3f> nodeForces = new HashMap<>();

            // start node repulsion computations
            timer.startTiming("node scheduling");
            int index = 0;
            while (index < nodes.size()) {
                int startIndex = index;
                int endIndex = Math.min(index + batchSize, nodes.size());

                Callable<Vector3f[]> task = () -> computeRepulsions(nodes, startIndex, endIndex);

                Future<Vector3f[]> future = executor.submit(task);
                futureResults.add(future);

                index = endIndex;
            }

            timer.endTiming("node scheduling");
            timer.startTiming("node attraction computation");
            // node edge attraction
            for (EdgeMesh.Edge edge : edges) {
                computeNodeAttractionForces(nodeForces, edge);
            }

            timer.endTiming("node attraction computation");
            timer.startTiming("edge handle computation");

            Map<EdgeMesh.Edge, Vector3f> edgeHandleForces = new HashMap<>();

            // edge handle centering
            for (EdgeMesh.Edge edge : edges) {
                Vector3f target = new Vector3f(edge.aPosition).lerp(edge.bPosition, 0.5f);
                Vector3f force = getAttractionQuadratic(edge.handlePos, target, EDGE_HANDLE_FORCE_FACTOR, EDGE_HANDLE_DISTANCE);

                if (!Vectors.isNaN(force)) {
                    edgeHandleForces.put(edge, force);
                } else {
                    assert false : force;
                }
            }

            if (edgeRepulsion > 0) {
                // edge handle repulsion
                for (NodeMesh.Node node : nodes) {
                    PairList<EdgeMesh.Edge, NodeMesh.Node> neighbours = graph.connectionsOf(node);

                    for (int i = 0; i < neighbours.size(); i++) {
                        EdgeMesh.Edge a = neighbours.left(i);

                        for (int j = i + 1; j < neighbours.size(); j++) {
                            EdgeMesh.Edge b = neighbours.left(j);

                            Vector3f force = getRepulsion(a.handlePos, b.handlePos, EDGE_HANDLE_DISTANCE, edgeRepulsion);

                            if (!Vectors.isNaN(force)) {
                                edgeHandleForces.get(a).add(force);
                                edgeHandleForces.get(b).add(force.negate());
                            } else {
                                assert false : force;
                            }
                        }
                    }
                }
            }

            timer.endTiming("edge handle computation");
            timer.startTiming("node force collection");

            // collect node repulsion computations
            int i = 0;
            for (Future<Vector3f[]> future : futureResults) {
                Vector3f[] batch = future.get();

                for (int j = 0; j < batch.length; j++) {
                    assert !Vectors.isNaN(batch[j]);
                    NodeMesh.Node node = nodes.get(i + j);
                    Vector3f nodeForce = nodeForces.computeIfAbsent(node, k -> new Vector3f());
                    nodeForce.add(batch[j]);
                }

                i += batch.length;
            }
            timer.endTiming("node force collection");
            timer.startTiming("position update");

            // apply forces
            for (EdgeMesh.Edge edge : edges) {
                Vector3f force = edgeHandleForces.get(edge);

                // also include forces of the parent nodes to have edges move along with the parents
                Vector3f parentForces = new Vector3f();
                if (!edge.a.isFixed) parentForces.add(nodeForces.get(edge.a));
                if (!edge.b.isFixed) parentForces.add(nodeForces.get(edge.b));
                parentForces.div(2);

                force.add(parentForces);

                Vector3f movement = force.mul(speed); // modifies edgeHandleForces

                if (movement.length() > MAX_NODE_MOVEMENT) {
                    movement.normalize(MAX_NODE_MOVEMENT);
                }
                if (!allow3D) {
                    movement.z = 0;
                }

                edge.handlePos.add(movement);
                assert !Vectors.isNaN(edge.handlePos) : movement;
            }

            for (NodeMesh.Node node : nodes) {
                if (node.isFixed) continue;

                Vector3f force = nodeForces.get(node);
                Vector3f movement = force.mul(speed); // modifies nodeForces

                if (movement.length() > MAX_NODE_MOVEMENT) {
                    movement.normalize(MAX_NODE_MOVEMENT);
                }
                if (!allow3D) {
                    movement.z = 0;
                }

                node.position.add(movement);
                assert !Vectors.isNaN(node.position) : movement;
            }

            timer.endTiming("position update");
        }

        // notifying listeners must not be synchronized on this
        updateListeners.forEach(Runnable::run);

//        if (tension < 1f) stopLoop();
    }

    private Vector3f[] computeRepulsions(List<NodeMesh.Node> nodes, int startIndex, int endIndex) {
        int nrOfNodes = endIndex - startIndex;
        Vector3f[] forces = new Vector3f[nrOfNodes];

        for (int i = 0; i < nrOfNodes; i++) {
            forces[i] = new Vector3f();
            NodeMesh.Node node = nodes.get(startIndex + i);

            for (NodeMesh.Node other : nodes) {
                if (node == other) continue;
                Vector3f otherToThis = getRepulsion(node.position, other.position, natLength, repulsion);
                forces[i].add(otherToThis);
            }

            if (Thread.interrupted()) return forces;
        }

        return forces;
    }

    private void computeNodeAttractionForces(Map<NodeMesh.Node, Vector3f> nodeForces, EdgeMesh.Edge edge) {
        Vector3f aForce = nodeForces.computeIfAbsent(edge.a, node -> new Vector3f());
        Vector3f bForce = nodeForces.computeIfAbsent(edge.b, node -> new Vector3f());

        Vector3f force = getAttractionLTS(edge.aPosition, edge.bPosition, attraction, natLength);
        assert !Vectors.isNaN(force);

        aForce.add(force);
        bForce.add(force.negate());
    }

    public float getEdgeRepulsionFactor() {
        return edgeRepulsion;
    }

    public void setEdgeRepulsionFactor(float edgeRepulsion) {
        this.edgeRepulsion = edgeRepulsion;
    }

    public float getRepulsionFactor() {
        return repulsion;
    }

    public void setRepulsionFactor(float repulsion) {
        this.repulsion = repulsion;
    }

    public float getAttractionFactor() {
        return attraction;
    }

    public void setAttractionFactor(float attraction) {
        this.attraction = attraction;
    }

    public void addUpdateListeners(Runnable action) {
        updateListeners.add(action);
    }

    @Override
    public synchronized void cleanup() {
        executor.shutdownNow();
        updateListeners.clear();
    }

    public boolean doAllow3D() {
        return allow3D;
    }

    public void setAllow3D(boolean allow3D) {
        this.allow3D = allow3D;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getNatLength() {
        return natLength;
    }

    public void setNatLength(float natLength) {
        this.natLength = Math.max(natLength, 0.01f);
    }

    private static Vector3f getAttractionQuadratic(Vector3fc a, Vector3fc b, float attraction, float natLength) {
        Vector3f aToB = new Vector3f(b).sub(a);

        float length = aToB.length() * attraction + 1f;
        float factor = length * length * natLength - 0.1f;

        return aToB.mul(factor);
    }

    private static Vector3f getAttractionLTS(Vector3fc a, Vector3fc b, float attraction, float natLength) {
        Vector3f aToB = new Vector3f(b).sub(a);

        float dist = Math.max(aToB.length(), 1.0f);
        float factor = (float) (attraction * 100 * log(dist / (natLength + 1.0f)) / dist);

        return aToB.mul(factor);
    }

    private static Vector3f getRepulsion(Vector3fc a, Vector3fc b, float natLength, float repulsion) {
        Vector3f otherToThis = new Vector3f(a).sub(b);
        float length = otherToThis.length();

        if (length < 1f / 100) {
            return Vectors.randomOrb().normalize(100);

        } else {
            float lengthFraction = Math.max(length / 2.0f, natLength / 10);
            float r = repulsion / (lengthFraction * lengthFraction * lengthFraction);
            return otherToThis.mul(r);
        }
    }
}
