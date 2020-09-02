package NG.Core;

import NG.Camera.Camera;
import NG.Camera.FlatCamera;
import NG.Camera.PointCenteredCamera;
import NG.GUIMenu.FrameManagers.FrameManagerImpl;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Menu;
import NG.Graph.*;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.EdgeShader;
import NG.Graph.Rendering.NodeMesh;
import NG.Graph.Rendering.NodeShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.MouseToolCallbacks;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.Rendering.Shaders.SGL;
import NG.Settings.Settings;
import NG.Tools.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * A game of planning and making money.
 * <p>
 * This class initializes all gameAspects, allow for starting a game, loading mods and cleaning up afterwards. It
 * provides all aspects of the game engine through the {@link Main} interface.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Main {
    private static final Version GAME_VERSION = new Version(0, 1);
    public static final int INITIAL_VIEW_DIST = 100;

    public final RenderLoop renderer;
    private final UIFrameManager frameManager;
    private final SpringLayout springLayout;
    private final Settings settings;
    private final GLFWWindow window;
    private final MouseToolCallbacks inputHandler;
    private final KeyControl keyControl;
    private Camera camera;
    private final Thread mainThread;

    public NodeClustering nodeCluster;
    private final AutoLock graphLock = new AutoLock.Instance();
    private SourceGraph graph;
    private boolean doComputeSourceLayout = true;
    private ClusterMethod clusterMethod = ClusterMethod.NO_CLUSTERING;
    private Menu menu;

    public enum ClusterMethod {
        NO_CLUSTERING, TAU, EDGE_ATTRIBUTE
    }

    public Main() throws IOException {
        Logger.INFO.print("Starting up...");

        Logger.DEBUG.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tTool version:       " + getVersionNumber() +
                "\n\tWorking directory:  " + Directory.workDirectory()
        );

        // these are not GameAspects, and thus the init() rule does not apply.
        settings = new Settings();
        GLFWWindow.Settings videoSettings = new GLFWWindow.Settings(settings);

        window = new GLFWWindow(Settings.GAME_NAME, videoSettings, true);
        renderer = new RenderLoop(settings.TARGET_FPS);
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        mainThread = Thread.currentThread();
        camera = new PointCenteredCamera(Vectors.O);
        nodeCluster = new NodeClustering();

        graph = new SourceGraph(0, 0);
        springLayout = new SpringLayout();
    }

    /**
     * start all elements required for showing the main frame of the game.
     * @throws Exception when the initialisation fails.
     */
    public void init() throws Exception {
        Logger.DEBUG.print("Initializing...");
        // init all fields
        renderer.init(this);
        inputHandler.init(this);
        frameManager.init(this);
        springLayout.init(this);
        camera.init(this);
        graph.init(this);

        // read graph
        nodeCluster.init(this);

        renderer.renderSequence(new NodeShader())
                .add(this::renderNodes);

        renderer.renderSequence(new EdgeShader())
                .add(this::renderEdges);

        renderer.addHudItem(frameManager::draw);

        menu = new Menu(this);
        frameManager.setMainGUI(menu);

        springLayout.addUpdateListeners(this::onNodePositionChange);

        Logger.INFO.print("Finished initialisation\n");
    }

    public void root() throws Exception {
        init();
        Logger.INFO.print("Starting tool...\n");

        window.open();
        springLayout.start();

        renderer.run();

        window.close();
        springLayout.stopLoop();

        cleanup();
    }

    public void onNodePositionChange() {
        if (doComputeSourceLayout) {
            nodeCluster.pullClusterPositions();
        } else {
            nodeCluster.pushClusterPositions();
        }

        executeOnRenderThread(() -> {
            Graph graph = getVisibleGraph();
            graph.getNodeMesh().reload();
            graph.getEdgeMesh().reload();
        });
    }

    public Camera camera() {
        return camera;
    }

    public Settings settings() {
        return settings;
    }

    public GLFWWindow window() {
        return window;
    }

    public MouseToolCallbacks inputHandling() {
        return inputHandler;
    }

    public KeyControl keyControl() {
        return keyControl;
    }

    public Version getVersionNumber() {
        return GAME_VERSION;
    }

    public Graph graph() {
        return graph;
    }

    public SpringLayout getSpringLayout() {
        return springLayout;
    }

    /**
     * Schedules the specified action to be executed in the OpenGL context. The action is guaranteed to be executed
     * before two frames have been rendered.
     * @param action the action to execute
     */
    public void executeOnRenderThread(Runnable action) {
        if (Thread.currentThread() == mainThread) {
            action.run();
        } else {
            renderer.defer(action);
        }
    }

    public UIFrameManager gui() {
        return frameManager;
    }

    private void cleanup() {
        inputHandler.cleanup();
        graph.cleanup();
        window.cleanup();
    }

    public void setGraphSafe(File newGraph) {
        try {
            LTSParser ltsParser = new LTSParser(newGraph);

            try (AutoLock.Section section = graphLock.open()) {
                graph.cleanup();
                graph = ltsParser.get();
                graph.init(this);

                springLayout.setGraph(graph);

                nodeCluster = new NodeClustering();
                nodeCluster.init(this);
                onNodePositionChange();
            }

            springLayout.setGraph(graph);

            Logger.DEBUG.print("Loaded graph with " + graph.nodes.length + " nodes");

        } catch (IOException e) {
            Logger.ERROR.print(newGraph.getName(), e);
        }
    }

    public void doSourceLayout(boolean doSource) {
        doComputeSourceLayout = doSource;

        springLayout.setGraph(doComputeSourceLayout ? graph : nodeCluster);
    }

    public void setClusterMethod(ClusterMethod method) {
        this.clusterMethod = method;

        onNodePositionChange();
    }

    private void renderEdges(SGL gl, Main root) {
        try (AutoLock.Section section = graphLock.open()) {
            Graph target = getVisibleGraph();
            gl.render(target.getEdgeMesh());
        }
    }

    private void renderNodes(SGL gl, Main root) {
        try (AutoLock.Section section = graphLock.open()) {
            Graph target = getVisibleGraph();
            gl.render(target.getNodeMesh());
        }
    }

    public int getClickShaderResult() {
        try {
            return computeOnRenderThread(renderer::getClickShaderResult).get();

        } catch (InterruptedException | ExecutionException e) {
            Logger.ERROR.print(e);
            return -1;
        }
    }

    public Graph getVisibleGraph() {
        return (clusterMethod == ClusterMethod.NO_CLUSTERING) ? graph : nodeCluster;
    }

    public void set3DView(boolean on) {
        if (!on) {
            Graph graph = doComputeSourceLayout ? this.graph : nodeCluster;

            // flatten graph in view direction
            Matrix4f viewMatrix = new Matrix4f().lookAt(
                    camera.getEye(),
                    camera.getFocus(),
                    camera.getUpVector()
            );
            for (NodeMesh.Node node : graph.getNodeMesh().nodeList()) {
                node.position.mulPosition(viewMatrix);
                node.position.z = 0;
            }
            for (EdgeMesh.Edge edge : graph.getEdgeMesh().edgeList()) {
                edge.handlePos.mulPosition(viewMatrix);
                edge.handlePos.z = 0;
            }
            float viewDist = camera.vectorToFocus().length();
            camera = new FlatCamera(new Vector3f(0, 0, viewDist));
            camera.init(this);

            springLayout.setAllow3D(false);

        } else {
            springLayout.setAllow3D(true);
            camera = new PointCenteredCamera(camera.getFocus(), camera.getEye());
            camera.init(this);

            for (NodeMesh.Node node : graph.getNodeMesh().nodeList()) {
                node.position.z += Toolbox.randomBetween(-1, 1);
            }
        }

        onNodePositionChange();
    }

    public void toggleClusterAttribute(String label) {
        String[] actionLabels = menu.actionLabels;
        for (int i = 0; i < actionLabels.length; i++) {
            if (label.equals(actionLabels[i])) {
                menu.clusterButtons[i].toggle();
            }
        }
    }

    /**
     * Schedules the specified action to be executed in the OpenGL context. The action is guaranteed to be executed
     * before two frames have been rendered.
     * @param action the action to execute
     * @param <V>    the return type of action
     * @return a reference to obtain the result of the execution, or null if it threw an exception
     */
    public <V> Future<V> computeOnRenderThread(Callable<V> action) {
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
}
