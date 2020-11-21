package NG.Core;

import NG.Camera.Camera;
import NG.Camera.FlatCamera;
import NG.Camera.PointCenteredCamera;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.SToggleButton;
import NG.GUIMenu.FrameManagers.FrameManagerImpl;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Menu;
import NG.Graph.*;
import NG.Graph.Rendering.EdgeShader;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeShader;
import NG.InputHandling.KeyControl;
import NG.InputHandling.MouseTools.MouseToolCallbacks;
import NG.MuChecker.FormulaParser;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.Operands.*;
import NG.MuChecker.StateSet;
import NG.Rendering.GLFWWindow;
import NG.Rendering.RenderLoop;
import NG.Resources.LazyInit;
import NG.Settings.Settings;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import NG.Tools.Vectors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import static NG.Graph.Rendering.GraphElement.Priority.MU_FORMULA;
import static org.lwjgl.opengl.GL11.glDepthMask;

/**
 * A tool for visualising large graphs
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Main {
    public static final Color4f EDGE_MARK_COLOR = Color4f.rgb(220, 150, 0); // yellow
    public static final Color4f HOVER_COLOR = Color4f.rgb(44, 58, 190); // blue
    public static final Color4f PATH_COLOR = Color4f.rgb(200, 83, 0); // orange
    public static final Color4f INITAL_STATE_COLOR = Color4f.rgb(4, 150, 13); // green

    public static final Color4f MU_FORMULA_COLOR = Color4f.rgb(255, 0, 255); // purple-pink
    public static final Color4f MU_FORMULA_UNREACHABLE = Color4f.rgb(200, 0, 0, 0.5f); // red
    public static final Color4f MU_FORMULA_UNAVOIDABLE = Color4f.rgb(0, 200, 0, 0.5f); // green


    private static final Version VERSION = new Version(0, 2);
    private static final Pattern PATTERN_COMMA = Pattern.compile(",");

    private static final int MAX_ITERATIONS_PER_SECOND = 50;
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
    private EdgeShader edgeShader;

    public enum DisplayMethod {
        HIGHLIGHT_ACTIONS, SHOW_SECONDARY_GRAPH, COMPARE_GRAPHS, HIDE_ACTIONS, CLUSTER_ON_SELECTED, CLUSTER_ON_SELECTED_IGNORE_LOOPS, CONFLUENCE
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

        this.settings = settings;
        GLFWWindow.Settings videoSettings = new GLFWWindow.Settings(settings);

        window = new GLFWWindow(Settings.GAME_NAME, videoSettings, true);
        renderer = new RenderLoop(this.settings.TARGET_FPS);
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        mainThread = Thread.currentThread();
        camera = new PointCenteredCamera(Vectors.O);

        graph = SourceGraph.empty(this);
        secondGraph = SourceGraph.empty(this);
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
                .add((gl, root) -> {
                    synchronized (graphLock) {
                        Graph target = getVisibleGraph();
                        gl.render(target.getNodeMesh());
                    }
                });

        edgeShader = new EdgeShader();
        renderer.renderSequence(edgeShader)
                .add((gl, root) -> {
                    glDepthMask(false); // read but not write
                    synchronized (graphLock) {
                        Graph target = getVisibleGraph();
                        gl.render(target.getEdgeMesh());
                    }
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
        if (displayMethod == DisplayMethod.CLUSTER_ON_SELECTED
                || displayMethod == DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS
                || displayMethod == DisplayMethod.CONFLUENCE
        ) {
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

        Graph graph = getVisibleGraph();
        graph.getNodeMesh().schedulePositionReload();
        graph.getEdgeMesh().schedulePositionReload();
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
        return VERSION;
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
            setGraph(SourceGraph.parse(newGraphFile, this));

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

                nodeCluster.drop();
                graphComparator.drop();
                subGraph.drop();

                springLayout.setGraph(doComputeSourceLayout ? graph : getVisibleGraph());

                onNodePositionChange();
                Logger.INFO.print("Loaded graph with " + graph.states.length + " nodes and " + graph.edges.length + " edges");
            }

            menu.reloadUI();
        });
    }

    public void setSecondaryGraph(File newGraphFile) {
        try {
            setSecondaryGraph(SourceGraph.parse(newGraphFile, this));

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
            setDisplayMethod(DisplayMethod.COMPARE_GRAPHS);
            onNodePositionChange();
        });
    }

    public void doSourceLayout(boolean doSource) {
        doComputeSourceLayout = doSource;

        springLayout.setGraph(doSource ? graph : getVisibleGraph());
    }

    public void setDisplayMethod(DisplayMethod method) {
        this.displayMethod = method;
        Logger.DEBUG.print("Set display method", displayMethod);

        if (method == DisplayMethod.CLUSTER_ON_SELECTED || method == DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS) {
            NodeClustering nodeClustering = nodeCluster.get();
            nodeClustering.setShowSelfLoop(method != DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS);

            String[] actionLabels = menu.actionLabels;
            SToggleButton[] attributeButtons = menu.attributeButtons;
            for (int i = 0; i < actionLabels.length; i++) {
                nodeClustering.addEdgeAttribute(actionLabels[i], attributeButtons[i].isActive());
            }

        } else if (method == DisplayMethod.CONFLUENCE) {
            NodeClustering nodeClustering = nodeCluster.get();
            Map<State, State> leaderMap = new ConfluenceDetector(graph).getLeaderMap();
            nodeClustering.createCluster(leaderMap, false);
        }

        springLayout.setGraph(doComputeSourceLayout ? graph : getVisibleGraph());
        onNodePositionChange();
    }

    public DisplayMethod getDisplayMethod() {
        return displayMethod;
    }

    public void applyFileMarkings(File file) {
        try (Scanner scanner = new Scanner(file)) {
            synchronized (graphLock) {
                State[] nodes = graph.states;
                for (State node : nodes) {
                    node.classIndex = -1;
                }

                int classIndex = 0;

                while (scanner.hasNextLine()) {
                    String[] elements = PATTERN_COMMA.split(scanner.nextLine());

                    for (String elt : elements) {
                        int nodeIndex = Integer.parseInt(elt);
                        nodes[nodeIndex].classIndex = classIndex;
                    }

                    classIndex++;
                }
            }

        } catch (FileNotFoundException e) {
            Logger.ERROR.print(e);
        }
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

    public List<Transition> getMarkedEdges() {
        Collection<String> markedActionLabels = getMarkedLabels();

        List<Transition> markedEdges = new ArrayList<>();
        for (Transition edge : graph.getEdgeMesh().edgeList()) {
            if (markedActionLabels.contains(edge.label)) {
                markedEdges.add(edge);
            }
        }

        return markedEdges;
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

            case CONFLUENCE:
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
            for (State node : graph.getNodeMesh().nodeList()) {
                node.position.mulPosition(viewMatrix);
                node.position.z = 0;
            }
            for (Transition edge : graph.getEdgeMesh().edgeList()) {
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

            for (State node : graph.getNodeMesh().nodeList()) {
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
            graph.forAttribute(label, edge -> edge.addColor(EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE));
        } else {
            graph.forAttribute(label, e -> e.resetColor(GraphElement.Priority.ATTRIBUTE));
        }

        graph.getEdgeMesh().scheduleColorReload();
        nodeCluster.ifPresent(g -> g.addEdgeAttribute(label, on));
        subGraph.ifPresent(g -> g.update(getMarkedLabels()));
    }

    public void applyMuFormulaMarking(File file) {
        try {
            for (State state : graph.states) {
                state.resetColor(MU_FORMULA);
            }
            FormulaParser formulaParser = new FormulaParser(file);
            ModelChecker modelChecker = new ModelChecker(graph, formulaParser);
            Logger.DEBUG.print(formulaParser);

            Thread thread = new Thread(() -> {
                StateSet result = modelChecker.call();
                Logger.INFO.printf("Formula holds for %d states", result.size());
                for (State state : result) {
                    state.addColor(MU_FORMULA_COLOR, MU_FORMULA);
                }
                graph.getNodeMesh().scheduleColorReload();

                List<FixedPoint> fixedPoints = formulaParser.getFixedPoints();

                FixedPoint fp1 = new SmallestFixedPoint('_', fixedPoints.size());
                fixedPoints.forEach(fp1::addDescendant);
                fp1.setRight(new LogicalOr(formulaParser.get(), new Box("true", fp1.variable)));

                StateSet unavoidables = new ModelChecker(graph, formulaParser).call();
                Logger.INFO.printf("Unavoidable for %d states", unavoidables.size());
                for (State state : unavoidables) {
                    if (!result.contains(state)) {
                        state.addColor(MU_FORMULA_UNAVOIDABLE, MU_FORMULA);
                    }
                }
//                for (Transition edge : graph.edges) {
//                    if (unavoidables.contains(edge.to)) {
//                        edge.addColor(MU_FORMULA_UNAVOIDABLE, MU_FORMULA);
//                    }
//                }
                graph.getEdgeMesh().scheduleColorReload();

                FixedPoint fp2 = new LargestFixedPoint('_', fixedPoints.size());
                fixedPoints.forEach(fp2::addDescendant);
                fp2.setRight(new LogicalAnd(new Negation(formulaParser.get()), new Box("true", fp2.variable)));

                StateSet unreachables = new ModelChecker(graph, formulaParser).call();
                unreachables.diff(result); // remove all states where the property holds
                Logger.INFO.printf("Unreachable for %d states", unreachables.size());
                for (State state : unreachables) {
                    if (!result.contains(state)) {
                        state.addColor(MU_FORMULA_UNREACHABLE, MU_FORMULA);
                    }
                }
                for (Transition edge : graph.edges) {
                    if (unreachables.contains(edge.to) && !unreachables.contains(edge.from)) {
                        edge.addColor(MU_FORMULA_UNREACHABLE, MU_FORMULA);
                    }
                }
                graph.getEdgeMesh().scheduleColorReload();

                StateSet states = new StateSet(unavoidables);
                states.intersect(unreachables);
                assert states.isEmpty() : String.format("%d unavoidable and unreachable states: %s", states.size(), states);

            });
            thread.setDaemon(true);
            thread.start();

        } catch (FileNotFoundException e) {
            Logger.ERROR.print(e);
        }
    }

    public void setEdgeShape(EdgeShader.EdgeShape edgeShape) {
        edgeShader.currentShape = edgeShape;
    }

    public EdgeShader.EdgeShape getEdgeShape() {
        return edgeShader.currentShape;
    }
}
