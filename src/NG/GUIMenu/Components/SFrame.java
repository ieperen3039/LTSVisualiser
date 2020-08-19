package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * A Frame object similar to {@link javax.swing.JFrame} objects. The {@link #setMainPanel(SComponent)} can be used to
 * control over the contents of the SFrame.
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SPanel {
    public static final int FRAME_TITLE_BAR_SIZE = 50;

    private final String title;
    private final STextArea titleComponent;
    private final SContainer bodyComponent;
    private boolean isDisposed = false;

    /**
     * Creates a SFrame with the given title, width and height
     * @param title the title of the new frame
     * @see SPanel
     */
    public SFrame(String title, int width, int height) {
        this(title, width, height, true);
    }

    /**
     * Creates a SFrame with the given title, width and height. Moving, minimizing and closing by the user is disabled
     * for this frame.
     * @param title the title of the new frame
     * @see SPanel
     */
    public SFrame(String title, int width, int height, boolean manipulable) {
        super(new GridLayoutManager(1, 2));
        setGrowthPolicy(false, false);
        this.title = title;

        if (manipulable) {
            SExtendedTextArea titleComponent = new SExtendedTextArea(
                    title, 0, FRAME_TITLE_BAR_SIZE, true,
                    NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER
            );
            this.titleComponent = titleComponent;
            titleComponent.setDragListener((dx, dy, x, y) -> addToPosition(dx, dy));
            titleComponent.setClickListener((x, y, b) -> Logger.DEBUG.print(minWidth(), getSize()));

            SContainer titleBar = SContainer.row(
                    titleComponent, new SCloseButton(this)
            );
            add(new SPanel(titleBar), new Vector2i(0, 0));

        } else {
            titleComponent = new STextArea(title, FRAME_TITLE_BAR_SIZE, 0, true, NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_TOP);
            add(new SPanel(titleComponent), new Vector2i(0, 0));
        }

        bodyComponent = SContainer.singleton(new SFiller());
        add(bodyComponent, new Vector2i(0, 1));

        setSize(width, height);
        setGrowthPolicy(false, false);
    }

    /**
     * Creates a SFrame with the given title and minimum size
     * @param title the title of the new frame
     * @see SPanel
     */
    public SFrame(String title) {
        this(title, 0, 0);
    }

    public SFrame(String title, SComponent mainPanel) {
        this(title);
        setMainPanel(mainPanel);
        pack();
    }

    public void setTitle(String title) {
        titleComponent.setText(title);
    }

    /**
     * sets the area below the title bar to contain the given component. The size of this component is determined by
     * this frame.
     * @param comp the new middle component
     * @return this
     */
    public SFrame setMainPanel(SComponent comp) {
        bodyComponent.add(comp, null);
        invalidateLayout();
        return this;
    }

    /**
     * sets the size of this frame to the minimum as a call to {@code setSize(minWidth(), minHeight())}
     */
    public void pack() {
        setSize(minWidth(), minHeight());
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        if (!isVisible()) return;
        super.draw(design, screenPosition);
    }

    @Override
    public String toString() {
        return "SFrame (" + title + ")";
    }

    /**
     * removes this component and release any resources used
     */
    public void dispose() {
        this.setVisible(false);
        isDisposed = true;
    }

    /**
     * @return true if {@link #dispose()} has been successfully called
     */
    public boolean isDisposed() {
        return isDisposed;
    }
}
