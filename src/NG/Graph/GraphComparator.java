package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Compares two deterministic graphs
 * @author Geert van Ieperen created on 3-9-2020.
 */
public class GraphComparator extends Graph {
    public static final Color4f A_COLOR = Color4f.rgb(200, 83, 0, 0.2f);
    public static final Color4f B_COLOR = Color4f.rgb(0, 134, 19, 0.3f);
    public static final Color4f COMBINED_COLOR = Color4f.rgb(0, 0, 0, 0.7f);

    private final Map<State, PairList<Transition, State>> incomingTransitions;
    private final Map<State, PairList<Transition, State>> outgoingTransitions;

    private final Collection<String> attributes;
    private final NodeMesh nodeMesh = new NodeMesh();
    private final EdgeMesh edgeMesh = new EdgeMesh();
    private final State initialState;

    public GraphComparator(Graph a, Graph b) {
        super(a.root);
        this.incomingTransitions = new HashMap<>();
        this.outgoingTransitions = new HashMap<>();
        this.attributes = new ArrayList<>(a.getEdgeAttributes());

        // edge case: trivial graphs
        if (a.getNrOfEdges() < 1 || b.getNrOfEdges() < 1) {
            initialState = null;
            return;
        }

        for (String attribute : b.getEdgeAttributes()) {
            if (!attributes.contains(attribute)) attributes.add(attribute);
        }

        this.initialState = new State(a.getInitialState().position, "initial", 0, 0);
        initialState.addColor(Main.INITAL_STATE_COLOR, GraphElement.Priority.BASE);
        initialState.border = Main.INITAL_STATE_COLOR;
        nodeMesh.addNode(initialState);

        addMatching(initialState, a, a.getInitialState(), b, b.getInitialState(), new HashMap<>());
    }

    /**
     * using the values in similarity map, writes both graphs into this graphs, combining as many nodes as possible
     * @param generatedNode a node in the current graph
     * @param aGraph        graph a
     * @param a             the node in graph a that relates to generatedNode
     * @param bGraph        graph b
     * @param b             the node in graph b that matches node a, and relates to generatedNode
     * @param seen          a map mapping all nodes in graph a and graph b to nodes in the current graph, excluding
     *                      generatedNode. Upon returning, a and b both map to generatedNode
     */
    void addMatching(
            State generatedNode, Graph aGraph, State a, Graph bGraph, State b, HashMap<State, State> seen
    ) {
        PairList<Transition, State> aConnections = aGraph.connectionsOf(a);
        PairList<Transition, State> bConnections = bGraph.connectionsOf(b);
        int aSize = aConnections.size();
        int bSize = bConnections.size();
        boolean[] aMatching = new boolean[aSize];
        boolean[] bMatching = new boolean[bSize];

        for (int i = 0; i < aSize; i++) {
            Transition aEdge = aConnections.left(i);

            for (int j = 0; j < bSize; j++) {
//                if (bMatching[j]) continue; // no need to match a second time
                Transition bEdge = bConnections.left(j);

                if (aEdge.label.equals(bEdge.label)) {
                    assert !bMatching[j] : String.format("Non-deterministic match : %s at nodes (%d | %d)", bEdge.label, a.index, b.index);
                    // match found
                    aMatching[i] = true;
                    bMatching[j] = true;

                    State aNode = aConnections.right(i);
                    State bNode = bConnections.right(j);

                    boolean exists = seen.containsKey(aNode);
                    State newNode = exists ? seen.get(aNode) : new State(aNode.position, "A" + aNode.label + "|B" + bNode.label, i, aNode.classIndex);
                    Transition newEdge = new Transition(generatedNode, newNode, aEdge.label);

                    edgeMesh.addParticle(newEdge);
                    newEdge.addColor(COMBINED_COLOR, GraphElement.Priority.BASE);
                    outgoingTransitions.computeIfAbsent(generatedNode, s -> new PairList<>()).add(newEdge, newNode);
                    incomingTransitions.computeIfAbsent(newNode, s -> new PairList<>()).add(newEdge, generatedNode);

                    if (!exists) {
                        seen.put(aNode, newNode);
                        seen.put(bNode, newNode);
                        nodeMesh.addNode(newNode);
                        addMatching(newNode, aGraph, aNode, bGraph, bNode, seen);
                    }

                    break;
                }
            }
        }

        for (int i = 0; i < aSize; i++) {
            if (!aMatching[i]) {
                add(generatedNode, aGraph, "A", A_COLOR, seen, aConnections, i);
            }
        }

        for (int j = 0; j < bSize; j++) {
            if (!bMatching[j]) {
                add(generatedNode, bGraph, "B", B_COLOR, seen, bConnections, j);
            }
        }
    }

    /**
     * adds all children of targetNode to this graph, as a not-matching side-graph of the given color, where each node
     * has the given prefix.
     */
    private void addChildren(
            State parentNode, State targetNode, String prefix, Color4f color,
            Map<State, State> seen, Graph graph
    ) {
        PairList<Transition, State> connections = graph.connectionsOf(targetNode);
        for (int i = 0; i < connections.size(); i++) {
            if (connections.left(i).from != targetNode) continue;
            add(parentNode, graph, prefix, color, seen, connections, i);
        }
    }

    /**
     * adds targetNode and all its children to this graph, as a not-matching side-graph of the given color, where each
     * node has the given prefix. Each new node assumes parentNode as its parent
     */
    private void add(
            State parentNode, Graph graph, String prefix, Color4f color,
            Map<State, State> seen, PairList<Transition, State> connections,
            int index
    ) {
        State node = connections.right(index);
        Transition edge = connections.left(index);

        boolean exists = seen.containsKey(node);
        State newNode = exists ? seen.get(node) : new State(node.position, prefix + node.label, index, index);
        Transition newEdge = new Transition(parentNode, newNode, edge.label);

        edgeMesh.addParticle(newEdge);
        newEdge.addColor(color, GraphElement.Priority.BASE);
        outgoingTransitions.computeIfAbsent(parentNode, s -> new PairList<>()).add(newEdge, newNode);

        if (!exists) {
            nodeMesh.addNode(newNode);
            newNode.addColor(color.opaque(), GraphElement.Priority.BASE);

            seen.put(node, newNode);
            addChildren(newNode, node, prefix, color, seen, graph);
        }
    }

    @Override
    protected State getInitialState() {
        return initialState;
    }

    @Override
    public PairList<Transition, State> incomingOf(State node) {
        return incomingTransitions.getOrDefault(node, PairList.empty());
    }

    @Override
    public PairList<Transition, State> outgoingOf(State node) {
        return outgoingTransitions.getOrDefault(node, PairList.empty());
    }

    @Override
    public NodeMesh getNodeMesh() {
        return nodeMesh;
    }

    @Override
    public EdgeMesh getEdgeMesh() {
        return edgeMesh;
    }

    @Override
    public Collection<String> getEdgeAttributes() {
        return attributes;
    }

    @Override
    public void cleanup() {
        root.executeOnRenderThread(() -> {
            nodeMesh.dispose();
            edgeMesh.dispose();
        });
    }

    public void updateEdges() {
        for (Transition edge : edgeMesh.edgeList()) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
            assert !Vectors.isNaN(edge.handlePos) : edge;
        }
    }
}
