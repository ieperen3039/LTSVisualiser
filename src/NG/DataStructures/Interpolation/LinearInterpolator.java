package NG.DataStructures.Interpolation;

import NG.DataStructures.Generic.BlockingTimedArrayQueue;

import java.util.Iterator;

/**
 * @author Geert van Ieperen created on 15-12-2017.
 */
public abstract class LinearInterpolator<T> extends BlockingTimedArrayQueue<T> {
    /**
     * @param capacity       the expected maximum number of entries
     * @param initialElement this item will initially be placed in the queue twice.
     * @param initialTime    the time of starting
     */
    public LinearInterpolator(int capacity, T initialElement, double initialTime) {
        super(capacity);
        add(initialElement, initialTime - 1);
        add(initialElement, initialTime);
    }

    /**
     * crates an interpolator with the first two values already given
     * @param capacity      the expected maximum number of entries
     * @param firstElement  the item that occurs first
     * @param firstTime     the time of occurence
     * @param secondElement the item that occurs second
     * @param secondTime    the time of occurence of the second, where first < second
     */
    public LinearInterpolator(int capacity, T firstElement, double firstTime, T secondElement, double secondTime) {
        super(capacity);
        add(firstElement, firstTime);
        add(secondElement, secondTime);
    }

    /**
     * @return the interpolated object defined by implementation
     */
    public synchronized T getInterpolated(double timeStamp) {
        assert timeStamps.size() > 1 : timeStamps;

        Iterator<Double> times = timeStamps.iterator();
        Iterator<T> things = elements.iterator();

        T firstElt;
        double firstTime;
        double secondTime = times.next();

        // consider the next time period, and check whether timeStamp falls in it
        do {
            firstElt = things.next();
            firstTime = secondTime;
            secondTime = times.next();
        } while (secondTime < timeStamp && times.hasNext());


        float fraction = (float) ((timeStamp - firstTime) / (secondTime - firstTime));
        if (Float.isNaN(fraction)) return firstElt;

        T secondElt = things.next();
        return interpolate(firstElt, secondElt, fraction);
    }

    /**
     * interpolate using linear interpolation
     * @return firstElt + (secondElt - firstElt) * fraction
     */
    protected abstract T interpolate(T firstElt, T secondElt, float fraction);

    /**
     * @param timeStamp
     * @return the derivative of the value returned by getInterpolated(time)
     */
    public T getDerivative(double timeStamp) {
        assert timeStamps.size() > 1 : timeStamps;

        Iterator<Double> times = timeStamps.iterator();
        Iterator<T> things = elements.iterator();

        T firstElt = things.next();
        double firstTime = times.next();
        double secondTime = times.next();

        // consider the next time period, and check whether timeStamp falls in it
        // we check from the 2nd time onwards to allow extrapolation
        while (secondTime < timeStamp) {
            firstElt = things.next();
            firstTime = secondTime;
            secondTime = times.next();
        }

        T secondElt = things.next();
        return derivative(firstElt, secondElt, (float) (secondTime - firstTime));
    }

    protected abstract T derivative(T firstElt, T secondElt, float deltaTime);
}
