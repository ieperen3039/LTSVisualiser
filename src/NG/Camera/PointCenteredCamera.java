package NG.Camera;

import NG.Core.Root;
import NG.Tools.Vectors;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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

    private float vDist = 100f;

    private boolean isHeld = false;
    private Root root;

    public PointCenteredCamera(Vector3fc focus) {
        this.focus = new Vector3f(focus);
        this.rotation = new Quaternionf();
    }

    @Override
    public void init(Root root) throws Exception {
        this.root = root;
    }

    @Override
    public void onScroll(float value) {
        vDist *= (ZOOM_SPEED * value) + 1f;
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

        if (root.keyControl().isShiftPressed()) {
            Vector3f right = new Vector3f(0, 1, 0).rotate(rotation);
            focus.add(right.mul(xDelta * -DRAG_MOVE_SPEED * vDist));

            Vector3f up = new Vector3f(0, 0, 1).rotate(rotation);
            focus.add(up.mul(yDelta * DRAG_MOVE_SPEED * vDist));

        } else {
            rotation.rotateZ(xDelta * DRAG_ROTATE_SPEED);
            rotation.rotateY(yDelta * DRAG_ROTATE_SPEED);
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        isHeld = false;
    }

    @Override
    public Vector3fc vectorToFocus() {
        return Vectors.newXVector().rotate(rotation).mul(-vDist);
    }

    @Override
    public void updatePosition(float deltaTime) {
    }

    @Override
    public Vector3fc getEye() {
        Vector3f vecToEye = Vectors.newXVector().rotate(rotation).mul(vDist);
        return vecToEye.add(focus);
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return Vectors.newZVector().rotate(rotation);
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye) {
        this.focus.set(focus);
//        this.eye.set(eye);
    }

    @Override
    public boolean isIsometric() {
        return root.settings().ISOMETRIC_VIEW;
    }
}
