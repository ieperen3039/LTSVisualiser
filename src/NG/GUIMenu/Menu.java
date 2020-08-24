package NG.GUIMenu;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.GUIMenu.LayoutManagers.SingleElementLayout;
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
    public static final int SPACE_BETWEEN_UI_SECTIONS = 10;

    // private static final List<File> files = getFiles(Directory.graphs.getFile(), ".aut");
    private static final List<File> files = List.of(
            Directory.graphs.getFile("railway", "RailwaySafetySystem_spec.aut"),
            Directory.graphs.getFile("industrial", "lift", "lift3-init.aut"),
            Directory.graphs.getFile("industrial", "lift", "lift3-final.aut"),
            Directory.graphs.getFile("industrial", "DIRAC", "SMS.aut")
    );

    private final SContainer rightPanel;
    private final Main main;

    public Menu(Main main) {
        this.main = main;
        rightPanel = new SContainer.GhostContainer(new SingleElementLayout());
        setMainPanel(SContainer.row(
                new SFiller(),
                new SPanel(SContainer.column(
                        new GraphFileSelector(main.gui(), main, this),
                        rightPanel
                )).setGrowthPolicy(false, true)
        ));

        main.setGraphSafe(files.get(0));
        reloadUI();
    }

    public void reloadUI() {
        Graph graph = main.graph();
        NodeClustering nodeCluster = main.nodeCluster;
        SpringLayout updateLoop = main.updateLoop;
        UIFrameManager frameManager = main.frameManager;
        RenderLoop renderLoop = main.renderer;

        rightPanel.add(SContainer.column(
                new SimulationSliderUI(updateLoop),
                new SFiller(0, SPACE_BETWEEN_UI_SECTIONS).setGrowthPolicy(false, false),
                new ClusterMethodSelector(frameManager, nodeCluster),
                new SFiller(0, 10).setGrowthPolicy(false, false),
                new ColorFrame(graph.actionLabels, nodeCluster),
                new SFiller(0, 10).setGrowthPolicy(false, false),
                new TimingUI(updateLoop, renderLoop),
                new SFiller()
        ), null);
    }

    private static class GraphFileSelector extends SContainer.GhostContainer {

        public GraphFileSelector(UIFrameManager gui, Main main, Menu menu) {
            super(SContainer.column(
                    new STextArea("Graph Selector", BUTTON_PROPS),
                    new SDropDown(gui, BUTTON_PROPS, 0, files, File::getName)
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

    private static class ColorFrame extends SPanel {
        private static final int MAX_CHARACTERS_ACTION_LABELS = 35;

        public ColorFrame(String[] actionLabels, NodeClustering nodeCluster) {
            super(SContainer.column(
                    new STextArea("Color selection", BUTTON_PROPS),
                    new SScrollableList(6, getButtons(actionLabels, nodeCluster))
            ));
            setGrowthPolicy(false, false);
        }

        private static SComponent[] getButtons(String[] actionLabels, NodeClustering nodeCluster) {
            SToggleButton[] buttons = new SToggleButton[actionLabels.length];

            for (int i = 0; i < actionLabels.length; i++) {
                String label = actionLabels[i];
                buttons[i] = new SToggleButton(label, BUTTON_PROPS)
                        .addStateChangeListener(on -> nodeCluster.setAttributeColor(label, on ? EDGE_MARK_COLOR : EdgeMesh.EDGE_BASE_COLOR));
                buttons[i].setActive(false);
                buttons[i].setMaximumCharacters(MAX_CHARACTERS_ACTION_LABELS);
            }

            return buttons;
        }

    }

    private static class ClusterMethodSelector extends SDropDown {
        private static final List<Main.ClusterMethod> CLUSTER_METHODS = List.of(Main.ClusterMethod.values());

        public ClusterMethodSelector(UIFrameManager frameManager, NodeClustering nodeCluster) {
            super(frameManager, BUTTON_PROPS, 0, CLUSTER_METHODS, Enum::name);
            setGrowthPolicy(true, false);

            final String[] actionLabels = nodeCluster.getEdgeAttributes().toArray(new String[0]);

            addStateChangeListener(i -> {
                nodeCluster.setMethod(CLUSTER_METHODS.get(i));
                SFrame gui = getGUI(CLUSTER_METHODS.get(i), actionLabels, nodeCluster);
                if (gui != null) frameManager.addFrame(gui);
            });

            setCurrent(0);
        }

        private SFrame getGUI(Main.ClusterMethod clusterMethod, String[] actionLabels, NodeClustering nodeCluster) {
            switch (clusterMethod) {
                case EDGE_ATTRIBUTE:

                    SToggleButton[] buttons = new SToggleButton[actionLabels.length];

                    for (int i = 0; i < actionLabels.length; i++) {
                        String label = actionLabels[i];
                        buttons[i] = new SToggleButton(label, BUTTON_PROPS)
                                .addStateChangeListener(on -> nodeCluster.edgeAttribute(label, on));
                    }

                    return new SFrame("Collapse Edge Attributes", new SScrollableList(10, buttons));

                default:
                    return null;
            }
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
        public static final float SPEED_MAXIMUM = 1f / (1 << 11);

        public SimulationSliderUI(SpringLayout updateLoop) {
            super(SContainer.grid(new SComponent[][]{{
                            new SActiveTextArea(() -> String.format("Attraction %5.03f", updateLoop.getAttraction()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getAttraction(), BUTTON_PROPS, updateLoop::setAttraction)
                    }, {
                            new SActiveTextArea(() -> String.format("Repulsion %5.03f", updateLoop.getRepulsion()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getRepulsion(), BUTTON_PROPS, updateLoop::setRepulsion)
                    }, {
                            new SActiveTextArea(() -> String.format("NatLength %5.03f", updateLoop.getNatLength()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getNatLength(), BUTTON_PROPS, updateLoop::setNatLength)
                    }, {
                            new SActiveTextArea(() -> String.format("Speed %5.03f", updateLoop.getSpeed() / SPEED_MAXIMUM), BUTTON_PROPS),
                            new SSlider(0, SPEED_MAXIMUM, updateLoop.getSpeed(), BUTTON_PROPS, updateLoop::setSpeed)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }
}
