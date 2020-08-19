package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;

/**
 * a simple panel with text.
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class STextArea extends STextComponent {
    public STextArea(String text, SComponentProperties props) {
        super(text, props);
    }

    public STextArea(
            String text, int minHeight, int minWidth, boolean doGrowInWidth, NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment
    ) {
        super(text, textType, alignment, minWidth, minHeight);
        setGrowthPolicy(doGrowInWidth, false);
    }

    public STextArea(String text, int minHeight) {
        this(text, minHeight, 0, true, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT);
    }

    @Override
    public String toString() {
        String text = getText();
        String substring = text.length() > 25 ? text.substring(0, 20) + "..." : text;
        return this.getClass().getSimpleName() + " (" + substring + ")";
    }
}
