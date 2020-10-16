package NG.MuChecker.Operands;

import NG.Graph.SourceGraph;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class LogicalAnd implements Formula {
    public Formula left;
    public Formula right;

    public LogicalAnd(Formula left, Formula right) {
        assert left != null && right != null;
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogicalAnd that = (LogicalAnd) o;

        if (!left.equals(that.left)) return false;
        return right.equals(that.right);
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        StateSet leftStates = left.eval(graph, environment, surroundingBinder);
        StateSet rightStates = right.eval(graph, environment, surroundingBinder);
        leftStates.intersect(rightStates);
        return leftStates;
    }

    @Override
    public int hashCode() {
        int result = left.hashCode();
        result = 31 * result + right.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + left + " && " + right + ")";
    }
}
