package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseDragListener;
import NG.InputHandling.MouseReleaseListener;

/**
 * @author Geert van Ieperen. Created on 25-9-2018.
 */
public class SExtendedTextArea extends STextArea
        implements MouseClickListener, MouseReleaseListener, MouseDragListener {
    private MouseDragListener dragListener;
    private MouseClickListener clickListener;
    private MouseReleaseListener releaseListener;

    public SExtendedTextArea(String text, SComponentProperties props) {
        super(text, props);
    }

    public SExtendedTextArea(
            String frameTitle, int minWidth, int minHeight, boolean doGrowInWidth, NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment
    ) {
        super(frameTitle, minHeight, minWidth, doGrowInWidth, textType, alignment);
    }

    public SExtendedTextArea(STextArea source) {
        this(source.getText(), source.minWidth, source.minHeight, source.wantHorizontalGrow(), source.textType, source.alignment);
    }

    @Override
    public void onClick(int button, int xRel, int yRel) {
        if (clickListener != null) {
            clickListener.onClick(button, xRel, yRel);
        }
    }

    @Override
    public void mouseDragged(int xDelta, int yDelta, float xPos, float yPos) {
        if (dragListener != null) {
            dragListener.mouseDragged(xDelta, yDelta, xPos, yPos);
        }
    }

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
        }
    }

    public void setDragListener(MouseDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setClickListener(MouseClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setReleaseListener(MouseReleaseListener releaseListener) {
        this.releaseListener = releaseListener;
    }
}
