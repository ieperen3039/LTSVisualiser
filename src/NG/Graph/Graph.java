package NG.Graph;

import NG.Core.Root;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Rendering.GLFWWindow;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static NG.Graph.Rendering.NodeShader.NODE_RADIUS;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public class Graph implements ToolElement, MouseClickListener, MouseMoveListener, MouseReleaseListener {
    private static final Pattern dash = Pattern.compile("-");
    private static final Pattern separator = Pattern.compile(";");

    public final NodeMesh.Node[] nodes;
    public final EdgeMesh.Edge[] edges;
    public final String[] actionLabels;

    // initialized at init()
    private Root root;
    public final NodeMesh nodeMesh;
    public final EdgeMesh edgeMesh;
    public final Map<NodeMesh.Node, Collection<NodeMesh.Node>> mapping;

    // the node that the mouse is holding
    private NodeMesh.Node selectedNode = null;
    private float selectedNodeZPlane = 0;

    private Graph(int numStates, int numTransitions, String[] actionLabels) {
        this.mapping = new HashMap<>();
        this.nodeMesh = new NodeMesh();
        this.edgeMesh = new EdgeMesh();

        this.actionLabels = actionLabels;
        this.nodes = new NodeMesh.Node[numStates];
        this.edges = new EdgeMesh.Edge[numTransitions];
    }

    public void init(Root root) {
        this.root = root;

        for (EdgeMesh.Edge edge : edges) {
            // create mapping
            mapping.computeIfAbsent(edge.a, node -> new ArrayList<>(0))
                    .add(edge.b);
            // make sure deadlocks are part of the mapping keys
            mapping.computeIfAbsent(edge.b, node -> new ArrayList<>(0));
        }

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
            nodeMesh.addParticle(node);
        }
        for (EdgeMesh.Edge edge : edges) {
            edgeMesh.addParticle(edge);
        }
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        GLFWWindow window = root.window();
        float ratio = (float) window.getWidth() / window.getHeight();
        Matrix4f viewProjection = root.camera().getViewProjection(ratio);

        NodeMesh.Node candidate = null;
        float lowestZ = 1; // z values higher than this are not visible

        for (NodeMesh.Node node : nodes) {
            Vector3f scrPos = new Vector3f(node.position).mulPosition(viewProjection);
            if (scrPos.x - NODE_RADIUS < xRel &&
                    scrPos.x + NODE_RADIUS > xRel &&
                    scrPos.y - NODE_RADIUS < yRel &&
                    scrPos.y + NODE_RADIUS > yRel &&
                    scrPos.z > -1 &&
                    scrPos.z < lowestZ
            ) {
                candidate = node;
                lowestZ = scrPos.z;
            }
        }

        selectedNode = candidate;
        selectedNodeZPlane = lowestZ;
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (selectedNode != null) {
            GLFWWindow window = root.window();
            float ratio = (float) window.getWidth() / window.getHeight();
            Matrix4f invViewProjection = root.camera().getViewProjection(ratio).invert();
            float x = (2 * xPos) / window.getWidth() - 1;
            float y = 1 - (2 * yPos) / window.getHeight();

            Vector3f newPosition = new Vector3f(x, y, selectedNodeZPlane).mulPosition(invViewProjection);
            selectedNode.position.set(newPosition);
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        selectedNode = null;
    }

    @Override
    public void cleanup() {
//        nodeMesh.dispose();
//        edgeMesh.dispose();
        mapping.clear();
    }

    public static Graph readPlainString(String data) {
        String[] lines = data.split("\n");

        String[] stateLabels = separator.split(lines[0]);
        String[] actionLabels = separator.split(lines[1]);
        String[] transitions = separator.split(lines[2]);
        Graph graph = new Graph(stateLabels.length, transitions.length, actionLabels);

        for (int i = 0; i < stateLabels.length; i++) {
            graph.nodes[i] = new NodeMesh.Node(Vectors.O, stateLabels[i]);
        }

        graph.nodes[0].color = Color4f.GREEN;

        for (int i = 0; i < transitions.length; i++) {
            String elt = transitions[i];
            String[] elements = dash.split(elt);
            int a = Integer.parseInt(elements[0]);
            int b = Integer.parseInt(elements[1]);
            int labelInd = Integer.parseInt(elements[2]);

            NodeMesh.Node aNode = graph.nodes[a];
            NodeMesh.Node bNode = graph.nodes[b];
            String label = actionLabels[labelInd];

            graph.edges[i] = new EdgeMesh.Edge(aNode, bNode, label);
        }

        return graph;
    }
}
