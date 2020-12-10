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
import java.util.function.Consumer;
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

    public static final Color4f MU_FORMULA_COLOR = Color4f.rgb(220, 150, 0); // yellow
    public static final Color4f MU_FORMULA_UNREACHABLE = Color4f.rgb(200, 0, 0, 0.8f); // red
    public static final Color4f MU_FORMULA_UNAVOIDABLE = Color4f.rgb(0, 100, 0, 0.8f); // green

    private static final Version VERSION = new Version(0, 3);
    private static final Pattern PATTERN_COMMA = Pattern.compile(",");

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
    private final LazyInit<IgnoringGraph> ignoringGraph;
    private final LazyInit<NodeClustering> confluenceGraph;
    private DisplayMethod displayMethod = DisplayMethod.PRIMARY_GRAPH;
    private EdgeShader edgeShader;

    public enum DisplayMethod {
        PRIMARY_GRAPH, SECONDARY_GRAPH, COMPARE_GRAPHS, HIDE_ACTIONS, CLUSTER_ON_SELECTED, CLUSTER_ON_SELECTED_IGNORE_LOOPS, CONFLUENCE
    }

    public Main(Settings settings) throws Exception {
        Logger.INFO.print("Starting up...");

        Logger.DEBUG.print("General debug information: "
                // manual aligning will do the trick
                + "\n\tSystem OS:          " + System.getProperty("os.name")
                + "\n\tJava VM:            " + System.getProperty("java.runtime.version")
                + "\n\tTool version:       " + getVersionNumber()
                + "\n\tNumber of workers:  " + settings.NUM_WORKER_THREADS
        );

        this.settings = settings;
        GLFWWindow.Settings videoSettings = new GLFWWindow.Settings(settings);

        window = new GLFWWindow(Settings.TITLE, videoSettings);
        renderer = new RenderLoop(settings.TARGET_FPS);
        inputHandler = new MouseToolCallbacks();
        keyControl = inputHandler.getKeyControl();
        frameManager = new FrameManagerImpl();
        mainThread = Thread.currentThread();
        camera = new PointCenteredCamera(Vectors.O);

        graph = SourceGraph.empty(this);
        secondGraph = SourceGraph.empty(this);
        springLayout = new SpringLayout(settings.MAX_ITERATIONS_PER_SECOND, settings.NUM_WORKER_THREADS);

        nodeCluster = new LazyInit<>(
                () -> new NodeClustering(graph, getMarkedLabels(menu.clusterButtons)),
                Graph::cleanup
        );
        confluenceGraph = new LazyInit<>(
                () -> new NodeClustering(graph, new ConfluenceDetector(graph).getLeaderMap(), false, "tau"),
                Graph::cleanup
        );
        graphComparator = new LazyInit<>(
                () -> new GraphComparator(graph, secondGraph),
                Graph::cleanup
        );
        ignoringGraph = new LazyInit<>(
                () -> new IgnoringGraph(graph, getMarkedLabels(menu.clusterButtons)),
                Graph::cleanup
        );
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
    }

    public void root() throws Exception {
        init();

        window.open();
        springLayout.start();

        Logger.INFO.print("Finished startup\n");

        renderer.run();

        // log timing results when closing
        Logger.DEBUG.print('\n' + springLayout.timer.resultsTable()
                + '\n' + renderer.timer.resultsTable()
        );

        window.close();
        springLayout.stopLoop();

        cleanup();

        Logger.INFO.print("Tool has been closed successfully");
    }

    public void onNodePositionChange() {
        if (displayMethod == DisplayMethod.CLUSTER_ON_SELECTED
                || displayMethod == DisplayMethod.CLUSTER_ON_SELECTED_IGNORE_LOOPS
        ) {
            if (doComputeSourceLayout) {
                nodeCluster.get().pullClusterPositions();

            } else {
                nodeCluster.get().pushClusterPositions();
            }

        } else if (displayMethod == DisplayMethod.CONFLUENCE) {
            if (doComputeSourceLayout) {
                confluenceGraph.get().pullClusterPositions();

            } else {
                confluenceGraph.get().pushClusterPositions();
            }
        }

        if (displayMethod == DisplayMethod.COMPARE_GRAPHS && doComputeSourceLayout) {
            graphComparator.get().updateEdges();
        }

        if (displayMethod == DisplayMethod.HIDE_ACTIONS && doComputeSourceLayout) {
            ignoringGraph.get().updateEdges();
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
                confluenceGraph.drop();
                graphComparator.drop();
                ignoringGraph.drop();

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

        if (method == DisplayMethod.CONFLUENCE) {
            confluenceGraph.getOrElse(cGraph -> {
                String[] actionLabels = menu.actionLabels;
                SToggleButton[] actionLabelButtons = menu.markButtons;
                for (int i = 0; i < actionLabels.length; i++) {
                    if (actionLabelButtons[i].isActive()) {
                        cGraph.forActionLabel(actionLabels[i], edge -> edge.addColor(EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE));
                    }
                }
            });
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

    public Collection<String> getMarkedLabels(SToggleButton[] buttons) {
        Collection<String> markedActionLabels = new HashSet<>();
        String[] actionLabels = menu.actionLabels;
        for (int i = 0; i < actionLabels.length; i++) {
            if (buttons[i].isActive()) {
                markedActionLabels.add(actionLabels[i]);
            }
        }
        return markedActionLabels;
    }

    public List<Transition> getMarkedEdges() {
        Collection<String> markedActionLabels = getMarkedLabels(menu.markButtons);

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
            case PRIMARY_GRAPH:
                return graph;

            case SECONDARY_GRAPH:
                return secondGraph;

            case HIDE_ACTIONS:
                return ignoringGraph.get();

            case CLUSTER_ON_SELECTED:
            case CLUSTER_ON_SELECTED_IGNORE_LOOPS:
                return nodeCluster.get();

            case CONFLUENCE:
                return confluenceGraph.get();

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

    public void toggleClusterActionLabel(String label) {
        String[] actionLabels = menu.actionLabels;
        for (int i = 0; i < actionLabels.length; i++) {
            if (label.equals(actionLabels[i])) {
                menu.markButtons[i].toggle();
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

    public void labelMark(String label, boolean on) {
        Consumer<Transition> colorAction = on ?
                (edge -> edge.addColor(EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE)) :
                (edge -> edge.resetColor(GraphElement.Priority.ATTRIBUTE));

        graph.forActionLabel(label, colorAction);
        graph.getEdgeMesh().scheduleColorReload();

        secondGraph.forActionLabel(label, colorAction);
        secondGraph.getEdgeMesh().scheduleColorReload();

        confluenceGraph.ifPresent(g -> g.forActionLabel(label, colorAction));
        confluenceGraph.ifPresent(g -> g.getEdgeMesh().scheduleColorReload());

        nodeCluster.ifPresent(g -> g.forActionLabel(label, colorAction));
        nodeCluster.ifPresent(g -> g.getEdgeMesh().scheduleColorReload());

        ignoringGraph.ifPresent(g -> g.forActionLabel(label, colorAction));
        ignoringGraph.ifPresent(g -> g.getEdgeMesh().scheduleColorReload());
    }

    public void labelCluster(String label, boolean on) {
        ignoringGraph.ifPresent(g -> g.setIgnore(label, on));

        NodeClustering clusterGraph = nodeCluster.get();
        clusterGraph.setLabelCluster(label, on);

        for (String marked : getMarkedLabels(menu.markButtons)) {
            clusterGraph.forActionLabel(marked, edge -> edge.addColor(EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE));
        }
        clusterGraph.getEdgeMesh().scheduleColorReload();
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
                Formula unavoidable = fp1.setRight(
                        new LogicalOr(formulaParser.get(), new Box("true", fp1.variable))
                );

                fixedPoints.add(0, fp1);
                StateSet unavoidables = new ModelChecker(graph, unavoidable, fixedPoints).call();
                fixedPoints.remove(fp1);

                Logger.INFO.printf("Unavoidable for %d states", unavoidables.size());
                for (State state : unavoidables) {
                    if (!result.contains(state)) { // only color states that are not in the result itself
                        state.addColor(MU_FORMULA_UNAVOIDABLE, MU_FORMULA);
                    }
                }
                for (Transition edge : graph.edges) {
                    if (unavoidables.contains(edge.to)) {
                        edge.addColor(MU_FORMULA_UNAVOIDABLE, MU_FORMULA);
                    }
                }
                graph.getEdgeMesh().scheduleColorReload();

                FixedPoint fp2 = new LargestFixedPoint('_', fixedPoints.size());
                fixedPoints.forEach(fp2::addDescendant);
                Formula unreachable = fp2.setRight(
                        new LogicalAnd(new Negation(formulaParser.get()), new Box("true", fp2.variable))
                );

                fixedPoints.add(0, fp2);
                StateSet unreachables = new ModelChecker(graph, unreachable, fixedPoints).call();
                fixedPoints.remove(fp2);

                Logger.INFO.printf("Unreachable for %d states", unreachables.size());
                for (State state : unreachables) {
                    assert !result.contains(state);
                    state.addColor(MU_FORMULA_UNREACHABLE, MU_FORMULA);
                }
                for (Transition edge : graph.edges) {
                    if (unreachables.contains(edge.to) && !unreachables.contains(edge.from)) {
                        edge.addColor(MU_FORMULA_UNREACHABLE, MU_FORMULA);
                    }
                }
                graph.getEdgeMesh().scheduleColorReload();

                StateSet states = new StateSet(unavoidables);
                states.intersect(unreachables);
                assert states.isEmpty() : String.format("%d unavoidable and unreachable states", states.size());

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
