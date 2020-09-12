package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.util.*;

/**
 * @author Geert van Ieperen created on 9-9-2020.
 */
public class IgnoringGraph extends Graph {
    public static final Color4f IGNORED_COLOR = new Color4f(0, 0, 0, 0.02f);
    private final Graph source;
    private final Collection<String> edgeAttributes;
    private final EdgeMesh edgeMesh;
    private final Map<NodeMesh.Node, PairList<EdgeMesh.Edge, NodeMesh.Node>> mapping;

    public IgnoringGraph(Graph source, Collection<String> ignoredLabels) {
        super(source.root);
        this.source = source;
        this.edgeMesh = new EdgeMesh();
        this.edgeAttributes = new HashSet<>(source.getEdgeAttributes());
        this.edgeAttributes.removeAll(ignoredLabels);
        this.mapping = new HashMap<>();

        List<EdgeMesh.Edge> edges = source.getEdgeMesh().edgeList();
        for (EdgeMesh.Edge sourceEdge : edges) {
            EdgeMesh.Edge newEdge = new EdgeMesh.Edge(sourceEdge.from, sourceEdge.to, sourceEdge.label);

            Color4f color = ignoredLabels.contains(sourceEdge.label) ? IGNORED_COLOR : sourceEdge.getColor();
            newEdge.addColor(color, GraphElement.Priority.IGNORE);
            newEdge.handlePos.set(sourceEdge.handlePos);

            edgeMesh.addParticle(newEdge);

            mapping.computeIfAbsent(sourceEdge.from, s -> new PairList<>()).add(newEdge, sourceEdge.to);
            mapping.computeIfAbsent(sourceEdge.to, s -> new PairList<>()).add(newEdge, sourceEdge.from);
        }
    }

    public void update(Collection<String> ignoredLabels) {
        edgeAttributes.clear();
        edgeAttributes.addAll(source.getEdgeAttributes());
        edgeAttributes.removeAll(ignoredLabels);

        List<EdgeMesh.Edge> edges = getEdgeMesh().edgeList();
        List<EdgeMesh.Edge> sourceEdges = source.getEdgeMesh().edgeList();
        for (int i = 0, edgesSize = edges.size(); i < edgesSize; i++) {
            EdgeMesh.Edge p = edges.get(i);
            Color4f color = ignoredLabels.contains(p.label) ? IGNORED_COLOR : sourceEdges.get(i).getColor();
            p.addColor(color, GraphElement.Priority.IGNORE);
        }

        root.executeOnRenderThread(edgeMesh::reload);
    }

    @Override
    public void cleanup() {
        root.executeOnRenderThread(edgeMesh::dispose);
    }

    @Override
    public PairList<EdgeMesh.Edge, NodeMesh.Node> connectionsOf(NodeMesh.Node node) {
        return mapping.getOrDefault(node, PairList.empty());
    }

    @Override
    public NodeMesh getNodeMesh() {
        return source.getNodeMesh();
    }

    @Override
    public EdgeMesh getEdgeMesh() {
        return edgeMesh;
    }

    @Override
    public Collection<String> getEdgeAttributes() {
        return edgeAttributes;
    }

    @Override
    protected NodeMesh.Node getInitialState() {
        return source.getInitialState();
    }

    public void updateEdges() {
        for (EdgeMesh.Edge edge : edgeMesh.edgeList()) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
            assert !Vectors.isNaN(edge.handlePos) : edge;
        }
    }
}
