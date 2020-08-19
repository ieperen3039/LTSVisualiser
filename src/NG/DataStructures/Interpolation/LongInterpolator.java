package NG.DataStructures.Interpolation;

/**
 * a long interpolator that uses rounding on the returned values and with additional care taken for precision.
 * @author Geert van Ieperen created on 15-12-2017.
 */
public class LongInterpolator extends LinearInterpolator<Long> {

    public LongInterpolator(int capacity, long initialValue, double initialTime) {
        super(capacity, initialValue, initialTime);
    }

    public LongInterpolator(int capacity, long firstElement, double firstTime, long secondElement, double secondTime) {
        super(capacity, firstElement, firstTime, secondElement, secondTime);
    }

    @Override
    protected Long interpolate(Long firstElt, Long secondElt, float fraction) {
        return Math.round((secondElt - firstElt) * (double) fraction) + firstElt;
    }

    @Override
    protected Long derivative(Long firstElt, Long secondElt, float deltaTime) {
        return Math.round((secondElt - firstElt) / (double) deltaTime);
    }
}
