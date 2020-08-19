package NG.GUIMenu;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.FrameManagers.UIFrameManager;
import NG.Graph.NodeClustering;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.SpringLayout;
import NG.Graph.Graph;
import NG.Rendering.RenderLoop;
import NG.Tools.Logger;

import java.util.List;

/**
 * @author Geert van Ieperen created on 7-8-2020.
 */
public class Menu extends SDecorator {
    public static final SComponentProperties BUTTON_PROPS = new SComponentProperties(150, 30);
    public static final Color4f EDGE_MARK_COLOR = Color4f.rgb(240, 190, 0);
    private final NodeClustering nodeCluster;
    private final Graph parser;

    public Menu(Main main) {
        this.nodeCluster = main.nodeCluster;
        this.parser = main.graph;
        SpringLayout updateLoop = main.updateLoop;
        UIFrameManager frameManager = main.frameManager;
        RenderLoop renderLoop = main.renderer;

        setMainPanel(SContainer.row(
                new SFiller(),
                new SPanel(SContainer.column(
                        new SimulationSliderUI(updateLoop),
                        new SFiller(),
                        new ClusterMethodSelector(frameManager, nodeCluster, parser.actionLabels),
                        new SFiller(),
                        new ColorFrame(parser.actionLabels, nodeCluster),
                        new SFiller(),
                        new TimingUI(updateLoop, renderLoop),
                        new SFiller()
                )).setGrowthPolicy(false, true)
        ));
    }

    private static class ColorFrame extends SPanel {
        public ColorFrame(String[] actionLabels, NodeClustering nodeCluster) {
            super(SContainer.column(
                    new STextArea("Color selection", BUTTON_PROPS),
                    new SScrollableList(5, getButtons(actionLabels, nodeCluster))
            ));
            setGrowthPolicy(false, false);
        }

        private static SComponent[] getButtons(String[] actionLabels, NodeClustering nodeCluster){
            SToggleButton[] buttons = new SToggleButton[actionLabels.length];

            for (int i = 0; i < actionLabels.length; i++) {
                String label = actionLabels[i];
                buttons[i] = new SToggleButton(label, BUTTON_PROPS)
                        .addStateChangeListener(on -> nodeCluster.setAttributeColor(label, on ? EDGE_MARK_COLOR : EdgeMesh.EDGE_BASE_COLOR));
                buttons[i].setActive(false);
            }

            return buttons;
        }

    }

    private static class ClusterMethodSelector extends SDropDown {
        private static final List<Main.ClusterMethod> CLUSTER_METHODS = List.of(Main.ClusterMethod.values());

        public ClusterMethodSelector(UIFrameManager frameManager, NodeClustering nodeCluster, String[] actionLabels) {
            super(frameManager, BUTTON_PROPS, 0, CLUSTER_METHODS, Enum::name);
            setGrowthPolicy(true, false);

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
                    },{
                            new SActiveTextArea(() -> String.format("Repulsion %5.03f", updateLoop.getRepulsion()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getRepulsion(), BUTTON_PROPS, updateLoop::setRepulsion)
                    },{
                            new SActiveTextArea(() -> String.format("NatLength %5.03f", updateLoop.getNatLength()), BUTTON_PROPS),
                            new SSlider(0, 10f, updateLoop.getNatLength(), BUTTON_PROPS, updateLoop::setNatLength)
                    },{
                            new SActiveTextArea(() -> String.format("Speed %5.03f", updateLoop.getSpeed() / SPEED_MAXIMUM), BUTTON_PROPS),
                            new SSlider(0, SPEED_MAXIMUM, updateLoop.getSpeed(), BUTTON_PROPS, updateLoop::setSpeed)
                    }}
            ));
            setGrowthPolicy(true, false);
        }
    }
}
