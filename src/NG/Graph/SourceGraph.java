package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public class SourceGraph extends Graph {
    public final NodeMesh.Node[] nodes;
    public final EdgeMesh.Edge[] edges;
    public final String[] actionLabels;
    // maps nodes to their neighbours
    public final Map<NodeMesh.Node, PairList<EdgeMesh.Edge, NodeMesh.Node>> mapping;

    private final NodeMesh nodeMesh;
    private final EdgeMesh edgeMesh;
    public int initialState = 0;

    public SourceGraph(Main root, int numStates, int numTransitions) {
        super(root);
        this.mapping = new HashMap<>();
        this.nodeMesh = new NodeMesh();
        this.edgeMesh = new EdgeMesh();

        this.nodes = new NodeMesh.Node[numStates];
        this.edges = new EdgeMesh.Edge[numTransitions];
        this.actionLabels = new String[numTransitions];
    }

    public void init() {
        if (edges.length < 1) return;

        // create position mapping
        double[][] positions = HDEPositioning.position(edges, nodes);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].position.set(
                    positions[i].length > 0 ? (float) positions[i][0] : 0,
                    positions[i].length > 1 ? (float) positions[i][1] : 0,
                    positions[i].length > 2 ? (float) positions[i][2] : 0
            );
        }

        // set positions to graph
        for (NodeMesh.Node node : nodes) {
            getNodeMesh().addParticle(node);
        }
        for (EdgeMesh.Edge edge : edges) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
            getEdgeMesh().addParticle(edge);
        }

        getInitialState().addColor(Color4f.GREEN, GraphElement.Priority.INITIAL_STATE);
    }

    @Override
    public PairList<EdgeMesh.Edge, NodeMesh.Node> connectionsOf(NodeMesh.Node node) {
        return mapping.getOrDefault(node, PairList.empty());
    }

    @Override
    public void cleanup() {
        root.executeOnRenderThread(nodeMesh::dispose);
        root.executeOnRenderThread(edgeMesh::dispose);
    }

    @Override
    public NodeMesh getNodeMesh() {
        return nodeMesh;
    }

    @Override
    public EdgeMesh getEdgeMesh() {
        return edgeMesh;
    }

    @Override
    public Collection<String> getEdgeAttributes() {
        return Arrays.asList(actionLabels);
    }

    @Override
    public NodeMesh.Node getInitialState() {
        if (nodes.length == 0) return null;
        return nodes[initialState];
    }
}
