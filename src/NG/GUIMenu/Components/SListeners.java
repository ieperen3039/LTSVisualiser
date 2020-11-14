package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseDragListener;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import org.joml.Vector2ic;

/**
 * An SListener allows you to bind a callback to a component. If an SListener wraps multiple components that are not
 * listeners themselves, the callback will be captured by this. As the callback searches up the hierarchy for the first
 * valid listener, this wrapping will provide a capture point. If a listener is contained within this SListener, it
 * receives priority.
 * @author Geert van Ieperen created on 24-10-2020.
 * @see ClickListener
 * @see DragListener
 * @see ScrollListener
 */
public class SListeners extends SComponent {
    private final SComponent wrapped;

    private SListeners(SComponent panel) {
        this.wrapped = panel;
        panel.setParent(this);
    }

    @Override
    public int minWidth() {
        return wrapped.minWidth();
    }

    @Override
    public int minHeight() {
        return wrapped.minHeight();
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        validateLayout();
        return wrapped.getComponentAt(xRel, yRel);
    }

    @Override
    public void doValidateLayout() {
        wrapped.setSize(getWidth(), getHeight());
        wrapped.validateLayout();
        setSize(getWidth(), getHeight());
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        wrapped.draw(design, screenPosition);
    }

    /**
     * captures clicks and releases of underlying components
     */
    public static class ClickListener extends SListeners implements MouseClickListener, MouseReleaseListener {
        private MouseClickListener onClick = (button, xRel, yRel) -> {};
        private MouseReleaseListener onRelease = button -> {};

        public ClickListener(SComponent wrapped) {
            super(wrapped);
        }

        public ClickListener setClickListener(MouseClickListener onClick) {
            this.onClick = onClick;
            return this;
        }

        public ClickListener setReleaseListener(MouseReleaseListener onRelease) {
            this.onRelease = onRelease;
            return this;
        }

        @Override
        public void onClick(int button, int xRel, int yRel) {
            onClick.onClick(button, xRel, yRel);
        }

        @Override
        public void onRelease(int button) {
            onRelease.onRelease(button);
        }
    }

    public static class ScrollListener extends SListeners implements MouseScrollListener {
        private MouseScrollListener onScroll;

        private ScrollListener(SComponent wrapped) {
            super(wrapped);
        }

        public ScrollListener setScrollListener(MouseScrollListener onScroll) {
            this.onScroll = onScroll;
            return this;
        }

        @Override
        public void onScroll(float value) {
            onScroll.onScroll(value);
        }
    }

    public static class DragListener extends SListeners implements MouseDragListener {
        private MouseDragListener action;

        public DragListener(SComponent wrapped) {
            super(wrapped);
        }

        public DragListener setDragListener(MouseDragListener action) {
            this.action = action;
            return this;
        }

        @Override
        public void onMouseDrag(int xDelta, int yDelta, float xPos, float yPos) {
            action.onMouseDrag(xDelta, yDelta, xPos, yPos);
        }
    }
}
