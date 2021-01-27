package NG.Graph;

import NG.DataStructures.Generic.PairList;
import NG.Tools.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Compares two deterministic graphs
 * @author Geert van Ieperen created on 3-9-2020.
 */
public class NodeComparator {

    private final Map<Transition, Transition> aToBMap = new HashMap<>();

    public NodeComparator(Graph aGraph, Graph bGraph, State aState, State bState) {
//        addMatching(aGraph, aState, bGraph, bState, NodeComparator::equivalence);
        Match match = addMatching2(aGraph, aState, bGraph, bState, NodeComparator::equivalence, new HashSet<>());
        Logger.DEBUG.print(match.pairs.size());

        match.pairs.forEach(aToBMap::put);
    }

    /**
     * using the values in similarity map, writes both graphs into this graphs, combining as many nodes as possible
     * @param aGraph graph a
     * @param a      the node in graph a that relates to generatedNode
     * @param bGraph graph b
     * @param b      the node in graph b that matches node a, and relates to generatedNode
     */
    private void addMatching(
            Graph aGraph, State a, Graph bGraph, State b, BiPredicate<Transition, Transition> equivalence
    ) {
        PairList<Transition, State> aConnections = aGraph.connectionsOf(a);
        PairList<Transition, State> bConnections = bGraph.connectionsOf(b);
        int aSize = aConnections.size();
        int bSize = bConnections.size();

        // assuming determinism
        for (int i = 0; i < aSize; i++) {
            Transition aEdge = aConnections.left(i);
            if (aToBMap.containsKey(aEdge)) continue;

            for (int j = 0; j < bSize; j++) {
                Transition bEdge = bConnections.left(j);
                if (aToBMap.containsValue(bEdge)) continue;

                if (equivalence.test(aEdge, bEdge)) {
                    // match found
                    aToBMap.put(aEdge, bEdge);

                    State aNode = aConnections.right(i);
                    State bNode = bConnections.right(j);

                    addMatching(aGraph, aNode, bGraph, bNode, equivalence);

                    break;
                }
            }
        }
    }

    private Match addMatching2(
            Graph aGraph, State a, Graph bGraph, State b, BiPredicate<Transition, Transition> equivalence,
            Set<State> seen
    ) {
        PairList<Transition, State> aConnections = aGraph.outgoingOf(a);
        PairList<Transition, State> bConnections = bGraph.outgoingOf(b);
        int aSize = aConnections.size();
        int bSize = bConnections.size();

        Match bestMatch = new Match();
        PairList<Transition, Transition> matches = bestMatch.pairs;

        seen.add(a);
//        seen.add(b);

        for (int i = 0; i < aSize; i++) {
            Transition aEdge = aConnections.left(i);
            State aOther = aConnections.right(i);
            boolean aSeen = seen.contains(aOther);

            for (int j = 0; j < bSize; j++) {
                Transition bEdge = bConnections.left(j);
                State bOther = bConnections.right(j);

                if (equivalence.test(aEdge, bEdge)) {
                    boolean bSeen = seen.contains(bOther);
                    matches.add(aEdge, bEdge);

                    if (!aSeen && !bSeen) {
                        // continue searching
                        Match match = addMatching2(aGraph, aOther, bGraph, bOther, equivalence, seen);
//                        bestMatch.pairs.addAll(match.pairs);
                    }
                }
            }
        }

        seen.remove(a);
//        seen.remove(b);

        return bestMatch;
    }

    public Iterable<Transition> getAMatching() {
        return aToBMap.keySet();
    }

    public Iterable<Transition> getBMatching() {
        return aToBMap.values();
    }

    private static boolean equivalence(Transition a, Transition b) {
        return a.label.equals(b.label);
    }

    private static boolean isomorphism(Transition a, Transition b) {
        return a.from.getOutgoing().size() == b.from.getOutgoing().size();
    }

    private static class Match {
        PairList<Transition, Transition> pairs = new PairList<>();
    }
}
