package NG.Camera;

import NG.Core.Main;
import NG.Settings.Settings;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 5-11-2017. The standard camera that rotates using dragging some of the code
 * originates from the RobotRace sample code of the TU/e
 */
public class FlatCamera implements Camera {
    private static final float ZOOM_SPEED = -0.1f;

    private final Vector3f focus;

    private float vDist;

    private boolean isHeld = false;
    private Main root;
    private float xMoveScalar;
    private float yMoveScalar;

    public FlatCamera(Vector3fc eye) {
        this.focus = new Vector3f(eye.x(), eye.y(), 0);
        vDist = eye.z();
    }

    @Override
    public void init(Main root) {
        this.root = root;

        xMoveScalar = -1.0f / 32;
        yMoveScalar = 1.0f / 32;
    }

    @Override
    public void onScroll(float value) {
        vDist *= (ZOOM_SPEED * value) + 1f;

        Settings s = root.settings();
        vDist = Math.min(Math.max(vDist, s.MIN_CAMERA_DIST), s.MAX_CAMERA_DIST);
    }


    @Override
    public void cleanup() {

    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        isHeld = (button == GLFW_MOUSE_BUTTON_RIGHT);
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (!isHeld) return;

        focus.add(xDelta * xMoveScalar, yDelta * yMoveScalar, 0);
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        isHeld = false;
    }

    @Override
    public Vector3fc vectorToFocus() {
        return new Vector3f(0, 0, -vDist);
    }

    @Override
    public void updatePosition(float deltaTime) {
    }

    @Override
    public Vector3fc getEye() {
        return new Vector3f(focus).add(0, 0, vDist);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return new Vector3f(0, 1, 0);
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye, Vector3fc up) {
        this.focus.set(focus);
        vDist = eye.distance(focus);
    }

    @Override
    public boolean isIsometric() {
        return root.settings().ISOMETRIC_VIEW;
    }
}
