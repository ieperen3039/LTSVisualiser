package NG.GUIMenu.FrameManagers;

import NG.Core.Main;
import NG.Core.Version;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.Components.SFiller;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Rendering.NVGOverlay;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.Rendering.WindowsLF;
import NG.InputHandling.*;
import NG.Rendering.GLFWWindow;
import NG.Tools.Logger;
import org.joml.Vector2i;

import java.util.*;

/**
 * Objects of this class can manage an in-game window system that is behaviourally similar to classes in the {@link
 * javax.swing} package. New {@link SFrame} objects can be added using {@link #addFrame(SFrame)}.
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class FrameManagerImpl implements UIFrameManager {
    protected Main root;
    protected MouseDragListener dragListener = null;
    protected MouseReleaseListener releaseListener = null;
    protected KeyTypeListener typeListener = null;

    private final Deque<SFrame> frames; // the first element in this list has focus
    private SComponent mainPanel;
    private SComponent modalComponent;
    private SComponent hoveredComponent;

    private SFrameLookAndFeel lookAndFeel;

    public FrameManagerImpl() {
        this.frames = new ArrayDeque<>();
        lookAndFeel = new WindowsLF();
        mainPanel = new SFiller(0, 0);
    }

    @Override
    public void init(Main root) throws Version.MisMatchException {
        if (this.root != null) return;
        this.root = root;

        lookAndFeel.init(root);
        GLFWWindow window = root.window();
        mainPanel.setSize(window.getWidth(), window.getHeight());
    }

    @Override
    public void setMainGUI(SComponent container) {
        this.mainPanel = container;

        if (root != null) {
            GLFWWindow window = root.window();
            container.setSize(window.getWidth(), window.getHeight());
        }
    }

    @Override
    public void draw(NVGOverlay.Painter painter) {
        assert hasLookAndFeel();

        GLFWWindow window = root.window();
        if (window.getWidth() != mainPanel.getWidth() || window.getHeight() != mainPanel.getHeight()) {
            mainPanel.setSize(window.getWidth(), window.getHeight());
        }

        lookAndFeel.setPainter(painter);
        mainPanel.validateLayout();
        mainPanel.draw(lookAndFeel, new Vector2i(0, 0));

        frames.removeIf(SFrame::isDisposed);

        Iterator<SFrame> itr = frames.descendingIterator();
        while (itr.hasNext()) {
            final SFrame f = itr.next();

            if (f.isVisible()) {
                f.validateLayout();
                f.draw(lookAndFeel, f.getPosition());

                // if anything caused invalidation of the layout (e.g. text size information) then redraw this frame
                while (!f.layoutIsValid()) {
                    f.validateLayout();
                    f.draw(lookAndFeel, f.getPosition());
                }
            }
        }

        if (modalComponent != null) {
            modalComponent.validateLayout();
            modalComponent.draw(lookAndFeel, modalComponent.getScreenPosition());
        }
    }

    @Override
    public boolean removeElement(SComponent component) {
        if (component instanceof SFrame) {
            ((SFrame) component).dispose();
            return true;
        }

        Optional<SComponent> optParent = component.getParent();
        if (optParent.isPresent()) {
            SComponent parent = optParent.get();
            if (parent instanceof SFrame) {
                ((SFrame) parent).dispose();
                return true;
            }
        }
        return false;
    }

    @Override
    public void addFrame(SFrame frame, int x, int y) {
        // if the frame was already visible, still add it to make it focused.
        frames.remove(frame);

        boolean success = frames.offerFirst(frame);
        if (!success) {
            Logger.DEBUG.print("Too much subframes opened, removing the last one");
            frames.removeLast().dispose();
            frames.addFirst(frame);
        }

        frame.setPosition(x, y);
    }

    @Override
    public void focus(SFrame frame) {
        if (frame.isDisposed()) {
            throw new NoSuchElementException(frame + " is disposed");
        }

        // even if the frame was not opened, show it
        frame.setVisible(true);

        // no further action when already focused
        if (frame.equals(frames.peekFirst())) return;

        boolean success = frames.remove(frame);
        if (!success) {
            throw new NoSuchElementException(frame + " was not part of the window");
        }

        frames.addFirst(frame);
    }

    @Override
    public void setLookAndFeel(SFrameLookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    @Override
    public boolean hasLookAndFeel() {
        return lookAndFeel != null;
    }

    @Override
    public void setModalListener(SComponent listener) {
        modalComponent = listener;
    }

    @Override
    public void setTextListener(KeyTypeListener listener) {
        typeListener = listener;
    }

    @Override
    public void clear() {
        frames.forEach(SFrame::dispose);
        frames.clear();
        mainPanel = new SFiller(0, 0);
    }

    @Override
    public void cleanup() {
        clear();
    }

    @Override
    public boolean covers(int xSc, int ySc) {
        if (modalComponent != null && modalComponent.isVisible() && modalComponent.contains(xSc, ySc)) {
            return true;
        }

        for (SFrame frame : frames) {
            if (frame.isVisible() && frame.contains(xSc, ySc)) {
                return true;
            }
        }

        SComponent c = mainPanel.getComponentAt(xSc, ySc);
        return c != null && c.isVisible();
    }

    @Override
    public boolean checkMouseClick(int button, final int xSc, final int ySc) {
        // check modal dialogues
        if (modalComponent != null && modalComponent.isVisible()) {
            if (modalComponent.contains(xSc, ySc)) {
                processClick(button, modalComponent, xSc, ySc);
            }
            modalComponent = null;

        } else {
            // check all frames, starting from the front-most frame
            SFrame frame = getFrame(xSc, ySc);
            if (frame != null) {
                int xr = xSc - frame.getX();
                int yr = ySc - frame.getY();
                SComponent component = frame.getComponentAt(xr, yr);

                focus(frame);
                processClick(button, component, xSc, ySc);
                return true;
            }


            SComponent component = mainPanel.getComponentAt(xSc, ySc);
            if (component == null) return false;

            processClick(button, component, xSc, ySc);
        }
        return true;
    }

    private void processClick(int button, SComponent component, int xSc, int ySc) {
//        Logger.DEBUG.print(component);

        // click listener
        SComponent target = component;
        do {
            if (target instanceof MouseClickListener) {
                MouseClickListener cl = (MouseClickListener) target;
                // by def. of MouseRelativeClickListener, give relative coordinates
                Vector2i pos = component.getScreenPosition();
                cl.onClick(button, xSc - pos.x, ySc - pos.y);
                break;
            }

            target = target.getParent().orElse(null);

        } while (target != null);

        // drag listener
        target = component;
        do {
            if (target instanceof MouseDragListener) {
                dragListener = (MouseDragListener) target;
                break;
            }

            target = target.getParent().orElse(null);

        } while (target != null);

        // release listener
        target = component;
        do {
            if (target instanceof MouseReleaseListener) {
                releaseListener = (MouseReleaseListener) target;
                break;
            }

            target = target.getParent().orElse(null);

        } while (target != null);
    }

    @Override
    public SComponent getComponentAt(int xSc, int ySc) {
        // check all frames, starting from the front-most frame
        SFrame frame = getFrame(xSc, ySc);
        if (frame != null) {
            int xr = xSc - frame.getX();
            int yr = ySc - frame.getY();
            return frame.getComponentAt(xr, yr);

        } else {
            return mainPanel.getComponentAt(xSc, ySc);
        }

    }

    private SFrame getFrame(int xSc, int ySc) {
        for (SFrame frame : frames) {
            if (frame.isVisible() && frame.contains(xSc, ySc)) {
                return frame;
            }
        }
        return null;
    }

    @Override
    public void keyTyped(char letter) {
        if (typeListener == null) return;
        typeListener.keyTyped(letter);
    }

    @Override
    public void onRelease(int button) {
        dragListener = null;
        if (releaseListener == null) return;
        releaseListener.onRelease(button);
        releaseListener = null;
    }

    public void onMouseMove(int xDelta, int yDelta, float xPos, float yPos) {
        if (hoveredComponent != null) hoveredComponent.setHovered(false);
        hoveredComponent = getComponentAt((int) xPos, (int) yPos);
        if (hoveredComponent != null) hoveredComponent.setHovered(true);

        if (dragListener != null) {
            dragListener.onMouseDrag(xDelta, yDelta, xPos, yPos);
        }
    }

    @Override
    public void onScroll(float value) {
        SComponent target = hoveredComponent;
        while (target != null) {
            if (target instanceof MouseScrollListener) {
                MouseScrollListener listener = (MouseScrollListener) target;
                listener.onScroll(value);
                break;
            }

            target = target.getParent().orElse(null);
        }
        ;
    }
}
