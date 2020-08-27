package NG.Graph;

import NG.Core.Root;
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
    private Graph sourceGraph;
    private NodeMesh clusterNodes = null;
    private EdgeMesh clusterEdges = null;

    @Override
    public void init(Root root) {
        super.init(root);
        this.sourceGraph = root.graph();

        createCluster(Collections.emptyMap());
    }

    public synchronized void createCluster(Map<NodeMesh.Node, NodeMesh.Node> clusterLeaderMap) {
        clusterMapping.clear();
        neighbourMapping.clear();

        NodeMesh nodes = sourceGraph.getNodeMesh();
        EdgeMesh edges = sourceGraph.getEdgeMesh();

        // maps each node to a 'leader' node where all nodes in one cluster refer to

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
                PairList<EdgeMesh.Edge, NodeMesh.Node> neighbours = sourceGraph.connectionsOf(element);
                for (int i = 0; i < neighbours.size(); i++) {
                    NodeMesh.Node other = neighbours.right(i);
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
        NodeMesh oldNodes = this.clusterNodes;
        EdgeMesh oldEdges = this.clusterEdges;
        if (oldNodes != null && oldEdges != null) {
            root.executeOnRenderThread(() -> {
                oldNodes.dispose();
                oldEdges.dispose();
            });
        }

        // create new cluster nodes
        clusterNodes = new NodeMesh();
        clusterEdges = new EdgeMesh();

        // set positions to graph
        for (NodeMesh.Node node : newNodes.values()) {
            clusterNodes.addParticle(node);
            PairList<EdgeMesh.Edge, NodeMesh.Node> neighbours = new PairList<>();

            for (NodeMesh.Node other : clusterEdgeMap.get(node)) {
                assert other != null;
                EdgeMesh.Edge edge = new EdgeMesh.Edge(node, other, "");
                edge.handle.set(node.position).lerp(other.position, 0.5f);

                clusterEdges.addParticle(edge);
                neighbours.add(edge, other);
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
            edge.handle.set(edge.aPosition).lerp(edge.bPosition, 0.5f);
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
            edge.handle.set(edge.aPosition).lerp(edge.bPosition, 0.5f);
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
        return neighbourMapping.get(node);
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
        if (oldNodes != null && oldEdges != null) {
            root.executeOnRenderThread(() -> {
                oldNodes.dispose();
                oldEdges.dispose();
            });
        }
    }
}
