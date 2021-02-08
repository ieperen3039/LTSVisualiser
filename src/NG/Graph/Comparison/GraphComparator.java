package NG.Graph.Comparison;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Graph;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.Tools.Vectors;

import java.util.*;

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

    private final Collection<String> actions;
    private final NodeMesh nodeMesh = new NodeMesh();
    private final EdgeMesh edgeMesh = new EdgeMesh();
    private final State initialState;
    private int index = 0;

    public GraphComparator(Main root, Graph a, Graph b) {
        super(root);
        this.incomingTransitions = new HashMap<>();
        this.outgoingTransitions = new HashMap<>();
        this.actions = new ArrayList<>(a.getEdgeLabels());

        // edge case: trivial graphs
        if (a.getNrOfEdges() < 1 || b.getNrOfEdges() < 1) {
            initialState = null;
            return;
        }

        for (String label : b.getEdgeLabels()) {
            if (!actions.contains(label)) actions.add(label);
        }

        State aInitialState = a.getInitialState();
        State bInitialState = b.getInitialState();
        this.initialState = new State(aInitialState.position, "initial", index, index++);
        initialState.addColor(Main.INITAL_STATE_COLOR, GraphElement.Priority.BASE);
        initialState.border = Main.INITAL_STATE_COLOR;
        nodeMesh.addNode(initialState);

        HashMap<State, State> seen = new HashMap<>();
        seen.put(aInitialState, initialState);
        seen.put(bInitialState, initialState);

        addMatching(initialState, a, aInitialState, b, bInitialState, seen);
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
//                    assert !bMatching[j] : String.format("Non-deterministic match : %s at nodes (%d | %d)", bEdge.label, a.index, b.index);
                    // match found
                    aMatching[i] = true;
                    bMatching[j] = true;

                    State aNode = aConnections.right(i);
                    State bNode = bConnections.right(j);

                    boolean exists = seen.containsKey(aNode);
                    State newNode = exists ? seen.get(aNode) : new State(
                            aNode.position, "A" + aNode.label + "|B" + bNode.label, index, index++
                    );
                    Transition newEdge = new Transition(generatedNode, newNode, aEdge.label);

                    edgeMesh.addParticle(newEdge);
                    newEdge.addColor(COMBINED_COLOR, GraphElement.Priority.BASE);
                    outgoingTransitions.computeIfAbsent(generatedNode, s -> new PairList<>()).add(newEdge, newNode);
                    incomingTransitions.computeIfAbsent(newNode, s -> new PairList<>()).add(newEdge, generatedNode);

                    if (!exists) {
                        bNode.position.set(aNode.position);
                        bEdge.handlePos.set(aEdge.handlePos);

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
                State node = aConnections.right(i);
                Transition transition = aConnections.left(i);
                add(generatedNode, aGraph, "A", A_COLOR, seen, node, transition);
            }
        }

        for (int j = 0; j < bSize; j++) {
            if (!bMatching[j]) {
                State node = bConnections.right(j);
                Transition transition = bConnections.left(j);
                add(generatedNode, bGraph, "B", B_COLOR, seen, node, transition);
            }
        }
    }

    /**
     * adds targetNode and all its children to this graph, as a not-matching side-graph of the given color, where each
     * node has the given prefix. Each new node assumes parentNode as its parent
     */
    private void add(
            State parentNode, Graph graph, String prefix, Color4f color,
            Map<State, State> seen, State node, Transition edge
    ) {
        boolean exists = seen.containsKey(node);
        State newNode = exists ? seen.get(node) : new State(node.position, prefix + node.label, index, index++);
        Transition newEdge = new Transition(parentNode, newNode, edge.label);

        edgeMesh.addParticle(newEdge);
        newEdge.addColor(color, GraphElement.Priority.BASE);
        outgoingTransitions.computeIfAbsent(parentNode, s -> new PairList<>()).add(newEdge, newNode);
        incomingTransitions.computeIfAbsent(newNode, s -> new PairList<>()).add(newEdge, parentNode);

        if (!exists) {
            nodeMesh.addNode(newNode);
            newNode.addColor(color.opaque(), GraphElement.Priority.BASE);

            seen.put(node, newNode);

            List<Transition> connections = node.getOutgoing();
            for (Transition connection : connections) {
                assert (connection.from == node);
                State state = connection.to;
                add(newNode, graph, prefix, color, seen, state, connection);
            }
        }
    }

    @Override
    public State getInitialState() {
        return initialState;
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
    public Collection<String> getEdgeLabels() {
        return actions;
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
