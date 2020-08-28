package NG.Core;

import NG.Camera.Camera;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.Graph.Graph;
import NG.Graph.SpringLayout;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.MouseToolCallbacks;
import NG.Rendering.GLFWWindow;
import NG.Settings.Settings;
import NG.Tools.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * A collection of references to any major element of the game.
 * @author Geert van Ieperen. Created on 16-9-2018.
 */
public interface Root {
    void onNodePositionChange();

    Camera camera();

    Settings settings();

    GLFWWindow window();

    MouseToolCallbacks inputHandling();

    UIFrameManager gui();

    KeyControl keyControl();

    Version getVersionNumber();

    Graph graph();

    SpringLayout getSpringLayout();

    /**
     * Schedules the specified action to be executed in the OpenGL context. The action is guaranteed to be executed
     * before two frames have been rendered.
     * @param action the action to execute
     */
    void executeOnRenderThread(Runnable action);

    /**
     * Schedules the specified action to be executed in the OpenGL context. The action is guaranteed to be executed
     * before two frames have been rendered.
     * @param action the action to execute
     * @param <V>    the return type of action
     * @return a reference to obtain the result of the execution, or null if it threw an exception
     */
    default <V> Future<V> computeOnRenderThread(Callable<V> action) {
        FutureTask<V> task = new FutureTask<>(() -> {
            try {
                return action.call();

            } catch (Exception ex) {
                Logger.ERROR.print(ex);
                return null;
            }
        });

        executeOnRenderThread(task);
        return task;
    }

    Graph getVisibleGraph();
}
