package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public class Graph {
    private static final Pattern dash = Pattern.compile("-");
    private static final Pattern separator = Pattern.compile(";");

    public final NodeMesh.Node[] states;
    public final EdgeMesh.Edge[] edges;
    public final String[] actionLabels;

    // initialized at init()
    public final NodeMesh nodeMesh;
    public final EdgeMesh edgeMesh;
    public final Map<NodeMesh.Node, Collection<NodeMesh.Node>> mapping;

    private Graph(int numStates, int numTransitions, String[] actionLabels) {
        this.mapping = new HashMap<>();
        this.nodeMesh = new NodeMesh();
        this.edgeMesh = new EdgeMesh();

        this.actionLabels = actionLabels;
        this.states = new NodeMesh.Node[numStates];
        this.edges = new EdgeMesh.Edge[numTransitions];
    }

    public void init(){
        for (EdgeMesh.Edge edge : edges) {
            // create mapping
            mapping.computeIfAbsent(edge.a, node -> new ArrayList<>(0))
                    .add(edge.b);
            // make sure deadlocks are part of the mapping keys
            mapping.computeIfAbsent(edge.b, node -> new ArrayList<>(0));
        }

        // create position mapping
        double[][] positions = HDEPositioning.position(edges, states);
        for (int i = 0; i < states.length; i++) {
            states[i].position.set(
                    positions[i].length > 0 ? (float) positions[i][0] : 0,
                    positions[i].length > 1 ? (float) positions[i][1] : 0,
                    positions[i].length > 2 ? (float) positions[i][2] : 0
            );
        }

        // set positions to graph
        for (NodeMesh.Node node : states) {
            nodeMesh.addParticle(node);
        }
        for (EdgeMesh.Edge edge : edges) {
            edgeMesh.addParticle(edge);
        }
    }

    public static Graph readPlainString(String data) {
        String[] lines = data.split("\n");

        String[] stateLabels = separator.split(lines[0]);
        String[] actionLabels = separator.split(lines[1]);
        String[] transitions = separator.split(lines[2]);
        Graph graph = new Graph(stateLabels.length, transitions.length, actionLabels);

        for (int i = 0; i < stateLabels.length; i++) {
            graph.states[i] = new NodeMesh.Node(Vectors.O, stateLabels[i]);
        }

        graph.states[0].color = Color4f.GREEN;

        for (int i = 0; i < transitions.length; i++) {
            String elt = transitions[i];
            String[] elements = dash.split(elt);
            int a = Integer.parseInt(elements[0]);
            int b = Integer.parseInt(elements[1]);
            int labelInd = Integer.parseInt(elements[2]);

            NodeMesh.Node aNode = graph.states[a];
            NodeMesh.Node bNode = graph.states[b];
            String label = actionLabels[labelInd];

            graph.edges[i] = new EdgeMesh.Edge(aNode, bNode, label);
        }

        return graph;
    }
}
