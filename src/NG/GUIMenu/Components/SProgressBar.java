package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.function.Supplier;

import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.PANEL;
import static NG.GUIMenu.Rendering.SFrameLookAndFeel.UIComponent.SELECTION;

/**
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public class SProgressBar extends SComponent {
    private final Supplier<Float> progress;
    private int minWidth;
    private int minHeight;

    public SProgressBar(Supplier<Float> progress, SComponentProperties properties) {
        this.progress = progress;
        this.minWidth = properties.minWidth;
        this.minHeight = properties.minHeight;
        setGrowthPolicy(properties.wantHzGrow, properties.wantVtGrow);
    }

    public SProgressBar(int minWidth, int minHeight, Supplier<Float> progressSource) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.progress = progressSource;
        setGrowthPolicy(false, false);
    }

    @Override
    public int minWidth() {
        return minWidth;
    }

    @Override
    public int minHeight() {
        return minHeight;
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        design.draw(PANEL, screenPosition, getSize());
        Float heath = progress.get();

        if (heath > 0) {
            Vector2i bar = new Vector2i((int) (getWidth() * heath), getHeight());
            design.draw(SELECTION, screenPosition, bar);
        }
    }
}
