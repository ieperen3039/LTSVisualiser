package NG.Core;

import NG.Camera.Camera;
import NG.Camera.FlatCamera;
import NG.Camera.PointCenteredCamera;
import NG.GUIMenu.Components.SComponent;
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
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.RenderLoop;
import NG.Settings.Settings;
import NG.Tools.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    public static final int INITIAL_VIEW_DIST = 100;

    public final RenderLoop renderer;
    public final UIFrameManager frameManager;
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

        renderer.renderSequence(new EdgeShader())
                .add(this::renderEdges);
        renderer.renderSequence(new NodeShader())
                .add(this::renderNodes);

        renderer.addHudItem(frameManager::draw);

        SComponent menu = new Menu(this);
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
    public SpringLayout getSpringLayout() {
        return springLayout;
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
}
