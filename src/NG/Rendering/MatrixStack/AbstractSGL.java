package NG.Rendering.MatrixStack;

import org.joml.*;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An implementation of the Shader GL object, which handles a model matrix and stack operations. No shader-specific
 * operations are covered.
 * @author Geert van Ieperen created on 30-1-2019.
 */
public abstract class AbstractSGL implements SGL {
    protected static final Painter LOCK = new Painter();
    private static final Matrix4fc QUARTER_X = new Matrix4f(
            1, 0, 0, 0,
            0, 0, 1, 0,
            0, -1, 0, 0,
            0, 0, 0, 1
    );
    private static final Matrix4fc QUARTER_Y = new Matrix4f(
            0, 0, -1, 0,
            0, 1, 0, 0,
            1, 0, 0, 0,
            0, 0, 0, 1
    );
    private static final Matrix4fc QUARTER_Z = new Matrix4f(
            0, 1, 0, 0,
            -1, 0, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    );
    private Deque<Matrix4f> matrixStack;
    private Matrix4f modelMatrix;

    public AbstractSGL() {
        matrixStack = new ArrayDeque<>();
        modelMatrix = new Matrix4f();
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        modelMatrix.rotate(angle, x, y, z);
    }

    @Override
    public void rotate(Vector3fc axis, float angle) {
        modelMatrix.rotate(angle, axis);
    }

    @Override
    public void translate(float x, float y, float z) {
        modelMatrix.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        modelMatrix.scale(x, y, z);
    }

    @Override
    public void scale(Vector3fc s) {
        modelMatrix.scale(s);
    }

    @Override
    public Vector3f getPosition(Vector3fc position) {
        Vector3f result = new Vector3f();
        position.mulPosition(modelMatrix, result);
        return result;
    }

    @Override
    public Vector3f getDirection(Vector3fc direction) {
        Vector3f result = new Vector3f();
        direction.mulDirection(modelMatrix, result);
        return result;
    }

    @Override
    public void pushMatrix() {
        matrixStack.push(new Matrix4f(modelMatrix));
    }

    @Override
    public void popMatrix() {
        modelMatrix = matrixStack.pop();
    }

    @Override
    public void rotate(Quaternionfc rotation) {
        modelMatrix.rotate(rotation);
    }

    @Override
    public void translate(Vector3fc v) {
        modelMatrix.translate(v);
    }

    @Override
    public void multiplyAffine(Matrix4fc postTransformation) {
        modelMatrix.mulAffine(postTransformation);
    }

    @Override
    public void rotateXYZ(float x, float y, float z) {
        modelMatrix.rotateXYZ(x, y, z);
    }

    @Override
    public void rotateQuarter(int x, int y, int z) {
        assert x >= 0 && y >= 0 && z >= 0;

        for (int i = 0; i < x; i++) {
            modelMatrix.mulAffine(QUARTER_X);
        }
        for (int i = 0; i < y; i++) {
            //noinspection SuspiciousNameCombination
            modelMatrix.mulAffine(QUARTER_Y);
        }
        for (int i = 0; i < z; i++) {
            modelMatrix.mulAffine(QUARTER_Z);
        }
    }

    protected Matrix4f getModelMatrix() {
        return modelMatrix;
    }
}
