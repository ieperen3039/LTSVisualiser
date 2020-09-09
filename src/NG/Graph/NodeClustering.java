package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import org.joml.Vector3f;

import java.util.*;

/**
 * @author Geert van Ieperen created on 5-8-2020.
 */
public class NodeClustering extends Graph {
    private final Map<NodeMesh.Node, PairList<EdgeMesh.Edge, NodeMesh.Node>> neighbourMapping = new HashMap<>();

    // maps a new cluster node to the set of elements representing that cluster
    private final Map<NodeMesh.Node, Collection<NodeMesh.Node>> clusterMapping = new HashMap<>();
    private final Set<String> edgeAttributeCluster = new HashSet<>();
    private final Graph sourceGraph;
    private NodeMesh clusterNodes = new NodeMesh();
    private EdgeMesh clusterEdges = new EdgeMesh();
    private NodeMesh.Node clusterInitialState;

    public NodeClustering(Graph sourceGraph) {
        this.sourceGraph = sourceGraph;
    }

    @Override
    public void init(Main root) {
        super.init(root);
        createCluster(Collections.emptyMap());
    }

    @Override
    public void setNodePosition(NodeMesh.Node node, Vector3f newPosition) {
        super.setNodePosition(node, newPosition);
        pushClusterPositions();
    }

    /**
     * sets this graph to a cluster based on the given cluster map
     * @param clusterLeaderMap maps each node to a 'leader' node where all nodes in one cluster refer to
     */
    public synchronized void createCluster(Map<NodeMesh.Node, NodeMesh.Node> clusterLeaderMap) {
        clusterMapping.clear();
        neighbourMapping.clear();

        NodeMesh nodes = sourceGraph.getNodeMesh();
        EdgeMesh edges = sourceGraph.getEdgeMesh();

        // maps a cluster leader to a new node representing the cluster
        Map<NodeMesh.Node, NodeMesh.Node> newNodes = new HashMap<>();
        for (NodeMesh.Node node : nodes.nodeList()) {
            // if node is not found in clusterMap, it is a leader
            NodeMesh.Node clusterLeader = getClusterLeader(clusterLeaderMap, node);
            assert clusterLeader != null;
            // map the leader to the clusterNode, or create when absent
            NodeMesh.Node clusterNode = newNodes.computeIfAbsent(clusterLeader, old -> new NodeMesh.Node(old.position, old.label));

            if (node == sourceGraph.getInitialState()) {
                clusterInitialState = clusterNode;
                clusterNode.addColor(Color4f.GREEN, GraphElement.Priority.INITIAL_STATE);
            }

            // we create a new cluster if necessary, and add this node
            clusterMapping.computeIfAbsent(clusterNode, k -> new HashSet<>()).add(node);
        }
        // maps a node representing a cluster to connected nodes representing clusters
        Map<NodeMesh.Node, Collection<EdgeMesh.Edge>> clusterEdgeMap = new HashMap<>();

        clusterMapping.forEach((clusterNode, cluster) -> {
            Map<NodeMesh.Node, EdgeMesh.Edge> connections = new HashMap<>();

            if (cluster.size() > 1) clusterNode.label = "cluster";

            // for every element in this cluster
            for (NodeMesh.Node element : cluster) {
                // for each node connected to this element
                PairList<EdgeMesh.Edge, NodeMesh.Node> neighbours = sourceGraph.connectionsOf(element);
                assert neighbours != null : element;

                for (int i = 0; i < neighbours.size(); i++) {
                    NodeMesh.Node other = neighbours.right(i);
                    EdgeMesh.Edge edge = neighbours.left(i);
                    // for each element to outside this cluster
                    if (cluster.contains(other)) continue;
                    // add a connection from this cluster to the cluster of that element
                    NodeMesh.Node otherLeader = getClusterLeader(clusterLeaderMap, other);
                    NodeMesh.Node connectedNode = newNodes.get(otherLeader);

                    if (!connections.containsKey(connectedNode)) {
                        String label = edge.label;
                        connections.put(connectedNode, new EdgeMesh.Edge(clusterNode, connectedNode, label));

                    } else {
                        connections.get(connectedNode).label += "+" + edge.label;
                    }
                }
            }

            clusterEdgeMap.put(clusterNode, connections.values());
        });

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

        // set positions to graph
        for (NodeMesh.Node node : newNodes.values()) {
            clusterNodes.addParticle(node);
            PairList<EdgeMesh.Edge, NodeMesh.Node> neighbours = new PairList<>();

            for (EdgeMesh.Edge edge : clusterEdgeMap.get(node)) {
                assert node == edge.a;
                edge.handlePos.set(node.position).lerp(edge.b.position, 0.5f);

                clusterEdges.addParticle(edge);
                neighbours.add(edge, edge.b);
            }

            neighbourMapping.put(node, neighbours);
        }
    }

    private NodeMesh.Node getClusterLeader(Map<NodeMesh.Node, NodeMesh.Node> leaderMap, NodeMesh.Node node) {
        while (leaderMap.containsKey(node)) {
            node = leaderMap.get(node);
        }
        return node;
    }

    /** Sets the position of the clustered nodes to the average of its source */
    public synchronized void pullClusterPositions() {
        clusterMapping.forEach((node, cluster) -> {
            Vector3f total = new Vector3f();
            for (NodeMesh.Node element : cluster) {
                total.add(element.position);
            }
            total.div(cluster.size());
            node.position.set(total);
        });

        for (EdgeMesh.Edge edge : clusterEdges.edgeList()) {
            edge.handlePos.set(edge.aPosition).lerp(edge.bPosition, 0.5f);
        }
    }

    /** Sets the average of the source nodes of each cluster to the clustered node */
    public synchronized void pushClusterPositions() {
        clusterMapping.forEach((node, cluster) -> {
            Vector3f total = new Vector3f();
            for (NodeMesh.Node element : cluster) {
                total.add(element.position);
            }
            total.div(cluster.size());
            Vector3f movement = new Vector3f(node.position).sub(total);

            for (NodeMesh.Node element : cluster) {
                element.position.add(movement);
            }
        });

        for (EdgeMesh.Edge edge : sourceGraph.getEdgeMesh().edgeList()) {
            edge.handlePos.set(edge.aPosition).lerp(edge.bPosition, 0.5f);
        }
    }

    private Map<NodeMesh.Node, NodeMesh.Node> attributeCluster(Set<String> attributeLabels) {
        Map<NodeMesh.Node, NodeMesh.Node> leaderMap = new HashMap<>();

        for (EdgeMesh.Edge edge : sourceGraph.getEdgeMesh().edgeList()) {
            if (!attributeLabels.contains(edge.label)) continue;

            NodeMesh.Node aLeader = getClusterLeader(leaderMap, edge.a);
            NodeMesh.Node bLeader = getClusterLeader(leaderMap, edge.b);

            if (edge.a != aLeader) leaderMap.put(edge.a, aLeader);
            if (edge.b != aLeader) leaderMap.put(edge.b, aLeader);
            if (bLeader != aLeader) leaderMap.put(bLeader, aLeader);
        }

        return leaderMap;
    }

    @Override
    public PairList<EdgeMesh.Edge, NodeMesh.Node> connectionsOf(NodeMesh.Node node) {
        return neighbourMapping.getOrDefault(node, PairList.empty());
    }

    public synchronized NodeMesh getNodeMesh() {
        return clusterNodes;
    }

    public synchronized EdgeMesh getEdgeMesh() {
        return clusterEdges;
    }

    @Override
    public Collection<String> getEdgeAttributes() {
        Collection<String> edgeAttributes = new ArrayList<>(sourceGraph.getEdgeAttributes());
        edgeAttributes.removeAll(edgeAttributeCluster);
        return edgeAttributes;
    }

    public Set<String> getClusterAttributes() {
        return edgeAttributeCluster;
    }

    public void clusterEdgeAttribute(String label, boolean on) {
        if (on) {
            edgeAttributeCluster.add(label);
        } else {
            edgeAttributeCluster.remove(label);
        }

        createCluster(attributeCluster(edgeAttributeCluster));
    }

    @Override
    public void cleanup() {
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
    protected NodeMesh.Node getInitialState() {
        return clusterInitialState;
    }
}
