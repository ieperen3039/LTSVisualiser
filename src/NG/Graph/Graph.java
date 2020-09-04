package NG.Graph;

import NG.Core.Main;
import NG.Core.ToolElement;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Rendering.GLFWWindow;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 26-8-2020.
 */
public abstract class Graph implements ToolElement, MouseClickListener, MouseMoveListener, MouseReleaseListener {
    public static final Color4f BLUISH = Color4f.rgb(44, 58, 190);
    protected Main root;
    // the node that the mouse is holding
    private NodeMesh.Node selectedNode = null;
    private float selectedNodeZPlane = 0;

    private EdgeMesh.Edge hoveredEdge = null;
    private NodeMesh.Node hoveredNode = null;

    @Override
    public void init(Main root) {
        this.root = root;
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        checkMouseClick(button);
    }

    public boolean checkMouseClick(int button) {
        return doOnMouseSelection(
                node -> {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        selectedNode = node;
                        selectedNodeZPlane = new Vector3f(node.position)
                                .mulPosition(root.camera().getViewProjection(root.window())).z;
                        node.isFixed = true;

                    } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                        if (!node.stayFixed) {
                            node.isFixed = true;
                            node.stayFixed = true;
                            node.addColor(Color4f.GREY, GraphElement.Priority.FIXATE_POSITION);

                        } else {
                            node.isFixed = false;
                            node.stayFixed = false;
                            node.resetColor(GraphElement.Priority.FIXATE_POSITION);
                        }

                        root.onNodePositionChange();
                    }
                },
                edge -> {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        root.toggleClusterAttribute(edge.label);
                    }
                }
        );
    }

    public boolean doOnMouseSelection(Consumer<NodeMesh.Node> nodeAction, Consumer<EdgeMesh.Edge> edgeAction) {
        int index = root.getClickShaderResult();
        if (index == -1) return false;

        List<NodeMesh.Node> nodes = getNodeMesh().nodeList();
        if (index < nodes.size()) {
            nodeAction.accept(nodes.get(index));

        } else {
            index -= nodes.size();
            List<EdgeMesh.Edge> edges = getEdgeMesh().edgeList();
            edgeAction.accept(edges.get(index));
        }

        return true;
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (selectedNode == null) {
            EdgeMesh.Edge oldHoveredEdge = hoveredEdge;
            NodeMesh.Node oldHoveredNode = hoveredNode;
            boolean reloadNodes = false;

            hoveredEdge = null;
            hoveredNode = null;
            // highlight nodes
            boolean doHover = doOnMouseSelection(
                    node -> hoveredNode = node,
                    edge -> hoveredEdge = edge
            );

            if (oldHoveredNode != null) {
                if (oldHoveredNode != hoveredNode) {
                    oldHoveredNode.resetColor(GraphElement.Priority.HOVER);
                    reloadNodes = true;
                }

            } else if (oldHoveredEdge != null) {
                if (oldHoveredEdge != hoveredEdge) {
                    forEachAttribute(oldHoveredEdge.label, e -> e.resetColor(GraphElement.Priority.HOVER));
                }
            }

            if (hoveredNode != null) {
                hoveredNode.addColor(BLUISH, GraphElement.Priority.HOVER);
                reloadNodes = true;

            } else if (hoveredEdge != null) {
                setAttributeColor(hoveredEdge.label, BLUISH, GraphElement.Priority.HOVER);
            }

            if (reloadNodes) root.executeOnRenderThread(getNodeMesh()::reload);

        } else {
            // move node
            GLFWWindow window = root.window();
            Matrix4f invViewProjection = root.camera().getViewProjection(window).invert();
            float x = (2 * xPos) / window.getWidth() - 1;
            float y = 1 - (2 * yPos) / window.getHeight();

            Vector3f newPosition = new Vector3f(x, y, selectedNodeZPlane).mulPosition(invViewProjection);
            setNodePosition(selectedNode, newPosition);

            root.onNodePositionChange();
        }
    }

    public void setNodePosition(NodeMesh.Node node, Vector3f newPosition) {
        node.position.set(newPosition);
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

    public int getNrOfNodes() {
        return getNodeMesh().nodeList().size();
    }

    public abstract EdgeMesh getEdgeMesh();

    public int getNrOfEdges() {
        return getEdgeMesh().edgeList().size();
    }

    public abstract Collection<String> getEdgeAttributes();

    public void setAttributeColor(String label, Color4f color, GraphElement.Priority priority) {
        forEachAttribute(label, edge -> edge.addColor(color, priority));
    }

    public void forEachAttribute(String label, Consumer<EdgeMesh.Edge> action) {
        EdgeMesh edges = getEdgeMesh();

        for (EdgeMesh.Edge edge : edges.edgeList()) {
            if (edge.label.equals(label)) {
                action.accept(edge);
            }
        }

        root.executeOnRenderThread(edges::reload);
    }

    public GraphElement getHovered() {
        return hoveredNode != null ? hoveredNode : hoveredEdge;
    }

    protected NodeMesh.Node getInitialState() {
        return getNodeMesh().nodeList().get(0);
    }
}
