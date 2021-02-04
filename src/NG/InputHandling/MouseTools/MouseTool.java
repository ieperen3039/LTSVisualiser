package NG.InputHandling.MouseTools;

import NG.Camera.Camera;
import NG.Core.Main;
import NG.GUIMenu.Components.SToggleButton;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.SComponentProperties;
import NG.Graph.Graph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.InputHandling.MouseListener;
import NG.InputHandling.MouseReleaseListener;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class MouseTool implements MouseListener {
    protected Main root;

    private MouseReleaseListener releaseListener;
    private Runnable onCancel = null;

    public MouseTool(Main root) {
        this.root = root;
        releaseListener = root.camera();
    }

    public SToggleButton button(String buttonText, SComponentProperties properties) {
        SToggleButton button = new SToggleButton(buttonText, properties);
        button.addStateChangeListener(on -> root.inputHandling().setMouseTool(on ? this : null));
        onCancel = () -> button.setActive(false);
        return button;
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (root.gui().checkMouseClick(button, x, y)) {
            releaseListener = root.gui();
            return;
        }

        Graph graph = root.getVisibleGraph();
        boolean didClickGraph = root.doOnMouseSelection(
                node -> onNodeClick(button, graph, node),
                edge -> onEdgeClick(button, graph, edge)
        );

        if (didClickGraph) {
            releaseListener = root.getVisibleGraph();

        } else {
            onAirClick(button, x, y);
        }
    }

    protected void onAirClick(int button, int x, int y) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && onCancel != null) {
            disableThis();
            return;
        }

        Camera camera = root.camera();
        camera.onClick(button, x, y);
        releaseListener = camera;
    }

    public abstract void onNodeClick(int button, Graph graph, State node);

    public void disableThis() {
        if (onCancel == null) {
            root.inputHandling().setMouseTool(null);

        } else {
            onCancel.run();
        }
    }

    public abstract void onEdgeClick(int button, Graph graph, Transition edge);

    @Override
    public void onRelease(int button) {
        // this prevents the case when a mouse-down caused a mouse tool switch
        if (releaseListener != null) {
            releaseListener.onRelease(button);
            releaseListener = null;
        }
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = root.window().getMousePosition();
        UIFrameManager gui = root.gui();

        if (gui.covers(pos.x, pos.y)) {
            gui.onScroll(value);
            return;
        }

        // camera
        root.camera().onScroll(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public final void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {
        root.gui().onMouseMove(xDelta, yDelta, xPos, yPos);
        if (root.gui().covers((int) xPos, (int) yPos)) return;

        root.camera().onMouseMove(xDelta, yDelta, xPos, yPos);
        root.getVisibleGraph().onMouseMove(xDelta, yDelta, xPos, yPos);
    }

    /**
     * activates when this mousetool is deactivated
     */
    public void dispose() {
    }
}
