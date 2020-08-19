package NG.InputHandling;

/**
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public interface KeyTypeListener {
    /**
     * is invoked whenever a key is typed
     * @param letter the new character that was typed
     */
    void keyTyped(char letter);
}
