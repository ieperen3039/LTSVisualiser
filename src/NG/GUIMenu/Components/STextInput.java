package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.KeyPressListener;
import NG.InputHandling.MouseClickListener;
import org.joml.Vector2ic;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.SELECTION;

/**
 * @author Geert van Ieperen. Created on 5-10-2018.
 */
public class STextInput extends STextComponent implements KeyPressListener, MouseClickListener {
    public STextInput(
            String text, int minHeight, int minWidth, boolean doGrowInWidth, NGFonts.TextType textType,
            SFrameLookAndFeel.Alignment alignment
    ) {
        super(text, textType, alignment, minWidth, minHeight);
        setGrowthPolicy(doGrowInWidth, false);
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(SELECTION, screenPosition, getSize());
        design.draw(SFrameLookAndFeel.UIComponent.PANEL, screenPosition, getSize());
        super.draw(design, screenPosition);
    }

    @Override
    public void keyPressed(int keyCode) {

    }

    @Override
    public void onClick(int button, int xSc, int ySc) {

    }

    @Override
    public String toString() {
        String text = getText();
        String substring = text.length() > 25 ? text.substring(0, 20) + "..." : text;
        return this.getClass().getSimpleName() + " (" + substring + ")";
    }
}
