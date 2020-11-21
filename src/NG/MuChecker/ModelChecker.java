package NG.MuChecker;

import NG.Graph.SourceGraph;
import NG.MuChecker.Operands.FixedPoint;
import NG.MuChecker.Operands.Formula;
import NG.MuChecker.Operands.LargestFixedPoint;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Tom Franken, Geert van Ieperen, Floris Zeven.
 */
public class ModelChecker implements Callable<StateSet> {
    private final SourceGraph ltsGraph;
    private final Formula muFormula;
    private final List<FixedPoint> fixedPoints;

    public enum Binder {
        NU, MU, NONE
    }

    public ModelChecker(SourceGraph graph, FormulaParser formula) {
        this(graph, formula.get(), formula.getFixedPoints());
    }

    public ModelChecker(
            SourceGraph graph, Formula formula, List<FixedPoint> fixedPoints
    ) {
        this.ltsGraph = graph;
        this.muFormula = formula;
        this.fixedPoints = fixedPoints;
    }

    @Override
    public StateSet call() {
        StateSet[] environment = new StateSet[fixedPoints.size()];

        for (int i = 0; i < fixedPoints.size(); i++) {
            FixedPoint current = fixedPoints.get(i);
            if (current instanceof LargestFixedPoint) {
                environment[i] = ltsGraph.getUniverse();

            } else {
                environment[i] = ltsGraph.getEmptySet();
            }
        }

        return muFormula.eval(ltsGraph, environment, Binder.NONE);
    }
}
