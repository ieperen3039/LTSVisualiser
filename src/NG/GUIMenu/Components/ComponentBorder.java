package NG.GUIMenu.Components;

import org.joml.Vector2i;

/**
 * @author Geert van Ieperen created on 16-5-2019.
 */
public class ComponentBorder {
    public int left;
    public int right;
    public int top;
    public int bottom;

    public ComponentBorder(int left, int right, int top, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public ComponentBorder(int value) {
        this.left = value;
        this.right = value;
        this.top = value;
        this.bottom = value;
    }

    public ComponentBorder() {
        this(0);
    }

    /**
     * maps an outer position and size of this border to the inside position and size of this border.
     * @param position   the position to be reduced
     * @param dimensions the dimensions to be reduced
     */
    public void reduce(Vector2i position, Vector2i dimensions) {
        dimensions.sub(left + right, top + bottom);
        position.add(left, top);
    }

    /**
     * maps an inner position and size of this border to the outside position and size of this border.
     * @param position   the position to be increased
     * @param dimensions the dimensions to be increased
     */
    public void increase(Vector2i position, Vector2i dimensions) {
        dimensions.add(left + right, top + bottom);
        position.sub(left, top);
    }

    /**
     * adds the given values to each element
     * @return this
     */
    public ComponentBorder add(int left, int right, int up, int down) {
        this.left += left;
        this.right += right;
        this.top += up;
        this.bottom += down;
        return this;
    }

    /**
     * adds the size of the other border to this border
     * @return this
     */
    public ComponentBorder add(ComponentBorder other) {
        this.left += other.left;
        this.right += other.right;
        this.top += other.top;
        this.bottom += other.bottom;
        return this;
    }

    /**
     * subtracts the given values from each element
     * @return this
     */
    public ComponentBorder sub(int left, int right, int up, int down) {
        this.left -= left;
        this.right -= right;
        this.top -= up;
        this.bottom -= down;
        return this;
    }

    /**
     * subtracts the size of the other border from this border
     * @return this
     */
    public ComponentBorder sub(ComponentBorder other) {
        this.left -= other.left;
        this.right -= other.right;
        this.top -= other.top;
        this.bottom -= other.bottom;
        return this;
    }

    @Override
    public String toString() {
        return String.format("[l: %d, r: %d, t: %d, b: %d]", left, right, top, bottom);
    }
}
