package NG.Graph;

import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import org.joml.Vector3f;

import java.util.*;

import static NG.Core.Main.INITAL_STATE_COLOR;

/**
 * @author Geert van Ieperen created on 5-8-2020.
 */
public class NodeClustering extends Graph {

    // maps a new cluster node to the set of elements representing that cluster
    private final Map<State, Collection<State>> clusterMapping = new HashMap<>();
    private final Graph graph;
    private NodeMesh clusterNodes = new NodeMesh();
    private EdgeMesh clusterEdges = new EdgeMesh();
    private State clusterInitialState;

    public NodeClustering(SourceGraph graph) {
        this(graph, Collections.emptySet());
    }

    public NodeClustering(SourceGraph graph, Set<String> markedLabels) {
        this(graph, actionLabelCluster(graph, markedLabels), false);
    }

    public NodeClustering(SourceGraph graph, Map<State, State> leaderMap, boolean showSelfLoop) {
        super(graph.root, "Clustering of ");
        this.graph = graph;

        createCluster(leaderMap, showSelfLoop);
    }

    @Override
    public void setNodePosition(State node, Vector3f newPosition) {
        super.setNodePosition(node, newPosition);
        pushClusterPositions();
    }

    /**
     * sets this graph to a cluster based on the given cluster map
     * @param clusterLeaderMap maps each node to a 'leader' node where all nodes in one cluster refer to
     * @param showSelfLoop     if false, non-clustered edges resulting in self-loops are removed.
     */
    public synchronized void createCluster(Map<State, State> clusterLeaderMap, boolean showSelfLoop) {
        clusterMapping.clear();

        NodeMesh nodes = graph.getNodeMesh();
        EdgeMesh edges = graph.getEdgeMesh();

        // schedule disposal
        NodeMesh oldNodes = this.clusterNodes;
        EdgeMesh oldEdges = this.clusterEdges;
        root.executeOnRenderThread(() -> {
            oldNodes.dispose();
            oldEdges.dispose();
        });

        // create new cluster nodes
        clusterNodes = new NodeMesh();
        clusterEdges = new EdgeMesh();

        // maps a cluster leader to a new node representing the cluster
        Map<State, State> newNodes = new HashMap<>();
        List<State> nodeList = new ArrayList<>();
        // compute the clusters and create new cluster nodes
        for (State node : nodes.nodeList()) {
            // if node is not found in clusterMap, it is a leader
            State clusterLeader = getClusterLeader(clusterLeaderMap, node);
            assert clusterLeader != null;

            // map the leader to the clusterNode, or create when absent
            State clusterNode = newNodes.get(clusterLeader);
            if (clusterNode == null) {
                clusterNode = new State(clusterLeader.position, clusterLeader.label, nodeList.size(), clusterLeader.classIndex);
                nodeList.add(clusterNode);
                newNodes.put(clusterLeader, clusterNode);
            }

            if (node == graph.getInitialState()) {
                clusterInitialState = clusterNode;
                clusterNode.border = INITAL_STATE_COLOR;
            }

            // we create a new cluster if necessary, and add this node
            clusterMapping.computeIfAbsent(clusterNode, k -> new HashSet<>()).add(node);
        }

        // add new nodes to the graph
        for (State node : nodeList) {
            clusterNodes.addNode(node);
        }

        // add all edges
        for (Transition edge : edges.edgeList()) {
            State aNode = edge.from;
            State bNode = edge.to;

            State aTarget = newNodes.get(getClusterLeader(clusterLeaderMap, aNode));
            State bTarget = newNodes.get(getClusterLeader(clusterLeaderMap, bNode));

            // self loop
            if (aTarget == bTarget && !showSelfLoop) continue;

            // already exists an equal edge
            // even for non-deterministic graphs, this does not change the meaning of the graph
            if (edgeExists(aTarget, bTarget, edge.label)) continue;

            Transition newEdge = new Transition(aTarget, bTarget, edge.label);
            newEdge.handlePos.set(edge.handlePos);
            clusterEdges.addParticle(newEdge);
        }
    }

    /** Sets the position of the clustered nodes to the average of its source */
    public synchronized void pullClusterPositions() {
        clusterMapping.forEach((node, cluster) -> {
            Vector3f total = new Vector3f();
            for (State element : cluster) {
                total.add(element.position);
            }
            total.div(cluster.size());
            node.position.set(total);
        });

        for (Transition edge : clusterEdges.edgeList()) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
        }
    }

    /** Sets the average of the source nodes of each cluster to the clustered node */
    public synchronized void pushClusterPositions() {
        clusterMapping.forEach((node, cluster) -> {
            Vector3f total = new Vector3f();
            for (State element : cluster) {
                total.add(element.position);
            }
            total.div(cluster.size());
            Vector3f movement = new Vector3f(node.position).sub(total);

            for (State element : cluster) {
                element.position.add(movement);
            }
        });

        for (Transition edge : graph.getEdgeMesh().edgeList()) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
        }
    }

    @Override
    public Collection<String> getEdgeLabels() {
        return graph.getEdgeLabels();
    }

    public synchronized NodeMesh getNodeMesh() {
        return clusterNodes;
    }

    public synchronized EdgeMesh getEdgeMesh() {
        return clusterEdges;
    }

    private static State getClusterLeader(Map<State, State> leaderMap, State node) {
        while (leaderMap.containsKey(node)) {
            node = leaderMap.get(node);
        }
        return node;
    }

    /** returns an actionlabel based clustering */
    public static Map<State, State> actionLabelCluster(Graph graph, Set<String> actionLabels) {
        return actionLabelCluster(graph, actionLabels, new HashMap<>());
    }

    /** adds an actionlabel based clustering to the given leader map */
    public static Map<State, State> actionLabelCluster(
            Graph graph, Set<String> actionLabels, Map<State, State> initialMap
    ) {
        for (Transition edge : graph.getEdgeMesh().edgeList()) {
            if (!actionLabels.contains(edge.label)) continue;

            State aLeader = getClusterLeader(initialMap, edge.from);
            State bLeader = getClusterLeader(initialMap, edge.to);

            if (edge.from != aLeader) initialMap.put(edge.from, aLeader);
            if (edge.to != aLeader) initialMap.put(edge.to, aLeader);
            if (bLeader != aLeader) initialMap.put(bLeader, aLeader);
        }

        return initialMap;
    }

    @Override
    public synchronized void cleanup() {
        NodeMesh oldNodes = this.clusterNodes;
        EdgeMesh oldEdges = this.clusterEdges;
        root.executeOnRenderThread(() -> {
            oldNodes.dispose();
            oldEdges.dispose();
        });
        clusterNodes = null;
        clusterEdges = null;
    }

    @Override
    public State getInitialState() {
        return clusterInitialState;
    }

    /**
     * @return true iff there is already an edge starting at aTarget, ending at bTarget, with a label {@link
     * String#equals(Object) equal} to the given label
     */
    public static boolean edgeExists(State aTarget, State bTarget, String label) {
        List<Transition> outgoing = aTarget.getOutgoing();
        if (outgoing.isEmpty()) return false;

        for (Transition transition : outgoing) {
            if (transition.to == bTarget) {
                String labelOfExisting = transition.label;

                if (labelOfExisting.equals(label)) {
                    return true;
                }
            }
        }

        return false;
    }
}
