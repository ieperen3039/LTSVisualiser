package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.Optional;

/**
 * The S stands for Sub-
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SComponent {
    private final Vector2i position = new Vector2i();
    private final Vector2i dimensions = new Vector2i();
    protected boolean isHovered = false;
    private boolean layoutIsValid = false;
    private boolean isVisible = true;
    private SComponent parent = null;
    private boolean wantHzGrow = true;
    private boolean wantVtGrow = true;

    /**
     * @return minimum width of this component in pixels. The final width can be assumed to be at least this size unless
     * the layout manger decides otherwise.
     */
    public abstract int minWidth();

    /**
     * @return minimum height of this component in pixels. The final height is at least this size unless the layout
     * manger decides otherwise.
     */
    public abstract int minHeight();

    /**
     * sets the layout validity flag of this component and all of its parents to false.
     */
    protected final void invalidateLayout() {
        if (layoutIsValid) {
            layoutIsValid = false;
            if (parent != null) parent.invalidateLayout();
        }
    }

    /**
     * restores the validity of the layout of this component.
     * @see #doValidateLayout()
     */
    public final synchronized void validateLayout() {
        if (!layoutIsValid) {
            doValidateLayout();
            layoutIsValid = true;
        }
    }

    /**
     * set the validity of this component and all of its children
     */
    protected void doValidateLayout() {
        dimensions.x = Math.max(dimensions.x, minWidth());
        dimensions.y = Math.max(dimensions.y, minHeight());
    }

    /**
     * sets the want-grow policies.
     * @param horizontal if true, the next invocation of {@link #wantHorizontalGrow()} will return true. Otherwise, it
     *                   will return true iff any of its child components returns true on that method.
     * @param vertical   if true, the next invocation of {@link #wantVerticalGrow()} will return true. Otherwise, it
     *                   will return true iff any of its child components returns true on that method.
     * @return this
     */
    public SComponent setGrowthPolicy(boolean horizontal, boolean vertical) {
        wantHzGrow = horizontal;
        wantVtGrow = vertical;
        invalidateLayout();
        return this;
    }

    /**
     * @return true if this component should expand horizontally when possible. when false, the components should always
     * be its minimum width.
     */
    public boolean wantHorizontalGrow() {
        return wantHzGrow;
    }

    /**
     * @return true if this component should expand horizontally when possible. When false, the components should always
     * be its minimum height.
     */
    public boolean wantVerticalGrow() {
        return wantVtGrow;
    }

    /**
     * if this has sub-components, it will find the topmost component {@code c} for which {@code c.contains(x, y)}.
     * @param xRel a relative x coordinate
     * @param yRel a relative y coordinate
     * @return the topmost component {@code c} for which {@code c.contains(x, y)}.
     */
    public SComponent getComponentAt(int xRel, int yRel) {
        return this;
    }

    public boolean contains(Vector2i v) {
        return contains(v.x, v.y);
    }

    /**
     * checks whether the given coordinate is within this component
     * @param x x position relative to parent
     * @param y y position relative to parent
     * @return true iff the given coordinate lies within the bounds defined by position and dimensions
     */
    public boolean contains(int x, int y) {
        int xr = x - getX();
        if (xr < 0 || xr >= getWidth()) {
            return false;
        }

        int yr = y - getY();
        return !(yr < 0 || yr >= getHeight());
    }

    /** Adds the given x and y to the position, like a call of {@code setPosition(getX() + xDelta, getY() + yDelta);} */
    public void addToPosition(int xDelta, int yDelta) {
        position.add(xDelta, yDelta);
    }

    /**
     * sets the position of this component relative to its parent. If this component is part of a layout, then this
     * method should only be called by the layout manager.
     * @return this
     */
    public final SComponent setPosition(int x, int y) {
        position.set(x, y);
        return this;
    }

    /**
     * sets the size of this component. If any of the given dimensions are smaller than the minimum ({@link
     * #minWidth()}, {@link #minHeight()}), then that dimension is set to the minimum
     * @param width  the preferred width
     * @param height the preferred height
     * @return this
     */
    public final SComponent setSize(int width, int height) {
        width = Math.max(width, minWidth());
        height = Math.max(height, minHeight());

        dimensions.set(width, height);
        invalidateLayout();
        return this;
    }

    public void addToSize(int xDelta, int yDelta) {
        setSize(dimensions.x + xDelta, dimensions.y + yDelta);
    }

    /** @see #getPosition() */
    public int getX() {
        return position.x;
    }

    // getters

    /** @see #getPosition() */
    public int getY() {
        return position.y;
    }

    /** @return the position of this object in regard to its parent */
    public Vector2ic getPosition() {
        return position;
    }

    /** @see #setPosition(int, int) */
    public final SComponent setPosition(Vector2ic position) {
        this.position.set(position);
        return this;
    }

    public Vector2i getScreenPosition() {
        if (parent == null) {
            return new Vector2i(position);
        } else {
            return parent.getScreenPosition().add(position);
        }
    }

    public int getWidth() {
        return dimensions.x;
    }

    public int getHeight() {
        return dimensions.y;
    }

    public Vector2ic getSize() {
        return dimensions;
    }

    /**
     * Draw this component.
     * @param design         The element that provides functions for drawing
     * @param screenPosition the position where this component is drawn instead of the local (relative) position.
     */
    public abstract void draw(SFrameLookAndFeel design, Vector2ic screenPosition);

    /** @return whether this component is drawn */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * sets the visibility and invalidates the layout of the parent.
     * @param doVisible if true, the component is set visible and if possible, its parent is updated. If false, the
     *                  component will not be drawn.
     */
    public void setVisible(boolean doVisible) {
        isVisible = doVisible;
        if (doVisible) validateLayout();
    }

    public Optional<SComponent> getParent() {
        return Optional.ofNullable(parent);
    }

    public void setParent(SComponent parent) {
        this.parent = parent;
    }

    /**
     * @return true iff the dimensions of this component are validated. If not, a call to {@link #validateLayout()}
     * should fix this.
     */
    public boolean layoutIsValid() {
        return layoutIsValid;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * sets whether the cursor is hovering over this component.
     * @param hovered true if the mouse is on this component, false if not
     */
    public void setHovered(boolean hovered) {
        isHovered = hovered;
    }

    public Vector2i getMiddle() {
        return new Vector2i(position).add(dimensions.x / 2, dimensions.y / 2);
    }

    /**
     * @param other
     * @return the vector from the furthest corner of other inside this object, or null if these do not overlap
     */
    public Vector2i getOverlapWith(SComponent other) {
        Vector2i thisMid = getMiddle();
        Vector2i otherMid = other.getMiddle();

        Vector2i thisPosition = this.position;
        Vector2i otherPosition = other.position;

        Vector2i thisToOther = otherMid.sub(thisMid);
        if (thisToOther.x > 0) {
            thisPosition.x += getWidth();
            if (thisPosition.x < otherPosition.x) return null;
        } else {
            otherPosition.x += other.getWidth();
            if (thisPosition.x > otherPosition.x) return null;
        }

        if (thisToOther.y > 0) {
            thisPosition.y += getHeight();
            if (thisPosition.y < otherPosition.y) return null;
        } else {
            otherPosition.y += other.getHeight();
            if (thisPosition.y > otherPosition.y) return null;
        }

        return thisPosition.sub(otherPosition);
    }
}
