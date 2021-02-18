package NG.Graph.Comparison;

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
    private final Map<String, StateSet[]> edgePredicateOut = new HashMap<>(); // (label -> M_ij)
    private final Map<String, StateSet[]> edgePredicateIn = new HashMap<>(); // (label -> M_hk)

    private final State[] aStates; // V_alpha
    private final State[] bStates; // V_beta
    private final Graph aGraph;
    private final Graph bGraph;

    /** triangular matrix for choosing variables */
    private final int[][] variableWeights;

    private final List<PairList<State, State>> solutions = new ArrayList<>();
    private final State[] varSequence;
    private int maxSolutionSize = 0;
    private long choices = 0;
    private int endSubscript;

    public ConstraintComparator(Graph subGraph, Graph superGraph) {
        Logger.printOnline(() -> String.format(
                "choices performed: %d | current num of multivalued variables: %2d | max solution size: %d",
                choices, endSubscript, maxSolutionSize
        ));

        int nrOfANodes = subGraph.getNrOfNodes();
        int nrOfBNodes = superGraph.getNrOfNodes();
        this.aGraph = subGraph;
        this.bGraph = superGraph;

        this.aStates = subGraph.getNodeMesh().nodeList().toArray(new State[0]);
        this.bStates = superGraph.getNodeMesh().nodeList().toArray(new State[0]);
        this.varSequence = aStates.clone();
        this.endSubscript = varSequence.length - 1;

        this.variableWeights = new int[nrOfANodes][];
        for (int i = 1; i < variableWeights.length; i++) {
            int[] floats = new int[i];
            Arrays.fill(floats, 1);
            variableWeights[i] = floats;
        }

        // we only set up the predicates for the intersection of the edge labels
        Set<String> edgeLabels = new HashSet<>(superGraph.getEdgeLabels());
        edgeLabels.retainAll(subGraph.getEdgeLabels());

        for (String edgeLabel : edgeLabels) {
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

        StateSet[] domains = new StateSet[nrOfANodes];

        for (int i = 0; i < nrOfANodes; i++) {
            State aState = aStates[i];
            StateSet set = StateSet.fromPredicate(bStates, s -> unaryConstraint(aState, s));

            domains[i] = set;
        }

        // pick any variable to start
        State choice = chooseNext(domains);
        if (choice != null) {
            searchRecursive(choice, domains, solutions);
        }
        Logger.INFO.print("Comparison resulted in " + solutions.size() + " solutions");
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
            choices++;

            // remove all impossible values from the local domain
            reduce(iState, localDomain);

            int localEndSubscript = endSubscript;
            State next = chooseNext(localDomain);

            if (next != null) {
                // i->v is possible, and there is another open variable
                searchRecursive(next, localDomain, solutions);

            } else {
                assert endSubscript < 0 : endSubscript;
                PairList<State, State> solution = readSolution(localDomain);
                int numSingleValuedDomains = solution.size();

                // if we find a bigger solution, throw away all smaller solutions
                if (numSingleValuedDomains > maxSolutionSize) {
                    solutions.clear();
                    maxSolutionSize = numSingleValuedDomains;
                }
                if (numSingleValuedDomains == maxSolutionSize) {
                    // output solution
                    solutions.add(solution);
                }
            }

            endSubscript = localEndSubscript;
        }
    }

    private PairList<State, State> readSolution(StateSet[] domains) {
        PairList<State, State> solution = new PairList<>(aStates.length);

        for (int i = 0; i < domains.length; i++) {
            StateSet domain = domains[i];

            if (isSingleValued(domain)) {
                solution.add(aStates[i], domain.any());
            }
        }

        return solution;
    }

    private State chooseNext(StateSet[] domains) {
        if (endSubscript < 0) return null; // no free variable exists

        int minScore = Integer.MAX_VALUE;
        State nextState = null;
        int nextIndex = Integer.MAX_VALUE;

        // varSequence[0 ... endSubscript] are possibly multivalued, the remainder is single valued
        int i = 0;
        do {
            State kState = varSequence[i];
            StateSet kDomain = domains[kState.index];

            if (kDomain.isEmpty() || isSingleValued(kDomain)) {
                varSequence[i] = varSequence[endSubscript];
                varSequence[endSubscript] = kState;
                endSubscript--;

            } else {
                int score = heuristicScore(kState, domains);

                if (score < minScore) {
                    minScore = score;
                    nextState = kState;
                    nextIndex = i;
                }

                i++;
            }
        } while (i <= endSubscript);

        if (endSubscript < 0) { // no free variable exists
            return null;
        }

        if (nextIndex < endSubscript) {
            // swap new variable into the single-valued part
            varSequence[nextIndex] = varSequence[endSubscript];
            varSequence[endSubscript] = nextState;
        }

        return nextState;
    }

    /**
     * @param variable a state in aGraph
     * @param domains  the domain to consider
     * @return a score, lower means better to choose
     */
    private int heuristicScore(State variable, StateSet[] domains) {
        //  For the variable Vi, this score is
        //  |Di|/(SUM(j∈A_i) w_ij), where
        //      - |Di| is the current cardinality of domain Di
        //      - A_i = {V_k| (V_k is adjacent to V_i) AND (|Dk| > 1)}
        //      - w_ij is a weight associated with the unordered pair {Vi, Vj}, initially 1.
        //  During the search, the weight w_ij is increased by one when no value in Di is supported by Dj or when no value
        //  in Dj is supported by Di;

        int wTotal = 1;
        int thisIndex = variable.index;

        for (Transition t : variable.getOutgoing()) {
            int outIndex = t.to.index;
            if (!isSingleValued(domains[outIndex])) {
                wTotal += getWeight(outIndex, thisIndex);
            }
        }
        for (Transition t : variable.getIncoming()) {
            int inIndex = t.from.index;
            if (!isSingleValued(domains[inIndex])) {
                wTotal += getWeight(inIndex, thisIndex);
            }
        }

        return domains[thisIndex].size() / wTotal;
    }

    /**
     * reduces all domains such that all constraints are satisfied
     * @param focus   a state in aGraph that has just been changed
     * @param domains the domain to reduce over
     */
    private void reduce(State focus, StateSet[] domains) {
        Queue<State> queue = new ArrayDeque<>();
        queue.add(focus);

        while (!queue.isEmpty()) {
            State jState = queue.remove(); // state in a

            checkAllDifferent(domains, domains[jState.index]);

            for (Transition jOut : jState.getOutgoing()) {
                State iState = jOut.to; // state in a

                StateSet[] predicatesIn = edgePredicateIn.get(jOut.label);
                reduceVariable(iState, jState, domains, predicatesIn, queue);
            }

            for (Transition jOut : jState.getIncoming()) {
                State iState = jOut.from; // state in a

                StateSet[] predicatesOut = edgePredicateOut.get(jOut.label);
                reduceVariable(iState, jState, domains, predicatesOut, queue);
            }
        }
    }

    /**
     * reduces the domain of iState such that its constraints are satisfied
     * @param iState     a state in aGraph which must be updated
     * @param jState     a state adjacent to iState which has just been changed
     * @param domains    the domain to reduce over
     * @param predicates the predicates to check against
     * @param queue      if iState is changed, it is added to this queue
     */
    private void reduceVariable(
            State iState, State jState, StateSet[] domains, StateSet[] predicates, Queue<State> queue
    ) {
        if (predicates == null) {
            incrementWeight(iState.index, jState.index);
            return;
        }

        StateSet iDomain = domains[iState.index]; // states in b
        StateSet jDomain = domains[jState.index]; // states in b

        if (iDomain.isEmpty() || isSingleValued(iDomain)) return;
        if (jDomain.isEmpty()) return;

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
                incrementWeight(iState.index, jState.index);

            } else if (!queue.contains(iState)) {
                queue.add(iState);
            }
        }
    }

    private int getWeight(int iIndex, int jIndex) {
        if (iIndex == jIndex) {
            return 1;

        } else if (iIndex > jIndex) {
            return variableWeights[iIndex][jIndex];

        } else { // jIndex < iIndex
            return variableWeights[jIndex][iIndex];
        }
    }

    private void incrementWeight(int iIndex, int jIndex) {
        if (iIndex == jIndex) return;

        if (iIndex > jIndex) {
            variableWeights[iIndex][jIndex]++;
        } else {
            variableWeights[jIndex][iIndex]++;
        }
    }

    private void checkAllDifferent(StateSet[] allDomains, StateSet domain) {
        if (isSingleValued(domain)) {
            // single-valued: remove from other domains
            for (StateSet otherDomain : allDomains) {
                if (otherDomain == domain) continue;
                if (isSingleValued(otherDomain)) continue;

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

    private boolean isSingleValued(StateSet domain) {
        return domain.size() == 1;
    }

    /**
     * @param aState a state in graph a
     * @param bState a state in graph b
     * @return false iff there is an a-priori reason why these two states can not be isomorphic
     */
    private boolean unaryConstraint(State aState, State bState) {
        if (aState.equals(aGraph.getInitialState())) return bState.equals(bGraph.getInitialState());
        return true;

    }

    public List<PairList<State, State>> getSolutions() {
        return solutions;
    }

    public PairList<State, State> getAnySolution() {
        if (solutions.isEmpty()) return PairList.empty();
        return solutions.get(0);
    }
}
