package NG.MuChecker.Operands;

import NG.Graph.SourceGraph;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class FixedPointVariable implements Formula {
    /** the operator on this fixed point */
    public final FixedPoint parent;
    /** the character representing this fixed point variable */
    public final char character;

    public FixedPointVariable(FixedPoint parent, char character) {
        this.parent = parent;
        this.character = character;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixedPointVariable that = (FixedPointVariable) o;

        return character == that.character;
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        return environment[parent.index];
    }

    @Override
    public int hashCode() {
        return character;
    }

    @Override
    public String toString() {
        return String.valueOf(character);
    }

}
