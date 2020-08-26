package NG.Graph;

import NG.Camera.Camera;
import NG.Core.Root;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Graph.Rendering.NodeShader;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Rendering.GLFWWindow;
import org.joml.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    public final Map<NodeMesh.Node, PairList<EdgeMesh.Edge, NodeMesh.Node>> mapping;

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
            edge.handle.set(edge.aPosition).lerp(edge.bPosition, 0.5f);
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
        Matrix4f invViewProjection = camera.getViewProjection(ratio).invert();

        float xvp = (2.0f * xRel / width) - 1;
        float yvp = 1 - (2.0f * yRel / height);
        Vector3fc cameraOrigin = new Vector3f(xvp, yvp, 1).mulPosition(invViewProjection);
        Vector3fc cameraDirection = new Vector3f(0, 0, -1).mulDirection(invViewProjection).normalize();

        NodeMesh.Node candidate = null;
        float closestIntersect = Float.NEGATIVE_INFINITY;

        for (NodeMesh.Node node : nodes) {
            Vector2f result = new Vector2f();
            boolean doIntersect = Intersectionf.intersectRaySphere(
                    cameraOrigin, cameraDirection,
                    node.position, NodeShader.NODE_RADIUS * NodeShader.NODE_RADIUS,
                    result
            );
            float intersect = (result.x + result.y) / 2; // results are assuming an orb, we assume a perpendicular disc

            if (doIntersect && intersect > closestIntersect) {
                candidate = node;
                closestIntersect = intersect;
            }
        }
        if (candidate == null) return false;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            selectedNode = candidate;
            candidate.isFixed = true;
            // calculate z-coordinate in view plane
            selectedNodeZPlane = new Vector3f(candidate.position)
                    .mulPosition(camera.getViewProjection(ratio)).z;

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

            root.updateMeshes();
        }

        return true;
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (selectedNode == null) {
            // highlight nodes


        } else {
            // move node
            GLFWWindow window = root.window();
            float ratio = (float) window.getWidth() / window.getHeight();
            Matrix4f invViewProjection = root.camera().getViewProjection(ratio).invert();
            float x = (2 * xPos) / window.getWidth() - 1;
            float y = 1 - (2 * yPos) / window.getHeight();

            Vector3f newPosition = new Vector3f(x, y, selectedNodeZPlane).mulPosition(invViewProjection);
            selectedNode.position.set(newPosition);

            root.updateMeshes();
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

    public Collection<String> getEdgeAttributes() {
        return List.of(actionLabels);
    }
}
