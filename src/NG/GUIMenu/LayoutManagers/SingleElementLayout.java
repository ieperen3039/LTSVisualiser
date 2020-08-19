package NG.GUIMenu.LayoutManagers;

import NG.GUIMenu.Components.SComponent;
import org.joml.Vector2ic;

import java.util.Collection;
import java.util.Collections;

/**
 * A layout with only one element. The property class is Void (always null).
 * <p>
 * The {@link #add(SComponent, Object)} method will replace the given component with the new component
 * @author Geert van Ieperen. Created on 29-10-2018.
 */
public class SingleElementLayout implements SLayoutManager {
    private SComponent target;

    @Override
    public void add(SComponent comp, Object prop) {
        target = comp;
    }

    @Override
    public void remove(SComponent comp) {
        if (comp == target) {
            target = null;
        }
    }

    @Override
    public void recalculateProperties() {
    }

    @Override
    public void placeComponents(Vector2ic position, Vector2ic dimensions) {
        if (target == null) return;
        target.setPosition(position);
        target.setSize(dimensions.x(), dimensions.y());
    }

    @Override
    public Collection<SComponent> getComponents() {
        if (target == null) return Collections.emptyList();
        return Collections.singleton(target);
    }

    @Override
    public int getMinimumWidth() {
        if (target == null) return 0;
        return target.minWidth();
    }

    @Override
    public int getMinimumHeight() {
        if (target == null) return 0;
        return target.minHeight();
    }

    @Override
    public Class<?> getPropertyClass() {
        return null;
    }

    @Override
    public void clear() {
        target = null;
    }

    @Override
    public boolean wantHorizontalGrow() {
        if (target == null) return false;
        return target.wantHorizontalGrow();
    }

    @Override
    public boolean wantVerticalGrow() {
        if (target == null) return false;
        return target.wantVerticalGrow();
    }
}
