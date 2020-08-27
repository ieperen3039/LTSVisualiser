package NG.InputHandling.MouseTools;

import NG.Camera.Camera;
import NG.Core.Root;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.Graph.Graph;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class AbstractMouseTool implements MouseTool {
    protected Root root;

    private MouseReleaseListener releaseListener;

    public enum MouseAction {
        PRESS_ACTIVATE, PRESS_DEACTIVATE, DRAG_ACTIVATE, DRAG_DEACTIVATE, HOVER
    }

    public AbstractMouseTool(Root root) {
        this.root = root;
        releaseListener = root.camera();
    }

    @Override
    public void onClick(int button, int x, int y) {
        // TODO keybindings
        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                MouseToolCallbacks callbacks = root.inputHandling();
                if (callbacks.getMouseTool() != callbacks.getDefaultMouseTool()) {
                    callbacks.setMouseTool(null);
                    return;
                }
            case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                break;
        }

        if (root.gui().checkMouseClick(button, x, y)) {
            releaseListener = root.gui();
            return;
        }

        Graph graph = root.getVisibleGraph();
        boolean didClickGraph = graph.checkMouseClick(button, x, y);

        if (didClickGraph) {
            releaseListener = graph;

        } else {
            Camera camera = root.camera();
            camera.onClick(button, x, y);
            releaseListener = camera;
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {

        // this is the case when a mouse-down caused a mouse tool switch
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
            releaseListener = null;
        }
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = root.window().getMousePosition();
        UIFrameManager gui = root.gui();

        SComponent component = gui.getComponentAt(pos.x, pos.y);

        if (component != null) {
            if (component instanceof MouseScrollListener) {
                MouseScrollListener listener = (MouseScrollListener) component;
                listener.onScroll(value);
            }

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
    public final void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        root.gui().mouseMoved(xDelta, yDelta, xPos, yPos);
        root.camera().mouseMoved(xDelta, yDelta, xPos, yPos);
        root.graph().mouseMoved(xDelta, yDelta, xPos, yPos);
    }

    @Override
    public void draw(SGL gl) {
        // TODO fancy cursor?
    }

}
