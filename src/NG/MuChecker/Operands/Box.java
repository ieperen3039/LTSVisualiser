package NG.MuChecker.Operands;

import NG.DataStructures.Generic.PairList;
import NG.Graph.SourceGraph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class Box implements Formula {
    public String label;
    public Formula right;

    public Box(String label, Formula right) {
        assert label != null && right != null;
        this.label = label;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Box box = (Box) o;

        if (!label.equals(box.label)) return false;
        return right.equals(box.right);
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        // {s in S such that for all t in S : (s (l)to t) implies (t in eval(g))}
        // for all s, if there is a l transition from s to t, then t must be in eval(g)
        // hence, all s for which all l transitions lie in eval(g)
        StateSet rightSet = right.eval(graph, environment, surroundingBinder);
        StateSet result = graph.getEmptySet();
        Collection<State> out = new ArrayList<>();

        for (State s : graph.states) {
            out.clear();

            PairList<Transition, State> connections = graph.connectionsOf(s);
            int nrOfConnections = connections.size();
            for (int i = 0; i < nrOfConnections; i++) {
                Transition edge = connections.left(i);

                if (edge.from.equals(s) && Formula.labelMatch(edge.label, label)) {
                    out.add(edge.to);
                }
            }

            // if s has no "label" transitions, the box holds vacuously for s.
            // if s has "label" transitions, check whether all states t after doing a "label" transition
            // are in eval(g). If so, s is in the result.
            if (out.isEmpty() || rightSet.containsAll(out)) result.add(s);
        }

        return result;
    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + right.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" + label + "]" + right;
    }
}
