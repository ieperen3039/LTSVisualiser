package NG.GUIMenu.LayoutManagers;

import NG.GUIMenu.Components.SComponent;
import org.joml.Vector2ic;

import java.util.Collection;

/**
 * a layout manager with the same purpose as an {@link java.awt.LayoutManager}: it lays the components in a nice order.
 * @author Geert van Ieperen. Created on 21-9-2018.
 * @see GridLayoutManager
 */
public interface SLayoutManager {

    /**
     * adds a component to this layout manager. Every component that has been added must be returned by {@link
     * #getComponents()} (but not necessarily in order). Adding null values is not permitted. This does not invalidate
     * the layout.
     * @param comp the component to be added
     * @param prop the properties that describe position, policies etc.
     * @see #remove(SComponent)
     * @see #recalculateProperties()
     */
    void add(SComponent comp, Object prop);

    /**
     * inverts an action of {@link #add(SComponent, Object)}. After returning, the layout manager has no references to
     * the object. This does not invalidate the layout.
     * @param comp the component to be removed from the layout.
     */
    void remove(SComponent comp);

    /**
     * invalidates and recomputes the properties of the components, and thus the state of the layout manager. After
     * returning, all getters will return the correct value. Call {@link #placeComponents(Vector2ic, Vector2ic)} to let
     * the new layout have effect.
     */
    void recalculateProperties();

    /**
     * places the components of the layout at the previously set dimensions. When this method returns, all components
     * will be positioned and sized correctly
     * @param position   upper left position of the layout
     * @param dimensions size of the layout, as (width, height)
     */
    void placeComponents(Vector2ic position, Vector2ic dimensions);

    /**
     * @return an iterable view of the loaded components
     */
    Collection<SComponent> getComponents();

    /**
     * The returned value is only valid after a call to {@link #recalculateProperties()}
     * @return the minimum width of the components together in this layout
     */
    int getMinimumWidth();

    /**
     * The returned value is only valid after a call to {@link #recalculateProperties()}
     * @return the minimum height of the components together in this layout
     */
    int getMinimumHeight();

    /**
     * @return the properties class expected when adding elements
     * @see #add(SComponent, Object)
     */
    Class<?> getPropertyClass();

    /**
     * remove all elements from this layout
     */
    void clear();

    boolean wantHorizontalGrow();

    boolean wantVerticalGrow();
}
