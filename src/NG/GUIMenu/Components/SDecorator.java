package NG.GUIMenu.Components;

import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import org.joml.Vector2ic;

import java.util.Collection;

/**
 * A helper class for building specific containers
 * @author Geert van Ieperen created on 21-2-2020.
 */
public abstract class SDecorator extends SComponent {
    private SContainer contents;

    public SDecorator() {
    }

    public SDecorator(SContainer panel) {
        setMainPanel(panel);
    }

    protected void setMainPanel(SContainer panel){
        contents = panel;
        panel.setParent(this);
    }

    protected void add(SComponent component, Object property) {
        contents.add(component, property);
    }

    protected void removeComponent(SComponent component) {
        contents.removeCompoment(component);
    }

    protected Collection<SComponent> getChildren() {
        return contents.children();
    }

    @Override
    public int minWidth() {
        return contents.minWidth();
    }

    @Override
    public int minHeight() {
        return contents.minHeight();
    }

    @Override
    public SComponent getComponentAt(int xRel, int yRel) {
        validateLayout();
        return contents.getComponentAt(xRel, yRel);
    }

    @Override
    public void doValidateLayout() {
        contents.setSize(getWidth(), getHeight());
        contents.validateLayout();
        setSize(getWidth(), getHeight());
    }

    @Override
    public void draw(SFrameLookAndFeel design, Vector2ic screenPosition) {
        contents.draw(design, screenPosition);
    }
}
