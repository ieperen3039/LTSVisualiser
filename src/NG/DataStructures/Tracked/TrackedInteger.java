package NG.DataStructures.Tracked;

/**
 * @author Geert van Ieperen created on 5-11-2017.
 */
public class TrackedInteger extends TrackedObject<Integer> {
    public TrackedInteger(Integer current, Integer previous) {
        super(previous, current);
    }

    public TrackedInteger(Integer initial) {
        super(initial);
    }

    /**
     * updates the value by adding the parameter to the current value
     * @param addition the value that is added to the current. actual results may vary
     */
    public void addUpdate(Integer addition) {
        update(current() + addition);
    }

    public Integer difference() {
        return current() - previous();
    }
}
