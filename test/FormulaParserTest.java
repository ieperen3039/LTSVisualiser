import NG.MuChecker.FormulaParser;
import NG.MuChecker.Operands.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Geert van Ieperen created on 17-2-2020.
 */
public class FormulaParserTest {
    @Test
    public void testParserTrivial() {
        {
            Formula formula = new FormulaParser("true").get();
            assertEquals(new True(), formula);
        }
        {
            Formula formula = new FormulaParser("false").get();
            assertEquals(new False(), formula);
        }
    }

    @Test
    public void testParserAndOr() {
        {
            Formula formula = new FormulaParser("(true && false)").get();
            assertEquals(new LogicalAnd(new True(), new False()), formula);
        }
        {
            Formula formula = new FormulaParser("(true || false)").get();
            assertEquals(new LogicalOr(new True(), new False()), formula);
        }
    }

    @Test
    public void testParserFixedPoint() {
        {
            Formula formula = new FormulaParser("mu X.X").get();
            SmallestFixedPoint xFix = new SmallestFixedPoint('X', 0, null, false);
            assertEquals(xFix.setRight(xFix.variable), formula);
        }
        {
            Formula formula = new FormulaParser("nu X.X").get();
            LargestFixedPoint xFix = new LargestFixedPoint('X', 0, null, false);
            assertEquals(xFix.setRight(xFix.variable), formula);
        }
    }

    @Test
    public void testParserBoxDiamond() {
        {
            Formula formula = new FormulaParser("<action>false").get();
            assertEquals(new Diamond("action", new False()), formula);
        }
        {
            Formula formula = new FormulaParser("[thing]false").get();
            assertEquals(new Box("thing", new False()), formula);
        }
    }

    @Test
    public void testParserParenthesis() {
        {
            Formula formula = new FormulaParser("((true && false) || false)").get();
            assertEquals(new LogicalOr(new LogicalAnd(new True(), new False()), new False()), formula);
        }
        {
            Formula formula = new FormulaParser("(true && (false || true))").get();
            assertEquals(new LogicalAnd(new True(), new LogicalOr(new False(), new True())), formula);
        }
    }

    @Test
    public void testParserNested() {
        {
            Formula formula = new FormulaParser("mu X.(<a>X || nu Y.(false || (true && [a]Y)))").get();

            SmallestFixedPoint xFix = new SmallestFixedPoint('X', 0, null, false);
            LargestFixedPoint yFix = new LargestFixedPoint('Y', 1, xFix, false);

            xFix.setRight(new LogicalOr(
                    new Diamond("a", xFix.variable),
                    yFix.setRight(new LogicalOr(
                            new False(),
                            new LogicalAnd(
                                    new True(),
                                    new Box("a", yFix.variable)
                            )
                    ))
            ));

            assertEquals(xFix, formula);
        }
    }
}
