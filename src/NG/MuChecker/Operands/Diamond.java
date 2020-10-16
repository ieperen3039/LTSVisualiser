package NG.MuChecker.Operands;

import NG.DataStructures.Generic.PairList;
import NG.Graph.SourceGraph;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class Diamond implements Formula {
    public String label;
    public Formula right;

    public Diamond(String label, Formula right) {
        assert label != null && right != null;
        this.label = label;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Diamond diamond = (Diamond) o;

        if (!label.equals(diamond.label)) return false;
        return right.equals(diamond.right);
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        // {s in S such that for some t in S : (s (l)to t) implies (t in eval(g))}
        // all s which have an l transition to eval(g), hence all s incoming to eval(g)
        StateSet rightSet = right.eval(graph, environment, surroundingBinder);
        StateSet result = graph.getEmptySet();

        for (State s : rightSet) {
            PairList<Transition, State> connections = graph.connectionsOf(s);
            int nrOfConnections = connections.size();
            for (int i = 0; i < nrOfConnections; i++) {
                Transition transition = connections.left(i);

                if (transition.to.equals(s) && transition.label.equals(label)) {
                    result.add(connections.right(i));
                }
            }
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
        return "<" + label + ">" + right;
    }
}
