package NG.Camera;


import NG.Core.Main;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * @author Geert van Ieperen created on 22-12-2017. a camera that doesn't move
 */
public class StaticCamera implements Camera {
    private Vector3fc eye, focus;
    private Vector3fc up;
    private boolean isometric;

    public StaticCamera(Vector3fc eye, Vector3fc focus, Vector3fc up, boolean isometric) {
        this.eye = eye;
        this.focus = focus;
        this.up = up;
        this.isometric = isometric;
    }

    @Override
    public void init(Main root) {

    }

    @Override
    public Vector3f vectorToFocus() {
        return new Vector3f(focus).sub(eye);
    }

    @Override
    public void updatePosition(float deltaTime) {

    }

    @Override
    public Vector3fc getEye() {
        return eye;
    }

    @Override
    public Vector3fc getFocus() {
        return focus;
    }

    @Override
    public Vector3fc getUpVector() {
        return up;
    }

    @Override
    public void set(Vector3fc focus, Vector3fc eye, Vector3fc up) {
        this.focus = new Vector3f(focus);
        this.eye = new Vector3f(eye);
    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean isIsometric() {
        return isometric;
    }

    @Override
    public void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {

    }

    @Override
    public void onScroll(float value) {

    }

    @Override
    public void onClick(int button, int xRel, int yRel) {

    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {

    }
}
