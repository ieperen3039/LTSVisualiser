package NG.MuChecker.Operands;

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
            State[] universe, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        // {s in S such that for some t in S : (s (l)to t) implies (t in eval(g))}
        // all s which have an l transition to eval(g), hence all s incoming to eval(g)
        StateSet rightSet = right.eval(universe, environment, surroundingBinder);
        StateSet result = StateSet.noneOf(universe);

        for (State s : rightSet) {
            for (Transition transition : s.getIncoming()) {
                if (Formula.labelMatch(label, transition.label)) {
                    result.add(transition.from);
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
