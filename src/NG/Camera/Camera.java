package NG.Camera;

import NG.Core.Main;
import NG.Core.ToolElement;
import NG.InputHandling.MouseListener;
import NG.Rendering.GLFWWindow;
import NG.Settings.Settings;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A camera class manages movement and position of the camera. The actual implementation of creating the perspective
 * matrix is done based on the values of {@link #getEye()}, {@link #getFocus()} and {@link #getUpVector()}
 * @author Geert van Ieperen created on 29-10-2017.
 * @see MovableCamera
 */
public interface Camera extends ToolElement, MouseListener {
    /**
     * a copy of the direction vector of the eye of the camera to the focus of the camera.
     * @return {@link #getEye()}.to({@link #getFocus()}) The length of this vector may differ by implementation
     */
    Vector3fc vectorToFocus();

    @Override
    void init(Main root);

    /**
     * updates the state of this camera according to the given passed time.
     * @param deltaTime the number of seconds passed since last update. This may be real-time or in-game time
     */
    void updatePosition(float deltaTime);

    /** a copy of the position of the camera itself */
    Vector3fc getEye();

    /** a copy of the point in space where the camera looks to */
    Vector3fc getFocus();

    /** a copy of the direction of up, the length of this vector is undetermined. */
    Vector3fc getUpVector();

    void set(Vector3fc focus, Vector3fc eye, Vector3fc up);

    /**
     * Calculates a projection matrix based on a camera position and the given parameters of the viewport
     * @param window the window used to visualise the current space
     * @return a projection matrix, such that modelspace vectors multiplied with this matrix will be transformed to
     * viewspace.
     */
    default Matrix4f getViewProjection(GLFWWindow window) {
        float ratio = (float) window.getWidth() / window.getHeight();
        Matrix4f vpMatrix = getProjectionMatrix(ratio);
        return getViewMatrix(vpMatrix);
    }

    default Matrix4f getViewMatrix(Matrix4f vpMatrix) {
        return vpMatrix.lookAt(
                getEye(),
                getFocus(),
                getUpVector()
        );
    }

    default Matrix4f getProjectionMatrix(float aspectRatio) {
        Matrix4f vpMatrix = new Matrix4f();

        if (isIsometric()) {
            float visionSize = (vectorToFocus().length() - Settings.Z_NEAR) / 4;
            vpMatrix.setOrthoSymmetric(aspectRatio * visionSize, visionSize, Settings.Z_NEAR, Settings.Z_FAR);
        } else {
            vpMatrix.setPerspective(Settings.FOV, aspectRatio, Settings.Z_NEAR, Settings.Z_FAR);
        }
        return vpMatrix;
    }

    boolean isIsometric();

    default Vector2f project(Vector3fc vector, GLFWWindow window) {
        // view + projection transform
        Vector3f scPos = new Vector3f(vector).mulPosition(getViewProjection(window));
        // window transform
        float xPix = (scPos.x + 1) * window.getWidth() * 0.5f;
        float yPix = (scPos.y - 1) * window.getHeight() * -0.5f;

        return new Vector2f(xPix, yPix);
    }

    ;
}
