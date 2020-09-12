package NG.InputHandling.MouseTools;

import NG.InputHandling.MouseListener;
import NG.Rendering.Shaders.SGL;

/**
 * Determines the behaviour of clicking
 * @author Geert van Ieperen. Created on 22-11-2018.
 */
public interface MouseTool extends MouseListener {

    /**
     * draws any visual indications used by this tool
     * @param gl the rendering context
     */
    void draw(SGL gl);

    /**
     * activates when this mousetool is deactivated
     */
    default void dispose() {
    }
}
