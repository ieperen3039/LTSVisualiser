package NG.Graph;

import NG.DataStructures.Generic.Color4f;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.GraphElement;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.util.*;

/**
 * @author Geert van Ieperen created on 3-9-2020.
 */
public class GraphComparator extends Graph {
    public static final Color4f A_COLOR = Color4f.rgb(200, 83, 0, 0.1f);
    public static final Color4f B_COLOR = Color4f.rgb(0, 134, 19, 0.2f);
    public static final Color4f COMBINED_COLOR = Color4f.rgb(0, 0, 0, 0.8f);

    private final Map<State, PairList<Transition, State>> mapping;
    private final Collection<String> attributes;
    private final NodeMesh nodeMesh = new NodeMesh();
    private final EdgeMesh edgeMesh = new EdgeMesh();
    private final State initialState;

    public GraphComparator(Graph a, Graph b) {
        super(a.root);
        this.mapping = new HashMap<>();
        this.attributes = new ArrayList<>(a.getEdgeAttributes());

        // edge case: trivial graphs
        if (a.getNrOfEdges() < 1 || b.getNrOfEdges() < 1) {
            initialState = null;
            return;
        }

        for (String attribute : b.getEdgeAttributes()) {
            if (!attributes.contains(attribute)) attributes.add(attribute);
        }

        HashMap<State, Map<State, Integer>> similarityMap = new HashMap<>();
        findMatching(a, a.getInitialState(), b, b.getInitialState(), similarityMap);

        this.initialState = new State(a.getInitialState().position, "initial", 0, 0);
        initialState.addColor(Color4f.GREEN, GraphElement.Priority.INITIAL_STATE);
        nodeMesh.addNode(initialState);

        addMatching(initialState, a, a.getInitialState(), b, b.getInitialState(), similarityMap, new HashMap<>());
    }

    /**
     * Recursively generates the combined graph assuming {@code original} is the node representing the combination of a
     * and b.
     * @param aGraph graph a
     * @param a      a node in graph a
     * @param bGraph graph b
     * @param b      a node in graph b the is equivalent to node a
     * @return the number of matching nodes
     */
    private int findMatching(
            Graph aGraph, State a, Graph bGraph, State b, Map<State,
            Map<State, Integer>> similarityMap
    ) {
        if (similarityMap.containsKey(a)) {
            Map<State, Integer> aMap = similarityMap.get(a);
            if (aMap.containsKey(b)) {
                return aMap.get(b);
            }
        }
        // prevent loops when recursing
        similarityMap.computeIfAbsent(a, node -> new HashMap<>(2)).put(b, 0);

        PairList<Transition, State> aConnections = aGraph.connectionsOf(a);
        PairList<Transition, State> bConnections = bGraph.connectionsOf(b);

        int bSize = bConnections.size();
        int aSize = aConnections.size();
        Map<String, Collection<State>> aLabelIndices = new HashMap<>(aSize);
        Map<String, Collection<State>> bLabelIndices = new HashMap<>(bSize);

        for (int i = 0; i < aSize; i++) {
            Transition left = aConnections.left(i);

            if (left.from == a) {
                String aLabel = left.label;
                aLabelIndices.computeIfAbsent(aLabel, s -> new HashSet<>()).add(aConnections.right(i));
            }

        }

        for (int i = 0; i < bSize; i++) {
            Transition left = bConnections.left(i);

            if (left.from == b) {
                String bLabel = left.label;
                bLabelIndices.computeIfAbsent(bLabel, s -> new HashSet<>()).add(bConnections.right(i));
            }
        }

        ArrayList<String> intersect = new ArrayList<>(aLabelIndices.keySet());
        intersect.retainAll(bLabelIndices.keySet());

        if (intersect.isEmpty()) return 0;

        int mostMatchingNodes = -1;

        for (String label : intersect) {
            Collection<State> aNodes = aLabelIndices.get(label);
            Collection<State> bNodes = bLabelIndices.get(label);

            for (State aNode : aNodes) {
                for (State bNode : bNodes) {
                    int length = findMatching(aGraph, aNode, bGraph, bNode, similarityMap);
                    if (length > mostMatchingNodes) {
                        mostMatchingNodes = length;
                    }
                }
            }
        }

        assert mostMatchingNodes != -1;

        int thisMatchingNodes = mostMatchingNodes + 1;

        similarityMap.get(a).put(b, thisMatchingNodes);
        return thisMatchingNodes;
    }

    /**
     * using the values in similarity map, writes both graphs into this graphs, combining as many nodes as possible
     * @param generatedNode a node in the current graph
     * @param aGraph        graph a
     * @param a             the node in graph a that relates to generatedNode
     * @param bGraph        graph b
     * @param b             the node in graph b that matches node a, and relates to generatedNode
     * @param similarityMap maps a node in graph a to nodes in graph b and the similarity of that pair of nodes. Is not
     *                      modified in this method.
     * @param seen          a map mapping all nodes in graph a and graph b to nodes in the current graph, excluding
     *                      generatedNode. Upon returning, a and b both map to generatedNode
     */
    void addMatching(
            State generatedNode, Graph aGraph, State a, Graph bGraph, State b,
            Map<State, Map<State, Integer>> similarityMap, HashMap<State, State> seen
    ) {
        seen.put(a, generatedNode);
        seen.put(b, generatedNode);

        PairList<Transition, State> aConnections = aGraph.connectionsOf(a);
        PairList<Transition, State> bConnections = bGraph.connectionsOf(b);

        Collection<Integer> aNonMatching = new ArrayList<>(0);
        Collection<Integer> bNonMatching = new HashSet<>(0);
        // we find for each aNode whether there is a matching bNode.
        // thus we only find matching bNodes, and have to track the non-matching nodes
        for (int i = 0; i < bConnections.size(); i++) {
            bNonMatching.add(i);
        }

        for (int i = 0; i < aConnections.size(); i++) {
            if (aConnections.left(i).from != a) continue;
            State aNode = aConnections.right(i);

            Map<State, Integer> bSimilarities = similarityMap.get(aNode);
            State bNode = null;
            if (bSimilarities != null && !bSimilarities.isEmpty()) {
                int maxValue = -1;
                for (int j = 0; j < bConnections.size(); j++) {
                    if (bConnections.left(j).from != b) continue;
                    State bCandidate = bConnections.right(j);
                    int value = bSimilarities.getOrDefault(bCandidate, -1);
                    if (value > maxValue) {
                        maxValue = value;
                        bNode = bCandidate;
                    }
                }
            }

            if (bNode == null) {
                aNonMatching.add(i);

            } else {
                int index = bConnections.indexOfRight(bNode);
                assert index != -1 : bNode;
                bNonMatching.remove(index);

                boolean exists = seen.containsKey(aNode);
                Transition aEdge = aConnections.left(i);
                State newNode = exists ? seen.get(aNode) : new State(aNode.position, "A" + aNode.label + "|B" + bNode.label, i, index);
                Transition newEdge = new Transition(generatedNode, newNode, aEdge.label);

                edgeMesh.addParticle(newEdge);
                newEdge.addColor(COMBINED_COLOR, GraphElement.Priority.BASE);
                mapping.computeIfAbsent(generatedNode, s -> new PairList<>()).add(newEdge, newNode);

                if (!exists) {
                    nodeMesh.addNode(newNode);
                    addMatching(newNode, aGraph, aNode, bGraph, bNode, similarityMap, seen);
                }
            }
        }

        for (Integer index : aNonMatching) {
            add(generatedNode, aGraph, "A", A_COLOR, seen, aConnections, index);
        }
        for (Integer index : bNonMatching) {
            add(generatedNode, bGraph, "B", B_COLOR, seen, bConnections, index);
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
        mapping.computeIfAbsent(parentNode, s -> new PairList<>()).add(newEdge, newNode);

        if (!exists) {
            nodeMesh.addNode(newNode);
            newNode.addColor(color, GraphElement.Priority.BASE);

            seen.put(node, newNode);
            addChildren(newNode, node, prefix, color, seen, graph);
        }
    }

    @Override
    protected State getInitialState() {
        return initialState;
    }

    @Override
    public PairList<Transition, State> connectionsOf(
            State node
    ) {
        return mapping.getOrDefault(node, PairList.empty());
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
