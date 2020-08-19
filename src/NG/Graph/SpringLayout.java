package NG.Graph;

import NG.Core.AbstractGameLoop;
import NG.Core.Root;
import NG.Core.ToolElement;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.TimeObserver;
import NG.Tools.Vectors;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Math.log;
import static java.lang.Math.max;

/**
 * @author Geert van Ieperen created on 2-8-2020.
 */
public class SpringLayout extends AbstractGameLoop implements ToolElement {
    private static final int NUM_THREADS = 1;
    public final TimeObserver timer = new TimeObserver(10, false);
    private final NodeMesh nodes;
    private final EdgeMesh edges;
    private final ExecutorService executor;
    List<Runnable> updateListeners = new ArrayList<>();
    private float natLength = 1f;
    private float repulsion = 5f;
    private float attraction = 5f; // 1/100th of what LTSGraph uses
    private float speed = 0;
    private Root root;

    public SpringLayout(NodeMesh nodes, EdgeMesh edges) {
        super("layout", 200);
        this.nodes = nodes;
        this.edges = edges;

        executor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @Override
    public void init(Root root) throws Exception {
        this.root = root;
    }

    @Override
    protected void update(float deltaTime) throws Exception {
        timer.startNewLoop();

        List<NodeMesh.Node> nodeList = nodes.nodeList();
        int batchSize = (nodeList.size() / NUM_THREADS) + 1;
        List<Future<Vector3f[]>> futureResults = new ArrayList<>();
        Map<NodeMesh.Node, Vector3f> nodeForces = new HashMap<>();

        // start node repulsion computations
        timer.startTiming("node scheduling");
        int index = 0;
        while (index < nodeList.size()) {
            int startIndex = index;
            int endIndex = Math.min(index + batchSize, nodeList.size());

            Callable<Vector3f[]> task = () -> computeRepulsions(nodeList, startIndex, endIndex);

            Future<Vector3f[]> future = executor.submit(task);
            futureResults.add(future);

            index = endIndex;
        }

        timer.endTiming("node scheduling");
        timer.startTiming("edge computation");
        // do edge computation
        List<EdgeMesh.Edge> edgeList = edges.edgeList();
        for (EdgeMesh.Edge edge : edgeList) {
            attractionComputation(nodeForces, edge);
        }
        timer.endTiming("edge computation");
        timer.startTiming("node force collection");

        // collect node repulsion computations
        int i = 0;
        for (Future<Vector3f[]> future : futureResults) {
            Vector3f[] batch = future.get();

            for (int j = 0; j < batch.length; j++) {
                NodeMesh.Node node = nodeList.get(i + j);
                Vector3f nodeForce = nodeForces.computeIfAbsent(node, k -> new Vector3f());
                nodeForce.add(batch[j]);
            }

            i += batch.length;
        }
        timer.endTiming("node force collection");
        timer.startTiming("position update");

        // apply forces
        float tension = 0;
        for (NodeMesh.Node node : nodeList) {
            Vector3f force = nodeForces.get(node);
            tension += force.length();

            Vector3f movement = force.mul(speed);
            node.position.add(movement);
        }
        timer.endTiming("position update");

        updateListeners.forEach(Runnable::run);

//        if (tension < 1f) stopLoop();
    }

    public Vector3f[] computeRepulsions(List<NodeMesh.Node> nodeList, int startIndex, int endIndex) {
        int nrOfNodes = endIndex - startIndex;
        Vector3f[] forces = new Vector3f[nrOfNodes];

        for (int i = 0; i < nrOfNodes; i++) {
            forces[i] = new Vector3f();
            NodeMesh.Node node = nodeList.get(startIndex + i);

            for (NodeMesh.Node other : nodeList) {
                if (node == other) continue;
                Vector3f otherToThis = getRepulsion(node, other);
                forces[i].add(otherToThis);
            }
        }

        return forces;
    }

    private void attractionComputation(Map<NodeMesh.Node, Vector3f> nodeForces, EdgeMesh.Edge edge) {
        Vector3f aForce = nodeForces.computeIfAbsent(edge.a, node -> new Vector3f());
        Vector3f bForce = nodeForces.computeIfAbsent(edge.b, node -> new Vector3f());

        Vector3f aToB = new Vector3f(edge.bPosition).sub(edge.aPosition);

        float dist = Math.max(aToB.length(), 1.0f);
        float factor = (float) (attraction * 100 * log(dist / (natLength + 1.0f)) / dist);
        aToB.mul(factor);

        aForce.add(aToB);
        bForce.add(aToB.negate());
    }

    private Vector3f getRepulsion(NodeMesh.Node node, NodeMesh.Node other) {
        Vector3f otherToThis = new Vector3f(node.position).sub(other.position);

        float length = otherToThis.length();

        float natFraction = 1f / 100;
        if (length < natFraction) {
            return Vectors.randomOrb().normalize(100);

        } else {
            float lengthFraction = max(length / 2.0f, natLength / 10);
            float r = repulsion / (lengthFraction * lengthFraction * lengthFraction);
            return otherToThis.mul(r);
        }
    }

    public float getRepulsion() {
        return repulsion;
    }

    public void setRepulsion(float repulsion) {
        this.repulsion = repulsion;
    }

    public float getAttraction() {
        return attraction;
    }

    public void setAttraction(float attraction) {
        this.attraction = attraction;
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
        this.natLength = natLength;
    }

    public void addUpdateListeners(Runnable action){
        updateListeners.add(action);
    }

    @Override
    public void cleanup() {
        updateListeners.clear();
        executor.shutdown();
    }
}
