package NG.Graph;

import NG.Core.Main;
import NG.DataStructures.Generic.PairList;
import NG.Tools.Vectors;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class LTSParser {
    private static final Pattern headerPattern = Pattern.compile("des \\((\\d+),\\s?(\\d+),\\s?(\\d+)\\)\\s*");
    private static final Pattern edgePattern = Pattern.compile("\\((\\d+),\\s?\"(.*)\",\\s?(\\d+)\\)\\s*");

    SourceGraph graph;

    /**
     * Starts with 1 header line
     * <ul>
     * <li>aut_header           ::=  'des (' first_state ',' nr_of_transitions ',' nr_of_states ')'</li>
     * <li>first_state          ::=  number</li>
     * <li>nr_of_transitions    ::=  number</li>
     * <li>nr_of_states         ::=  number</li>
     * </ul>
     * Followed by a number of edges
     * <ul>
     * <li>aut_edge             ::=  '(' start_state ',' label ',' end_state ')'</li>
     * <li>start_state          ::=  number</li>
     * <li>label                ::=  '"' string '"'</li>
     * <li>end_state            ::=  number</li>
     * </ul>
     * @param ltsFile
     * @param root
     */
    public LTSParser(File ltsFile, Main root) throws IOException {
        this(new Scanner(ltsFile, "UTF8"), root);
    }

    /** @see #LTSParser(File, Main) */
    public LTSParser(Scanner scanner, Main root) throws IOException {
        // parse header
        String header = scanner.nextLine();

        Matcher matcher = headerPattern.matcher(header);
        boolean doesMatch = matcher.find();
        if (!doesMatch) throw new IOException(header);

        int initialStateIndex = Integer.parseInt(matcher.group(1));
        int nrOfTransitions = Integer.parseInt(matcher.group(2));
        int nrOfStates = Integer.parseInt(matcher.group(3));

        graph = new SourceGraph(root, nrOfStates, nrOfTransitions, root.getSpringLayout().getNatLength());
        graph.initialState = initialStateIndex;

        // prepare states
        for (int i = 0; i < nrOfStates; i++) {
            graph.nodes[i] = new State(Vectors.O, Integer.toString(i), i);
        }

        // parse edges
        int edgeIndex = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            matcher = edgePattern.matcher(line);
            boolean doesMatchEdge = matcher.find();
            if (!doesMatchEdge) throw new IOException(line);

            int startStateIndex = Integer.parseInt(matcher.group(1));
            String label = matcher.group(2);
            int endStateIndex = Integer.parseInt(matcher.group(3));

            State startState = graph.nodes[startStateIndex];
            State endState = graph.nodes[endStateIndex];
            Transition edge = new Transition(startState, endState, label);

            graph.actionLabels[edgeIndex] = label;
            graph.edges[edgeIndex] = edge;
            graph.mapping.computeIfAbsent(startState, s -> new PairList<>()).add(edge, endState);
            graph.mapping.computeIfAbsent(endState, s -> new PairList<>()).add(edge, startState);

            edgeIndex++;
        }
    }

    public SourceGraph get() {
        return graph;
    }
}
