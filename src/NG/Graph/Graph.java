package NG.Graph;

import NG.Camera.Camera;
import NG.Core.Root;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Graph.Rendering.NodeShader;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Rendering.GLFWWindow;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public class Graph implements ToolElement, MouseClickListener, MouseMoveListener, MouseReleaseListener {
    public final NodeMesh.Node[] nodes;
    public final EdgeMesh.Edge[] edges;
    public final String[] actionLabels;
    // maps nodes to their neighbours
    public final Map<NodeMesh.Node, Collection<NodeMesh.Node>> mapping;

    // initialized at init()
    private Root root;
    private final NodeMesh nodeMesh;
    private final EdgeMesh edgeMesh;
    public int initialState = 0;

    // the node that the mouse is holding
    private NodeMesh.Node selectedNode = null;
    private float selectedNodeZPlane = 0;

    public Graph(int numStates, int numTransitions) {
        this.mapping = new HashMap<>();
        this.nodeMesh = new NodeMesh();
        this.edgeMesh = new EdgeMesh();

        this.nodes = new NodeMesh.Node[numStates];
        this.edges = new EdgeMesh.Edge[numTransitions];
        this.actionLabels = new String[numTransitions];
    }

    public void init(Root root) {
        this.root = root;
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
            getEdgeMesh().addParticle(edge);
        }
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        checkMouseClick(button, xRel, yRel);
    }

    public boolean checkMouseClick(int button, int xRel, int yRel) {
        if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT) return false;

        GLFWWindow window = root.window();
        int width = window.getWidth();
        int height = window.getHeight();
        float ratio = (float) width / height;
        Camera camera = root.camera();
        Matrix4f viewProjection = camera.getViewProjection(ratio);

        assert camera.isIsometric() : "Click detection doesnt work in perspective mode";

        NodeMesh.Node candidate = null;
        float lowestZ = 1; // z values higher than this are not visible

        Vector3fc right = new Vector3f(camera.vectorToFocus())
                .cross(camera.getUpVector())
                .normalize(NodeShader.NODE_RADIUS);

        float xPix = (2.0f * xRel / width) - 1;
        float yPix = 1 - (2.0f * yRel / height);

        for (NodeMesh.Node node : nodes) {
            Vector3f scrPos = new Vector3f(node.position).mulPosition(viewProjection);
            float scSize = new Vector3f(right).mulDirection(viewProjection).length();

            if (scrPos.x < xPix + scSize && scrPos.x > xPix - scSize &&
                    scrPos.y < yPix + scSize && scrPos.y > yPix - scSize &&
                    scrPos.z > -1 && scrPos.z < lowestZ
            ) {
                candidate = node;
                lowestZ = scrPos.z;
            }
        }
        if (candidate == null) return false;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            selectedNode = candidate;
            selectedNodeZPlane = lowestZ;
            candidate.isFixed = true;

        } else { // button == GLFW_MOUSE_BUTTON_RIGHT
            if (!candidate.stayFixed) {
                candidate.isFixed = true;
                candidate.stayFixed = true;
                candidate.color = Color4f.GREY;

            } else {
                candidate.isFixed = false;
                candidate.stayFixed = false;
                candidate.resetColor();
            }
        }

        return true;
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
        if (selectedNode != null) {
            if (!selectedNode.stayFixed) {
                selectedNode.isFixed = false;
            }

            selectedNode = null;
        }
    }

    @Override
    public void cleanup() {
        root.executeOnRenderThread(() -> {
            nodeMesh.dispose();
            edgeMesh.dispose();
        });
        mapping.clear();
    }

    public NodeMesh getNodeMesh() {
        return nodeMesh;
    }

    public EdgeMesh getEdgeMesh() {
        return edgeMesh;
    }
}
