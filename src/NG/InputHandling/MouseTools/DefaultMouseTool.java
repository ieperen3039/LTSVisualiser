package NG.InputHandling.MouseTools;

import NG.Core.Main;
import NG.Graph.Graph;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Logger;

/**
 * A mouse tool that implements the standard behaviour of the pointer user input.
 *
 * <dl>
 * <dt>Entities:</dt>
 * <dd>The entity gets selected</dd>
 * <dt>Map:</dt>
 * <dd>If an entity is selected, open an action menu</dd>
 * </dl>
 * @author Geert van Ieperen. Created on 26-11-2018.
 */
public class DefaultMouseTool extends MouseTool {

    public DefaultMouseTool(Main root) {
        super(root);
    }

    @Override
    public void onNodeClick(int button, Graph graph, NodeMesh.Node node) {
        Logger.DEBUG.print(node);
    }

    @Override
    public void onEdgeClick(int button, Graph graph, EdgeMesh.Edge edge) {
        Logger.DEBUG.print(edge);
    }

}
