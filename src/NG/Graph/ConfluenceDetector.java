package NG.Graph;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * computes groups of confluent states, such that for each collection in the return value of {@link #call()}, all states
 * in that collection are confluent. Algorithm is based on page 387-388 of Mathematical Foundations of Computer Science
 * 2000 : https://link.springer.com/book/10.1007%2F3-540-44612-5
 * @author Geert van Ieperen created on 18-11-2020.
 */
public class ConfluenceDetector implements Callable<Collection<List<State>>> {
    private final Graph graph;
    private final Set<String> internalActions;

    /**
     * creates a confluence detector that considers all "tau" labels as internal
     */
    public ConfluenceDetector(Graph graph) {
        this.graph = graph;
        this.internalActions = new HashSet<>();
        internalActions.add("tau");
    }

    /**
     * creates a confluence detector that considers the given actions as internal
     */
    public ConfluenceDetector(Graph graph, Set<String> internalActions) {
        this.graph = graph;
        this.internalActions = new HashSet<>(internalActions);
    }

    /**
     * computes groups of confluent states.
     * @return a number of collections of confluent states, where all states in the inner collections are branching
     * bi-similar to each other.
     */
    public Collection<List<State>> call() {
        Set<Transition> candidates = computeConfluentTransitions();
        Map<State, State> leaderMap = extractLeaderMap(candidates);
        return classify(leaderMap);
    }

    /**
     * computes groups of confluent states.
     * @return a number of collections of confluent states, where all states in the inner collections are branching
     * bi-similar to each other.
     */
    public Map<State, State> getLeaderMap() {
        Set<Transition> candidates = computeConfluentTransitions();
        return extractLeaderMap(candidates);
    }

    /**
     * @return the set of silent transitions T_conf, where each transition connects two branching bi-similar states
     */
    public Set<Transition> computeConfluentTransitions() {
        List<Transition> edges = graph.getEdgeMesh().edgeList();
        Deque<Transition> stack = new ArrayDeque<>(edges);

        Set<Transition> candidates = new HashSet<>();
        for (Transition edge : edges) {
            if (isInternal(edge)) {
                candidates.add(edge);
            }
        }

        while (!stack.isEmpty()) {
            Transition target = stack.remove();
            // target = s -a> s'
            List<Transition> sPrimeConnections = target.to.getOutgoing();

            // collect all transitions where for any s''': s' -tau> s''', and is candidate
            List<Transition> targetNextTau = new ArrayList<>();
            for (Transition t : sPrimeConnections) {
//                if (!t.label.equals("tau")) continue; // follows from being a candidate
                if (!candidates.contains(t)) continue;

                targetNextTau.add(t);
            }

            List<Transition> fromConnections = target.from.getOutgoing();
            boolean anyFail = false;

            for (Transition other : fromConnections) {
//                if (!other.label.equals("tau")) continue;
                if (!candidates.contains(other)) continue;
                // other = s -tau> s'' && candidate

                boolean isConfluent = checkConfluence(target, other, targetNextTau, candidates);

                if (!isConfluent) {
                    candidates.remove(other);
                    anyFail = true;
                }
            }

            if (anyFail) {
                List<Transition> incoming = target.from.getIncoming();
                stack.addAll(incoming);
            }
        }

        return candidates;
    }

    public boolean isInternal(Transition edge) {
//        return edge.label.equals("tau");
        return internalActions.contains(edge.label);
    }

    /**
     * @param target        s -a> s'
     * @param other         s -tau> s'' and candidate
     * @param targetNextTau {for all s''' in S | s' -tau> s''' && candidate}
     */
    private boolean checkConfluence(
            Transition target, Transition other, List<Transition> targetNextTau,
            Set<Transition> candidates
    ) {
        // a == tau && s' == s''
        // other ~= target : any edge is confluent with itself
        if (isInternal(target) && target.to == other.to) return true;

        // all neighbors of s''
        List<State> outgoingStates = new ArrayList<>();
        for (Transition t : other.to.getOutgoing()) {
            outgoingStates.add(t.to);
        }

        // s'' -a> s'
        int index = outgoingStates.indexOf(target.to);
        if (index >= 0) return true;

        // for any s''': s'' -a> s''' && s' -tau> s''', where (s' -tau> s''') is candidate
        for (Transition nextTarget : targetNextTau) {
            if (!candidates.contains(nextTarget)) continue;

            // nextTarget = s' -tau> s'''
            int index2 = outgoingStates.indexOf(nextTarget.to);
            if (index2 >= 0) return true;
        }

        // a == tau and (s' -tau> s'') is candidate
        if (isInternal(target)) {
            // search for candidate s' -tau> s''' where s''' == s''
            for (Transition nextTauTarget : targetNextTau) {
                if (!candidates.contains(nextTauTarget)) continue;

                if (nextTauTarget.to == other.to) return true;
            }
        }

        return false;
    }

    private List<List<State>> classify(Map<State, State> leaderMap) {
        Map<State, List<State>> confluenceMap = new HashMap<>();

        for (State state : leaderMap.keySet()) {
            State leader = state;
            while (leaderMap.containsKey(leader)) {
                leader = leaderMap.get(leader);
            }

            List<State> confluentGroup = confluenceMap.get(leader);
            if (confluentGroup == null) {
                confluentGroup = new ArrayList<>();
                confluenceMap.put(leader, confluentGroup);
                confluentGroup.add(leader);
            }

            confluentGroup.add(state);
        }

        return new ArrayList<>(confluenceMap.values());
    }

    private Map<State, State> extractLeaderMap(Set<Transition> confluentSet) {
        Map<State, State> leaderMap = new HashMap<>();

        // have each state point to a confluent state with lower index
        for (Transition transition : confluentSet) {
            // ignore self-loops
            if (transition.from == transition.to) continue;

            if (transition.from.index > transition.to.index) {
                leaderMap.put(transition.from, transition.to);
            } else {
                leaderMap.put(transition.to, transition.from);
            }
        }
        return leaderMap;
    }
}
