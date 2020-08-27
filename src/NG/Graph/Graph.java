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

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 26-8-2020.
 */
public abstract class Graph implements ToolElement, MouseClickListener, MouseMoveListener, MouseReleaseListener {
    protected Root root;
    // the node that the mouse is holding
    private NodeMesh.Node selectedNode = null;
    private float selectedNodeZPlane = 0;

    @Override
    public void init(Root root) {
        this.root = root;
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        checkMouseClick(button, xRel, yRel);
    }

    public boolean checkMouseClick(int button, int xRel, int yRel) {
        if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT) return false;

        NodeMesh.Node candidate = getNode(xRel, yRel);
        if (candidate == null) return false;

        GLFWWindow window = root.window();
        int width = window.getWidth();
        int height = window.getHeight();
        float ratio = (float) width / height;

        Camera camera = root.camera();

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

            root.onNodePositionChange();
        }

        return true;
    }

    public NodeMesh.Node getNode(int xPixel, int yPixel) {
        GLFWWindow window = root.window();
        int width = window.getWidth();
        int height = window.getHeight();
        float ratio = (float) width / height;

        Camera camera = root.camera();

        float xvp = (2.0f * xPixel / width) - 1;
        float yvp = 1 - (2.0f * yPixel / height);
        Matrix4f invViewProjection = camera.getViewProjection(ratio).invert();
        Vector3fc cameraOrigin = new Vector3f(xvp, yvp, 1).mulPosition(invViewProjection);
        Vector3fc cameraDirection = new Vector3f(0, 0, -1).mulDirection(invViewProjection).normalize();

        NodeMesh.Node candidate = null;
        float closestIntersect = Float.NEGATIVE_INFINITY;

        for (NodeMesh.Node node : getNodeMesh().nodeList()) {
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

        return candidate;
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (selectedNode == null) {
            // highlight nodes
//            NodeMesh.Node node = getNode((int) xPos, (int) yPos);

        } else {
            // move node
            GLFWWindow window = root.window();
            float ratio = (float) window.getWidth() / window.getHeight();
            Matrix4f invViewProjection = root.camera().getViewProjection(ratio).invert();
            float x = (2 * xPos) / window.getWidth() - 1;
            float y = 1 - (2 * yPos) / window.getHeight();

            Vector3f newPosition = new Vector3f(x, y, selectedNodeZPlane).mulPosition(invViewProjection);
            selectedNode.position.set(newPosition);

            root.onNodePositionChange();
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

    public abstract PairList<EdgeMesh.Edge, NodeMesh.Node> connectionsOf(NodeMesh.Node node);

    public abstract NodeMesh getNodeMesh();

    public abstract EdgeMesh getEdgeMesh();

    public abstract Collection<String> getEdgeAttributes();

    public void setAttributeColor(String label, Color4f color) {
        EdgeMesh edges = getEdgeMesh();

        for (EdgeMesh.Edge edge : edges.edgeList()) {
            if (edge.label.equals(label)) {
                edge.color = color;
            }
        }

        root.executeOnRenderThread(edges::reload);
    }
}
