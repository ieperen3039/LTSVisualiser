package NG.GUIMenu;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;

/**
 * @author Geert van Ieperen created on 28-5-2020.
 */
public class SComponentProperties {
    public int minWidth = 200;
    public int minHeight = 50;
    public boolean wantHzGrow = true;
    public boolean wantVtGrow = true;
    public NGFonts.TextType textType = NGFonts.TextType.REGULAR;
    public SFrameLookAndFeel.Alignment alignment = SFrameLookAndFeel.Alignment.CENTER_MIDDLE;

    public SComponentProperties() {
    }

    public SComponentProperties(int minWidth, int minHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public SComponentProperties(int minWidth, int minHeight, boolean wantHzGrow, boolean wantVtGrow) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.wantHzGrow = wantHzGrow;
        this.wantVtGrow = wantVtGrow;
    }

    public SComponentProperties(
            int minWidth, int minHeight, boolean wantHzGrow, boolean wantVtGrow,
            NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment
    ) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.wantHzGrow = wantHzGrow;
        this.wantVtGrow = wantVtGrow;
        this.textType = textType;
        this.alignment = alignment;
    }
}
