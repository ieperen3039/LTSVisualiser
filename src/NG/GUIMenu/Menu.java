package NG.GUIMenu;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.Graph.Graph;
import NG.Graph.GraphElement;
import NG.Graph.NodeClustering;
import NG.Graph.SpringLayout;
import NG.Rendering.RenderLoop;
import NG.Tools.Directory;
import NG.Tools.Logger;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Geert van Ieperen created on 7-8-2020.
 */
public class Menu extends SDecorator {
    public static final SComponentProperties BUTTON_PROPS = new SComponentProperties(150, 30, true, false);
    public static final SComponentProperties WAILA_TEXT_PROPERTIES = new SComponentProperties(
            150, 50, false, true, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_TOP
    );
    public static final Color4f EDGE_MARK_COLOR = Color4f.rgb(240, 190, 0);
    public static final int SPACE_BETWEEN_UI_SECTIONS = 20;
    public static final int MAX_CHARACTERS_ACTION_LABELS = 35;
    public static final File BASE_FILE_CHOOSER_DIRECTORY = Directory.graphs.getDirectory();
    public static final List<Main.DisplayMethod> DISPLAY_METHOD_LIST = Arrays.asList(Main.DisplayMethod.values());

    private final Main main;

    public String[] actionLabels = new String[0];
    public SToggleButton[] attributeButtons = new SToggleButton[0];
    private File currentGraphFile = BASE_FILE_CHOOSER_DIRECTORY;

    public Menu(Main main) {
        this.main = main;
        reloadUI();
    }

    public void reloadUI() {
        Graph graph = main.graph();
        NodeClustering nodeCluster = main.getNodeCluster();
        SpringLayout updateLoop = main.getSpringLayout();
        UIFrameManager frameManager = main.gui();
        RenderLoop renderLoop = main.renderer;

        actionLabels = graph.getEdgeAttributes().stream().distinct().sorted().toArray(String[]::new);
        attributeButtons = getButtons(nodeCluster, graph, actionLabels);

        SDropDown displayMethodDropdown = new SDropDown(
                frameManager, BUTTON_PROPS, 0, DISPLAY_METHOD_LIST,
                displayMethod -> displayMethod.name().replace("_", " ")
        )
                .addStateChangeListener(i -> main.setDisplayMethod(DISPLAY_METHOD_LIST.get(i)));

        setMainPanel(SContainer.row(
                new SFiller(),
                new SPanel(SContainer.column(
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // graph file selector
                        new SButton(
                                "Select Graph", () -> openFileDialog(
                                file -> {
                                    this.currentGraphFile = file;
                                    main.setGraph(file);
                                    reloadUI();
                                }), BUTTON_PROPS
                        ),
                        new SButton(
                                "Compare with second graph", () -> openFileDialog(
                                file -> {
                                    main.setSecondaryGraph(file);
                                    displayMethodDropdown.setCurrent(
                                            DISPLAY_METHOD_LIST.indexOf(Main.DisplayMethod.COMPARE_GRAPHS)
                                    );
                                }), BUTTON_PROPS
                        ),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // graph information panel
                        new SPanel(SContainer.column(
                                new STextArea(String.format(
                                        currentGraphFile.getName() + ": %d nodes, %d edges",
                                        graph.getNrOfNodes(), graph.getNrOfEdges()
                                ), BUTTON_PROPS),
                                new SActiveTextArea(() -> {
                                    GraphElement element = main.getVisibleGraph().getHovered();
                                    return element == null ? "-" : element.toString();
                                }, WAILA_TEXT_PROPERTIES)
                                        .setMaximumCharacters(150)
                        )),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // simulation sliders
                        new SimulationSliderUI(updateLoop),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // Display manipulation
                        new SPanel(SContainer.column(
                                new STextArea("Display method", BUTTON_PROPS),
                                displayMethodDropdown,

                                new SToggleButton("3D View", BUTTON_PROPS, updateLoop.doAllow3D())
                                        .addStateChangeListener(main::set3DView)
                        )),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // attribute coloring
                        SContainer.column(
                                new STextArea("Attribute markings", BUTTON_PROPS),
                                new SScrollableList(10, attributeButtons),
                                new SToggleButton("Only compute layout of primary graph", BUTTON_PROPS, false)
                                        .addStateChangeListener(main::doSourceLayout)
                        ),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new TimingUI(updateLoop, renderLoop),
                        new SFiller()
                )).setGrowthPolicy(false, true)
        ));
    }

    private void openFileDialog(Consumer<File> action) {
        Frame parent = new Frame();
        try {
            FileDialog fd = new FileDialog(parent, "Choose a file", FileDialog.LOAD);
            fd.setDirectory(BASE_FILE_CHOOSER_DIRECTORY.getAbsolutePath());
            fd.setFile("*.aut");
            fd.setVisible(true);
            parent.requestFocus();
            String filename = fd.getFile();
            if (filename != null) {
                String directory = fd.getDirectory();
                action.accept(Paths.get(directory, filename).toFile());
            }

        } finally {
            parent.dispose();
        }
    }

    private static void selectAttribute(Graph sourceGraph, NodeClustering nodeCluster, String label, boolean on) {
        if (on) {
            sourceGraph.setAttributeColor(label, EDGE_MARK_COLOR, GraphElement.Priority.ATTRIBUTE);
        } else {
            sourceGraph.forEachAttribute(label, e -> e.resetColor(GraphElement.Priority.ATTRIBUTE));
        }

        nodeCluster.clusterEdgeAttribute(label, on);
    }

    private SToggleButton[] getButtons(NodeClustering nodeCluster, Graph graph, String[] actionLabels) {
        SToggleButton[] buttons = new SToggleButton[actionLabels.length];

        for (int i = 0; i < actionLabels.length; i++) {
            String label = actionLabels[i];
            buttons[i] = new SToggleButton(label, BUTTON_PROPS)
                    .addStateChangeListener(on -> selectAttribute(graph, nodeCluster, label, on));
            buttons[i].setActive(false);
            buttons[i].setMaximumCharacters(MAX_CHARACTERS_ACTION_LABELS);
        }

        return buttons;
    }

    private static class TimingUI extends SContainer.GhostContainer {
        public TimingUI(SpringLayout updateLoop, RenderLoop renderLoop) {
            super(SContainer.column(
                    new SButton("Get Simulation Timings", () -> Logger.DEBUG.print(updateLoop.timer.resultsTable()), BUTTON_PROPS),
                    new SButton("Get Render Timings", () -> Logger.DEBUG.print(renderLoop.timer.resultsTable()), BUTTON_PROPS),
                    new SToggleButton("Accurate Render Timing", BUTTON_PROPS).addStateChangeListener(s -> renderLoop.accurateTiming = s)
            ));
            setGrowthPolicy(true, false);
        }
    }

    private static class SimulationSliderUI extends SPanel {
        public static final float SPEED_MAXIMUM = 2f / (1 << 11);

        public SimulationSliderUI(SpringLayout updateLoop) {
            super(SContainer.grid(new SComponent[][]{{
                            new SActiveTextArea(() -> String.format("Attraction %5.03f", updateLoop.getAttractionFactor()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getAttractionFactor(), BUTTON_PROPS, updateLoop::setAttractionFactor)
                    }, {
                            new SActiveTextArea(() -> String.format("Repulsion %5.03f", updateLoop.getRepulsionFactor()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getRepulsionFactor(), BUTTON_PROPS, updateLoop::setRepulsionFactor)
                    }, {
                            new SActiveTextArea(() -> String.format("Natural Length %5.03f", updateLoop.getNatLength()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getNatLength(), BUTTON_PROPS, updateLoop::setNatLength)
                    }, {
                            new SActiveTextArea(() -> String.format("Simulation Step Size %5.03f", updateLoop.getSpeed() / SPEED_MAXIMUM), BUTTON_PROPS),
                            new SSlider(0, SPEED_MAXIMUM, updateLoop.getSpeed(), BUTTON_PROPS, updateLoop::setSpeed)
                    }, {
                            new SActiveTextArea(() -> String.format("Handle Repulsion %5.03f", updateLoop.getEdgeRepulsionFactor()), BUTTON_PROPS),
                    new SSlider(0, 2f, updateLoop.getEdgeRepulsionFactor(), BUTTON_PROPS, updateLoop::setEdgeRepulsionFactor)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }
}
