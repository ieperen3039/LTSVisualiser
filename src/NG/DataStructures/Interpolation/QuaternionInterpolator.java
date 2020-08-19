package NG.DataStructures.Interpolation;

import org.joml.Quaternionf;

/**
 * @author Geert van Ieperen created on 22-12-2017.
 */
public class QuaternionInterpolator extends LinearInterpolator<Quaternionf> {

    private static final float DOT_THRESHOLD = 0.125f;

    /**
     * @param capacity    the initial expected maximum number of entries
     * @param initialItem
     * @param initialTime
     */
    public QuaternionInterpolator(int capacity, Quaternionf initialItem, float initialTime) {
        super(capacity, initialItem, initialTime);
    }

    @Override
    protected Quaternionf interpolate(Quaternionf firstElt, Quaternionf secondElt, float fraction) {
        final Quaternionf result = new Quaternionf();

        if (firstElt.dot(secondElt) > DOT_THRESHOLD) {
            firstElt.nlerpIterative(secondElt, fraction, DOT_THRESHOLD, result);
        } else {
            firstElt.nlerp(secondElt, fraction, result);
        }
        return result;
    }

    @Override
    protected Quaternionf derivative(Quaternionf firstElt, Quaternionf secondElt, float deltaTime) {
        Quaternionf dx = new Quaternionf(firstElt).difference(secondElt);
        return dx.scale(1 / deltaTime);
    }
}
