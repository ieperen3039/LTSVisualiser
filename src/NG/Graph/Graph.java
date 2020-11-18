package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.InputHandling.MouseMoveListener;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.GLFWWindow;
import NG.Tools.Logger;
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
public abstract class Graph implements MouseMoveListener, MouseReleaseListener {
    protected transient Main root;
    // the node that the mouse is holding
    private State selectedNode = null;
    private float selectedNodeZPlane = 0;

    private Transition hoveredEdge = null;
    private State hoveredNode = null;

    public Graph(Main root) {
        this.root = root;
    }

    public boolean doOnMouseSelection(Consumer<State> nodeAction, Consumer<Transition> edgeAction) {
        int index = root.getClickShaderResult();
        if (index == -1) return false;

        List<State> nodes = getNodeMesh().nodeList();
        if (index < nodes.size()) {
            nodeAction.accept(nodes.get(index));

        } else {
            index -= nodes.size();
            List<Transition> edges = getEdgeMesh().edgeList();

            if (index > edges.size()) {
                Logger.ERROR.printf("Mouse hovered element %d which does not exist", index + nodes.size());
                return false;
            }

            edgeAction.accept(edges.get(index));
        }

        return true;
    }

    @Override
    public void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {
        if (selectedNode == null) {
            Transition oldHoveredEdge = hoveredEdge;
            State oldHoveredNode = hoveredNode;

            hoveredEdge = null;
            hoveredNode = null;
            // highlight nodes
            boolean doHover = doOnMouseSelection(
                    node -> hoveredNode = node,
                    edge -> hoveredEdge = edge
            );

            if (oldHoveredNode != null) {
                if (oldHoveredNode != hoveredNode) {
                    forNodeClass(oldHoveredNode.classIndex, n -> n.resetColor(GraphElement.Priority.HOVER));
                    getNodeMesh().scheduleReload();
                }

            } else if (oldHoveredEdge != null) {
                if (oldHoveredEdge != hoveredEdge) {
                    forAttribute(oldHoveredEdge.label, e -> e.resetColor(GraphElement.Priority.HOVER));
                    getEdgeMesh().scheduleReload();
                }
            }

            if (hoveredNode != null) {
                forNodeClass(hoveredNode.classIndex, n -> n.addColor(Main.HOVER_COLOR, GraphElement.Priority.HOVER));
                getNodeMesh().scheduleReload();

            } else if (hoveredEdge != null) {
                forAttribute(hoveredEdge.label, edge -> edge.addColor(Main.HOVER_COLOR, GraphElement.Priority.HOVER));
                getEdgeMesh().scheduleReload();
            }

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

    public void setNodePosition(State node, Vector3f newPosition) {
        node.position.set(newPosition);
    }

    @Override
    public void onRelease(int button) {
        if (selectedNode != null) {
            if (!selectedNode.stayFixed) {
                selectedNode.isFixed = false;
            }

            selectedNode = null;
        }
    }

    public abstract PairList<Transition, State> incomingOf(State node);

    public abstract PairList<Transition, State> outgoingOf(State node);

    public PairList<Transition, State> connectionsOf(State node) {
        PairList<Transition, State> pairs = new PairList<>();
        pairs.addAll(incomingOf(node));
        pairs.addAll(outgoingOf(node));
        return pairs;
    }

    public abstract NodeMesh getNodeMesh();

    public int getNrOfNodes() {
        return getNodeMesh().nodeList().size();
    }

    public abstract EdgeMesh getEdgeMesh();

    public int getNrOfEdges() {
        return getEdgeMesh().edgeList().size();
    }

    public abstract Collection<String> getEdgeAttributes();

    public void forAttribute(String label, Consumer<Transition> action) {
        EdgeMesh edges = getEdgeMesh();

        for (Transition edge : edges.edgeList()) {
            if (edge.label.equals(label)) {
                action.accept(edge);
            }
        }
    }

    public void forNodeClass(int classIndex, Consumer<State> action) {
        NodeMesh nodes = getNodeMesh();

        for (State node : nodes.nodeList()) {
            if (node.classIndex == classIndex) {
                action.accept(node);
            }
        }
    }

    public GraphElement getHovered() {
        return hoveredNode != null ? hoveredNode : hoveredEdge;
    }

    protected abstract State getInitialState();

    public abstract void cleanup();

    public void resetColors(GraphElement.Priority path) {
        for (State node : getNodeMesh().nodeList()) {
            node.resetColor(path);
        }
        for (Transition edge : getEdgeMesh().edgeList()) {
            edge.resetColor(path);
        }
    }

    public static class ManipulationTool extends MouseTool {
        public ManipulationTool(Main root) {
            super(root);
        }

        @Override
        public void onNodeClick(int button, Graph graph, State node) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                graph.selectedNode = node;
                graph.selectedNodeZPlane = new Vector3f(node.position)
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
        }

        @Override
        public void onEdgeClick(int button, Graph graph, Transition edge) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                root.toggleClusterAttribute(edge.label);
            }
        }

    }
}
