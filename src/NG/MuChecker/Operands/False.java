package NG.MuChecker.Operands;

import NG.Graph.SourceGraph;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class False implements Formula {
    @Override
    public boolean equals(Object other) {
        return other != null && getClass().equals(other.getClass());
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        return graph.getEmptySet();
    }

    @Override
    public String toString() {
        return "false";
    }
}
