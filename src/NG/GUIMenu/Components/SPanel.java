package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.GUIMenu.LayoutManagers.SLayoutManager;
import NG.GUIMenu.LayoutManagers.SingleElementLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.PANEL;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SPanel extends SContainer {

    /** when using the default constructor, you can use these values to denote the positions */
    public static final Object NORTH = new Vector2i(1, 0);
    public static final Object EAST = new Vector2i(2, 1);
    public static final Object SOUTH = new Vector2i(1, 2);
    public static final Object WEST = new Vector2i(0, 1);
    public static final Object NORTHEAST = new Vector2i(2, 0);
    public static final Object SOUTHEAST = new Vector2i(2, 2);
    public static final Object NORTHWEST = new Vector2i(0, 0);
    public static final Object SOUTHWEST = new Vector2i(0, 2);
    public static final Object MIDDLE = new Vector2i(1, 1);

    /**
     * the basic panel for holding components. Use {@link Vector2i} for positioning elements. Note that indices are
     * 0-indexed.
     * @param cols number of columns in x direction
     * @param rows number of rows in y direction
     */
    public SPanel(int cols, int rows) {
        super((cols * rows > 0 ? new GridLayoutManager(cols, rows) : new SingleElementLayout()));
    }

    /**
     * creates a panel with the given layout manager, minimum size of (0, 0)
     * @param layoutManager a layout manager for this component
     */
    public SPanel(SLayoutManager layoutManager) {
        super(layoutManager);
    }

    /**
     * creates a panel that uses a default GridLayout of 3x3 and with minimum size of (0, 0) and a no-growth policy.
     * Objects should be located using one of
     * {@link #NORTH},
     * {@link #EAST},
     * {@link #SOUTH},
     * {@link #WEST},
     * {@link #NORTHEAST},
     * {@link #SOUTHEAST},
     * {@link #NORTHWEST},
     * {@link #SOUTHWEST},
     * {@link #MIDDLE}
     */
    public SPanel() {
        super(new GridLayoutManager(3, 3));
    }

    /**
     * creates a panel with a single element. Calling {@link #add(SComponent, Object)} causes this element to be
     * replaced with the new element. Use {@code null} as add position.
     * @param content the element of this panel.
     */
    public SPanel(SComponent content) {
        super(new SingleElementLayout());
        add(content, null);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        assert getWidth() > 0 && getHeight() > 0 :
                String.format("Non-positive dimensions of %s: width = %d, height = %d", this, getWidth(), getHeight());

        design.draw(PANEL, screenPosition, getSize());
        drawChildren(design, screenPosition);
    }
}
