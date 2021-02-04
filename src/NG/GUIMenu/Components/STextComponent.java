package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen created on 28-4-2020.
 */
public abstract class STextComponent extends SComponent {
    public static final int TEXT_MIN_X_BORDER = 15;
    protected final int minHeight;
    protected final int minWidth;
    protected final NGFonts.TextType textType;
    protected final SFrameLookAndFeel.Alignment alignment;

    private String text;

    /** minimum border to the left and right of the text */
    protected int minXBorder = TEXT_MIN_X_BORDER;
    protected int textWidth;
    private boolean textWidthIsInvalid = true;
    private int maximumCharacters = -1;

    public STextComponent(
            String text, NGFonts.TextType textType, SFrameLookAndFeel.Alignment alignment, int width, int height
    ) {
        this.text = text;
        this.minWidth = width;
        this.minHeight = height;
        this.textType = textType;
        this.alignment = alignment;
    }

    public STextComponent(String text, SComponentProperties props) {
        this(text, props.textType, props.alignment, props.minWidth, props.minHeight);
        setGrowthPolicy(props.wantHzGrow, props.wantVtGrow);
    }

    @Override
    public int minWidth() {
        return Math.max(textWidth + 2 * minXBorder, minWidth);
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    public void setXBorder(int minXBorder) {
        this.minXBorder = minXBorder;
    }

    public String getText() {
        return text;
    }

    /**
     * When set to any value larger than 0, the text will be at most this number of characters long. When getText()
     * returns a longer string, the string is cut, and the last characters are shown as ...
     * @param maximumCharacters the upper limit on the number of characters
     */
    public STextComponent setMaximumCharacters(int maximumCharacters) {
        this.maximumCharacters = maximumCharacters;
        return this;
    }

    public void setText(String text) {
        textWidthIsInvalid = !text.equals(this.text);
        this.text = text;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        String text = getText();

        if (maximumCharacters > 0 && text.length() > maximumCharacters) {
            text = text.substring(0, maximumCharacters - 3).concat("...");
        }

        if (textWidthIsInvalid) {
            invalidateLayout();
            textWidth = design.getTextWidth(text, textType);
            textWidthIsInvalid = false;

        } else {
            design.drawText(screenPosition, getSize(), text, textType, alignment);
        }
    }
}
