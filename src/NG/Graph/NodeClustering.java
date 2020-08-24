package NG.Graph;

import NG.Core.Root;
import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 5-8-2020.
 */
public class NodeClustering {
    private final Root root;
    private final NodeMesh nodes;
    private final EdgeMesh edges;
    private final Map<NodeMesh.Node, Collection<NodeMesh.Node>> originalConnectivity;

    // maps a new cluster node to the set of elements representing that cluster
    private final Map<NodeMesh.Node, Collection<NodeMesh.Node>> clusterMapping = new HashMap<>();
    private final Set<String> edgeAttributeCluster = new HashSet<>();
    private NodeMesh clusterNodes = null;
    private EdgeMesh clusterEdges = null;
    private Main.ClusterMethod method;

    public NodeClustering(Root root, Graph graph, Main.ClusterMethod method) {
        this.root = root;
        this.nodes = graph.getNodeMesh();
        this.edges = graph.getEdgeMesh();
        this.originalConnectivity = graph.mapping;

        setMethod(method);
    }

    public synchronized void createCluster(Supplier<Map<NodeMesh.Node, NodeMesh.Node>> leaderMapGenerator) {
        clusterMapping.clear();

        // maps each node to a 'leader' node where all nodes in one cluster refer to
        Map<NodeMesh.Node, NodeMesh.Node> clusterLeaderMap = leaderMapGenerator.get();

        // maps a cluster leader to a new node representing the cluster
        Map<NodeMesh.Node, NodeMesh.Node> newNodes = new HashMap<>();
        for (NodeMesh.Node node : nodes.nodeList()) {
            // if node is not found in clusterMap, it is a leader
            NodeMesh.Node clusterLeader = getClusterLeader(clusterLeaderMap, node);
            assert clusterLeader != null;
            // map the leader to the clusterNode, or create when absent
            NodeMesh.Node clusterNode = newNodes.computeIfAbsent(clusterLeader, old -> new NodeMesh.Node(old.position, "cluster"));

            // we create a new cluster if necessary, and add this node
            clusterMapping.computeIfAbsent(clusterNode, k -> new HashSet<>())
                    .add(node);
        }
        // maps a node representing a cluster to connected nodes representing clusters
        Map<NodeMesh.Node, Collection<NodeMesh.Node>> clusterEdgeMap = new HashMap<>();

        clusterMapping.forEach((clusterNode, cluster) -> {
            HashSet<NodeMesh.Node> connections = new HashSet<>();

            // for every element in this cluster
            for (NodeMesh.Node element : cluster) {
                // for each node connected to this element
                for (NodeMesh.Node other : originalConnectivity.get(element)) {
                    // for each element to outside this cluster
                    if (cluster.contains(other)) continue;
                    // add a connection from this cluster to the cluster of that element
                    NodeMesh.Node otherLeader = getClusterLeader(clusterLeaderMap, other);
                    NodeMesh.Node connectedNode = newNodes.get(otherLeader);
                    connections.add(connectedNode);
                }
            }

            clusterEdgeMap.put(clusterNode, connections);
        });

        // schedule disposal
        NodeMesh nodes = this.clusterNodes;
        EdgeMesh edges = this.clusterEdges;
        if (nodes != null && edges != null) {
            root.executeOnRenderThread(() -> {
                nodes.dispose();
                edges.dispose();
            });
        }

        // create new cluster nodes
        clusterNodes = new NodeMesh();
        clusterEdges = new EdgeMesh();

        // set positions to graph
        for (NodeMesh.Node node : newNodes.values()) {
            clusterNodes.addParticle(node);

            for (NodeMesh.Node other : clusterEdgeMap.get(node)) {
                assert other != null;
                clusterEdges.addParticle(node, other, "");
            }
        }
    }

    private NodeMesh.Node getClusterLeader(Map<NodeMesh.Node, NodeMesh.Node> leaderMap, NodeMesh.Node node){
        while (leaderMap.containsKey(node)){
            node = leaderMap.get(node);
        }
        return node;
    }

    public void setMethod(Main.ClusterMethod method) {
        if (this.method == method) return;
        this.method = method;

        switch (method) {
            case TAU:
                createCluster(() -> attributeCluster(Collections.singleton("tau")));
                break;
            case EDGE_ATTRIBUTE:
                createCluster(() -> attributeCluster(edgeAttributeCluster));
            case NO_CLUSTERING:
            default:
        }

        update();
    }

    private Map<NodeMesh.Node, NodeMesh.Node> attributeCluster(Set<String> attributeLabels){
        Map<NodeMesh.Node, NodeMesh.Node> leaderMap = new HashMap<>();

        for (EdgeMesh.Edge edge : edges.edgeList()) {
            if (!attributeLabels.contains(edge.label)) continue;

            NodeMesh.Node aLeader = getClusterLeader(leaderMap, edge.a);
            NodeMesh.Node bLeader = getClusterLeader(leaderMap, edge.b);

            if (edge.a != aLeader) leaderMap.put(edge.a, aLeader);
            if (edge.b != aLeader) leaderMap.put(edge.b, aLeader);
            if (bLeader != aLeader) leaderMap.put(bLeader, aLeader);
        }

        return leaderMap;
    }

    public synchronized void update(){
        clusterMapping.forEach((node, cluster) -> {
            Vector3f total = new Vector3f();
            for (NodeMesh.Node element : cluster) {
                total.add(element.position);
            }
            total.div(cluster.size());
            node.position.set(total);
        });
    }

    public synchronized NodeMesh getNodes() {
        return method == Main.ClusterMethod.NO_CLUSTERING || clusterNodes == null ? nodes : clusterNodes;
    }

    public synchronized EdgeMesh getEdges() {
        return method == Main.ClusterMethod.NO_CLUSTERING || clusterEdges == null ? edges : clusterEdges;
    }

    public void edgeAttribute(String label, boolean on) {
        if (on){
            edgeAttributeCluster.add(label);
        } else {
            edgeAttributeCluster.remove(label);
        }

        createCluster(() -> attributeCluster(edgeAttributeCluster));
    }

    public void setAttributeColor(String label, Color4f color) {
        List<EdgeMesh.Edge> edgeList = edges.edgeList();

        for (EdgeMesh.Edge edge : edgeList) {
            if (edge.label.equals(label)) {
                edge.color = color;
            }
        }

        root.executeOnRenderThread(edges::reload);
    }

    public Collection<String> getEdgeAttributes() {
        return edgeAttributeCluster;
    }
}
