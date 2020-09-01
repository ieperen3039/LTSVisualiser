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
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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

        GLFWWindow window = root.window();
        int width = window.getWidth();
        int height = window.getHeight();
        float ratio = (float) width / height;
        Camera camera = root.camera();
        Matrix4f projectionMatrix = camera.getProjectionMatrix(ratio);
        Matrix4f viewMatrix = new Matrix4f().setLookAt(
                camera.getEye(),
                camera.getFocus(),
                camera.getUpVector()
        );

        NodeMesh.Node candidate = null;
        float lowestZ = 1; // z values higher than this are not visible

        Vector3fc right = new Vector3f(camera.vectorToFocus())
                .cross(camera.getUpVector())
                .normalize(NodeShader.NODE_RADIUS);

        float xvp = (2.0f * xRel / width) - 1;
        float yvp = 1 - (2.0f * yRel / height);

        for (NodeMesh.Node node : getNodeMesh().nodeList()) {
            Vector3f viewPos = new Vector3f(node.position).mulPosition(viewMatrix); // cache viewPos
            Vector3f scrPos = new Vector3f(viewPos).mulPosition(projectionMatrix);

            if (scrPos.z > -1 && scrPos.z < lowestZ) {
                Vector3f scrSide = viewPos.add(right).mulPosition(projectionMatrix);
                // radius
                float dxr = scrPos.x - scrSide.x;
                float dyr = scrPos.y - scrSide.y;
                float radius = Math.sqrt(dxr * dxr + dyr * dyr);
                // mouse distance
                float dxp = scrPos.x - xvp;
                float dyp = scrPos.y - yvp;
                float mouseDistance = Math.sqrt(dxp * dxp + dyp * dyp);

                if (mouseDistance < radius) {
                    candidate = node;
                    lowestZ = scrPos.z;
                }
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

            root.onNodePositionChange();
        }

        return true;
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
            Matrix4f invViewProjection = root.camera().getViewProjection(window).invert();
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
