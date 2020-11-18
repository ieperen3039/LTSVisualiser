package NG.Graph;

import NG.Core.AbstractGameLoop;
import NG.Core.Main;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.PairList;
import NG.Tools.TimeObserver;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Math.log;

/**
 * @author Geert van Ieperen created on 2-8-2020.
 */
public class SpringLayout extends AbstractGameLoop implements ToolElement {
    private static final float EDGE_HANDLE_DISTANCE = 0.2f;
    private static final float MAX_NODE_MOVEMENT = 2f;

    public final TimeObserver timer = new TimeObserver(4, false);
    private final ExecutorService executor;
    private final List<Runnable> updateListeners = new ArrayList<>();
    private final int numThreads;
    private float natLength = 2f;
    private float repulsion = 5f;
    private float attraction = 1f; // 1/100th of what LTSGraph uses
    private float speed = 0;
    private float edgeRepulsion = 0.1f;

    private Graph graph;
    private boolean allow3D = true;
    private float barnesHutTheta = 0.5f;

    public SpringLayout(int iterationsPerSecond, int numThreads) {
        super("layout", iterationsPerSecond);
        this.numThreads = numThreads;
        executor = Executors.newFixedThreadPool(this.numThreads);
    }

    @Override
    public void init(Main root) throws Exception {
        graph = root.graph();
    }

    public synchronized void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    protected synchronized void update(float deltaTime) throws Exception {
        if (speed == 0) return;
        timer.startNewLoop();

        List<NG.Graph.State> nodes = graph.getNodeMesh().nodeList();
        List<Transition> edges = graph.getEdgeMesh().edgeList();

        BarnesHutTree barnesTree;
        if (barnesHutTheta > 0) {
            timer.startTiming("Barnes-Hut setup");
            barnesTree = new BarnesHutTree(1 << 10);
            barnesTree.setForceComputation((a, b) -> getRepulsion(a, b, natLength, repulsion));
            barnesTree.setMaxDepth(13);
            barnesTree.setMaxTheta(barnesHutTheta);

            for (NG.Graph.State node : nodes) {
                barnesTree.add(node.position);
            }
            timer.endTiming("Barnes-Hut setup");

        } else {
            barnesTree = null;
        }

        int batchSize = (nodes.size() / numThreads) + 1;
        List<Future<Vector3f[]>> futureResults = new ArrayList<>();
        Map<NG.Graph.State, Vector3f> nodeForces = new HashMap<>();

        // start node repulsion computations
        timer.startTiming("node repulsion scheduling");
        int index = 0;
        while (index < nodes.size()) {
            int startIndex = index;
            int endIndex = Math.min(index + batchSize, nodes.size());

            Callable<Vector3f[]> task = () -> computeRepulsions(nodes, startIndex, endIndex, barnesTree);

            Future<Vector3f[]> future = executor.submit(task);
            futureResults.add(future);

            index = endIndex;
        }

        timer.endTiming("node repulsion scheduling");
        timer.startTiming("node attraction computation");
        // node edge attraction
        for (Transition edge : edges) {
            computeNodeAttractionForces(nodeForces, edge);
        }

        timer.endTiming("node attraction computation");
        timer.startTiming("edge handle computation");

        Map<Transition, Vector3f> edgeHandleForces = new HashMap<>();

        // linear-time edge handle centering and self-loop spacing
        for (Transition edge : edges) {
            Vector3f force;

            if (edge.from == edge.to) {
                force = getEdgeEffect(edge.handlePos, edge.fromPosition, repulsion, natLength);

            } else {
                float dist = edge.fromPosition.distance(edge.toPosition) / 8;
                force = getAttractionQuadratic(edge.handlePos, edge.fromPosition, 1f, dist);
                force.add(getAttractionQuadratic(edge.handlePos, edge.toPosition, 1f, dist));
            }

            if (Vectors.isNaN(force)) {
                assert false : force;
            } else {
                edgeHandleForces.put(edge, force);
            }
        }

        if (edgeRepulsion != 0) {
            // quadratic-time edge handle repulsion
            for (NG.Graph.State node : nodes) {
                PairList<Transition, NG.Graph.State> neighbours = graph.connectionsOf(node);
                assert neighbours != null : node;

                for (int i = 0; i < neighbours.size(); i++) {
                    Transition a = neighbours.left(i);

                    for (int j = i + 1; j < neighbours.size(); j++) {
                        Transition b = neighbours.left(j);

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
        timer.startTiming("node repulsion collection");

        // collect node repulsion computations
        int i = 0;
        for (Future<Vector3f[]> future : futureResults) {
            Vector3f[] batch = future.get();

            for (int j = 0; j < batch.length; j++) {
                assert !Vectors.isNaN(batch[j]) : Arrays.toString(batch);
                NG.Graph.State node = nodes.get(i + j);
                Vector3f nodeForce = nodeForces.computeIfAbsent(node, k -> new Vector3f());
                nodeForce.add(batch[j]);
            }

            i += batch.length;
        }
        timer.endTiming("node repulsion collection");
        timer.startTiming("position update");

        // apply forces
        for (Transition edge : edges) {
            Vector3f force = edgeHandleForces.get(edge);

            // also include forces of the parent nodes to have edges move along with the parents
            Vector3f parentForces = new Vector3f();
            if (!edge.from.isFixed) parentForces.add(nodeForces.get(edge.from));
            if (!edge.to.isFixed) parentForces.add(nodeForces.get(edge.to));
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

        for (NG.Graph.State node : nodes) {
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

        updateListeners.forEach(Runnable::run);

//        if (tension < 1f) stopLoop();
    }

    private Vector3f[] computeRepulsions(
            List<NG.Graph.State> nodes, int startIndex, int endIndex, BarnesHutTree optionalBarnes
    ) {
        int nrOfNodes = endIndex - startIndex;
        Vector3f[] forces = new Vector3f[nrOfNodes];

        for (int i = 0; i < nrOfNodes; i++) {
            NG.Graph.State node = nodes.get(startIndex + i);

            if (optionalBarnes == null) {
                // naive implementation
                forces[i] = new Vector3f();

                for (NG.Graph.State other : nodes) {
                    if (node == other) continue;
                    Vector3f otherToThis = getRepulsion(node.position, other.position, natLength, repulsion);
                    forces[i].add(otherToThis);
                }

            } else {
                forces[i] = optionalBarnes.getForceOn(node.position);
            }

            assert !Vectors.isNaN(forces[i]) : node;
            if (Thread.interrupted()) return forces;
        }

        return forces;
    }

    private void computeNodeAttractionForces(Map<NG.Graph.State, Vector3f> nodeForces, Transition edge) {
        if (edge.from == edge.to) return;

        Vector3f aForce = nodeForces.computeIfAbsent(edge.from, node -> new Vector3f());
        Vector3f bForce = nodeForces.computeIfAbsent(edge.to, node -> new Vector3f());

        Vector3f force = getEdgeEffect(edge.fromPosition, edge.toPosition, attraction, natLength);
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

    public float getBarnesHutTheta() {
        return barnesHutTheta;
    }

    public void setBarnesHutTheta(float barnesHutTheta) {
        this.barnesHutTheta = barnesHutTheta;
    }

    @Override
    public synchronized void cleanup() {
        executor.shutdownNow();
        updateListeners.clear();
    }

    /** returns attraction on on a, affected by b */
    private static Vector3f getAttractionQuadratic(Vector3fc a, Vector3fc b, float attraction, float natLength) {
        Vector3f aToB = new Vector3f(b).sub(a);

        float length = aToB.length() * attraction + 1f;
        float factor = length * length * natLength - 0.1f;

        return aToB.mul(factor);
    }

    /** returns attraction-repulsion on on a, affected by b */
    private static Vector3f getEdgeEffect(Vector3fc a, Vector3fc b, float attraction, float natLength) {
        Vector3f aToB = new Vector3f(b).sub(a);

        float dist = Math.max(aToB.length(), 1.0f);
        float factor = (float) (attraction * 100 * log(dist / (natLength + 1.0f)) / dist);

        return aToB.mul(factor);
    }

    /** returns repulsion on a, affected by b */
    public static Vector3f getRepulsion(Vector3fc a, Vector3fc b, float natLength, float repulsion) {
        Vector3f bToA = new Vector3f(a).sub(b);
        float length = bToA.length();

        if (length < 1f / 32) {
            return Vectors.randomOrb().normalize(100);

        } else {
            float lengthFraction = Math.max(length / 2.0f, natLength / 10);
            float r = repulsion / (lengthFraction * lengthFraction * lengthFraction);
            return bToA.mul(r);
        }
    }
}
