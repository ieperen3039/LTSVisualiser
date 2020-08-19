package NG.InputHandling;

/**
 * @author Geert van Ieperen. Created on 7-10-2018.
 */
public interface MouseClickListener {
    /**
     * Whenever the user clicks on this component, this is invoked with the used mouse button and relative positions
     * @param button the mouse button used for clicking, see {@link org.lwjgl.glfw.GLFW GLFW}
     * @param xRel   position in pixels relative to the position of this object
     * @param yRel   position in pixels relative to the position of this object
     */
    void onClick(int button, int xRel, int yRel);
}
