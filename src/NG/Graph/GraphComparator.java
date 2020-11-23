package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.MuChecker.StateSet;
import NG.Tools.Vectors;
import org.joml.Vector3f;

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

        HashMap<State, State> seen = new HashMap<>();
        seen.put(a.getInitialState(), initialState);
        seen.put(b.getInitialState(), initialState);

        addMatching(initialState, a, a.getInitialState(), b, b.getInitialState(), seen);
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
                add(generatedNode, aGraph, "A", A_COLOR, seen, i, node, transition);
            }
        }

        for (int j = 0; j < bSize; j++) {
            if (!bMatching[j]) {
                State node = bConnections.right(j);
                Transition transition = bConnections.left(j);
                add(generatedNode, bGraph, "B", B_COLOR, seen, j, node, transition);
            }
        }
    }

    /**
     * adds targetNode and all its children to this graph, as a not-matching side-graph of the given color, where each
     * node has the given prefix. Each new node assumes parentNode as its parent
     */
    private void add(
            State parentNode, Graph graph, String prefix, Color4f color,
            Map<State, State> seen, int index, State node, Transition edge
    ) {
        boolean exists = seen.containsKey(node);
        State newNode = exists ? seen.get(node) : new State(node.position, prefix + node.label, index, index);
        Transition newEdge = new Transition(parentNode, newNode, edge.label);

        edgeMesh.addParticle(newEdge);
        newEdge.addColor(color, GraphElement.Priority.BASE);
        outgoingTransitions.computeIfAbsent(parentNode, s -> new PairList<>()).add(newEdge, newNode);
        incomingTransitions.computeIfAbsent(newNode, s -> new PairList<>()).add(newEdge, parentNode);

        if (!exists) {
            nodeMesh.addNode(newNode);
            newNode.addColor(color.opaque(), GraphElement.Priority.BASE);

            seen.put(node, newNode);

            PairList<Transition, State> connections = graph.outgoingOf(node);
            for (int i = 0; i < connections.size(); i++) {
                assert (connections.left(i).from == node);
                State right = connections.right(i);
                Transition left = connections.left(i);
                add(newNode, graph, prefix, color, seen, i, right, left);
            }
        }
    }

    /**
     * Computes the largest bi-similar subgraph between two graphs. Based on the HKC algorithm described in {@code
     * https://hal.archives-ouvertes.fr/hal-00639716v5/document.}
     * <p>
     * (Filippo Bonchi, Damien Pous. Checking NFA equivalence with bisimulations up to congruence. Jan 2013.)
     * @param alpha one graph
     * @param beta  another graph
     */
    private void nonDeterministicEquivalence(SourceGraph alpha, SourceGraph beta) {
        PairList<StateSet, StateSet> relations = new PairList<>();
        Deque<HKCStep> todo = new ArrayDeque<>();
        int nodeIndex = 0;

        todo.add(new HKCStep(alpha, alpha.getInitialState(), beta, beta.getInitialState()));

        while (!todo.isEmpty()) {
            HKCStep element = todo.remove();
            StateSet unrelatedLeft = new StateSet(element.a);
            StateSet unrelatedRight = new StateSet(element.b);

            // ignore this new relation if this is congruent in relations
            int nrRelations = relations.size();
            for (int i = 0; i < nrRelations; i++) {
                // if the relation matches, all the states in this relation are not unrelated
                StateSet rLeft = relations.left(i);
                StateSet rRight = relations.right(i);

                if (rLeft.isSubsetOf(element.a) && rRight.isSubsetOf(element.b)) {
                    unrelatedLeft.removeAll(rLeft);
                    unrelatedRight.removeAll(rRight);
//                    if (unrelatedLeft.isEmpty() && unrelatedRight.isEmpty()) break;
                }
            }

            if (unrelatedLeft.isEmpty() && unrelatedRight.isEmpty()) continue;

            Map<String, StateSet> deltaSharpA = getTransitionMap(element.a);
            Map<String, StateSet> deltaSharpB = getTransitionMap(element.b);

            Set<String> allLabels = new HashSet<>(deltaSharpA.keySet());
            allLabels.addAll(deltaSharpB.keySet());

            for (String label : allLabels) {
                StateSet alphaStates = deltaSharpA.get(label);
                StateSet betaStates = deltaSharpB.get(label);

                if (alphaStates == null) {
                    // TODO
                } else if (betaStates == null) {
                    // TODO
                } else {
                    todo.add(new HKCStep(alphaStates, betaStates, label, null));
                }
            }

            relations.add(element.a, element.b);

            // now add the new node and transitions
            Vector3f position = element.getAveragePosition();
            String label = element.getCombinedName();
            State newNode = new State(position, label, nodeIndex, nodeIndex);
            nodeIndex++;
            nodeMesh.addNode(newNode);

            Transition newEdge = new Transition(element.source, newNode, element.label);

            edgeMesh.addParticle(newEdge);
            newEdge.addColor(COMBINED_COLOR, GraphElement.Priority.BASE);
            outgoingTransitions.computeIfAbsent(element.source, s -> new PairList<>()).add(newEdge, newNode);
            incomingTransitions.computeIfAbsent(newNode, s -> new PairList<>()).add(newEdge, element.source);
        }
    }

    /**
     * Collects all next transitions from the given set of states, and groups them by label.
     * @param states an initial set of states S
     * @return the map l -> S', where for each s in S', there is a state p in S such that p -l-> s
     */
    private Map<String, StateSet> getTransitionMap(StateSet states) {
        // delta-sharp(S)(a) means "the union of all states reached with an a-label from all states of S"
        Map<String, StateSet> deltaSharpA = new HashMap<>();
        for (State alphaState : states) {
            for (Pair<Transition, State> outAlpha : outgoingOf(alphaState)) {
                String label = outAlpha.left.label;
                deltaSharpA.computeIfAbsent(label, s -> StateSet.noneOf(states.universe))
                        .add(outAlpha.right);
            }
        }
        return deltaSharpA;
    }

    private static class HKCStep {
        public final StateSet a;
        public final StateSet b;
        public final String label;
        public final State source;

        public HKCStep(SourceGraph aGraph, State a, SourceGraph bGraph, State b) {
            this.a = aGraph.getEmptySet();
            this.a.add(a);
            this.b = bGraph.getEmptySet();
            this.b.add(b);
            this.label = "";
            this.source = null;
        }

        public HKCStep(StateSet a, StateSet b, String label, State source) {
            this.a = a;
            this.b = b;
            this.label = label;
            this.source = source;
        }

        private Vector3f getAveragePosition() {
            int nrElts = 0;
            Vector3f position = new Vector3f();
            for (State state : a) {
                position.add(state.position);
                nrElts++;
            }
            for (State state : b) {
                position.add(state.position);
                nrElts++;
            }
            position.div(nrElts);
            return position;
        }

        private String getCombinedName() {
            StringJoiner joiner = new StringJoiner(",");
            for (State state : a) {
                joiner.add(state.toString());
            }
            for (State state : b) {
                joiner.add(state.toString());
            }

            return joiner.toString();
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
