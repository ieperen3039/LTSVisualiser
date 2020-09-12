package NG.InputHandling.MouseTools;

import NG.Core.Main;
import NG.Core.ToolElement;
import NG.Graph.Graph;
import NG.InputHandling.KeyControl;
import NG.Rendering.GLFWWindow;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.lwjgl.glfw.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * A callback handler specialized on a tycoon-game
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class MouseToolCallbacks implements ToolElement {
    private final ExecutorService taskScheduler = Executors.newSingleThreadExecutor();
    private final KeyControl keyControl = new KeyControl();
    private MouseTool DEFAULT_MOUSE_TOOL;
    private Main root;
    private MouseTool currentTool;

    @Override
    public void init(Main root) {
        if (this.root != null) return;
        this.root = root;

        DEFAULT_MOUSE_TOOL = new Graph.ManipulationTool(root);
        currentTool = DEFAULT_MOUSE_TOOL;

        GLFWWindow target = root.window();
        Vector2i mousePosition = target.getMousePosition();
        target.setCallbacks(new KeyPressCallback(), new MouseButtonPressCallback(), new MouseMoveCallback(mousePosition), new MouseScrollCallback());
        target.setTextCallback(new CharTypeCallback());
    }

    @Override
    public void cleanup() {
        taskScheduler.shutdown();
    }

    public MouseTool getMouseTool() {
        return currentTool;
    }

    public void setMouseTool(MouseTool tool) {
        MouseTool newTool = (tool != null) ? tool : DEFAULT_MOUSE_TOOL;

        if (!currentTool.equals(newTool)) {
            if (currentTool != DEFAULT_MOUSE_TOOL) currentTool.dispose();
            currentTool = newTool;
            Logger.DEBUG.print("Set mousetool to " + newTool);
        }
    }

    public MouseTool getDefaultMouseTool() {
        return DEFAULT_MOUSE_TOOL;
    }

    public KeyControl getKeyControl() {
        return keyControl;
    }

    private void execute(Runnable action) {
        taskScheduler.submit(() -> {
            try {
                action.run();

            } catch (Throwable ex) {
                // Caught an error while executing an input handler.
                // Look at the second element of the stack trace
                Logger.ERROR.print(ex);
            }
        });
    }

    private class KeyPressCallback extends GLFWKeyCallback {
        @Override
        public void invoke(long window, int keyCode, int scanCode, int action, int mods) {
            if (keyCode < 0) return;
            if (action == GLFW_PRESS) {
                execute(() -> keyControl.keyPressed(keyCode));

            } else if (action == GLFW_RELEASE) {
                execute(() -> keyControl.keyReleased(keyCode));
            }
        }
    }

    private class MouseButtonPressCallback extends GLFWMouseButtonCallback {
        @Override
        public void invoke(long windowHandle, int button, int action, int mods) {
            Vector2i pos = root.window().getMousePosition();

            if (action == GLFW_PRESS) {
                execute(() -> currentTool.onClick(button, pos.x, pos.y));

            } else if (action == GLFW_RELEASE) {
                execute(() -> currentTool.onRelease(button, pos.x, pos.y));
            }
        }
    }

    private class MouseMoveCallback extends GLFWCursorPosCallback {
        // position when adding all integer move calls
        private int xGiven;
        private int yGiven;

        MouseMoveCallback(Vector2i mousePos) {
            xGiven = mousePos.x;
            yGiven = mousePos.y;
        }

        @Override
        public void invoke(long window, double xpos, double ypos) {
            int xDiff = (int) (xpos - xGiven);
            int yDiff = (int) (ypos - yGiven);
            float xFloat = (float) xpos;
            float yFloat = (float) ypos;

            if (xDiff != 0 || yDiff != 0) {
                xGiven += xDiff;
                yGiven += yDiff;
            }
            execute(() -> currentTool.mouseMoved(xDiff, yDiff, xFloat, yFloat));
        }
    }

    private class MouseScrollCallback extends GLFWScrollCallback {
        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            execute(() -> currentTool.onScroll((float) yoffset));
        }
    }

    private class CharTypeCallback extends GLFWCharCallback {
        @Override
        public void invoke(long window, int codepoint) {
            if (Character.isAlphabetic(codepoint)) {
                char s = Character.toChars(codepoint)[0];

                execute(() -> root.gui().keyTyped(s));
            }
        }
    }
}
