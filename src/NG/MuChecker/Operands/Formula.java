package NG.MuChecker.Operands;

import NG.Graph.State;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public interface Formula {
    boolean equals(Object other);

    StateSet eval(State[] universe, StateSet[] environment, ModelChecker.Binder surroundingBinder);

    /**
     * @return true if either label is true, or they are equal.
     */
    static boolean labelMatch(String label1, String label2) {
        return label1.equals("true") || label2.equals("true") || label2.equals(label1);
    }
}
