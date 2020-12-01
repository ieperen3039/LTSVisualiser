package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.InputHandling.MouseClickListener;
import NG.InputHandling.MouseReleaseListener;
import NG.Tools.Logger;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.Collection;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * A Button that may execute actions for both left and right clicks upon release.
 * @author Geert van Ieperen. Created on 22-9-2018.
 */
public class SButton extends STextComponent implements MouseReleaseListener, MouseClickListener {
    public static final int DEFAULT_MIN_WIDTH = 250;
    public static final int DEFAULT_MIN_HEIGHT = 30;
    public static final NGFonts.TextType DEFAULT_TEXT_TYPE = NGFonts.TextType.REGULAR;

    private Collection<Runnable> leftClickListeners = new ArrayList<>();
    private Collection<Runnable> rightClickListeners = new ArrayList<>();

    private boolean isPressed = false;

    /**
     * a button with no associated action (a dead button)
     * @param text the text of the button
     * @see #addLeftClickListener(Runnable)
     */
    public SButton(String text) {
        super(text, DEFAULT_TEXT_TYPE, SFrameLookAndFeel.Alignment.CENTER_MIDDLE, DEFAULT_MIN_WIDTH, DEFAULT_MIN_HEIGHT);
    }

    /**
     * a button with a basic associated action
     * @param text   the text of the button
     * @param action the action that is executed upon (releasing a) left click
     */
    public SButton(String text, Runnable action) {
        this(text);
        addLeftClickListener(action);
    }

    /**
     * a button with no associated action (a dead button)
     * @param text  the text of the button
     * @param props component properties
     * @see #addLeftClickListener(Runnable)
     */
    public SButton(String text, SComponentProperties props) {
        super(text, props);
    }

    /**
     * a button with a basic associated action
     * @param text   the text of the button
     * @param action the action that is executed upon (releasing a) left click
     */
    public SButton(String text, Runnable action, SComponentProperties props) {
        this(text, props);
        leftClickListeners.add(action);
    }

    /**
     * a button with both a left and a right click action
     * @param text         the text of the button
     * @param onLeftClick  the action that is executed upon (releasing a) left click
     * @param onRightClick the action that is executed upon (releasing a) right click
     */
    public SButton(String text, Runnable onLeftClick, Runnable onRightClick, SComponentProperties props) {
        this(text, onLeftClick, props);
        rightClickListeners.add(onRightClick);
    }

    public STextComponent addLeftClickListener(Runnable action) {
        leftClickListeners.add(action);
        return this;
    }

    public STextComponent addRightClickListeners(Runnable action) {
        rightClickListeners.add(action);
        return this;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(isPressed ? BUTTON_PRESSED : (isHovered ? BUTTON_HOVERED : BUTTON_ACTIVE), screenPosition, getSize());
        super.draw(design, screenPosition);
    }

    @Override
    public void onClick(int button, int xSc, int ySc) {
        isPressed = true;
    }

    @Override
    public void onRelease(int button) {
        isPressed = false;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            leftClickListeners.forEach(Runnable::run);

        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            rightClickListeners.forEach(Runnable::run);

        } else {
            Logger.DEBUG.print("button clicked with " + button + " which has no action");
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + getText() + ")";
    }
}
