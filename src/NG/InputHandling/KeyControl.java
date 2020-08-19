package NG.InputHandling;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author Geert van Ieperen created on 27-4-2020.
 */
public class KeyControl implements KeyPressListener, KeyReleaseListener {
    private boolean isShiftPressed;
    private boolean isControlPressed;
    private boolean isAltPressed;

    @Override
    public void keyPressed(int keyCode) {
        setKey(keyCode, true);
    }

    @Override
    public void keyReleased(int keyCode) {
        setKey(keyCode, false);
    }

    private void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case GLFW_KEY_LEFT_SHIFT:
            case GLFW_KEY_RIGHT_SHIFT:
                isShiftPressed = pressed;
            case GLFW_KEY_LEFT_CONTROL:
            case GLFW_KEY_RIGHT_CONTROL:
                isControlPressed = pressed;
            case GLFW_KEY_LEFT_ALT:
            case GLFW_KEY_RIGHT_ALT:
                isAltPressed = pressed;
        }
    }

    public boolean isShiftPressed() {
        return isShiftPressed;
    }

    public boolean isControlPressed() {
        return isControlPressed;
    }

    public boolean isAltPressed() {
        return isAltPressed;
    }
}
