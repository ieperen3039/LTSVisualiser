package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * A Frame object similar to {@link javax.swing.JFrame} objects. The {@link #setMainPanel(SComponent)} can be used to
 * control over the contents of the SFrame.
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
public class SFrame extends SDecorator {
    public static final int FRAME_TITLE_BAR_SIZE = 50;

    private final String title;
    private boolean isDisposed = false;

    private final SContainer bodyComponent;
    private STextArea titleComponent;

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
        super(
                new SPanel(1, 2, true, true)
        );
        this.title = title;

        SComponent upperBar;
        if (manipulable) {
            STextArea text = new STextArea(
                    title, 0, FRAME_TITLE_BAR_SIZE, true,
                    NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
            );
            titleComponent = text;

            upperBar = SContainer.row(
                    new SListeners.DragListener(new SPanel(text))
                            .setDragListener((dx, dy, x, y) -> addToPosition(dx, dy)),
                    new SCloseButton(this)
            );

        } else {
            titleComponent = new STextArea(title, FRAME_TITLE_BAR_SIZE, 0, true, NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_TOP);
            upperBar = new SPanel(titleComponent);
        }
        bodyComponent = SContainer.singleton(new SFiller());

        add(upperBar, new Vector2i(0, 0));
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

    private SPanel makeUpperBar(String frameTitle) {
        SExtendedTextArea title = new SExtendedTextArea(
                frameTitle, 0, FRAME_TITLE_BAR_SIZE, true,
                NGFonts.TextType.TITLE, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
        );
        titleComponent = title;
        title.setDragListener((dx, dy, x, y) -> addToPosition(dx, dy));

        return new SPanel(SContainer.row(
                title, new SCloseButton(this)
        ));
    }

    /**
     * sets the area below the title bar to contain the given component. The size of this component is determined by
     * this frame.
     * @param comp the new middle component
     * @return this
     */
    public SDecorator setMainPanel(SComponent comp) {
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
        validateLayout();
        design.draw(SFrameLookAndFeel.UIComponent.PANEL, screenPosition, getSize());
        super.draw(design, screenPosition);
    }

    @Override
    public String toString() {
        return "SFrame (" + title + ")";
    }

    @Override
    public Vector2i getScreenPosition() {
        return new Vector2i(getPosition());
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
