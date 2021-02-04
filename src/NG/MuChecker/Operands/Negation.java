package NG.MuChecker.Operands;

import NG.Graph.State;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 17-2-2020.
 */
public class Negation implements Formula {
    public Formula child;

    public Negation(Formula child) {
        assert child != null;
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Negation negation = (Negation) o;

        return child.equals(negation.child);
    }

    @Override
    public StateSet eval(
            State[] universe, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        StateSet states = child.eval(universe, environment, surroundingBinder);
        states.negate();
        return states;
    }

    @Override
    public int hashCode() {
        return child.hashCode();
    }

    @Override
    public String toString() {
        return "-" + child;
    }
}
