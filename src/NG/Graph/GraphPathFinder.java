package NG.Graph;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements Breadth first search to find the shortest path
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class GraphPathFinder implements Callable<List<Transition>> {
    private final State startNode;
    private final State endNode;
    private final Graph graph;

    public GraphPathFinder(State startNode, State endNode, Graph graph) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.graph = graph;
    }

    @Override
    public List<Transition> call() {
        ArrayDeque<State> open = new ArrayDeque<>();
        open.add(startNode);

        Map<State, Transition> predecessors = BFS(open);
        if (predecessors == null) return null;

        State current = this.endNode;
        List<Transition> edges = new ArrayList<>();

        while (current != startNode) {
            Transition edge = predecessors.get(current);
            edges.add(edge);
            current = edge.from;
        }

        Collections.reverse(edges);
        return edges;
    }

    /**
     * computes all nearest predecessors
     * @param open a collection of nodes to start searching from. Usually, contains only the starting node. Upon
     *             returning, its contents is undefined.
     * @return the nearest node in {@code targets} found.
     */
    private Map<State, Transition> BFS(ArrayDeque<State> open) {
        Map<State, Transition> predecessors = new HashMap<>();

        while (!open.isEmpty()) {
            State node = open.remove();
            if (node == endNode) return predecessors;

            List<Transition> outgoing = node.getOutgoing();

            for (Transition nextEdge : outgoing) {
                State nextNode = nextEdge.to;

                if (nextNode == node) continue; // self-loop
                if (predecessors.containsKey(nextNode)) continue;

                predecessors.put(nextNode, nextEdge);
                open.add(nextNode);
            }
        }

        return null;
    }
}
