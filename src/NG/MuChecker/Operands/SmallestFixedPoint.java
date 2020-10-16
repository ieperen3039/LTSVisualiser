package NG.MuChecker.Operands;

import NG.Graph.SourceGraph;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class SmallestFixedPoint extends FixedPoint {
    public SmallestFixedPoint(char fixVarName, int index, FixedPoint parent, boolean open) {
        super(fixVarName, index, parent, open);
    }

    @Override
    public String toString() {
        return "mu " + character + ".(" + right + ")";
    }

    @Override
    public StateSet eval(
            SourceGraph graph, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        if (surroundingBinder == ModelChecker.Binder.NU) {
            for (FixedPoint fp : getFixedPointsDesc()) { // for each smallest fixedpoint contained,
                if (fp instanceof SmallestFixedPoint) {
                    for (FixedPointVariable var : fp.getVarDesc()) { // for each variable contained in these fps,
                        FixedPoint parent = var.parent;// consider its fixed-point operator.
                        if (parent.isOpen()) { // if an open parent is found
                            environment[fp.index] = graph.getEmptySet();// reset the environment of fp to nothing
                            break;
                        }
                    }
                }
            }
        }

        StateSet Qold = graph.getUniverse();
        StateSet arrayValue = environment[index];

        while (!Qold.equals(arrayValue)) {
            Qold = arrayValue;
            environment[index] = arrayValue;
            setOpen(true);
            arrayValue = right.eval(graph, environment, ModelChecker.Binder.MU);
            setOpen(false);
        }

        environment[index] = arrayValue;
        return arrayValue;
    }
}
