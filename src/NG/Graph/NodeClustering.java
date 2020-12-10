package NG.Graph;

import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import org.joml.Vector3f;

import java.util.*;

import static NG.Core.Main.INITAL_STATE_COLOR;

/**
 * @author Geert van Ieperen created on 5-8-2020.
 */
public class NodeClustering extends Graph {
    // maps nodes to their neighbours
    private final Map<State, PairList<Transition, State>> incomingTransitions = new HashMap<>();
    private final Map<State, PairList<Transition, State>> outgoingTransitions = new HashMap<>();

    // maps a new cluster node to the set of elements representing that cluster
    private final Map<State, Collection<State>> clusterMapping = new HashMap<>();
    private final Set<String> edgeActionLabelCluster = new HashSet<>();
    private final Graph graph;
    private NodeMesh clusterNodes = new NodeMesh();
    private EdgeMesh clusterEdges = new EdgeMesh();
    private State clusterInitialState;
    private boolean showSelfLoop;
    private boolean isDirty = false;

    public NodeClustering(SourceGraph graph, Collection<String> markedLabels) {
        super(graph.root);
        this.graph = graph;
        this.showSelfLoop = true;

        edgeActionLabelCluster.addAll(markedLabels);
        createCluster(actionLabelCluster(graph, edgeActionLabelCluster), showSelfLoop);
    }

    public NodeClustering(SourceGraph graph, Map<State, State> leaderMap, boolean showSelfLoop, String... labels) {
        super(graph.root);
        this.graph = graph;
        this.showSelfLoop = showSelfLoop;

        Collections.addAll(edgeActionLabelCluster, labels);
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
     * @param showSelfLoop
     */
    public synchronized void createCluster(Map<State, State> clusterLeaderMap, boolean showSelfLoop) {
        clusterMapping.clear();
        incomingTransitions.clear();
        outgoingTransitions.clear();

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
        // compute the clusters and create new cluster nodes
        for (State node : nodes.nodeList()) {
            // if node is not found in clusterMap, it is a leader
            State clusterLeader = getClusterLeader(clusterLeaderMap, node);
            assert clusterLeader != null;
            // map the leader to the clusterNode, or create when absent
            State clusterNode = newNodes.computeIfAbsent(clusterLeader, old -> new State(old.position, old.label, old.index, old.classIndex));

            if (node == graph.getInitialState()) {
                clusterInitialState = clusterNode;
                clusterNode.border = INITAL_STATE_COLOR;
            }

            // we create a new cluster if necessary, and add this node
            clusterMapping.computeIfAbsent(clusterNode, k -> new HashSet<>()).add(node);
        }

        // add new nodes to the graph
        for (State node : newNodes.values()) {
            clusterNodes.addNode(node);
        }

        // add all edges
        for (Transition edge : edges.edgeList()) {
            State aNode = edge.from;
            State bNode = edge.to;

            State aTarget = newNodes.get(getClusterLeader(clusterLeaderMap, aNode));
            State bTarget = newNodes.get(getClusterLeader(clusterLeaderMap, bNode));

            // self loop
            if (aTarget == bTarget && (!showSelfLoop || edgeActionLabelCluster.contains(edge.label))) continue;

            // already exists an equal edge
            // even for non-deterministic graphs, this does not change the meaning of the graph
            if (edgeExists(outgoingTransitions, aTarget, bTarget, edge.label)) continue;

            Transition newEdge = new Transition(aTarget, bTarget, edge.label);
            newEdge.handlePos.set(edge.handlePos);
            clusterEdges.addParticle(newEdge);

            outgoingTransitions.computeIfAbsent(aTarget, n -> new PairList<>()).add(newEdge, bTarget);
            incomingTransitions.computeIfAbsent(bTarget, n -> new PairList<>()).add(newEdge, aTarget);
        }

        isDirty = false;
    }

    /** Sets the position of the clustered nodes to the average of its source */
    public synchronized void pullClusterPositions() {
        checkDirty();

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
        checkDirty();

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

    private synchronized void checkDirty() {
        if (isDirty) {
            createCluster(actionLabelCluster(graph, edgeActionLabelCluster), showSelfLoop);
            isDirty = false;
        }
    }

    @Override
    public PairList<Transition, State> incomingOf(State node) {
        return incomingTransitions.getOrDefault(node, PairList.empty());
    }

    @Override
    public PairList<Transition, State> outgoingOf(State node) {
        return outgoingTransitions.getOrDefault(node, PairList.empty());
    }

    @Override
    public Collection<String> getEdgeLabels() {
        Collection<String> edgeActionLabels = new ArrayList<>(graph.getEdgeLabels());
        edgeActionLabels.removeAll(edgeActionLabelCluster);
        return edgeActionLabels;
    }

    public synchronized NodeMesh getNodeMesh() {
        checkDirty();
        return clusterNodes;
    }

    public synchronized EdgeMesh getEdgeMesh() {
        checkDirty();
        return clusterEdges;
    }

    public synchronized void setLabelCluster(String label, boolean doCluster) {
        if (doCluster) {
            edgeActionLabelCluster.add(label);
        } else {
            edgeActionLabelCluster.remove(label);
        }

        isDirty = true;
    }

    private static State getClusterLeader(Map<State, State> leaderMap, State node) {
        while (leaderMap.containsKey(node)) {
            node = leaderMap.get(node);
        }
        return node;
    }

    public Set<String> getClusterActionLabels() {
        return edgeActionLabelCluster;
    }

    private static Map<State, State> actionLabelCluster(Graph sourceGraph, Set<String> actionLabels) {
        Map<State, State> leaderMap = new HashMap<>();

        for (Transition edge : sourceGraph.getEdgeMesh().edgeList()) {
            if (!actionLabels.contains(edge.label)) continue;

            State aLeader = getClusterLeader(leaderMap, edge.from);
            State bLeader = getClusterLeader(leaderMap, edge.to);

            if (edge.from != aLeader) leaderMap.put(edge.from, aLeader);
            if (edge.to != aLeader) leaderMap.put(edge.to, aLeader);
            if (bLeader != aLeader) leaderMap.put(bLeader, aLeader);
        }

        return leaderMap;
    }

    @Override
    public synchronized void cleanup() {
        isDirty = false;
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
    protected State getInitialState() {
        checkDirty();
        return clusterInitialState;
    }

    public void setShowSelfLoop(boolean showSelfLoop) {
        this.showSelfLoop = showSelfLoop;
    }

    /**
     * @return true iff there is already an edge starting at aTarget, ending at bTarget, with a label {@link
     * String#equals(Object) equal} to the given label
     */
    public static boolean edgeExists(
            Map<State, PairList<Transition, State>> neighbourMapping, State aTarget,
            State bTarget, String label
    ) {
        PairList<Transition, State> existingATargets = neighbourMapping.get(aTarget);
        if (existingATargets == null) return false;

        for (int i = 0; i < existingATargets.size(); i++) {
            State existingBTarget = existingATargets.right(i);

            if (existingBTarget == bTarget) {
                String labelOfExisting = existingATargets.left(i).label;

                if (labelOfExisting.equals(label)) {
                    return true;
                }
            }
        }

        return false;
    }
}
