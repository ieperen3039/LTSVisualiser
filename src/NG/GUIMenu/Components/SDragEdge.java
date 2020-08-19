package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.InputHandling.MouseDragListener;
import org.joml.Vector2ic;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.BUTTON_ACTIVE;

/**
 * @author Geert van Ieperen. Created on 25-9-2018.
 */
public class SDragEdge extends SComponent implements MouseDragListener {
    private final SComponent parent;
    private final int width;
    private final int height;

    public SDragEdge(SComponent parent, int width, int height) {
        this.width = width;
        this.height = height;
        this.parent = parent;
        setSize(width, height);
        setGrowthPolicy(false, false);
    }

    @Override
    public int minWidth() {
        return width;
    }

    @Override
    public int minHeight() {
        return height;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(BUTTON_ACTIVE, screenPosition, getSize());
        design.drawText(screenPosition, getSize(), "+", NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.LEFT);
    }

    @Override
    public void mouseDragged(int xDelta, int yDelta, float xPos, float yPos) {
        parent.addToSize(xDelta, yDelta);
    }
}
