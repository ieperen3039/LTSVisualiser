package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.GUIMenu.LayoutManagers.SLayoutManager;
import NG.GUIMenu.LayoutManagers.SingleElementLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.Collection;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public abstract class SContainer extends SComponent {
    protected final ComponentBorder layoutBorder;
    private final SLayoutManager layout;

    /**
     * a container that uses the given manager for its layout
     */
    public SContainer(SLayoutManager layout, ComponentBorder layoutBorder) {
        this.layout = layout;
        this.layoutBorder = layoutBorder;
    }

    /**
     * a container that uses the given manager for its layout
     */
    public SContainer(SLayoutManager layout) {
        this(layout, new ComponentBorder(4));
    }

    /**
     * constructor for a container that uses a grid layout of the given size and a growth policy of true
     * @param xElts nr of elements in width
     * @param yElts nr of elements in height
     */
    public SContainer(int xElts, int yElts) {
        this(new GridLayoutManager(xElts, yElts), new ComponentBorder(4));
    }

    /**
     * @param comp the component to be added
     * @param prop the property instance accepted by the current layout manager
     */
    public void add(SComponent comp, Object prop) {
        layout.add(comp, prop);
        comp.setParent(this);
        invalidateLayout();
    }

    protected Collection<SComponent> children() {
        return layout.getComponents();
    }

    /**
     * removes a component from this container
     * @param comp the component that should be added to this component first.
     */
    public void removeCompoment(SComponent comp) {
        layout.remove(comp);
        invalidateLayout();
    }

    public void drawChildren(SFrameLookAndFeel lookFeel, Vector2ic offset) {
        for (SComponent component : children()) {
            if (component.isVisible() && component.getWidth() != 0 && component.getHeight() != 0) {
                Vector2i scPos = new Vector2i(component.getPosition()).add(offset);
                component.draw(lookFeel, scPos);
            }
        }
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        validateLayout();
        for (SComponent component : children()) {
            if (component.isVisible() && component.contains(xRel, yRel)) {
                xRel -= component.getX();
                yRel -= component.getY();
                return component.getComponentAt(xRel, yRel);
            }
        }
        return this;
    }

    @Override
    public int minWidth() {
        validateLayoutSize();
        ComponentBorder borderSize = layoutBorder;
        return layout.getMinimumWidth() + borderSize.left + borderSize.right;
    }

    private void validateLayoutSize() {
        if (!layoutIsValid()) {
            layout.recalculateProperties();
        }
    }

    @Override
    public int minHeight() {
        validateLayoutSize();
        ComponentBorder borderSize = layoutBorder;
        return layout.getMinimumHeight() + borderSize.top + borderSize.bottom;
    }

    @Override
    public void doValidateLayout() {
        super.doValidateLayout();

        // ensure minimum width and height
        Vector2i layoutPos = new Vector2i(0, 0);
        Vector2i layoutDim = new Vector2i(getSize());
        layoutBorder.reduce(layoutPos, layoutDim);

        layout.recalculateProperties();
        layout.placeComponents(layoutPos, layoutDim);

        // then restructure the children
        for (SComponent child : children()) {
            child.validateLayout();
        }
    }

    @Override
    public boolean wantHorizontalGrow() {
        return super.wantHorizontalGrow() && layout.wantHorizontalGrow();
    }

    @Override
    public boolean wantVerticalGrow() {
        return super.wantVerticalGrow() && layout.wantVerticalGrow();
    }

    @Override
    public SContainer setGrowthPolicy(boolean horizontal, boolean vertical) {
        super.setGrowthPolicy(horizontal, vertical);
        return this;
    }

    /**
     * a wrapper for a target component, to have it behave as a container with one value being itself
     * @param target the component to wrap
     * @return the target as a container object
     */
    public static SContainer singleton(SComponent target) {
        return new GhostContainer(target);
    }

    /** creates a new invisible container with the given components on a single column */
    public static SContainer column(SComponent... components) {
        SContainer column = new GhostContainer(new GridLayoutManager(1, components.length));

        for (int i = 0; i < components.length; i++) {
            column.add(components[i], new Vector2i(0, i));
        }

        column.setSize(0, 0);
        return column;
    }

    /** creates a new invisible container with the given components in a single row */
    public static SContainer row(SComponent... components) {
        SContainer row = new GhostContainer(new GridLayoutManager(components.length, 1));

        for (int i = 0; i < components.length; i++) {
            row.add(components[i], new Vector2i(i, 0));
        }

        row.setSize(0, 0);
        return row;
    }

    /** creates a new invisible container with the given components in a grid */
    public static SComponent grid(SComponent[][] components) {
        int xSize = components[0].length;
        int ySize = components.length;
        SContainer row = new GhostContainer(new GridLayoutManager(xSize, ySize));

        for (int i = 0; i < xSize; i++) {
            for (int j = 0; j < ySize; j++) {
                row.add(components[j][i], new Vector2i(i, j));
            }
        }

        row.setSize(0, 0);
        return row;
    }

    public static class GhostContainer extends SContainer {
        public GhostContainer(SLayoutManager layout) {
            super(layout, new ComponentBorder(0));
            setGrowthPolicy(true, true);
        }

        public GhostContainer(SComponent target) {
            this(new SingleElementLayout());
            add(target, null);
        }

        @Override
        public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
            drawChildren(design, screenPosition);
        }

        @Override
        public SComponent getComponentAt(int xRel, int yRel) {
            SComponent found = super.getComponentAt(xRel, yRel);
            if (found == this) return null;
            return found;
        }
    }
}
