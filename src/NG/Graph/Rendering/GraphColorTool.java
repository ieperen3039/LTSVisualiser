package NG.Graph.Rendering;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.Graph.Graph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.InputHandling.MouseTools.MouseTool;
import org.lwjgl.glfw.GLFW;

import static NG.Graph.Rendering.GraphElement.Priority.USER_COLOR;

/**
 * @author Geert van Ieperen created on 10-9-2020.
 */
public class GraphColorTool extends MouseTool {
    private Color4f color;

    public GraphColorTool(Main root, Color4f initialColor) {
        super(root);
        this.color = initialColor;
    }

    @Override
    public void onNodeClick(int button, Graph graph, State node) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            node.addColor(color, USER_COLOR);

        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            node.resetColor(USER_COLOR);
        }
    }

    @Override
    public void onEdgeClick(int button, Graph graph, Transition edge) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            edge.addColor(color, USER_COLOR);

        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            edge.resetColor(USER_COLOR);
        }
    }

    public void setColor(Color4f color) {
        this.color = color;
    }

    public Color4f getColor() {
        return color;
    }
}
