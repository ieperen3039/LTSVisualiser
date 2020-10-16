import NG.Graph.SourceGraph;
import NG.Graph.State;
import NG.MuChecker.FormulaParser;
import NG.MuChecker.ModelChecker;
import NG.MuChecker.StateSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Geert van Ieperen created on 2-3-2020.
 */
public class ModelCheckerTest {
    public void test(String graph, String formula, int... expected) {
        SourceGraph g = SourceGraph.parse(graph);
        FormulaParser f = new FormulaParser(formula);
        Set<State> states = new ModelChecker(g, f).call();
        assertStatesEquals(states, expected);
    }

    @Test
    public void testEmpty() {
        SourceGraph graph = SourceGraph.parse(
                "des (0,0,1)\n"
        );
        FormulaParser f = new FormulaParser("false");
        Set<State> states = new ModelChecker(graph, f).call();
        assertEquals(0, states.size());
    }

    @Test
    public void testSmall1() {
        SourceGraph graph = SourceGraph.parse(
                "des (0,3,2)\n" +
                        "(0, \"a\", 0)\n" +
                        "(0, \"a\", 1)\n" +
                        "(1, \"b\", 1)\n"
        );

        assertEquals(2, graph.states.length);

        FormulaParser f = new FormulaParser("<a>true");
        System.out.println(f);

        Set<State> states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 0);
    }

    @Test
    public void testSmall2() {
        SourceGraph graph = SourceGraph.parse(
                "des (0,3,2)\n" +
                        "(0, \"a\", 0)\n" +
                        "(0, \"a\", 1)\n" +
                        "(1, \"b\", 1)\n"
        );

        assertEquals(2, graph.states.length);

        FormulaParser f = new FormulaParser("[a]false");
        System.out.println(f);

        Set<State> states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 1);
    }

    @Test
    public void testLargeWithoutFixedPoint1() {
        SourceGraph graph = SourceGraph.parse(
                "des (0, 9, 7)\n" +
                        "(0, \"a\", 1)\n" +
                        "(0, \"a\", 2)\n" +
                        "(1, \"b\", 3)\n" +
                        "(2, \"b\", 3)\n" +
                        "(4, \"a\", 1)\n" +
                        "(5, \"a\", 2)\n" +
                        "(1, \"b\", 6)\n" +
                        "(6, \"b\", 6)\n" +
                        "(3, \"c\", 6)\n"
        );

        assertEquals(7, graph.states.length);

        FormulaParser f = new FormulaParser("<a>[b]<c>true");
        System.out.println(f);

        StateSet states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 0, 5);
    }

    @Test
    public void testLargeWithoutFixedPoint2() {
        SourceGraph graph = SourceGraph.parse(
                "des (0, 9, 7)\n" +
                        "(0, \"a\", 1)\n" +
                        "(0, \"a\", 2)\n" +
                        "(1, \"b\", 3)\n" +
                        "(2, \"b\", 3)\n" +
                        "(4, \"a\", 1)\n" +
                        "(5, \"a\", 2)\n" +
                        "(1, \"b\", 6)\n" +
                        "(6, \"b\", 6)\n" +
                        "(3, \"c\", 6)\n"
        );

        assertEquals(7, graph.states.length);
        FormulaParser f = new FormulaParser("[a](<b><c>true && <b><b>true)");
        System.out.println(f);

        StateSet states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 1, 2, 3, 4, 6);
    }

    @Test(timeout = 2000)
    public void testSingleFixedPoint() {
        SourceGraph graph = SourceGraph.parse(
                "des (0,6,4)\n" +
                        "(0, \"a\", 1)\n" +
                        "(1, \"a\", 0)\n" +
                        "(1, \"a\", 2)\n" +
                        "(2, \"a\", 3)\n" +
                        "(2, \"a\", 1)\n" +
                        "(3, \"b\", 3)\n"
        );

        assertEquals(4, graph.states.length);

        FormulaParser f = new FormulaParser("nu X.(X && [a]false)");
        System.out.println(f);

        Set<State> states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 3);
    }

    @Test(timeout = 10000)
    public void testMultipleFixedpoint() {
        SourceGraph graph = SourceGraph.parse(
                "des (0,7,4)\n" +
                        "(0, \"a\", 0)\n" +
                        "(0, \"c\", 0)\n" +
                        "(1, \"c\", 0)\n" +
                        "(0, \"b\", 2)\n" +
                        "(2, \"a\", 0)\n" +
                        "(3, \"c\", 0)\n" +
                        "(3, \"a\", 3)\n"
        );

        FormulaParser f = new FormulaParser("nu X.(([a] mu Y.([b]Y || X)) && nu Z.(<c>X || <b>Z))");
        System.out.println(f);

        StateSet states = new ModelChecker(graph, f).call();
        System.out.println(states);

        assertStatesEquals(states, 0, 1, 3);
    }

    @Test
    public void testFileBoolean() throws IOException {
        runDir(new File("test/testcases/boolean"));
    }

    @Test
    public void testFileModal() throws IOException {
        runDir(new File("test/testcases/modal_operators"));
    }

    @Test
    public void testFileFixPoints() throws IOException {
        runDir(new File("test/testcases/fixpoints_only"));
    }

    @Test
    public void testFileCombined() throws IOException {
        runDir(new File("test/testcases/combined"));
    }

    public void runDir(File dir) throws IOException {
        File[] files = Objects.requireNonNull(dir.listFiles());
        List<FormulaParser> formulas = new ArrayList<>();
        SourceGraph graph = null;
        State initial = null;

        for (File elt : files) {
            if (elt.getName().endsWith(".mcf")) {
                formulas.add(new FormulaParser(elt));

            } else if (elt.getName().endsWith(".aut")) {
                graph = SourceGraph.parse(elt, null);
                initial = graph.getInitialState();
            }
        }

        assert graph != null;

        for (int i = 0; i < formulas.size(); i++) {
            FormulaParser f = formulas.get(i);
            StateSet states = new ModelChecker(graph, f).call();
            System.out.printf("%-15s : %-50s => %s%n", files[i].getName(), f, states.contains(initial));
        }
    }

    //** assertion check that the given set contains exactly states with the given indices */
    public static void assertStatesEquals(Collection<State> set, int... expected) {
        boolean[] found = new boolean[expected.length];
        Arrays.sort(expected);

        // check set <= expected
        for (State state : set) {
            int index = Arrays.binarySearch(expected, state.index);
            if (index < 0) {
                throw new AssertionError(String.format(
                        "State %d in result was not expected. Expected %s, Received %s",
                        state.index, Arrays.toString(expected), set
                ));
            } else {
                found[index] = true;
            }
        }

        // check expected <= set
        for (int i = 0; i < expected.length; i++) {
            if (!found[i]) {
                throw new AssertionError(String.format(
                        "State %d not found in result. Expected %s, Received %s",
                        expected[i], Arrays.toString(expected), set
                ));
            }
        }

        // set <= expected && expected <= set
        // set == expected
        // QED
    }
}
