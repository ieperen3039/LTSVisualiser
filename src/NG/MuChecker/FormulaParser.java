package NG.MuChecker;

import NG.MuChecker.Operands.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class FormulaParser {
    private static final int MAX_PRIORITY = 10;
    private static final int AND_OR_PRIORITY = 3;
    private static final int DIAMOND_BOX_PRIORITY = 2;
    private static final int FIXED_POINT_PRIORITY = 1;

    private static final Pattern PATTERN_PERIOD = Pattern.compile("\\.");
    private static final Pattern PATTERN_PLUS = Pattern.compile("\\+");

    private final List<FixedPoint> fixedPoints = new ArrayList<>();
    // maps characters to the respective fixed point object
    private final Map<Character, FixedPoint> fixedPointDict = new HashMap<>();
    // while parsing, tracks the current fix points
    private final Queue<FixedPoint> currentParentFixPoints = new ArrayDeque<>();
    private final Formula root;
    private int variableNumber = 0;

    /**
     * Parses the formula contained in the given file. The formula must be of the shape
     * <p>
     * {@code f; g ::= false | true | X | (f&&g) | (f||g) | <a>f | [a]f | mu X.f | nu X.f}
     * @param formulaFile the file with the formula to parse
     */
    public FormulaParser(File formulaFile) throws FileNotFoundException {
        Scanner sc = new Scanner(formulaFile);
        StringBuilder formula = new StringBuilder();
        while (sc.hasNextLine()) {
            String str = sc.nextLine();
            // Skip comments
            final int startOfComment = str.indexOf('%');
            if (startOfComment < 0) {
                formula.append(str);
            } else {
                formula.append(str, 0, startOfComment);
            }
        }

        ParseReturn result = parseRecursive(formula.toString(), 0, MAX_PRIORITY);
        assert result.end == formula.length();

        root = result.formula;
    }

    /**
     * Parses a formula of the shape
     * <p>
     * {@code f; g ::= false | true | X | (f&&g) | (f||g) | <a>f | [a]f | mu X.f | nu X.f}
     * @param formula the formula to parse
     */
    public FormulaParser(String formula) {
        ParseReturn result = parseRecursive(formula, 0, MAX_PRIORITY);
        assert result.end == formula.length();

        root = result.formula;
    }

    public Formula get() {
        return root;
    }

    public List<FixedPoint> getFixedPoints() {
        return fixedPoints;
    }

    /**
     * parses and returns the substring from start to end of formula. The parsing returns if a) the end is reached or b)
     * a superfluous closing parenthesis is found or c) an operator with a priority larger than {@code currentPriority}
     * @param formula the complete formula to parse
     * @param start   the index of the first character in formula to parse.
     * @return {@code [formula, end]} with formula the parsed formula, and end the character where the parsing stopped.
     */
    private ParseReturn parseRecursive(String formula, int start, int currentPriority) {
        Formula current = null;

        int i = start;
        while (i < formula.length()) {
            char firstChar = formula.charAt(i);
            String remainder = formula.substring(i);

            if (firstChar == ' ') { // skip spaces
                i++;

            } else if (remainder.startsWith("true")) {
                assert current == null : String.format("current == '%s'", current);
                current = new True();
                i += 4;

            } else if (remainder.startsWith("false")) {
                assert current == null : String.format("current == '%s'", current);
                current = new False();
                i += 5;

            } else {
                if (remainder.startsWith("&&")) {
                    if (currentPriority <= AND_OR_PRIORITY) return new ParseReturn(current, i);
                    // runs until closing parenthesis
                    ParseReturn right = parseRecursive(formula, i + 2, AND_OR_PRIORITY);
                    // now close the opening parenthesis
                    return new ParseReturn(new LogicalAnd(current, right.formula), right.end);

                } else if (remainder.startsWith("||")) {
                    if (currentPriority <= AND_OR_PRIORITY) return new ParseReturn(current, i);
                    // runs until closing parenthesis
                    ParseReturn right = parseRecursive(formula, i + 2, AND_OR_PRIORITY);
                    // now close the opening parenthesis
                    return new ParseReturn(new LogicalOr(current, right.formula), right.end);

                } else if (remainder.startsWith("mu ") || remainder.startsWith("nu ")) {
                    if (currentPriority <= FIXED_POINT_PRIORITY) return new ParseReturn(current, i);
                    assert current == null : String.format("current == '%s'", current);
                    assert formula.charAt(i + 4) == '.';

                    char fixVarName = formula.charAt(i + 3);
                    assert !fixedPointDict.containsKey(fixVarName) : "Reused variable " + formula.charAt(i + 1);

                    boolean smallestFixedPoint = remainder.startsWith("mu ");

                    FixedPoint fp;
                    if (smallestFixedPoint) {
                        fp = new SmallestFixedPoint(fixVarName, variableNumber++);
                    } else {
                        fp = new LargestFixedPoint(fixVarName, variableNumber++);
                    }

                    fixedPoints.add(fp);
                    fixedPointDict.put(fixVarName, fp);

                    for (FixedPoint parent : currentParentFixPoints) {
                        parent.addDescendant(fp);
                    }

                    currentParentFixPoints.add(fp);
                    ParseReturn right = parseRecursive(formula, i + 5, MAX_PRIORITY);
                    currentParentFixPoints.remove();

                    current = fp.setRight(right.formula);
                    i = right.end;

                } else if (firstChar == '(') {
                    assert current == null : String.format("current == '%s'", current);
                    ParseReturn inside = parseRecursive(formula, start + 1, MAX_PRIORITY);
                    current = inside.formula;

                    i = inside.end;

                } else if (firstChar == ')') {
                    return new ParseReturn(current, i + 1);

                } else if (firstChar == '<') {
                    if (currentPriority < DIAMOND_BOX_PRIORITY) return new ParseReturn(current, i);
                    assert current == null : String.format("current == '%s'", current);

                    int closing = i + 1;
                    while (formula.charAt(closing) != '>') closing++;
                    ParseReturn right = parseRecursive(formula, closing + 1, DIAMOND_BOX_PRIORITY);
                    current = new Diamond(formula.substring(i + 1, closing), right.formula);

                    i = right.end;

                } else if (firstChar == '[') {
                    if (current != null && currentPriority < DIAMOND_BOX_PRIORITY) return new ParseReturn(current, i);
                    assert current == null : String.format("current == '%s'", current);

                    int closing = i + 1;
                    while (formula.charAt(closing) != ']') closing++;
                    ParseReturn right = parseRecursive(formula, closing + 1, DIAMOND_BOX_PRIORITY);
                    current = new Box(formula.substring(i + 1, closing), right.formula);

                    i = right.end;

                } else {
                    final FixedPoint fixedPoint = fixedPointDict.get(firstChar);

                    if (fixedPoint == null) {
                        throw new IllegalArgumentException(String.format(
                                "Unexpected character '%s' starting at '%s' on line %d",
                                firstChar, formula.substring(i, i + 10), i
                        ));
                    }
                    FixedPointVariable variable = fixedPoint.variable;

                    for (FixedPoint fx : currentParentFixPoints) {
                        fx.addVar(variable);
                    }

                    assert current == null : String.format("current == '%s'", current);
                    current = variable;

                    i++;
                }
            }
        }

        assert current != null;
        return new ParseReturn(current, i);
    }

    @Override
    public String toString() {
        return get().toString();
    }

    private static class ParseReturn {
        public final Formula formula;
        public final int end;

        private ParseReturn(Formula formula, int newEnd) {
            this.formula = formula;
            this.end = newEnd;
        }
    }
}
