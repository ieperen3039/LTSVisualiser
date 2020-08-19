package NG.InputHandling;

/**
 * @author Geert van Ieperen. Created on 20-9-2018.
 */
@FunctionalInterface
public interface KeyPressListener {
    /**
     * is called when a key is pressed
     * @param keyCode
     */
    void keyPressed(int keyCode);
}
