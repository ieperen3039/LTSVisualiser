package NG.GUIMenu.Rendering;

import NG.Core.Main;
import NG.Core.Version;
import NG.DataStructures.Generic.Color4f;
import NG.Tools.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.EnumSet;

import static NG.GUIMenu.Rendering.NGFonts.LUCIDA_CONSOLE;
import static NG.GUIMenu.Rendering.NVGOverlay.Alignment.*;

/**
 * Little more than the absolute basic appearance of a GUI
 * @author Geert van Ieperen. Created on 21-9-2018.
 */
public class WindowsLF implements SFrameLookAndFeel {
    private static final int INDENT = 1;
    private static final int BUTTON_INDENT = 2;
    private static final int STROKE_WIDTH = 1;
    private static final int TEXT_SIZE_REGULAR = 12;
    private static final int TEXT_SIZE_LARGE = 16;

    private static final NGFonts FONT = LUCIDA_CONSOLE;

    private static final Color4f TEXT_COLOR = Color4f.BLACK;
    private static final Color4f PANEL_COLOR = Color4f.WHITE;
    private static final Color4f STROKE_COLOR = Color4f.BLACK;
    private static final Color4f BUTTON_COLOR = Color4f.LIGHT_GREY;
    private static final Color4f SELECTION_COLOR = BUTTON_COLOR.darken(0.1f);
    private static final Color4f INPUT_FIELD_COLOR = Color4f.LIGHT_GREY;

    private NVGOverlay.Painter hud;

    @Override
    public void init(Main root) {
        if (!root.getVersionNumber().isLessThan(2, 0)) {
            Logger.ASSERT.print(this + " is ugly. Please install something better");
        }
    }

    @Override
    public NVGOverlay.Painter getPainter() {
        return hud;
    }

    @Override
    public void setPainter(NVGOverlay.Painter painter) {
        this.hud = painter;
        painter.setFillColor(PANEL_COLOR);
        painter.setStroke(STROKE_COLOR, STROKE_WIDTH);
    }

    @Override
    public int getTextWidth(String text, NGFonts.TextType textType) {
        int actualSize = TEXT_SIZE_REGULAR;

        if (textType == NGFonts.TextType.TITLE || textType == NGFonts.TextType.ACCENT) {
            actualSize = TEXT_SIZE_LARGE;
        }

        return hud.getTextWidth(text, actualSize, FONT);
    }

    @Override
    public void draw(UIComponent type, Vector2ic pos, Vector2ic dim, Color4f color) {
        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();
        assert width > 0 && height > 0 : String.format("Non-positive dimensions: height = %d, width = %d", height, width);

        switch (type) {
            case SCROLL_BAR_BACKGROUND:
                break;

            case BUTTON_ACTIVE:
            case BUTTON_INACTIVE:
            case SCROLL_BAR_DRAG_ELEMENT:
                Color4f thisColor = color == null ? BUTTON_COLOR : color;
                drawButtonRectangle(x, y, width, height, thisColor);
                break;

            case BUTTON_HOVERED:
                Color4f thisColor1 = color == null ? SELECTION_COLOR.intensify(0.1f) : color;
                drawButtonRectangle(x, y, width, height, thisColor1);
                break;

            case BUTTON_PRESSED:
                Color4f thisColor2 = color == null ? BUTTON_COLOR.darken(0.5f) : color;
                drawButtonRectangle(x, y, width, height, thisColor2);
                break;

            case INPUT_FIELD:
                Color4f thisColor3 = color == null ? INPUT_FIELD_COLOR : color;
                drawButtonRectangle(x, y, width, height, thisColor3);
                break;

            case SELECTION:
                hud.setStroke(STROKE_COLOR, 0);
                Color4f thisColor4 = color == null ? SELECTION_COLOR : color;
                drawButtonRectangle(x, y, width, height, thisColor4);
                break;

            case DROP_DOWN_HEAD_CLOSED:
            case DROP_DOWN_HEAD_OPEN:
            case DROP_DOWN_OPTION_FIELD:
            case PANEL:
            case FRAME_HEADER:
            default:
                drawRoundedRectangle(x, y, width, height, color == null ? Color4f.WHITE : color);
        }
    }

    private void drawButtonRectangle(int x, int y, int width, int height, Color4f color) {
        int xMax2 = x + width;
        int yMax2 = y + height;

        hud.polygon(color, STROKE_COLOR, STROKE_WIDTH,
                new Vector2i(x + BUTTON_INDENT, y),
                new Vector2i(xMax2 - BUTTON_INDENT, y),
                new Vector2i(xMax2, y + BUTTON_INDENT),
                new Vector2i(xMax2, yMax2 - BUTTON_INDENT),
                new Vector2i(xMax2 - BUTTON_INDENT, yMax2),
                new Vector2i(x + BUTTON_INDENT, yMax2),
                new Vector2i(x, yMax2 - BUTTON_INDENT),
                new Vector2i(x, y + BUTTON_INDENT)
        );
    }

    private void drawRoundedRectangle(int x, int y, int width, int height, Color4f color) {
        int xMax = x + width;
        int yMax = y + height;

        hud.polygon(color, STROKE_COLOR, STROKE_WIDTH,
                new Vector2i(x + INDENT, y),
                new Vector2i(xMax - INDENT, y),
                new Vector2i(xMax, y + INDENT),
                new Vector2i(xMax, yMax - INDENT),
                new Vector2i(xMax - INDENT, yMax),
                new Vector2i(x + INDENT, yMax),
                new Vector2i(x, yMax - INDENT),
                new Vector2i(x, y + INDENT)
        );
    }

    @Override
    public void drawText(
            Vector2ic pos, Vector2ic dim, String text, NGFonts.TextType type, Alignment align
    ) {
        if (text == null || text.isEmpty()) return;

        int x = pos.x();
        int y = pos.y();
        int width = dim.x();
        int height = dim.y();
        int actualSize = TEXT_SIZE_REGULAR;
        Color4f textColor = TEXT_COLOR;
        NGFonts font = FONT;

        switch (type) {
            case TITLE:
            case ACCENT:
                actualSize = TEXT_SIZE_LARGE;
                break;
            case RED:
                textColor = new Color4f(0.8f, 0.1f, 0.1f);
                break;
        }

        switch (align) {
            case LEFT_MIDDLE:
                hud.text(x, y + (height / 2), actualSize,
                        font, EnumSet.of(ALIGN_LEFT), textColor, text, width
                );
                break;
            case LEFT_TOP:
                hud.text(x, y, actualSize,
                        font, EnumSet.of(ALIGN_TOP, ALIGN_LEFT), textColor, text, width
                );
                break;
            case CENTER_MIDDLE:
                hud.text(x, y + (height / 2), actualSize,
                        font, EnumSet.noneOf(NVGOverlay.Alignment.class), textColor, text, width
                );
                break;
            case CENTER_TOP:
                hud.text(x, y, actualSize,
                        font, EnumSet.of(ALIGN_TOP), textColor, text, width
                );
                break;
            case RIGHT_MIDDLE:
                hud.text(x, y + (height / 2), actualSize,
                        font, EnumSet.of(ALIGN_RIGHT), textColor, text, width
                );
                break;
            case RIGHT_TOP:
                hud.text(x, y, actualSize,
                        font, EnumSet.of(ALIGN_TOP, ALIGN_RIGHT), textColor, text, width
                );
                break;
            default:
                throw new IllegalArgumentException(align.toString());
        }
    }

    @Override
    public void cleanup() {
        hud = null;
    }

    @Override
    public Version getVersionNumber() {
        return new Version(0, 0);
    }
}
