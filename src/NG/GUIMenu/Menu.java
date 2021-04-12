package NG.GUIMenu;

import NG.Camera.Camera;
import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.Graph.Graph;
import NG.Graph.GraphPathFinder;
import NG.Graph.Layout.SpringLayout;
import NG.Graph.Rendering.EdgeShader;
import NG.Graph.Rendering.GraphColorTool;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.InputHandling.MouseTools.MouseTool;
import NG.Rendering.RenderLoop;
import NG.Tools.Directory;
import NG.Tools.Logger;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static NG.Core.Main.PATH_COLOR;

/**
 * @author Geert van Ieperen created on 7-8-2020.
 */
public class Menu extends SDecorator {
    public static final SComponentProperties BUTTON_PROPS = new SComponentProperties(180, 25, true, false);
    public static final SComponentProperties WAILA_TEXT_PROPERTIES = new SComponentProperties(
            150, 50, true, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_TOP
    );
    public static final int SPACE_BETWEEN_UI_SECTIONS = 10;
    public static final int MAX_CHARACTERS_ACTION_LABELS = 35;
    public static final float SPEED_MAXIMUM = 1f / (1 << 8);
    public static final File BASE_FILE_CHOOSER_DIRECTORY = Directory.graphs.getDirectory();
    public static final List<EdgeShader.EdgeShape> EDGE_SHAPE_LIST = Arrays.asList(EdgeShader.EdgeShape.values());
    public static final Color4f A_COLOR = Color4f.rgb(200, 83, 0, 0.8f);
    public static final Color4f B_COLOR = Color4f.rgb(0, 134, 19, 0.8f);

    private static final PairList<String, Color4f> PAINT_COLORS = new PairList.Builder<String, Color4f>()
            .add("Purple", Color4f.rgb(200, 20, 160, 0.8f))
            .add("Red", Color4f.rgb(200, 25, 25, 0.8f))
            .add("Orange", Color4f.rgb(220, 105, 20, 0.8f))
            .add("Yellow", Color4f.rgb(220, 150, 0, 0.8f))
            .add("Green", Color4f.rgb(4, 120, 13, 0.8f))
            .add("Faint Grey", new Color4f(0.5f, 0.5f, 0.5f, 0.08f))
            .get();
    public static final int INITIAL_COLOR_INDEX = PAINT_COLORS.indexOfLeft("Green");

    public String[] actionLabels = new String[0];
    private final Main main;
    public SToggleButton[] markButtons = new SToggleButton[0];
    public SToggleButton[] clusterButtons = new SToggleButton[0];
    public SToggleButton[] internalButtons = new SToggleButton[0];
    private File currentGraphFile = BASE_FILE_CHOOSER_DIRECTORY;

    public Menu(Main main) {
        this.main = main;
        reloadUI();
    }

    public void reloadUI() {
        Graph graph = main.graph();
        RenderLoop renderLoop = main.renderer;
        SpringLayout updateLoop = main.getSpringLayout();
        UIFrameManager frameManager = main.gui();
        GraphColorTool colorTool = new GraphColorTool(main, PAINT_COLORS.right(INITIAL_COLOR_INDEX));

        actionLabels = graph.getEdgeLabels().stream().distinct().sorted().toArray(String[]::new);

        markButtons = new SToggleButton[actionLabels.length];
        for (int i = 0; i < actionLabels.length; i++) {
            String label = actionLabels[i];
            SToggleButton button = new SToggleButton(label, BUTTON_PROPS);
            button.addStateChangeListener(on -> main.labelMark(label, on, colorTool.getColor()));
            button.addStateChangeListener(on -> button.setColor(on ? colorTool.getColor().opaque() : null));
            button.setActive(false);
            button.setMaximumCharacters(MAX_CHARACTERS_ACTION_LABELS);
            button.setGrowthPolicy(true, false);
            markButtons[i] = button;
        }

        clusterButtons = new SToggleButton[actionLabels.length];
        for (int i = 0; i < actionLabels.length; i++) {
            String label = actionLabels[i];
            //noinspection SuspiciousNameCombination
            SToggleButton button = new SToggleButton("C", BUTTON_PROPS.minHeight, BUTTON_PROPS.minHeight, false);
            button.addStateChangeListener(on -> main.resetCluster());
            button.setGrowthPolicy(false, false);
            clusterButtons[i] = button;
        }

        internalButtons = new SToggleButton[actionLabels.length];
        for (int i = 0; i < actionLabels.length; i++) {
            String label = actionLabels[i];
            //noinspection SuspiciousNameCombination
            SToggleButton button = new SToggleButton("I", BUTTON_PROPS.minHeight, BUTTON_PROPS.minHeight, false);
            button.addStateChangeListener(on -> main.resetCluster());
            button.setGrowthPolicy(false, false);
            internalButtons[i] = button;
        }

        SComponent[] actionComponents = new SComponent[actionLabels.length];
        for (int i = 0; i < markButtons.length; i++) {
            actionComponents[i] = SContainer.row(markButtons[i], internalButtons[i], clusterButtons[i]);
        }

        // automatic barnes-hut (de)activation
        if (graph.getNrOfNodes() < 500) {
            updateLoop.setBarnesHutTheta(0);
        } else if (updateLoop.getBarnesHutTheta() == 0) {
            updateLoop.setBarnesHutTheta(1.0f);
        }

        // automatic edge repulsion deactivation
        if (graph.getNrOfEdges() > 10_000) {
            updateLoop.setEdgeRepulsionFactor(0);
        }

        setMainPanel(SContainer.row(
                new SFiller(),
                new SPanel(SContainer.column(
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // file selectors
                        new SButton("Load Graph",
                                () -> {
                                    openFileDialog(
                                            file -> {
                                                currentGraphFile = file;
                                                main.setGraph(file);
                                            }, "*.aut"
                                    );
                                    // reset timers
                                    renderLoop.defer(renderLoop.timer::reset);
                                    updateLoop.defer(updateLoop.timer::reset);
                                }, BUTTON_PROPS
                        ),
                        SContainer.row(
                                new SButton("Load Modal Mu-Formula",
                                        () -> openFileDialog(main::applyMuFormulaMarking, "*.mcf"),
                                        BUTTON_PROPS
                                ),
                                new SCloseButton(BUTTON_PROPS.minHeight,
                                        () -> main.getVisibleGraph().resetColors(GraphElement.Priority.MU_FORMULA)
                                )
                        ),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // graph information panel
                        new SPanel(SContainer.column(
                                new STextArea(String.format( // always about the primary graph
                                        currentGraphFile.getName() + ": %d nodes, %d edges",
                                        graph.getNrOfNodes(), graph.getNrOfEdges()
                                ), BUTTON_PROPS),
                                // the WAILA element
                                new SActiveTextArea(() -> {
                                    GraphElement element = main.getVisibleGraph().getHovered();
                                    return element == null ? "-" : element.identifier();
                                }, WAILA_TEXT_PROPERTIES)
                                        .setMaximumCharacters(150)
                        )),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // simulation sliders
                        new SimulationSliderUI(updateLoop),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // action coloring
                        new STextArea("Current Color", BUTTON_PROPS),
                        new SDropDown(frameManager, BUTTON_PROPS, INITIAL_COLOR_INDEX, PAINT_COLORS, p -> p.left)
                                .addStateChangeListener(i -> colorTool.setColor(PAINT_COLORS.right(i))),

                        SContainer.column(
                                new STextArea("Action labels", BUTTON_PROPS),
                                new SScrollableList(9, actionComponents)
                        ),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // auxiliary buttons
                        new SFrame.Spawner("Display Options", main.gui(), SContainer.column(
                                // edge shape
                                new STextArea("Edge Shape", BUTTON_PROPS),
                                new SDropDown(
                                        frameManager, BUTTON_PROPS,
                                        EDGE_SHAPE_LIST.indexOf(main.getEdgeShape()), EDGE_SHAPE_LIST
                                ).addStateChangeListener(i -> main.setEdgeShape(EDGE_SHAPE_LIST.get(i))),
                                new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                                // color tool
                                SContainer.row(
                                        colorTool.button("Activate Painting", BUTTON_PROPS),
                                        new SCloseButton(BUTTON_PROPS.minHeight,
                                                () -> main.getVisibleGraph()
                                                        .resetColors(GraphElement.Priority.USER_COLOR)
                                        )
                                ),
                                new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                                new SToggleButton("3D View", BUTTON_PROPS, updateLoop.doAllow3D())
                                        .addStateChangeListener(main::set3DView),
                                new SToggleButton("Always use layout of primary graph", BUTTON_PROPS, false)
                                        .addStateChangeListener(main::doSourceLayout),
                                new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                                new SButton("Log Simulation Timings", () -> Logger.DEBUG.print(updateLoop.timer.resultsTable()), BUTTON_PROPS),
                                new SButton("Log Render Timings", () -> Logger.DEBUG.print(renderLoop.timer.resultsTable()), BUTTON_PROPS)
                        ), BUTTON_PROPS),
                        SContainer.row(
                                new PathVisualisationTool(main).button("Find shortest path", BUTTON_PROPS),
                                new SCloseButton(BUTTON_PROPS.minHeight,
                                        () -> main.getVisibleGraph().resetColors(GraphElement.Priority.PATH)
                                )
                        ),
//                        new ComparatorMouseTool(main).button("Compare Nodes", BUTTON_PROPS),
                        new CameraCenterTool(main).button("Center camera on...", BUTTON_PROPS),
                        new SFiller()
                )).setGrowthPolicy(false, true)
        ));
    }

    private void openFileDialog(Consumer<File> action, String extension) {
        FileDialog fd = new FileDialog((Frame) null, "Choose a file", FileDialog.LOAD);
        fd.setDirectory(currentGraphFile.getAbsolutePath());
        fd.setFile(extension);
        fd.setVisible(true);

        String filename = fd.getFile();
        if (filename != null) {
            String directory = fd.getDirectory();
            File file = Paths.get(directory, filename).toFile();
            action.accept(file);
        }

        fd.dispose();
    }

    private static class SimulationSliderUI extends SPanel {

        public SimulationSliderUI(SpringLayout updateLoop) {
            super(SContainer.grid(new SComponent[][]{{
                            new STextArea("Simulation Step Size", BUTTON_PROPS),
                            new SSlider(0, SPEED_MAXIMUM, updateLoop.getSpeed(), BUTTON_PROPS, updateLoop::setSpeed)
                    }, {
                            new STextArea("Attraction", BUTTON_PROPS),
                            new SSlider(0, 5f, updateLoop.getAttractionFactor(), BUTTON_PROPS, updateLoop::setAttractionFactor)
                    }, {
                            new STextArea("Repulsion", BUTTON_PROPS),
                            new SSlider(0, 25f, updateLoop.getRepulsionFactor(), BUTTON_PROPS, updateLoop::setRepulsionFactor)
                    }, {
                            new STextArea("Natural Length", BUTTON_PROPS),
                            new SSlider(0, 5f, updateLoop.getNatLength(), BUTTON_PROPS, updateLoop::setNatLength)
                    }, {
                            new STextArea("Handle Repulsion", BUTTON_PROPS),
                            new SSlider(0, 1f, updateLoop.getEdgeRepulsionFactor(), BUTTON_PROPS, updateLoop::setEdgeRepulsionFactor)
                    }, {
                            new SActiveTextArea(() -> String.format("Heuristic Effect: %4.02f", updateLoop.getBarnesHutTheta()), BUTTON_PROPS),
                            new SSlider(0, 2f, updateLoop.getBarnesHutTheta(), BUTTON_PROPS, updateLoop::setBarnesHutTheta)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }

    private class CameraCenterTool extends MouseTool {
        public CameraCenterTool(Main root) {
            super(root);
        }

        @Override
        public void onNodeClick(int button, Graph graph, State node) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                Camera camera = main.camera();
                camera.set(node.position);
                Logger.DEBUG.print("Center camera on " + node);
            }
            disableThis();
        }

        @Override
        public void onEdgeClick(int button, Graph graph, Transition edge) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                Camera camera = main.camera();
                camera.set(edge.handlePos);
                Logger.DEBUG.print("Center camera on " + edge);
            }
            disableThis();
        }

    }

    private static class PathVisualisationTool extends MouseTool {
        private State startNode = null;

        public PathVisualisationTool(Main root) {
            super(root);
        }

        @Override
        public void onNodeClick(int button, Graph graph, State node) {
            if (startNode == null) {
                graph.resetColors(GraphElement.Priority.PATH);
                startNode = node;
                node.addColor(PATH_COLOR, GraphElement.Priority.PATH);

            } else {
                colorPath(startNode, node, graph);
                disableThis();
                startNode = null;
            }
        }

        @Override
        public void onEdgeClick(int button, Graph graph, Transition edge) {
            if (startNode == null) {
                graph.resetColors(GraphElement.Priority.PATH);
                startNode = edge.to;
                edge.addColor(PATH_COLOR, GraphElement.Priority.PATH);
                edge.to.addColor(PATH_COLOR, GraphElement.Priority.PATH);

            } else {
                colorPath(startNode, edge.from, graph);
                disableThis();
                startNode = null;
            }
        }

        private void colorPath(State startNode, State endNode, Graph graph) {
            GraphPathFinder dijkstra = new GraphPathFinder(startNode, endNode, graph);

            Logger.DEBUG.print("Searching path from " + startNode.label + " to " + endNode.label);

            List<Transition> edges = dijkstra.call();

            if (edges == null) {
                Logger.WARN.print("No path found from " + startNode.label + " to " + endNode.label);

                startNode.addColor(Color4f.RED, GraphElement.Priority.PATH);
                endNode.addColor(Color4f.RED, GraphElement.Priority.PATH);
                graph.getNodeMesh().scheduleColorReload();
                return;
            }

            Logger.DEBUG.print("Path from " + startNode.label + " to " + endNode.label + " found of length " + edges.size());

            for (Transition edge : edges) {
                edge.addColor(PATH_COLOR, GraphElement.Priority.PATH);
                edge.to.addColor(PATH_COLOR, GraphElement.Priority.PATH);
            }

            graph.getEdgeMesh().scheduleColorReload();
            graph.getNodeMesh().scheduleColorReload();
        }
    }

    public static class ComparatorMouseTool extends MouseTool {
        private State startNode = null;

        public ComparatorMouseTool(Main root) {
            super(root);
        }

        @Override
        public void onNodeClick(int button, Graph graph, State node) {
            if (startNode == null) {
                graph.resetColors(GraphElement.Priority.COMPARE);
                startNode = node;
                node.addColor(Color4f.YELLOW, GraphElement.Priority.COMPARE);

            } else {
                colorCompare(startNode, node, graph);
                disableThis();
                startNode = null;
            }
        }

        private void colorCompare(State startNode, State endNode, Graph graph) {
            Thread thread = new Thread(() -> {
                Logger.ASSERT.print("not implemented");
            }, "Node Compare");

            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void onEdgeClick(int button, Graph graph, Transition edge) {
            onNodeClick(button, graph, edge.to);
        }
    }
}
