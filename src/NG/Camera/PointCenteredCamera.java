package NG.Camera;

import NG.Core.Main;
import NG.Settings.Settings;
import NG.Tools.Vectors;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * @author Geert van Ieperen created on 5-11-2017. The standard camera that rotates using dragging some of the code
 * originates from the RobotRace sample code of the TU/e
 */
public class PointCenteredCamera implements Camera {
    private static final float ZOOM_SPEED = -0.1f;
    private static final float DRAG_ROTATE_SPEED = -0.005f;
    private static final float DRAG_MOVE_SPEED = 0.0005f;

    private final Vector3f focus;
    private final Quaternionf rotation;

    private float vDist;

    private boolean isHeld = false;
    private Main root;

    public PointCenteredCamera(Vector3fc focus) {
        this.focus = new Vector3f(focus);
        this.rotation = new Quaternionf();
        this.vDist = 100f;
    }

    public PointCenteredCamera(Vector3fc focus, Vector3fc eye) {
        this.vDist = eye.distance(focus);
        this.focus = new Vector3f(focus);
        this.rotation = new Quaternionf().lookAlong(new Vector3f(focus).sub(eye), Vectors.Y);
    }

    @Override
    public void init(Main root) {
        this.root = root;
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
        isHeld = (button == GLFW_MOUSE_BUTTON_RIGHT || button == GLFW_MOUSE_BUTTON_MIDDLE);
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        if (!isHeld) return;

        if (root.keyControl().isShiftPressed()) {
            Vector3f right = new Vector3f(1, 0, 0).rotate(rotation);
            focus.add(right.mul(xDelta * -DRAG_MOVE_SPEED * vDist));

            Vector3f up = new Vector3f(0, 1, 0).rotate(rotation);
            focus.add(up.mul(yDelta * DRAG_MOVE_SPEED * vDist));

        } else {
            rotation.rotateY(xDelta * DRAG_ROTATE_SPEED);
            rotation.rotateX(yDelta * DRAG_ROTATE_SPEED);
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        isHeld = false;
    }

    @Override
    public Vector3fc vectorToFocus() {
        return new Vector3f(0, 0, 1).rotate(rotation).mul(-vDist);
    }

    @Override
    public void updatePosition(float deltaTime) {
    }

    @Override
    public Vector3fc getEye() {
        Vector3f vecToEye = new Vector3f(0, 0, 1).rotate(rotation).mul(vDist);
        return vecToEye.add(focus);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return new Vector3f(0, 1, 0).rotate(rotation);
    }

    @Override
    public void set(Vector3fc focus) {
        this.focus.set(focus);
    }

    @Override
    public boolean isIsometric() {
        return root.settings().ISOMETRIC_VIEW;
    }
}
