package NG.Graph;

import java.util.*;

/**
 * @author Geert van Ieperen created on 20-7-2020.
 */
public final class HDEPositioning {
    public static final int PCA_OFFSET = 0; // sometimes ignoring the first axis gives better results
    private static final int NUM_INITIAL_DIMENSIONS = 30; // m
    private static final int NUM_TARGET_DIMENSIONS = 3; // k
    private static final double THRESHOLD = 1 / 128f;

    public static void applyTo(SourceGraph graph, float edgeLength) {
        State[] states = graph.states;
        Transition[] edges = graph.edges;

        // create position mapping
        double[][] positions = position(edges, states);

        // set state position
        for (int i = 0; i < states.length; i++) {
            int numDimensions = positions[i].length;
            states[i].position.set(
                    numDimensions > 0 ? (float) positions[i][0] : 0,
                    numDimensions > 1 ? (float) positions[i][1] : 0,
                    numDimensions > 2 ? (float) positions[i][2] : 0
            ).mul(edgeLength);
        }

        // reset edge handle to the middle of the pair
        for (Transition edge : edges) {
            edge.handlePos.set(edge.fromPosition).lerp(edge.toPosition, 0.5f);
        }

        graph.getNodeMesh().schedulePositionReload();
        graph.getEdgeMesh().schedulePositionReload();
    }

    public static double[][] position(Transition[] edges, State[] nodes) {
        int initialDimensions = Math.min(NUM_INITIAL_DIMENSIONS, nodes.length);

        // make mapping bidirectional
        HashMap<State, Collection<State>> biMapping = new HashMap<>();
        for (Transition edge : edges) {
            biMapping.computeIfAbsent(edge.from, n -> new HashSet<>())
                    .add(edge.to);
            biMapping.computeIfAbsent(edge.to, n -> new HashSet<>())
                    .add(edge.from);
        }

        // get coordinates as [nodes.length][NUM_INITIAL_DIMENSIONS]
        double[][] coordinates = getHighDimensionLayout(biMapping, nodes, initialDimensions); // X

        // get covariance matrix as [NUM_INITIAL_DIMENSIONS][NUM_INITIAL_DIMENSIONS]
        center(coordinates);
        double[][] covariance = ArrayMatrix.getCovariance(coordinates); // S

        // projection axes as [NUM_TARGET_DIMENSIONS][NUM_INITIAL_DIMENSIONS]
        double[][] axes = powerIterationPCA(NUM_TARGET_DIMENSIONS + PCA_OFFSET, covariance); // u1 ... uk

        // positions as [nodes.length][NUM_TARGET_DIMENSIONS]
        double[][] result = new double[coordinates.length][NUM_TARGET_DIMENSIONS];
        // we project each coordinate on one axis, using that as the new coordinate
        // this is practically a matrix multiplication.
        for (int i = 0; i < NUM_TARGET_DIMENSIONS; i++) {
            double[] v = ArrayMatrix.normalize(axes[i + PCA_OFFSET]);

            for (int j = 0; j < coordinates.length; j++) {
                double[] u = coordinates[j];
                result[j][i] = ArrayMatrix.dot(u, v);
            }
        }
//        System.out.println(Arrays.deepToString(result));

        return result;
    }

    private static void center(double[][] coordinates) {
        double[] eltAverages = new double[coordinates[0].length];
        // accumulate totals for each dimension
        for (double[] coordinate : coordinates) {
            for (int j = 0; j < eltAverages.length; j++) {
                eltAverages[j] += coordinate[j];
            }
        }
        // compute means
        for (int i = 0; i < eltAverages.length; i++) {
            eltAverages[i] /= coordinates.length;
        }
        // subtract means from each coordinate
        for (double[] coordinate : coordinates) {
            for (int i = 0; i < coordinate.length; i++) {
                coordinate[i] -= eltAverages[i];
            }
        }
    }

    private static double[][] getHighDimensionLayout(
            Map<State, Collection<State>> mapping, State[] nodes, int initialDimensions
    ) {
        double[][] coordinates = new double[nodes.length][initialDimensions];
        int[] anchorDistance = new int[nodes.length]; // distance to any picked coordinate
        Arrays.fill(anchorDistance, Integer.MAX_VALUE);

        State pivot = nodes[0];
        for (int i = 0; i < initialDimensions; i++) {
            // compute all distances to pivot
            Map<State, Integer> distances = getAllDistances(pivot, mapping);
            int bestPivotDistance = 0;
            int bestPivotIndex = 0;

            for (int j = 0; j < nodes.length; j++) {
                State node = nodes[j];
                assert distances.containsKey(node) : node;

                // write distances from this node to this pivot
                int dist = distances.get(node);
                coordinates[j][i] = dist;

                // also look for the best next pivot
                int thisAnchorDist = Math.min(anchorDistance[j], dist);
                anchorDistance[j] = thisAnchorDist;
                if (thisAnchorDist > bestPivotDistance) {
                    bestPivotDistance = thisAnchorDist;
                    bestPivotIndex = j;
                }
            }

            // next pivot is the furthest away from the known pivots
            pivot = nodes[bestPivotIndex];
        }

        return coordinates;
    }

    private static double[][] getHighDimensionLayout2(
            Map<State, Collection<State>> mapping, State[] nodes, int initialDimensions
    ) {
        double[][] coordinates = new double[nodes.length][initialDimensions];

        return coordinates;
    }

    private static Map<State, Integer> getAllDistances(
            State p1, Map<State, Collection<State>> mapping
    ) {
        Map<State, Integer> distances = new HashMap<>();
        Queue<State> open = new ArrayDeque<>();
        open.add(p1);
        distances.put(p1, 0);

        while (!open.isEmpty()) {
            State node = open.remove();
            int baseDist = distances.get(node);

            if (mapping.containsKey(node)) { // deadlocks have no outgoing transitions
                for (State secondary : mapping.get(node)) {

                    if (distances.containsKey(secondary)) {
                        int secDist = distances.get(secondary);
                        if (baseDist + 1 < secDist) {
                            distances.put(secondary, baseDist + 1);
                        }
                        // mutual
                        if (baseDist > secDist + 1) {
                            distances.put(node, secDist + 1);
                        }

                    } else {
                        int current = baseDist + 1;
                        distances.put(secondary, current);
                        open.add(secondary);
                    }
                }
            }
        }

        return distances;
    }

    /**
     * Implementation of the non-linear iterative partial least squares algorithm on the data matrix for this Data
     * object. The number of PCs returned is specified by the user.
     * @param numComponents number of principal components desired
     * @param matrix
     * @return a double[][] where the ith double[] contains the scores of the ith principal component.
     */
    static double[][] powerIterationPCA(int numComponents, double[][] matrix) {
        double[][] out = new double[numComponents][];
        double[][] E = ArrayMatrix.copy(matrix);

        for (int i = 0; i < numComponents; i++) {
            double eigenOld = 0;
            double eigenNew = 0;
            double[] p = new double[matrix[0].length];
            double[] t = new double[matrix[0].length];
            double[][] tMatrix = {t};
            double[][] pMatrix = {p};
            System.arraycopy(matrix[i], 0, t, 0, t.length);

            do {
                eigenOld = eigenNew;
                double tMult = 1 / ArrayMatrix.dot(t, t);
                tMatrix[0] = t;
                p = ArrayMatrix.scale(ArrayMatrix.multiply(ArrayMatrix.transpose(E), tMatrix), tMult)[0];
                p = ArrayMatrix.normalize(p);
                double pMult = 1 / ArrayMatrix.dot(p, p);
                pMatrix[0] = p;
                t = ArrayMatrix.scale(ArrayMatrix.multiply(E, pMatrix), pMult)[0];
                eigenNew = ArrayMatrix.dot(t, t);
            } while (Math.abs(eigenOld - eigenNew) > THRESHOLD);

            tMatrix[0] = t;
            pMatrix[0] = p;
            E = ArrayMatrix.subtract(E, ArrayMatrix.multiply(tMatrix, ArrayMatrix.transpose(pMatrix)));

            out[i] = t;
        }
        return out;
    }
}
