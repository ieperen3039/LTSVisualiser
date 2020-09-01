package NG.GUIMenu;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.Graph.Graph;
import NG.Graph.NodeClustering;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.SpringLayout;
import NG.Rendering.RenderLoop;
import NG.Tools.Directory;
import NG.Tools.Logger;

import java.io.File;
import java.util.*;

/**
 * @author Geert van Ieperen created on 7-8-2020.
 */
public class Menu extends SDecorator {
    public static final SComponentProperties BUTTON_PROPS = new SComponentProperties(150, 30);
    public static final Color4f EDGE_MARK_COLOR = Color4f.rgb(240, 190, 0);
    public static final int SPACE_BETWEEN_UI_SECTIONS = 20;

    private static final List<File> files = GraphFileSelector.getFiles(Directory.graphs.getFile("probabilistic"), ".aut");
//     private static final List<File> files = GraphFileSelector.getFiles(Directory.workDirectory().toFile(), ".aut");
//    private static final List<File> files = List.of(
//            Directory.graphs.getFile("railway", "RailwaySafetySystem_spec.aut"),
//            Directory.graphs.getFile("industrial", "lift", "lift3-init.aut"),
//            Directory.graphs.getFile("industrial", "lift", "lift3-final.aut"),
//            Directory.graphs.getFile("industrial", "DIRAC", "SMS.aut")
//    );

    private final Main main;
    private final GraphFileSelector graphFileSelector;

    public Menu(Main main) {
        this.main = main;
        this.graphFileSelector = new GraphFileSelector(main, this);

        main.setGraphSafe(files.get(0));
        reloadUI();
    }

    public void reloadUI() {
        Graph graph = main.graph();
        NodeClustering nodeCluster = main.nodeCluster;
        SpringLayout updateLoop = main.getSpringLayout();
        UIFrameManager frameManager = main.frameManager;
        RenderLoop renderLoop = main.renderer;

        setMainPanel(SContainer.row(
                new SFiller(),
                new SPanel(SContainer.column(
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        graphFileSelector,
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new SimulationSliderUI(updateLoop),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new SToggleButton("3D View", BUTTON_PROPS, updateLoop.doAllow3D()).addStateChangeListener(main::set3DView),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new ClusterMethodSelector(frameManager, nodeCluster, main),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new AttributeFrame(nodeCluster, graph),
                        new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),

                        new TimingUI(updateLoop, renderLoop),
                        new SFiller()
                )).setGrowthPolicy(false, true)
        ));
    }

    private static void selectAttribute(Graph sourceGraph, NodeClustering nodeCluster, String label, boolean on) {
        sourceGraph.setAttributeColor(label, on ? EDGE_MARK_COLOR : EdgeMesh.EDGE_BASE_COLOR);
        nodeCluster.clusterEdgeAttribute(label, on);
    }

    private static class GraphFileSelector extends SPanel {

        public GraphFileSelector(Main main, Menu menu) {
            super(SContainer.column(
                    new STextArea("Graph Selector", BUTTON_PROPS),
                    new SDropDown(main.gui(), BUTTON_PROPS, 0, files, file -> file.getParentFile()
                            .getName() + "/" + file.getName())
                            .addStateChangeListener((i) -> {
                                main.setGraphSafe(files.get(i));
                                menu.reloadUI();
                            })
            ));
            setGrowthPolicy(false, false);
        }

        /** recursively finds all files under the given directory with the given suffix */
        private static List<File> getFiles(File directory, String suffix) {
            List<File> files = new ArrayList<>();
            Deque<File> open = new ArrayDeque<>();
            open.add(directory);

            while (!open.isEmpty()) {
                File file = open.remove();

                if (file.isDirectory()) {
                    // add all underlying files
                    File[] newFiles = file.listFiles();
                    assert newFiles != null;
                    open.addAll(Arrays.asList(newFiles));

                } else if (file.getName().endsWith(suffix)) {
                    files.add(file);
                }
            }

            return files;
        }
    }

    private static class AttributeFrame extends SPanel {
        private static final int MAX_CHARACTERS_ACTION_LABELS = 35;

        public AttributeFrame(NodeClustering nodeCluster, Graph graph) {
            super(SContainer.column(
                    new STextArea("Color selection", BUTTON_PROPS),
                    new SScrollableList(6, getButtons(nodeCluster, graph))
            ));
            setGrowthPolicy(false, false);
        }

        private static SComponent[] getButtons(NodeClustering nodeCluster, Graph graph) {
            String[] actionLabels = graph.getEdgeAttributes().stream()
                    .distinct().toArray(String[]::new);

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
    }

    private static class ClusterMethodSelector extends SPanel {
        private static final List<Main.ClusterMethod> CLUSTER_METHODS = Arrays.asList(Main.ClusterMethod.values());

        public ClusterMethodSelector(UIFrameManager frameManager, NodeClustering nodeCluster, Main main) {
            super(SContainer.column(
                    new STextArea("Cluster method", BUTTON_PROPS),
                    new SDropDown(frameManager, BUTTON_PROPS, 0, CLUSTER_METHODS, Enum::name)
                            .addStateChangeListener(i -> main.setClusterMethod(CLUSTER_METHODS.get(i))),
                    new SToggleButton("Compute layout of Cluster", BUTTON_PROPS)
                            .addStateChangeListener(on -> main.doSourceLayout(!on))
            ));
            setGrowthPolicy(true, false);
        }
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
                            new SSlider(0, 1f, updateLoop.getEdgeRepulsionFactor(), BUTTON_PROPS, updateLoop::setEdgeRepulsionFactor)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }
}
