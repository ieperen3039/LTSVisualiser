package NG.InputHandling;

/**
 * A class that allows binding callbacks of key and mouse listeners.
 * @author Geert van Ieperen. Created on 18-11-2018.
 */
public interface KeyMouseCallbacks {
    /**
     * @param listener when the mouse moves, the {@link MouseMoveListener#onMouseMove(int, int, float, float)} method is
     *                 called
     */
    void addMousePositionListener(MouseMoveListener listener);

    void addMouseScrollListener(MouseScrollListener listener);

    /**
     * @param listener when a key is pressed, receives the {@link org.lwjgl.glfw.GLFW} key that is pressed
     */
    void addKeyPressListener(KeyPressListener listener);

    void addKeyReleaseListener(KeyReleaseListener listener);

    /**
     * Try to remove the given listener from all of the listener types, except for mouse move listeners. Even if the
     * given listener is of multiple types, all of them are removed.
     * @param listener a previously installed listener
     * @return true iff any listener has been removed
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    boolean removeListener(Object listener);
}
