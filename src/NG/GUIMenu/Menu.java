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
import NG.Graph.GraphColorTool;
import NG.Graph.GraphElement;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Graph.SpringLayout;
import NG.InputHandling.MouseTools.MouseTool;
import NG.InputHandling.MouseTools.MouseToolCallbacks;
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

/**
 * @author Geert van Ieperen created on 7-8-2020.
 */
public class Menu extends SDecorator {
    public static final SComponentProperties BUTTON_PROPS = new SComponentProperties(150, 28, true, false);
    public static final SComponentProperties WAILA_TEXT_PROPERTIES = new SComponentProperties(
            150, 50, true, false, NGFonts.TextType.REGULAR, SFrameLookAndFeel.Alignment.CENTER_TOP
    );
    public static final Color4f EDGE_MARK_COLOR = Color4f.rgb(240, 190, 0);
    public static final int SPACE_BETWEEN_UI_SECTIONS = 10;
    public static final int MAX_CHARACTERS_ACTION_LABELS = 35;
    public static final File BASE_FILE_CHOOSER_DIRECTORY = Directory.graphs.getDirectory();
    public static final List<Main.DisplayMethod> DISPLAY_METHOD_LIST = Arrays.asList(Main.DisplayMethod.values());

    private static final PairList<String, Color4f> paintColors = new PairList.Builder<String, Color4f>()
            .add("Red", Color4f.rgb(200, 25, 25, 0.8f))
            .add("Green", Color4f.rgb(4, 120, 13, 0.8f))
            .add("Orange", Color4f.rgb(220, 105, 20, 0.8f))
            .add("Purple", Color4f.rgb(200, 20, 160, 0.8f))
            .add("Faint Grey", new Color4f(0.5f, 0.5f, 0.5f, 0.1f))
            .get();

    private final Main main;
    private final GraphColorTool colorTool;

    public String[] actionLabels = new String[0];
    public SToggleButton[] attributeButtons = new SToggleButton[0];
    private File currentGraphFile = BASE_FILE_CHOOSER_DIRECTORY;
    private SToggleButton colorToggleButton;

    public Menu(Main main) {
        this.main = main;
        this.colorTool = new GraphColorTool(main, () -> colorToggleButton.setActive(false), paintColors.right(0));
        reloadUI();
    }

    public void reloadUI() {
        Graph graph = main.graph();
        RenderLoop renderLoop = main.renderer;
        SpringLayout updateLoop = main.getSpringLayout();
        UIFrameManager frameManager = main.gui();
        MouseToolCallbacks inputHandling = main.inputHandling();

        actionLabels = graph.getEdgeAttributes().stream().distinct().sorted().toArray(String[]::new);

        attributeButtons = new SToggleButton[actionLabels.length];
        for (int i = 0; i < actionLabels.length; i++) {
            String label = actionLabels[i];
            attributeButtons[i] = new SToggleButton(label, BUTTON_PROPS)
                    .addStateChangeListener(on -> main.selectAttribute(label, on));
            attributeButtons[i].setActive(false);
            attributeButtons[i].setMaximumCharacters(MAX_CHARACTERS_ACTION_LABELS);
        }

        colorToggleButton = new SToggleButton("Activate Painting", BUTTON_PROPS)
                .addStateChangeListener(on -> inputHandling.setMouseTool(on ? colorTool : null));

        // dropdown for what graph is displayed
        SDropDown displayMethodDropdown = new SDropDown(
                frameManager, BUTTON_PROPS, 0, DISPLAY_METHOD_LIST,
                displayMethod -> displayMethod.name().replace("_", " ")
        ).addStateChangeListener(i -> main.setDisplayMethod(DISPLAY_METHOD_LIST.get(i)));

        // automatic barnes-hut activation
        if (main.graph().getNrOfNodes() < 500) {
            updateLoop.setBarnesHutTheta(0);
        } else if (updateLoop.getBarnesHutTheta() == 0) {
            updateLoop.setBarnesHutTheta(0.5f);
        }

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
                                }), BUTTON_PROPS
                        ),
//                        new SButton(
//                                "Compare with second graph", () -> openFileDialog(
//                                file -> {
//                                    main.setSecondaryGraph(file);
//                                    displayMethodDropdown.setCurrent(
//                                            DISPLAY_METHOD_LIST.indexOf(Main.DisplayMethod.COMPARE_GRAPHS)
//                                    );
//                                }), BUTTON_PROPS
//                        ),
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
                                        .addStateChangeListener(main::set3DView),
                                new SToggleButton("Only compute layout of primary graph", BUTTON_PROPS, false)
                                        .addStateChangeListener(main::doSourceLayout)
                        )),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // color tool
                        colorToggleButton,
                        new SDropDown(frameManager, BUTTON_PROPS, 0, paintColors, p -> p.left)
                                .addStateChangeListener(i -> colorTool.setColor(paintColors.right(i))),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // attribute coloring
                        SContainer.column(
                                new STextArea("Attribute markings", BUTTON_PROPS),
                                new SScrollableList(9, attributeButtons)
                        ),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new SButton("Add marking from file", () -> openFileDialog(main::applyFileMarkings), BUTTON_PROPS),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        // auxiliary buttons
                        new SButton("Center camera on...",
                                () -> main.inputHandling().setMouseTool(new CameraCenterTool(main)), BUTTON_PROPS
                        ),
                        new SButton("Get Simulation Timings", () -> Logger.DEBUG.print(updateLoop.timer.resultsTable()), BUTTON_PROPS),
                        new SButton("Get Render Timings", () -> Logger.DEBUG.print(renderLoop.timer.resultsTable()), BUTTON_PROPS),
                        new SToggleButton("Accurate Render Timing", BUTTON_PROPS, renderLoop.accurateTiming)
                                .addStateChangeListener(s -> renderLoop.accurateTiming = s),
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

            String filename = fd.getFile();
            if (filename != null) {
                String directory = fd.getDirectory();
                action.accept(Paths.get(directory, filename).toFile());
            }

        } finally {
            parent.dispose();
        }
    }

    private class CameraCenterTool extends MouseTool {
        public CameraCenterTool(Main root) {
            super(root);
        }

        @Override
        public void onNodeClick(int button, Graph graph, NodeMesh.Node node) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                Camera camera = main.camera();
                camera.set(node.position);
            }
            main.inputHandling().setMouseTool(null);
        }

        @Override
        public void onEdgeClick(int button, Graph graph, EdgeMesh.Edge edge) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                Camera camera = main.camera();
                camera.set(edge.handlePos);
            }
            main.inputHandling().setMouseTool(null);
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
                            new SSlider(0, 1f, updateLoop.getEdgeRepulsionFactor(), BUTTON_PROPS, updateLoop::setEdgeRepulsionFactor)
                    }, {
                            new SActiveTextArea(() -> String.format("Heuristic Effect %5.03f", updateLoop.getBarnesHutTheta()), BUTTON_PROPS),
                            new SSlider(0, 2f, updateLoop.getBarnesHutTheta(), BUTTON_PROPS, updateLoop::setBarnesHutTheta)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }
}
