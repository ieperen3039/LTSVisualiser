package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.LimitedVisibilityLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen created on 13-5-2019.
 */
public class SScrollableList extends SContainer.GhostContainer {
    private final SScrollBar scroller;
    private final LimitedVisibilityLayout layout;
    private int nrOfShownElts;

    public SScrollableList(int nrOfShownElts, SComponent... elements) {
        this(nrOfShownElts, new LimitedVisibilityLayout(nrOfShownElts, 0, true), elements);
        this.nrOfShownElts = nrOfShownElts;
    }

    private SScrollableList(
            int nrOfShownElts, LimitedVisibilityLayout layout, SComponent... elements
    ) {
        super(layout);
        this.layout = layout;

        for (SComponent element : elements) {
            super.add(element, null);
        }

        scroller = new SScrollBar(elements.length, nrOfShownElts);
        scroller.setParent(this);
        scroller.addListener(value -> {
            this.layout.setAsFirstVisible(value);
            invalidateLayout();
        });

        layoutBorder.add(0, scroller.minWidth(), 0, 0);
    }

    @Override
    public void add(SComponent comp, Object prop) {
        super.add(comp, prop);
        scroller.resize(children().size(), nrOfShownElts);
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        validateLayout();
        if (scroller.contains(xRel, yRel)) {
            Vector2ic position = scroller.getPosition();
            return scroller.getComponentAt(xRel - position.x(), yRel - position.y());

        } else {
            return super.getComponentAt(xRel, yRel);
        }
    }

    @Override
    public void doValidateLayout() {
        super.doValidateLayout();

        ComponentBorder border = layoutBorder;
        scroller.setSize(0, getHeight() - border.top - border.bottom);
        scroller.setPosition(getWidth() - border.right, border.top);
        scroller.validateLayout();
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        scroller.draw(design, new Vector2i(screenPosition).add(scroller.getPosition()));
        drawChildren(design, screenPosition);
    }

    @Override
    public int minHeight() {
        return Math.max(super.minHeight(), scroller.minHeight());
    }

    @Override
    public int minWidth() {
        return Math.max(super.minWidth(), scroller.minWidth());
    }
}
