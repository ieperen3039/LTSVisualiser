package NG.GUIMenu.Components;

import NG.GUIMenu.LayoutManagers.LimitedVisibilityLayout;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * @author Geert van Ieperen created on 13-5-2019.
 */
public class STileBrowser extends SContainer {
    private static final int SCROLL_BUTTON_WIDTH = 50;
    private static final SComponentProperties BUTTON_PROPERTIES = new SComponentProperties(SCROLL_BUTTON_WIDTH, 0, false, true);
    private final STextComponent buttonFurther;
    private final STextComponent buttonBack;
    private LimitedVisibilityLayout layoutManager;

    public STileBrowser(boolean growPolicy, int minWidth, int eltWidth, SComponent... elements) {
        this(eltWidth, growPolicy, minWidth - 2 * SCROLL_BUTTON_WIDTH, elements);
    }

    private STileBrowser(int eltWidth, boolean growPolicy, int layoutWidth, SComponent... elements) {
        this(growPolicy, new LimitedVisibilityLayout(layoutWidth / eltWidth, layoutWidth, false), elements);
    }

    private STileBrowser(boolean growPolicy, LimitedVisibilityLayout layoutManager, SComponent... elements) {
        super(layoutManager);
        this.layoutManager = layoutManager;

        buttonBack = new SButton("<", () -> inc(-1), BUTTON_PROPERTIES);
        buttonFurther = new SButton(">", () -> inc(1), BUTTON_PROPERTIES);

        for (SComponent elt : elements) {
            layoutManager.add(elt, null);
        }

        layoutBorder.add(buttonBack.minWidth(), buttonFurther.minWidth(), 0, 0);
        setGrowthPolicy(growPolicy, growPolicy);
    }

    private void inc(int v) {
        this.layoutManager.shiftVisible(v);
        invalidateLayout();
    }

    /**
     * appends the given element to the end of the browser
     * @param elt the element to add
     */
    public void add(SComponent elt) {
        super.add(elt, null);
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        if (buttonBack.contains(xRel, yRel)) {
            Vector2ic position = buttonBack.getPosition();
            return buttonBack.getComponentAt(xRel - position.x(), yRel - position.y());

        } else if (buttonFurther.contains(xRel, yRel)) {
            Vector2ic position = buttonFurther.getPosition();
            return buttonFurther.getComponentAt(xRel - position.x(), yRel - position.y());

        } else {
            return super.getComponentAt(xRel, yRel);
        }
    }

    @Override
    public void doValidateLayout() {
        super.doValidateLayout();

        // use original layout border for side buttons instead
        ComponentBorder border = layoutBorder;
        int height = getHeight() - border.top - border.bottom;

        buttonFurther.setSize(0, height);
        buttonFurther.setPosition(this.getWidth() - border.right, border.top);
        buttonFurther.validateLayout();
        buttonBack.setSize(0, height);
        buttonBack.setPosition(border.left - buttonBack.getWidth(), border.top);
        buttonBack.validateLayout();
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        buttonBack.draw(design, new Vector2i(screenPosition).add(buttonBack.getPosition()));
        buttonFurther.draw(design, new Vector2i(screenPosition).add(buttonFurther.getPosition()));
        drawChildren(design, screenPosition);
    }

}
