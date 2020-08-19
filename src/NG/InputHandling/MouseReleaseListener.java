package NG.InputHandling;

/**
 * @author Geert van Ieperen. Created on 24-9-2018.
 */
public interface MouseReleaseListener {

    /**
     * is fired whenever the mouse is released.
     * @param button the button that was previously pressed
     * @param xSc    the x position in screen coordinate
     * @param ySc    the y position in screen coordinates
     */
    void onRelease(int button, int xSc, int ySc);
}
