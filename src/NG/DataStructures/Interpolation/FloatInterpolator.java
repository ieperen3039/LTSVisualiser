package NG.DataStructures.Interpolation;

import NG.Tools.Toolbox;

/**
 * @author Geert van Ieperen created on 15-12-2017.
 */
public class FloatInterpolator extends LinearInterpolator<Float> {

    public FloatInterpolator(int capacity, float initialValue, float initialTime) {
        super(capacity, initialValue, initialTime);
    }

    public FloatInterpolator(int capacity, float firstElement, float firstTime, float secondElement, float secondTime) {
        super(capacity, firstElement, firstTime, secondElement, secondTime);
    }

    @Override
    protected Float interpolate(Float firstElt, Float secondElt, float fraction) {
        return Toolbox.interpolate(firstElt, secondElt, fraction);
    }

    @Override
    protected Float derivative(Float firstElt, Float secondElt, float deltaTime) {
        return (secondElt - firstElt) / deltaTime;
    }
}
