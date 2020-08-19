package NG.DataStructures.Tracked;

import java.util.Objects;

/**
 * Created by s152717 on 4-5-2017. may be superfluous
 */
public class LinearSmoothFloat extends TrackedFloat implements SmoothTracked<Float> {

    protected final float acceleration;

    public LinearSmoothFloat(Float initial, float acceleration) {
        super(initial);
        this.acceleration = acceleration;
    }

    public LinearSmoothFloat(Float current, Float previous, float acceleration) {
        super(previous, current);
        this.acceleration = acceleration;
    }

    /**
     * updates this value to a value that is closer toward {@code target} defined as the exact equal of {@code
     * preservedFraction}
     * @param target the target value
     */
    @Override
    public void updateFluent(Float target, float deltaTime) {
        if (!Objects.equals(current(), target)) {
            if (current() < target) {
                super.update(
                        Math.min(target, current() + (acceleration * deltaTime))
                );
            } else {
                super.update(
                        Math.max(target, current() - (acceleration * deltaTime))
                );
            }
        }
    }
}
