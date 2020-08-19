package NG.GUIMenu.LayoutManagers;

import NG.GUIMenu.Components.SComponent;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Uses {@link Integer} as placement information, which determines the index in the list. This index must be between 0
 * and the number of elements in this layout. Accepts null, which will set the index to the size. Removing elements will
 * cause the indexes of higher components to shift one down.
 * @author Geert van Ieperen created on 16-5-2019.
 */
public class LimitedVisibilityLayout implements SLayoutManager {
    private final boolean vertical;
    private final List<SComponent> elements;

    private int currentInd;
    private int nrOfVisibleElts;

    // the following properties only hold for the visible elements
    private int nrOfLongGrowVisible;
    private int minLayoutShortSize;
    private int minLayoutLongSize;
    private int sumVisibleMinLongSize;


    public LimitedVisibilityLayout(int nrOfVisibleElts) {
        this(nrOfVisibleElts, 0, true);
    }

    /**
     * @param nrOfVisibleElts number of elements that are made visible by this layout. Must be positive
     * @param minLengthPixels the minimum size of the layout on the horizontal/vertical axis. This axis depends on the
     *                        'vertical' parameter
     * @param vertical        if true, the layout will place its elements vertically, using minimumLayoutSize as its
     *                        horizontal minimum size. If false, the layout will place its elements horizontally, using
     *                        minimumLayoutSize as its vertical minimum size.
     */
    public LimitedVisibilityLayout(int nrOfVisibleElts, int minLengthPixels, boolean vertical) {
        assert nrOfVisibleElts > 0;
        this.nrOfVisibleElts = nrOfVisibleElts;
        this.vertical = vertical;
        this.minLayoutLongSize = minLengthPixels;
        this.elements = new ArrayList<>(nrOfVisibleElts);
        this.currentInd = 0;
    }

    @Override
    public void add(SComponent comp, Object prop) {
        assert comp != null;

        if (prop instanceof Integer) {
            elements.add((int) prop, comp);

        } else if (prop == null) {
            elements.add(comp);

        } else {
            throw new IllegalArgumentException("prop must be an Integer, but was " + prop.getClass());
        }
    }

    @Override
    public void remove(SComponent comp) {
        elements.remove(comp);
    }

    @Override
    public void recalculateProperties() {
        minLayoutShortSize = 0;
        sumVisibleMinLongSize = 0;
        nrOfLongGrowVisible = 0;

        assert isInBounds(currentInd);

        for (int i = 0; i < currentInd; i++) {
            elements.get(i).setVisible(false);
        }

        int endInd = Math.min(nrOfVisibleElts + currentInd, elements.size());
        for (int i = currentInd; i < endInd; i++) {
            SComponent elt = elements.get(i);
            elt.setVisible(true);

            int minShortSize = vertical ? elt.minWidth() : elt.minHeight();
            if (minShortSize > minLayoutShortSize) {
                minLayoutShortSize = minShortSize;
            }

            boolean wantLongGrow = vertical ? elt.wantVerticalGrow() : elt.wantHorizontalGrow();
            if (wantLongGrow) nrOfLongGrowVisible++;

            sumVisibleMinLongSize += (vertical ? elt.minHeight() : elt.minWidth());
        }

        for (int i = endInd; i < elements.size(); i++) {
            elements.get(i).setVisible(false);
        }
    }

    /**
     * shift the view to the given start index of visible elements. If the index falls below 0 or above the maximum
     * index, this call is ignored. Calls {@link #recalculateProperties()} if necessary.
     * @param addition the number of elements to move on.
     */
    public void shiftVisible(int addition) {
        if (addition != 0) {
            int newInd = currentInd + addition;
            if (isInBounds(newInd)) {
                currentInd = newInd;
                recalculateProperties();
            }
        }
    }

    /**
     * sets the element at the given index as the first visible element. Calls {@link #recalculateProperties()} if
     * necessary.
     * @param newInd the new first index. Indices {@code value} to {@code value + nrOfVisibleElts} will be shown
     */
    public void setAsFirstVisible(int newInd) {
        if (newInd != currentInd && isInBounds(newInd)) {
            currentInd = newInd;
            recalculateProperties();
        }
    }

    private boolean isInBounds(int newInd) {
        if (newInd >= 0) {
            int nrOfTotalElts = elements.size();
            if (nrOfTotalElts == 0) return true;
            if (nrOfTotalElts < nrOfVisibleElts && newInd < nrOfTotalElts) return true;
            return newInd <= (nrOfTotalElts - nrOfVisibleElts);
        }
        return false;
    }

    /**
     * sets how many elements are visible. New elements are first appended at the end of the list when available.
     * @param value the new number of elements shown by this layout.
     */
    public void setNrOfVisible(int value) {
        if (value != nrOfVisibleElts) {
            nrOfVisibleElts = value;
            recalculateProperties();
        }
    }

    @Override
    public void placeComponents(Vector2ic position, Vector2ic dimensions) {
        int longSize = vertical ? dimensions.y() : dimensions.x();
        float longSizeGain = longSize - sumVisibleMinLongSize;
        if (nrOfLongGrowVisible > 0) {
            longSizeGain /= nrOfLongGrowVisible;
        }

        Vector2i eltPos = new Vector2i(position);
        int endInd = Math.min(nrOfVisibleElts + currentInd, elements.size());
        for (int i = currentInd; i < endInd; i++) {
            SComponent elt = elements.get(i);
            elt.setPosition(eltPos);
            if (vertical) {
                int height = (int) (elt.minHeight() + (elt.wantVerticalGrow() ? longSizeGain : 0));
                elt.setSize(dimensions.x(), height);
                eltPos.add(0, height);

            } else {
                int width = (int) (elt.minHeight() + (elt.wantHorizontalGrow() ? longSizeGain : 0));
                elt.setSize(width, dimensions.y());
                eltPos.add(width, 0);
            }
        }
    }

    @Override
    public Collection<SComponent> getComponents() {
        return elements;
    }

    @Override
    public int getMinimumWidth() {
        return vertical ? minLayoutShortSize : Math.max(sumVisibleMinLongSize, minLayoutLongSize);
    }

    @Override
    public int getMinimumHeight() {
        return vertical ? Math.max(sumVisibleMinLongSize, minLayoutLongSize) : minLayoutShortSize;
    }

    @Override
    public Class<?> getPropertyClass() {
        return Integer.class;
    }

    @Override
    public void clear() {
        elements.clear();
        currentInd = 0;
    }

    @Override
    public boolean wantHorizontalGrow() {
        for (SComponent elt : elements) {
            if (elt.wantHorizontalGrow()) return true;
        }
        return false;
    }

    @Override
    public boolean wantVerticalGrow() {
        for (SComponent elt : elements) {
            if (elt.wantVerticalGrow()) return true;
        }
        return false;
    }
}
