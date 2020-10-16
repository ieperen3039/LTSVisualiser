package NG.InputHandling.MouseTools;

import NG.Camera.Camera;
import NG.Core.Main;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.Graph.Graph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.InputHandling.MouseListener;
import NG.InputHandling.MouseReleaseListener;
import NG.InputHandling.MouseScrollListener;
import org.joml.Vector2i;

/**
 * @author Geert van Ieperen created on 24-4-2020.
 */
public abstract class MouseTool implements MouseListener {
    protected Main root;

    private MouseReleaseListener releaseListener;

    public MouseTool(Main root) {
        this.root = root;
        releaseListener = root.camera();
    }

    @Override
    public void onClick(int button, int x, int y) {
        if (root.gui().checkMouseClick(button, x, y)) {
            releaseListener = root.gui();
            return;
        }

        Graph graph = root.getVisibleGraph();
        boolean didClickGraph = graph.doOnMouseSelection(
                node -> onNodeClick(button, graph, node),
                edge -> onEdgeClick(button, graph, edge)
        );

        if (didClickGraph) {
            releaseListener = root.getVisibleGraph();

        } else {
            onAirClick(button, x, y);
        }
    }

    protected void onAirClick(int button, int x, int y) {
        Camera camera = root.camera();
        camera.onClick(button, x, y);
        releaseListener = camera;
    }

    public abstract void onNodeClick(int button, Graph graph, State node);

    public abstract void onEdgeClick(int button, Graph graph, Transition edge);

    @Override
    public void onRelease(int button, int xSc, int ySc) {
        // this prevents the case when a mouse-down caused a mouse tool switch
        if (releaseListener != null) {
            releaseListener.onRelease(button, xSc, ySc);
            releaseListener = null;
        }
    }

    @Override
    public void onScroll(float value) {
        Vector2i pos = root.window().getMousePosition();
        UIFrameManager gui = root.gui();

        SComponent component = gui.getComponentAt(pos.x, pos.y);

        if (component != null) {
            if (component instanceof MouseScrollListener) {
                MouseScrollListener listener = (MouseScrollListener) component;
                listener.onScroll(value);
            }

            return;
        }

        // camera
        root.camera().onScroll(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public final void mouseMoved(int xDelta, int yDelta, float xPos, float yPos) {
        root.gui().mouseMoved(xDelta, yDelta, xPos, yPos);
        if (root.gui().covers((int) xPos, (int) yPos)) return;

        root.camera().mouseMoved(xDelta, yDelta, xPos, yPos);
        root.getVisibleGraph().mouseMoved(xDelta, yDelta, xPos, yPos);
    }

    /**
     * activates when this mousetool is deactivated
     */
    public void dispose() {
    }
}
