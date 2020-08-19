package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseDragListener;
import NG.InputHandling.MouseScrollListener;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.*;

/**
 * @author Geert van Ieperen created on 31-5-2020.
 */
public class SSlider extends SComponent implements MouseDragListener, MouseClickListener, MouseScrollListener {
    private static final int BASE_DRAG_BAR_WIDTH = 50;
    private static final float SCROLL_SPEED = 0.05f;
    private final List<SSliderListener> changeListeners = new ArrayList<>();
    private float minimum;
    private float maximum;
    private float current;
    private int minWidth;
    private int minHeight;
    private int dragBarWidth;

    public SSlider(SComponentProperties props) {
        this(0, 1, 0, props);
    }

    public SSlider(float minimum, float maximum, float current, SComponentProperties props) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.current = current;

        this.minWidth = props.minWidth;
        this.minHeight = props.minHeight;
        setGrowthPolicy(props.wantHzGrow, props.wantVtGrow);
        dragBarWidth = (minWidth == 0) ? BASE_DRAG_BAR_WIDTH : minWidth / 10;
    }

    public SSlider(float minimum, float maximum, float current, SComponentProperties props, SSliderListener listener) {
        this(minimum, maximum, current, props);
        addChangeListener(listener);
    }

    public void addChangeListener(SSliderListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(SSliderListener listener) {
        changeListeners.remove(listener);
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(PANEL, screenPosition, getSize());
        design.draw(SCROLL_BAR_BACKGROUND, screenPosition, getSize());

        int space = getWidth() - dragBarWidth;
        if (space > 0) {
            float shift = getFraction() * space;
            Vector2i dragBarPos = new Vector2i(screenPosition).add((int) shift, 0);
            design.draw(SCROLL_BAR_DRAG_ELEMENT, dragBarPos, new Vector2i(dragBarWidth, getHeight()));
        }
    }

    @Override
    public void mouseDragged(int xDelta, int yDelta, float xPos, float yPos) {
        int componentXPos = getScreenPosition().x + dragBarWidth / 2;
        int componentXSize = getWidth() - dragBarWidth;
        float fraction = (xPos - componentXPos) / componentXSize;
        setFraction(fraction);
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        int componentXSize = getWidth() - dragBarWidth;
        float fraction = (float) xRel / componentXSize;
        setFraction(fraction);
    }

    private float getFraction() {
        return (current - minimum) / (maximum - minimum);
    }

    private void setFraction(float fraction) {
        if (fraction > 1) fraction = 1;
        if (fraction < 0) fraction = 0;
        float newValue = (maximum - minimum) * fraction + minimum;
        setCurrent(newValue);
    }

    public float getMinimum() {
        return minimum;
    }

    public void setMinimum(float minimum) {
        this.minimum = minimum;
    }

    public float getMaximum() {
        return maximum;
    }

    public void setMaximum(float maximum) {
        this.maximum = maximum;
    }

    public float getCurrent() {
        return current;
    }

    public void setCurrent(float newValue) {
        if (current != newValue) {
            current = newValue;
            changeListeners.forEach(l -> l.onChange(newValue));
        }
    }

    @Override
    public void onScroll(float value) {
        setFraction(getFraction() + value * SCROLL_SPEED);
    }

    public interface SSliderListener {
        void onChange(float newValue);
    }
}
