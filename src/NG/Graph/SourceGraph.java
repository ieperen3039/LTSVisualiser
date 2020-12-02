package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.MuChecker.StateSet;
import NG.Tools.Logger;
import NG.Tools.Vectors;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static NG.Core.Main.INITAL_STATE_COLOR;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public class SourceGraph extends Graph {
    private static final Pattern headerPattern = Pattern.compile("des \\((\\d+),\\s?(\\d+),\\s?(\\d+)\\)\\s*");
    private static final Pattern edgePattern = Pattern.compile("\\((\\d+),\\s?\"(.*)\",\\s?(\\d+)\\)\\s*");

    public final State[] states;
    public final Transition[] edges;
    private final String[] actionLabels;
    // maps nodes to their neighbours
    private final Map<State, PairList<Transition, State>> incomingTransitions;
    private final Map<State, PairList<Transition, State>> outgoingTransitions;

    private final NodeMesh nodeMesh;
    private final EdgeMesh edgeMesh;
    private int initialState = 0;
    private final float natLength;

    private SourceGraph(Main root, int numStates, int numTransitions, float natLength) {
        super(root);
        this.natLength = natLength;
        this.incomingTransitions = new HashMap<>();
        this.outgoingTransitions = new HashMap<>();
        this.nodeMesh = new NodeMesh();
        this.edgeMesh = new EdgeMesh();

        this.states = new State[numStates];
        this.edges = new Transition[numTransitions];
        this.actionLabels = new String[numTransitions];
    }

    public void init() {
        if (edges.length < 1) return;

        // create position mapping
        double[][] positions = HDEPositioning.position(edges, states);
        for (int i = 0; i < states.length; i++) {
            states[i].position.set(
                    positions[i].length > 0 ? (float) positions[i][0] : 0,
                    positions[i].length > 1 ? (float) positions[i][1] : 0,
                    positions[i].length > 2 ? (float) positions[i][2] : 0
            ).mul(natLength);
        }

        // set positions to graph
        for (State node : states) {
            getNodeMesh().addNode(node);

            if (isDeadlocked(node)) {
                node.border = Color4f.RED;
            }
        }
        for (Transition edge : edges) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
            getEdgeMesh().addParticle(edge);
        }

        // initial state
        State initialState = getInitialState();
        initialState.addColor(INITAL_STATE_COLOR, GraphElement.Priority.BASE);
        initialState.border = INITAL_STATE_COLOR;

        // collect confluent classes
        long startMillis = System.currentTimeMillis();
        Collection<List<State>> confluentStates = new ConfluenceDetector(this).call();
        long endMillis = System.currentTimeMillis();

        Logger.DEBUG.printf("%d confluent groups (%d ms)", confluentStates.size(), endMillis - startMillis);

        for (List<State> list : confluentStates) {
            int classIndex = list.get(0).classIndex;

            for (State state : list) {
                state.classIndex = classIndex;
            }
        }
    }

    public boolean isDeadlocked(State node) {
        if (!outgoingTransitions.containsKey(node)) return true;
        assert (!outgoingTransitions.get(node).isEmpty());
        return false;
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
    public void cleanup() {
        root.executeOnRenderThread(nodeMesh::dispose);
        root.executeOnRenderThread(edgeMesh::dispose);
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
        return Arrays.asList(actionLabels);
    }

    public StateSet getUniverse() {
        return StateSet.allOf(states);
    }

    public StateSet getEmptySet() {
        return StateSet.noneOf(states);
    }

    @Override
    public State getInitialState() {
        if (states.length == 0) return null;
        return states[initialState];
    }

    /**
     * Starts with 1 header line
     * <ul>
     * <li>aut_header           ::=  'des (' first_state ',' nr_of_transitions ',' nr_of_states ')'</li>
     * <li>first_state          ::=  number</li>
     * <li>nr_of_transitions    ::=  number</li>
     * <li>nr_of_states         ::=  number</li>
     * </ul>
     * Followed by a number of edges
     * <ul>
     * <li>aut_edge             ::=  '(' start_state ',' label ',' end_state ')'</li>
     * <li>start_state          ::=  number</li>
     * <li>label                ::=  '"' string '"'</li>
     * <li>end_state            ::=  number</li>
     * </ul>
     * @param ltsFile the file containing the graph
     * @param root
     */
    public static SourceGraph parse(File ltsFile, Main root) throws IOException {
        return parse(new Scanner(ltsFile, "UTF8"), root);
    }

    /** @see #parse(File, Main) */
    public static SourceGraph parse(Scanner scanner, Main root) throws IOException {
        // parse header
        String header = scanner.nextLine();

        Matcher matcher = headerPattern.matcher(header);
        boolean doesMatch = matcher.find();
        if (!doesMatch) throw new IOException(header);

        int initialStateIndex = Integer.parseInt(matcher.group(1));
        int nrOfTransitions = Integer.parseInt(matcher.group(2));
        int nrOfStates = Integer.parseInt(matcher.group(3));

        Logger.DEBUG.printf("Loading graph with %d states and %d transitions...", nrOfStates, nrOfTransitions);

        float natLength = root == null ? 1 : root.getSpringLayout().getNatLength();
        SourceGraph graph = new SourceGraph(root, nrOfStates, nrOfTransitions, natLength);
        graph.initialState = initialStateIndex;

        // prepare states
        for (int i = 0; i < nrOfStates; i++) {
            graph.states[i] = new State(Vectors.O, Integer.toString(i), i, i);
        }

        // parse edges
        int edgeIndex = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            matcher = edgePattern.matcher(line);
            boolean doesMatchEdge = matcher.find();
            if (!doesMatchEdge) throw new IOException(line);

            int startStateIndex = Integer.parseInt(matcher.group(1));
            String label = matcher.group(2);
            int endStateIndex = Integer.parseInt(matcher.group(3));

            State startState = graph.states[startStateIndex];
            State endState = graph.states[endStateIndex];
            Transition edge = new Transition(startState, endState, label);

            graph.actionLabels[edgeIndex] = label;
            graph.edges[edgeIndex] = edge;
            graph.outgoingTransitions.computeIfAbsent(startState, s -> new PairList<>()).add(edge, endState);
            graph.incomingTransitions.computeIfAbsent(endState, s -> new PairList<>()).add(edge, startState);

            edgeIndex++;
        }

        return graph;
    }

    public static SourceGraph empty(Main root) {
        return new SourceGraph(root, 0, 0, 1);
    }

    public static SourceGraph parse(String asString) {
        try {
            return parse(new Scanner(asString), null);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
