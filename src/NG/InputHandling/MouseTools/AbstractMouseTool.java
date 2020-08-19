package NG.InputHandling.MouseTools;

import NG.Core.Root;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import NG.Rendering.MatrixStack.SGL;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import static NG.InputHandling.MouseTools.AbstractMouseTool.MouseAction.*;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class AbstractMouseTool implements MouseTool {
    protected Root root;

    private MouseReleaseListener releaseListener;
    private MouseAction mouseAction = HOVER;

    public enum MouseAction {
        PRESS_ACTIVATE, PRESS_DEACTIVATE, DRAG_ACTIVATE, DRAG_DEACTIVATE, HOVER
    }

    public AbstractMouseTool(Root root) {
        this.root = root;
        releaseListener = root.camera();
    }

    protected MouseAction getMouseAction() {
        return mouseAction;
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (mouseAction != HOVER) return;

        // TODO keybindings
        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                if (root.inputHandling().getMouseTool() != root.inputHandling().getDefaultMouseTool()) {
                    root.inputHandling().setMouseTool(null);
                    return;
                }
            case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                mouseAction = PRESS_ACTIVATE;
                break;
        }

        if (root.gui().checkMouseClick(button, x, y)) {
            releaseListener = root.gui();
            return;
        }

        root.camera().onClick(button, x, y);
        releaseListener = root.camera();
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        mouseAction = HOVER;

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
        if (mouseAction == PRESS_ACTIVATE) mouseAction = DRAG_ACTIVATE;
        if (mouseAction == PRESS_DEACTIVATE) mouseAction = DRAG_DEACTIVATE;

        root.gui().mouseMoved(xDelta, yDelta, xPos, yPos);
        root.camera().mouseMoved(xDelta, yDelta, xPos, yPos);
    }

    @Override
    public void draw(SGL gl) {
        // TODO fancy cursor?
    }

}
