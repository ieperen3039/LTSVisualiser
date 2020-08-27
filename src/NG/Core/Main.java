package NG.Core;

import NG.Camera.Camera;
import NG.Camera.PointCenteredCamera;
import NG.GUIMenu.Components.SComponent;
import NG.GUIMenu.FrameManagers.FrameManagerImpl;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Menu;
import NG.Graph.*;
import NG.Graph.Rendering.EdgeShader;
import NG.Graph.Rendering.NodeShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.MouseToolCallbacks;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.RenderLoop;
import NG.Settings.Settings;
import NG.Tools.AutoLock;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;

import java.io.File;
import java.io.IOException;

/**
 * A game of planning and making money.
 * <p>
 * This class initializes all gameAspects, allow for starting a game, loading mods and cleaning up afterwards. It
 * provides all aspects of the game engine through the {@link Root} interface.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Main implements Root {
    private static final Version GAME_VERSION = new Version(0, 1);

    public final RenderLoop renderer;
    public final UIFrameManager frameManager;
    private final SpringLayout updateLoop;
    private final Settings settings;
    private final GLFWWindow window;
    private final MouseToolCallbacks inputHandler;
    private final KeyControl keyControl;
    private final Camera camera;
    private final Thread mainThread;

    public NodeClustering nodeCluster;
    private final AutoLock graphLock = new AutoLock.Instance();
    private SourceGraph graph;
    private boolean doComputeSourceLayout = true;
    private ClusterMethod clusterMethod = ClusterMethod.NO_CLUSTERING;

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
        updateLoop = new SpringLayout();
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
        updateLoop.init(this);
        camera.init(this);
        graph.init(this);

        // read graph
        nodeCluster.init(this);

        renderer.renderSequence(new EdgeShader())
                .add(this::renderEdges);
        renderer.renderSequence(new NodeShader())
                .add(this::renderNodes);

        // GUIs
        renderer.addHudItem(frameManager::draw);

        SComponent menu = new Menu(this);
        frameManager.setMainGUI(menu);

        updateLoop.addUpdateListeners(this::onNodePositionChange);

        Logger.INFO.print("Finished initialisation\n");
    }

    public void root() throws Exception {
        init();
        Logger.INFO.print("Starting tool...\n");

        window.open();
        updateLoop.start();

        renderer.run();

        window.close();
        updateLoop.stopLoop();

        cleanup();
    }

    @Override
    public void onNodePositionChange() {
        try (AutoLock.Section section = graphLock.open()) {
            if (doComputeSourceLayout) {
                nodeCluster.pullClusterPositions();
            } else {
                nodeCluster.pushClusterPositions();
            }
        }

        executeOnRenderThread(() -> {
            Graph graph = getVisibleGraph();
            graph.getNodeMesh().reload();
            graph.getEdgeMesh().reload();
        });
    }

    @Override
    public Camera camera() {
        return camera;
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public GLFWWindow window() {
        return window;
    }

    @Override
    public MouseToolCallbacks inputHandling() {
        return inputHandler;
    }

    @Override
    public KeyControl keyControl() {
        return keyControl;
    }

    @Override
    public Version getVersionNumber() {
        return GAME_VERSION;
    }

    @Override
    public Graph graph() {
        return graph;
    }

    @Override
    public SpringLayout getUpdateLoop() {
        return updateLoop;
    }

    @Override
    public void executeOnRenderThread(Runnable action) {
        if (Thread.currentThread() == mainThread) {
            action.run();
        } else {
            renderer.defer(action);
        }
    }

    @Override
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

                updateLoop.setGraph(graph);

                nodeCluster = new NodeClustering();
                nodeCluster.init(this);
                onNodePositionChange();
            }

            Logger.DEBUG.print("Loaded graph with " + graph.nodes.length + " nodes");

        } catch (IOException e) {
            Logger.ERROR.print(e);
        }
    }

    public void doSourceLayout(boolean doSource) {
        doComputeSourceLayout = doSource;

        updateLoop.setGraph(doComputeSourceLayout ? graph : nodeCluster);
    }

    public void setClusterMethod(ClusterMethod method) {
        this.clusterMethod = method;

        onNodePositionChange();
    }

    private void renderEdges(SGL gl, Root root) {
        try (AutoLock.Section section = graphLock.open()) {
            Graph target = getVisibleGraph();
            gl.render(target.getEdgeMesh());
        }
    }

    private void renderNodes(SGL gl, Root root) {
        try (AutoLock.Section section = graphLock.open()) {
            Graph target = getVisibleGraph();
            gl.render(target.getNodeMesh());
        }
    }

    @Override
    public Graph getVisibleGraph() {
        return (clusterMethod == ClusterMethod.NO_CLUSTERING) ? graph : nodeCluster;
    }
}
