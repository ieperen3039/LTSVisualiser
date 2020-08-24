package NG.Graph;

import NG.Graph.Rendering.EdgeMesh;
import NG.Graph.Rendering.NodeMesh;
import NG.Tools.Vectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geert van Ieperen created on 15-2-2020.
 */
public class LTSParser {
    private static final Pattern headerPattern = Pattern.compile("des \\((\\d+),\\s?(\\d+),\\s?(\\d+)\\)\\s*");
    private static final Pattern edgePattern = Pattern.compile("\\((\\d+),\\s?\"(.*)\",\\s?(\\d+)\\)\\s*");

    Graph graph;

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
     */
    public LTSParser(File ltsFile) throws IOException {
        this(new Scanner(ltsFile));
    }

    /** @see #LTSParser(File) */
    public LTSParser(Scanner scanner) throws IOException {
        // parse header
        String header = scanner.nextLine();

        Matcher matcher = headerPattern.matcher(header);
        boolean doesMatch = matcher.find();
        if (!doesMatch) throw new IOException(header);

        int initialStateIndex = Integer.parseInt(matcher.group(1));
        int nrOfTransitions = Integer.parseInt(matcher.group(2));
        int nrOfStates = Integer.parseInt(matcher.group(3));

        graph = new Graph(nrOfStates, nrOfTransitions);
        graph.initialState = initialStateIndex;

        // prepare states
        for (int i = 0; i < nrOfStates; i++) {
            graph.nodes[i] = new NodeMesh.Node(Vectors.O, Integer.toString(i));
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

            NodeMesh.Node startState = graph.nodes[startStateIndex];
            NodeMesh.Node endState = graph.nodes[endStateIndex];

            graph.actionLabels[edgeIndex] = label;
            graph.edges[edgeIndex] = new EdgeMesh.Edge(startState, endState, label);
            graph.mapping.computeIfAbsent(startState, s -> new ArrayList<>()).add(endState);
            graph.mapping.computeIfAbsent(endState, s -> new ArrayList<>()).add(startState);

            edgeIndex++;
        }
    }

    public Graph get() {
        return graph;
    }
}
