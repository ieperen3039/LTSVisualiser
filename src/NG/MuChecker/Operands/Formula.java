package NG.MuChecker.Operands;

import NG.Graph.SourceGraph;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public interface Formula {
    boolean equals(Object other);

    StateSet eval(SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder);
}
