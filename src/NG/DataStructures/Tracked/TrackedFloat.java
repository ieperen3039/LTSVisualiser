package NG.DataStructures.Tracked;

/**
 * Created by Geert van Ieperen on 4-5-2017.
 */
public class TrackedFloat extends TrackedObject<Float> {

    public TrackedFloat(Float previous, Float current) {
        super(previous, current);
    }

    public TrackedFloat(Float initial) {
        super(initial);
    }

    /**
     * updates the value by adding the parameter to the current value
     * @param addition the value that is added to the current. actual results may vary
     */
    public void addUpdate(Float addition) {
        update(current() + addition);
    }

    /**
     * @return the increase of the last updatePosition, defined as (current - previous)
     */
    public Float difference() {
        return current() - previous();
    }
}
