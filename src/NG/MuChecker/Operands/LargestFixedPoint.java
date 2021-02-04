package NG.MuChecker.Operands;

import NG.Graph.State;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class LargestFixedPoint extends FixedPoint {
    public LargestFixedPoint(char fixVarName, int index) {
        super(fixVarName, index);
    }

    @Override
    public String toString() {
        return "nu " + character + ".(" + right + ")";
    }

    @Override
    public StateSet eval(
            State[] universe, StateSet[] environment, ModelChecker.Binder surroundingBinder
    ) {
        if (surroundingBinder == ModelChecker.Binder.MU) {
            for (FixedPoint fp : getFixedPointsDesc()) { // for each largest fixedpoint contained,
                if (fp instanceof LargestFixedPoint) {
                    for (FixedPointVariable var : fp.getVarDesc()) { // for each variable contained in these fps,
                        FixedPoint parent = var.parent; // consider its fixed-point operator.
                        if (parent.isOpen()) { // if an open parent is found
                            environment[fp.index] = StateSet.allOf(universe); // reset the environment of fp to everything
                            break;
                        }
                    }
                }
            }
        }

        StateSet Qold = StateSet.noneOf(universe);
        StateSet arrayValue = environment[index];

        while (!Qold.equals(arrayValue)) {
            Qold = arrayValue;
            environment[index] = arrayValue;
            setOpen(true);
            arrayValue = right.eval(universe, environment, ModelChecker.Binder.NU);
            setOpen(false);
        }

        environment[index] = arrayValue;
        return arrayValue;
    }
}
