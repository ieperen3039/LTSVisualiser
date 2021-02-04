package NG.MuChecker;

import NG.Graph.Graph;
import NG.Graph.SourceGraph;
import NG.Graph.State;
import NG.MuChecker.Operands.FixedPoint;
import NG.MuChecker.Operands.Formula;
import NG.MuChecker.Operands.LargestFixedPoint;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Tom Franken, Geert van Ieperen, Floris Zeven.
 */
public class ModelChecker implements Callable<StateSet> {
    private final State[] universe;
    private final Formula muFormula;
    private final List<FixedPoint> fixedPoints;

    public enum Binder {
        NU, MU, NONE
    }

    public ModelChecker(SourceGraph graph, FormulaParser formula) {
        this(formula.get(), formula.getFixedPoints(), graph);
    }

    public ModelChecker(Formula formula, List<FixedPoint> fixedPoints, SourceGraph graph) {
        this(formula, fixedPoints, graph.states);
    }

    public ModelChecker(Graph graph, FormulaParser formula) {
        this(formula.get(), formula.getFixedPoints(), graph);
    }

    public ModelChecker(Formula formula, List<FixedPoint> fixedPoints, Graph graph) {
        this(formula, fixedPoints, graph.getNodeMesh().nodeList().toArray(new State[0]));
    }

    public ModelChecker(
            Formula formula, List<FixedPoint> fixedPoints, State[] universe
    ) {
        this.universe = universe;
        this.muFormula = formula;
        this.fixedPoints = fixedPoints;
    }

    @Override
    public StateSet call() {
        StateSet[] environment = new StateSet[fixedPoints.size()];

        for (int i = 0; i < fixedPoints.size(); i++) {
            FixedPoint current = fixedPoints.get(i);
            if (current instanceof LargestFixedPoint) {
                environment[i] = StateSet.allOf(universe);

            } else {
                environment[i] = StateSet.noneOf(universe);
            }
        }

        return muFormula.eval(universe, environment, Binder.NONE);
    }
}
