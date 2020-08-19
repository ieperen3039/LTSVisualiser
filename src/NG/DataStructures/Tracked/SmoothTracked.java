package NG.DataStructures.Tracked;

/**
 * @author Geert van Ieperen created on 29-10-2017.
 */
public interface SmoothTracked<T> {

    /**
     * updates this object, but does not instantly move to the target. the actual result of this method is defined in
     * implementations
     * @param target    the target object what this should become
     * @param deltaTime the time since the last updatePosition, to allow speed and acceleration
     */
    void updateFluent(T target, float deltaTime);
}
