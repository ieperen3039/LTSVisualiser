package NG.DataStructures.Tracked;

import static java.lang.StrictMath.pow;

/**
 * @author Geert van Ieperen created on 29-10-2017.
 */
public class ExponentialSmoothFloat extends TrackedFloat implements SmoothTracked<Float> {

    protected final float preservedFraction;

    /**
     * a float that reduces its distance to its target with a preset fraction
     * @param preservedFraction the fraction that must be preserved per second
     */
    public ExponentialSmoothFloat(float initial, float preservedFraction) {
        super(initial);
        this.preservedFraction = preservedFraction;
    }

    /**
     * a float that reduces its distance to its target with a preset fraction
     * @param preservedFraction the fraction that must be preserved per second
     */
    public ExponentialSmoothFloat(float current, float previous, float preservedFraction) {
        super(previous, current);
        this.preservedFraction = preservedFraction;
    }

    @Override
    public void updateFluent(Float target, float deltaTime) {
        if (deltaTime == 0) {
            return;
        }
        float deceleration = (float) pow(preservedFraction, deltaTime);

        // non-absolute difference between current float and target float. negative if source moves down
        float diff = target - current();
        float reducedDiff = (1 - deceleration) * diff;

        addUpdate(reducedDiff);
    }
}
