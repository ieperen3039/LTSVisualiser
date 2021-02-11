package NG.Graph.Comparison;

import NG.DataStructures.Generic.Pair;
import NG.DataStructures.Generic.PairList;
import NG.Graph.Graph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.MuChecker.StateSet;
import NG.Tools.Logger;

import java.util.*;

/**
 * @author Geert van Ieperen created on 8-2-2021.
 */
public class ConstraintComparator {
    private final State[] aStates; // V_alpha
    private final State[] bStates; // V_beta

    private final Map<String, StateSet[]> edgePredicateOut = new HashMap<>(); // (label -> M_ij)
    private final Map<String, StateSet[]> edgePredicateIn = new HashMap<>(); // (label -> M_hk)

    private final List<PairList<State, State>> solutions = new ArrayList<>();

    private long chooseCalls = 0;
    private long reduceCalls = 0;

    public ConstraintComparator(Graph aGraph, Graph bGraph) {
        Logger.printOnline(() -> String.format(
                "choose: %d, reduce: %d, solutions: %d", chooseCalls, reduceCalls, solutions.size()
        ));

        aStates = aGraph.getNodeMesh().nodeList().toArray(new State[0]);
        bStates = bGraph.getNodeMesh().nodeList().toArray(new State[0]);

        int nrOfBNodes = bGraph.getNrOfNodes();
        for (String edgeLabel : bGraph.getEdgeLabels()) {
            StateSet[] predicatesOut = new StateSet[nrOfBNodes];
            StateSet[] predicatesIn = new StateSet[nrOfBNodes];

            for (int i = 0; i < nrOfBNodes; i++) {
                StateSet outSet = StateSet.noneOf(bStates);

                List<Transition> outTransitions = bStates[i].getOutgoing();
                for (Transition edge : outTransitions) {
                    if (edge.label.equals(edgeLabel)) {
                        outSet.add(edge.to);
                    }
                }

                predicatesOut[i] = outSet;

                StateSet inSet = StateSet.noneOf(bStates);

                List<Transition> inTransitions = bStates[i].getIncoming();
                for (Transition edge : inTransitions) {
                    if (edge.label.equals(edgeLabel)) {
                        inSet.add(edge.from);
                    }
                }

                predicatesIn[i] = inSet;
            }

            edgePredicateOut.put(edgeLabel, predicatesOut);
            edgePredicateIn.put(edgeLabel, predicatesIn);
        }

        int nrOfANodes = aGraph.getNrOfNodes();
        StateSet[] domains = new StateSet[nrOfANodes];

        for (int i = 0; i < nrOfANodes; i++) {
            State aState = aStates[i];
            StateSet set = StateSet.noneOf(bStates);

            for (State bState : bStates) {
                if (unaryConstraint(aState, bState)) {
                    set.add(bState);
                }
            }

            domains[i] = set;
        }

        State choice = chooseNext(domains);
        if (choice != null) searchRecursive(choice, domains, solutions);
        Logger.INFO.print("Comparison resulted in " + solutions.size() + " solutions");

        if (solutions.isEmpty()) return;

        PairList<State, State> result = solutions.get(0);
        for (Pair<State, State> pair : result) {
            Logger.DEBUG.print(pair.left, pair.right);
        }
    }

    private void searchRecursive(State iState, StateSet[] domains, List<PairList<State, State>> solutions) {
        StateSet iDomain = domains[iState.index];

        for (State vState : iDomain) { // graph b
            // create a local copy of the domain
            StateSet[] localDomain = deepCopy(domains);
            // set i to v
            StateSet iLocalDomain = localDomain[iState.index];
            iLocalDomain.clear();
            iLocalDomain.add(vState);
            // check whether this is possible
            boolean isConsistent = reduce(iState, localDomain);

            if (isConsistent) {
                State next = chooseNext(localDomain);

                if (next != null) {
                    // i->v is possible, and there is another open variable
                    searchRecursive(next, localDomain, solutions);

                } else {
                    // i->v is possible, and no other open variables: add solution
                    solutions.add(readSolution(localDomain));
                }
            }
        }
    }

    private PairList<State, State> readSolution(StateSet[] domainCopy) {
        PairList<State, State> solution = new PairList<>(aStates.length);

        for (int i = 0; i < domainCopy.length; i++) {
            // domainCopy[i] should only contain one element
            solution.add(aStates[i], domainCopy[i].any());
        }

        return solution;
    }

    private State chooseNext(StateSet[] domains) {
        chooseCalls++;

        for (int i = 0; i < domains.length; i++) {
            StateSet d = domains[i];
            if (d.size() > 1) return aStates[i];
        }

        return null;
    }

    /**
     * @param focus   a state in aGraph
     * @param domains the domain to reduce over
     * @return true iff no domain is empty
     */
    private boolean reduce(State focus, StateSet[] domains) {
        reduceCalls++;
        Queue<State> queue = new ArrayDeque<>();
        queue.add(focus);

        while (!queue.isEmpty()) {
            State jState = queue.remove(); // state in a
            StateSet jDomain = domains[jState.index]; // states in b
            assert !jDomain.isEmpty() : jDomain;

            checkAllDifferent(domains, jDomain);

            for (Transition jOut : jState.getOutgoing()) {
                State iState = jOut.to; // state in a

                StateSet[] predicatesIn = edgePredicateIn.get(jOut.label);
                boolean isConsistent = reduce(jDomain, iState, domains, predicatesIn, queue);
                if (isConsistent) return false;
            }

            for (Transition jOut : jState.getIncoming()) {
                State iState = jOut.from; // state in a

                StateSet[] predicatesOut = edgePredicateOut.get(jOut.label);
                boolean isConsistent = reduce(jDomain, iState, domains, predicatesOut, queue);
                if (isConsistent) return false;
            }
        }

        return true;
    }

    private boolean reduce(
            StateSet jDomain, State iState, StateSet[] domains, StateSet[] predicates, Queue<State> queue
    ) {
        StateSet iDomain = domains[iState.index];

        assert !iDomain.isEmpty() : iDomain;
        if (iDomain.size() <= 1) return false;

        boolean changed = false;
        Iterator<State> iterator = iDomain.iterator();

        while (iterator.hasNext()) {
            State uState = iterator.next(); // state in b
            StateSet Mij_u = predicates[uState.index]; // states in b

            if (StateSet.areDistinct(jDomain, Mij_u)) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            if (iDomain.isEmpty()) {
                return true;

            } else if (!queue.contains(iState)) {
                queue.add(iState);
            }
        }

        return false;
    }

    private void checkAllDifferent(StateSet[] allDomains, StateSet domain) {
        if (domain.size() == 1) {
            // single-valued: remove from other domains
            for (StateSet otherDomain : allDomains) {
                if (otherDomain == domain) continue;
                if (otherDomain.size() == 1) continue;

                otherDomain.diff(domain);
                checkAllDifferent(allDomains, otherDomain);
            }
        }
    }

    private StateSet[] deepCopy(StateSet[] domain) {
        StateSet[] copy = new StateSet[domain.length];
        for (int i = 0; i < domain.length; i++) {
            copy[i] = new StateSet(domain[i]);
        }

        return copy;
    }

    /**
     * @param aState a state in graph a
     * @param bState a state in graph b
     * @return false iff there is an a-priori reason why these two states can not be isomorphic
     */
    private boolean unaryConstraint(State aState, State bState) {
        if (aState.getOutgoing().size() < bState.getOutgoing().size()) return false;
        return true;

    }

    public List<PairList<State, State>> getSolutions() {
        return solutions;
    }

    public PairList<State, State> getAnySolution() {
        if (solutions.isEmpty()) return PairList.empty();
        return solutions.get(0);
    }

    private static class DStackElement {
        StateSet[] domain;
        State variable;

        public DStackElement(StateSet[] domain, State variable) {
            this.domain = domain;
            this.variable = variable;
        }
    }
}
