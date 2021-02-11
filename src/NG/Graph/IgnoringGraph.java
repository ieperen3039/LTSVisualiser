package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Geert van Ieperen created on 9-9-2020.
 */
public class IgnoringGraph extends Graph {
    public static final Color4f IGNORED_COLOR = new Color4f(0, 0, 0, 0.02f);
    private final Graph source;
    private final Set<String> edgeActionLabels;
    private final EdgeMesh edgeMesh;

    public IgnoringGraph(Graph source, Collection<String> ignoredLabels) {
        super(source.root, "Ignoring of " + source.toString());
        this.source = source;
        this.edgeMesh = new EdgeMesh();
        this.edgeActionLabels = new HashSet<>(source.getEdgeLabels());
        this.edgeActionLabels.removeAll(ignoredLabels);

        List<Transition> edges = source.getEdgeMesh().edgeList();
        for (Transition sourceEdge : edges) {
            Transition newEdge = new Transition(sourceEdge.from, sourceEdge.to, sourceEdge.label);

            Color4f color = ignoredLabels.contains(sourceEdge.label) ? IGNORED_COLOR : sourceEdge.getColor();
            newEdge.addColor(color, GraphElement.Priority.IGNORE);
            newEdge.handlePos.set(sourceEdge.handlePos);

            edgeMesh.addParticle(newEdge);
        }
    }

    public void setIgnore(String label, boolean doIgnore) {
        if (doIgnore) {
            edgeActionLabels.remove(label);
            forActionLabel(label, e -> e.addColor(IGNORED_COLOR, GraphElement.Priority.IGNORE));
        } else {
            edgeActionLabels.add(label);
            forActionLabel(label, e -> e.resetColor(GraphElement.Priority.IGNORE));
        }
    }

    @Override
    public void cleanup() {
        root.executeOnRenderThread(edgeMesh::dispose);
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
    public Collection<String> getEdgeLabels() {
        return edgeActionLabels;
    }

    @Override
    public State getInitialState() {
        return source.getInitialState();
    }

    public void updateEdges() {
        for (Transition edge : edgeMesh.edgeList()) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
            assert !Vectors.isNaN(edge.handlePos) : edge;
        }
    }
}
