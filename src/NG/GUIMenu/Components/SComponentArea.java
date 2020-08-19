package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.SingleElementLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Tools.Logger;
import org.joml.Vector2ic;

/**
 * an area with fixed minimum size that can show components or be hidden. Components are stretched to fit the designated
 * area. If the minimum size of the component is too large for this area, an assertion is thrown.
 * @author Geert van Ieperen created on 12-7-2019.
 */
public class SComponentArea extends SContainer {
    private static final SFiller FILLER = new SFiller();
    private int width;
    private int height;

    public SComponentArea(SComponentProperties props) {
        super(new SingleElementLayout());
        this.width = props.minWidth;
        this.height = props.minHeight;
        setVisible(false);
        setGrowthPolicy(props.wantHzGrow, props.wantVtGrow);
    }

    public SComponentArea(int width, int height) {
        super(new SingleElementLayout());
        this.width = width;
        this.height = height;
        setVisible(false);
        setGrowthPolicy(false, false);
    }

    /**
     * removes the current component, and sets this component's visibility to false
     */
    public void hide() {
        add(FILLER, null);
        setVisible(false);
    }

    public void show(SComponent element) {
        validateLayout();
        if (element.minWidth() < getWidth() && element.minHeight() < getHeight()) {
            add(element, null);
            setVisible(true);
        } else {
            Logger.ASSERT.print("Element too large to show", element, getSize(), element.getSize());
        }

    }

    @Override
    public int minWidth() {
        return width;
    }

    @Override
    public int minHeight() {
        return height;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        drawChildren(design, screenPosition);
    }
}
