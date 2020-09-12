package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.InputHandling.MouseTools.MouseTool;
import org.lwjgl.glfw.GLFW;

import static NG.Graph.GraphElement.Priority.USER_COLOR;

/**
 * @author Geert van Ieperen created on 10-9-2020.
 */
public class GraphColorTool extends MouseTool {
    private Color4f color;
    private Runnable onLeftClick;

    public GraphColorTool(Main root, Runnable onLeftClick, Color4f initialColor) {
        super(root);
        this.onLeftClick = onLeftClick;
        this.color = initialColor;
    }

    @Override
    public void onNodeClick(int button, Graph graph, NodeMesh.Node node) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            node.addColor(color, USER_COLOR);

        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            node.resetColor(USER_COLOR);
        }
    }

    @Override
    public void onEdgeClick(int button, Graph graph, EdgeMesh.Edge edge) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            edge.addColor(color, USER_COLOR);

        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            edge.resetColor(USER_COLOR);
        }
    }

    @Override
    protected void onAirClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            onLeftClick.run();
        } else {
            super.onAirClick(button, x, y);
        }
    }

    public void setColor(Color4f color) {
        this.color = color;
    }
}
