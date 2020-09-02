package NG.Camera;

import NG.Core.Main;
import NG.Rendering.GLFWWindow;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * A camera implementation that can be moved by holding the mouse to the corners of the screen
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public class MovableCamera implements Camera {
    public static final float DRAG_MODIFIER = 0.0015f;
    private static final float BORDER_MOVE_SPEED = 0.3f;
    private static final int SCREEN_MOVE_BORDER_SIZE = 50;
    private static final int SCREEN_DEAD_ZONE = 30;
    private static final float ZOOM_SPEED_LIMIT = 0.05f;
    private static final float ROTATION_MODIFIER = 0.002f;
    private final Vector3f focus = new Vector3f();
    private final Vector3f eyeOffset;

    private Main root;
    private float mouseXPos;
    private float mouseYPos;
    private boolean isBeingRotated = false;
    private boolean isBeginMoved = false;

    /**
     * a camera that always has the same angle to the ground. The angle can be set by the ratio between eyeOffset and
     * eyeHeight.
     * @param initialFocus the position of the camera focus
     * @param dist
     */
    public MovableCamera(Vector3fc initialFocus, float dist) {
        this.eyeOffset = new Vector3f(dist, dist, dist);
        focus.set(initialFocus);
    }

    @Override
    public void init(Main root) {
        this.root = root;

        Vector2i mousePosition = root.window().getMousePosition();
        mouseXPos = mousePosition.x;
        mouseYPos = mousePosition.y;
    }

    @Override
    public Vector3fc vectorToFocus() {
        return new Vector3f(eyeOffset).negate();
    }

    @Override
    public void updatePosition(float deltaTime) {
        // prevents side-of-window movement when rotating or dragging
        if (isBeingRotated || isBeginMoved) return;
        // prevent overshooting when camera is not updated.
        deltaTime = Math.min(deltaTime, 0.5f);

        Vector3f eyeDir = new Vector3f(eyeOffset);
        GLFWWindow window = root.window();
        int width = window.getWidth();
        int height = window.getHeight();

        // correction for toolbar
        int corrMouseYPos = (int) mouseYPos;
        int corrMouseXPos = (int) mouseXPos;

        // x movement
        if (corrMouseXPos < SCREEN_MOVE_BORDER_SIZE) {
            float value = positionToMovement(corrMouseXPos) * deltaTime;
            eyeDir.normalize(value).cross(getUpVector());
            focus.add(eyeDir.x, eyeDir.y, 0);

        } else {
            int xInv = width - corrMouseXPos;
            if (xInv < SCREEN_MOVE_BORDER_SIZE) {
                float value = positionToMovement(xInv) * deltaTime;
                eyeDir.normalize(value).cross(getUpVector());
                focus.sub(eyeDir.x, eyeDir.y, 0);
            }
        }

        eyeDir.set(eyeOffset);
        // y movement
        if (corrMouseYPos < SCREEN_MOVE_BORDER_SIZE) {
            float value = positionToMovement(corrMouseYPos) * deltaTime;
            eyeDir.normalize(value);
            focus.sub(eyeDir.x, eyeDir.y, 0);

        } else {
            int yInv = height - corrMouseYPos;
            if (yInv < SCREEN_MOVE_BORDER_SIZE) {
                float value = positionToMovement(yInv) * deltaTime;
                eyeDir.normalize(value);
                focus.add(eyeDir.x, eyeDir.y, 0);
            }
        }
    }

    @Override
    public Vector3fc getEye() {
        return new Vector3f(focus).add(eyeOffset);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return Vectors.Z;
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye, Vector3fc up) {
        this.focus.set(focus);
        Vector3f offset = new Vector3f(eye).sub(focus);
        this.eyeOffset.set(offset);
    }

    @Override
    public boolean isIsometric() {
        return root.settings().ISOMETRIC_VIEW;
    }

    @Override
    public void onScroll(float value) {

        Settings s = root.settings();
        float zoomSpeed = s.CAMERA_ZOOM_SPEED;
        float maxZoom = s.MAX_CAMERA_DIST;

        float v = Math.max(Math.min(zoomSpeed * -value, ZOOM_SPEED_LIMIT), -ZOOM_SPEED_LIMIT);
        eyeOffset.mul(v + 1f);
        float minZoom = s.MIN_CAMERA_DIST;

        if (eyeOffset.lengthSquared() > maxZoom * maxZoom) {
            eyeOffset.normalize(maxZoom);
        } else if (eyeOffset.lengthSquared() < minZoom * minZoom) {
            eyeOffset.normalize(minZoom);
        }
    }

    @Override
    public void cleanup() {
    }

    /**
     * gives how much the camera should move given how many pixels the mouse is from the edge of the screen
     * @param pixels the number of pixels between the mouse and the edge of the screen, at least 0
     * @return how fast the camera should move in the direction
     */
    protected float positionToMovement(int pixels) {
        if (pixels >= SCREEN_MOVE_BORDER_SIZE) return 0;

        int offset = Math.min(SCREEN_MOVE_BORDER_SIZE - pixels, SCREEN_DEAD_ZONE);
        return offset * (1f / (SCREEN_MOVE_BORDER_SIZE - SCREEN_DEAD_ZONE)) * BORDER_MOVE_SPEED * eyeOffset.length();
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
//        float exactDelta = xPos - mouseXPos;
        mouseXPos = xPos;
        mouseYPos = yPos;

        if (isBeingRotated) {
            float angle = -xDelta * ROTATION_MODIFIER;
            eyeOffset.rotateZ(angle);

        } else if (isBeginMoved) {
            // x movement
            Vector3f eyeDir = new Vector3f(eyeOffset);
            eyeDir.mul(xDelta * DRAG_MODIFIER).cross(getUpVector());
            focus.add(eyeDir.x, eyeDir.y, 0);

            // y movement
            eyeDir.set(eyeOffset).mul(yDelta * DRAG_MODIFIER);
            focus.sub(eyeDir.x, eyeDir.y, 0);
        }
    }

    @Override
    public void onClick(int button, int xPos, int yPos) {
        if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            isBeingRotated = true;

        } else if (button == GLFW_MOUSE_BUTTON_LEFT) {
            isBeginMoved = true;
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        isBeingRotated = false;
        isBeginMoved = false;
    }
}

