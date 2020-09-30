package NG.Core;

import NG.Camera.Camera;
import NG.Camera.FlatCamera;
import NG.Camera.PointCenteredCamera;
import NG.GUIMenu.Components.SToggleButton;
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
import NG.Resources.LazyInit;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.lwjgl.opengl.GL11.glDepthMask;

/**
 * A tool for visualising large graphs
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Main {
    private static final Version GAME_VERSION = new Version(0, 1);
    public static final int MAX_ITERATIONS_PER_SECOND = 50;
    private static final int NUM_WORKER_THREADS = 3;
    private final Thread mainThread;

    public final RenderLoop renderer;
    private final UIFrameManager frameManager;
    private final SpringLayout springLayout;
    private final Settings settings;
    private final GLFWWindow window;
    private final MouseToolCallbacks inputHandler;
    private final KeyControl keyControl;
    private Camera camera;
    private Menu menu;

    private boolean doComputeSourceLayout = false;
    private final LazyInit<GraphComparator> graphComparator;

    private final Object graphLock = new Object();
    private SourceGraph graph;
    private SourceGraph secondGraph;
    private final LazyInit<NodeClustering> nodeCluster;
    private final LazyInit<IgnoringGraph> subGraph;
    private DisplayMethod displayMethod = DisplayMethod.HIGHLIGHT_ACTIONS;

    public enum DisplayMethod {
        HIGHLIGHT_ACTIONS, SHOW_SECONDARY_GRAPH, COMPARE_GRAPHS, HIDE_ACTIONS, CLUSTER_ON_SELECTED, CLUSTER_ON_SELECTED_IGNORE_LOOPS
    }

    public Main(Settings settings) throws IOException {
        Logger.INFO.print("Starting up...");

        Logger.DEBUG.print("General debug information: " +
                // manual aligning will do the trick
                "\n\tSystem OS:          " + System.getProperty("os.name") +
                "\n\tJava VM:            " + System.getProperty("java.runtime.version") +
                "\n\tTool version:       " + getVersionNumber() +
                "\n\tWorking directory:  " + Directory.workDirectory()
        );

        // these are not GameAspects, and thus the init() rule does not apply.
        this.settings = settings;
        GLFWWindow.Settings videoSettings = new GLFWWindow.Settings(settings);

        window = new GLFWWindow(Settings.GAME_NAME, videoSettings, true);
        renderer = new RenderLoop(this.settings.TARGET_FPS);
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        mainThread = Thread.currentThread();
        camera = new PointCenteredCamera(Vectors.O);

        graph = new SourceGraph(this, 0, 0);
        secondGraph = new SourceGraph(this, 0, 0);
        springLayout = new SpringLayout(MAX_ITERATIONS_PER_SECOND, NUM_WORKER_THREADS);

        nodeCluster = new LazyInit<>(() -> new NodeClustering(graph), Graph::cleanup);
        graphComparator = new LazyInit<>(() -> new GraphComparator(graph, secondGraph), Graph::cleanup);
        subGraph = new LazyInit<>(() -> new IgnoringGraph(graph, getMarkedLabels()), Graph::cleanup);
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

        // init graphs
        graph.init();
        secondGraph.init();

        renderer.renderSequence(new NodeShader())
                .add(this::renderNodes);

        renderer.renderSequence(new EdgeShader())
                .add((gl1, root) -> {
                    glDepthMask(false); // read but not write
                    renderEdges(gl1, root);
                    glDepthMask(true);
                });

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
        if (displayMethod == DisplayMethod.CLUSTER_ON_SELECTED) {
            if (doComputeSourceLayout) {
                nodeCluster.get().pullClusterPositions();

            } else {
                nodeCluster.get().pushClusterPositions();
            }
        }

        if (displayMethod == DisplayMethod.COMPARE_GRAPHS && doComputeSourceLayout) {
            graphComparator.get().updateEdges();
        }

        if (displayMethod == DisplayMethod.HIDE_ACTIONS && doComputeSourceLayout) {
            subGraph.get().updateEdges();
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

    public UIFrameManager gui() {
        return frameManager;
    }

    private void cleanup() {
        synchronized (graphLock) {
            inputHandler.cleanup();
            graph.cleanup();
            window.cleanup();
        }
    }

    public void setGraph(File newGraphFile) {
        try {
            LTSParser ltsParser = new LTSParser(newGraphFile, this);
            setGraph(ltsParser.get());

        } catch (IOException e) {
            Logger.ERROR.print(newGraphFile.getName(), e);
        }
    }

    public void setGraph(SourceGraph newGraph) {
        springLayout.defer(() -> {
            synchronized (graphLock) {
                graph.cleanup();
                graph = newGraph;
                graph.init();

                springLayout.setGraph(doComputeSourceLayout ? graph : getVisibleGraph());

                nodeCluster.drop();
                graphComparator.drop();
                subGraph.drop();

                onNodePositionChange();
                Logger.INFO.print("Loaded graph with " + graph.nodes.length + " nodes and " + graph.edges.length + " edges");
            }

            menu.reloadUI();
        });
    }

    public void setSecondaryGraph(File newGraphFile) {
        try {
            LTSParser ltsParser = new LTSParser(newGraphFile, this);
            setSecondaryGraph(ltsParser.get());

        } catch (IOException e) {
            Logger.ERROR.print(newGraphFile.getName(), e);
        }
    }

    private void setSecondaryGraph(SourceGraph newGraph) {
        springLayout.defer(() -> {
            synchronized (graphLock) {
                secondGraph.cleanup();
                secondGraph = newGraph;
                secondGraph.init();

                graphComparator.drop();
            }
        });
    }

    public void doSourceLayout(boolean doSource) {
        doComputeSourceLayout = doSource;

        springLayout.setGraph(doSource ? graph : getVisibleGraph());
    }

    public void setDisplayMethod(DisplayMethod method) {
        this.displayMethod = method;

        if (method == DisplayMethod.CLUSTER_ON_SELECTED || method == DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS) {
            NodeClustering nodeClustering = nodeCluster.get();
            nodeClustering.setShowSelfLoop(method != DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS);

            String[] actionLabels = menu.actionLabels;
            SToggleButton[] attributeButtons = menu.attributeButtons;
            for (int i = 0; i < actionLabels.length; i++) {
                nodeClustering.addEdgeAttribute(actionLabels[i], attributeButtons[i].isActive());
            }
        }

        springLayout.setGraph(doComputeSourceLayout ? graph : getVisibleGraph());
        onNodePositionChange();
    }

    public Collection<String> getMarkedLabels() {
        Collection<String> markedActionLabels = new HashSet<>();
        String[] actionLabels = menu.actionLabels;
        SToggleButton[] attributeButtons = menu.attributeButtons;
        for (int i = 0; i < actionLabels.length; i++) {
            if (attributeButtons[i].isActive()) {
                markedActionLabels.add(actionLabels[i]);
            }
        }
        return markedActionLabels;
    }

    public List<EdgeMesh.Edge> getMarkedEdges() {
        Collection<String> markedActionLabels = getMarkedLabels();

        List<EdgeMesh.Edge> markedEdges = new ArrayList<>();
        for (EdgeMesh.Edge edge : graph.getEdgeMesh().edgeList()) {
            if (markedActionLabels.contains(edge.label)) {
                markedEdges.add(edge);
            }
        }

        return markedEdges;
    }

    private void renderEdges(SGL gl, Main root) {
        synchronized (graphLock) {
            Graph target = getVisibleGraph();
            gl.render(target.getEdgeMesh());
        }
    }

    private void renderNodes(SGL gl, Main root) {
        synchronized (graphLock) {
            Graph target = getVisibleGraph();
            gl.render(target.getNodeMesh());
        }
    }

    public int getClickShaderResult() {
        return renderer.getClickShaderResult();
    }

    public Graph getVisibleGraph() {
        switch (displayMethod) {
            case HIGHLIGHT_ACTIONS:
                return graph;

            case SHOW_SECONDARY_GRAPH:
                return secondGraph;

            case HIDE_ACTIONS:
                return subGraph.get();

            case CLUSTER_ON_SELECTED:
            case CLUSTER_ON_SELECTED_IGNORE_LOOPS:
                return nodeCluster.get();

            case COMPARE_GRAPHS:
                return graphComparator.get();

            default:
                assert false : displayMethod;
                return graph;
        }
    }

    public void set3DView(boolean on) {
        if (!on) {
            Graph graph = doComputeSourceLayout ? this.graph : getVisibleGraph();

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
                menu.attributeButtons[i].toggle();
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

    public void selectAttribute(String label, boolean on) {
        if (on) {
            graph.setAttributeColor(label, Menu.EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE);
        } else {
            graph.forEachAttribute(label, e -> e.resetColor(GraphElement.Priority.ATTRIBUTE));
        }

        nodeCluster.ifPresent(g -> g.addEdgeAttribute(label, on));
        subGraph.ifPresent(g -> g.update(getMarkedLabels()));
    }
}
