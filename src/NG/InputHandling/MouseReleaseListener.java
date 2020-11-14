package NG.InputHandling;

/**
 * @author Geert van Ieperen. Created on 24-9-2018.
 */
public interface MouseReleaseListener {

    /**
     * is fired whenever the mouse is released.
     * @param button the button that was previously pressed
     */
    void onRelease(int button);
}
