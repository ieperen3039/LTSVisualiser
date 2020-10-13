package NG.Graph;

import NG.DataStructures.Generic.PairList;
import NG.Graph.Rendering.EdgeMesh.Edge;

import java.util.*;
import java.util.concurrent.Callable;

import static NG.Graph.Rendering.NodeMesh.Node;

/**
 * Implements Breadth first search to find the shortest path
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class GraphPathFinder implements Callable<List<Edge>> {
    private final Node startNode;
    private final Node endNode;
    private final Graph graph;

    public GraphPathFinder(Node startNode, Node endNode, Graph graph) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.graph = graph;
    }

    @Override
    public List<Edge> call() {
        ArrayDeque<Node> open = new ArrayDeque<>();
        open.add(startNode);

        Map<Node, Edge> predecessors = BFS(open);
        if (predecessors == null) return null;

        Node current = this.endNode;
        List<Edge> edges = new ArrayList<>();

        while (current != startNode) {
            Edge edge = predecessors.get(current);
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
    private Map<Node, Edge> BFS(ArrayDeque<Node> open) {
        Map<Node, Edge> predecessors = new HashMap<>();

        while (!open.isEmpty()) {
            Node node = open.remove();
            if (node == endNode) return predecessors;

            PairList<Edge, Node> connections = graph.connectionsOf(node);

            for (int i = 0; i < connections.size(); i++) {
                Edge nextEdge = connections.left(i);
                Node nextNode = connections.right(i);

                if (nextEdge.from != node) continue; // incoming edge
                if (nextNode == node) continue; // self-loop
                if (predecessors.containsKey(nextNode)) continue;

                predecessors.put(nextNode, nextEdge);
                open.add(nextNode);
            }
        }

        return null;
    }
}
