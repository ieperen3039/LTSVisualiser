package NG.Tools;

import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * calculates a list of coordinates from A (exclusive) to B (inclusive), such that the returned path is the fastest path
 * to B. Must be overridden for a cost function.
 * @author Geert van Ieperen created on 19-2-2019.
 */
public abstract class AStarGrid implements Callable<Iterable<Vector2i>> {
    public final Vector2i target;
    private final int xMax;
    private final int yMax;

    private final PriorityQueue<ANode> found = new PriorityQueue<>();
    private final Map<Integer, Map<Integer, ANode>> openSet = new HashMap<>();
    private final Map<Integer, Set<Integer>> closedSet = new HashMap<>();
    private final ANode srcNode;
    private final int xMin;
    private final int yMin;

    public AStarGrid(Vector2ic source, Vector2ic target, int xMin, int yMin, int xMax, int yMax) {
        this.target = new Vector2i(target);
        this.xMax = xMax;
        this.yMax = yMax;
        this.xMin = xMin;
        this.yMin = yMin;
        this.srcNode = new ANode(source);

        HashMap<Integer, ANode> sourceSet = new HashMap<>();
        sourceSet.put(srcNode.y, srcNode);
        openSet.put(srcNode.x, sourceSet);
    }

    @Override
    public Collection<Vector2i> call() {
        ANode node = srcNode;

        do {
            int x = node.x;
            int y = node.y;
            // add to closed set
            closedSet.computeIfAbsent(x, k -> new HashSet<>()).add(y);

            // positive x
            checkNode(node, x + 1, y);
            // positive y
            checkNode(node, x, y + 1);
            // negative x
            checkNode(node, x - 1, y);
            // negative y
            checkNode(node, x, y - 1);

            // no node left: no solution
            if (found.isEmpty()) return Collections.emptyList();

            // take one from the found queue
            node = found.remove();
        } while (!node.is(target));

        List<ANode> path = new ArrayList<>();

        while (node != srcNode) {
            path.add(node);
            node = node.source; // all nodes but srcNode have a source
        }

        return new Path(path);
    }

    /**
     * analyse the neighbour (x, y) of the given node
     * @param previous the origin node
     * @param x        the x coordinate of the neighbour of this node
     * @param y        the y coordinate of the neighbour of this node
     */
    public void checkNode(ANode previous, int x, int y) {
        if (x < xMin || y < yMin || x > xMax || y > yMax) return;

        if (inClosedSet(x, y)) return;

        float distance = distanceAdjacent(previous.x, previous.y, x, y);
        if (Float.isInfinite(distance)) return;

        if (!inOpenSet(x, y)) {
            ANode newNode = new ANode(x, y, previous, distance);
            addToOpen(x, y, newNode);
            found.add(newNode);
            return;
        }

        // node already exists
        ANode neighbour = getNode(x, y);
        float neighbourNewGScore = previous.gScore + distance;

        if (neighbourNewGScore > neighbour.gScore) return;

        // got a new high-score
        neighbour.gScore = neighbourNewGScore;
        neighbour.source = previous;

        // target not found
    }

    private void addToOpen(int x, int y, ANode newNode) {
        openSet.computeIfAbsent(x, k -> new HashMap<>()).put(y, newNode);
    }

    public ANode getNode(int x, int y) {
        return openSet.get(x).get(y);
    }

    /**
     * Calculate the cost of going from (x1, y1) to (x2, y2). These two coordinates are adjacent.
     * @param x1 x of the first coordinate
     * @param y1 y of the first coordinate
     * @param x2 x of the second coordinate
     * @param y2 y of the second coordinate
     * @return the real cost of going past this.
     */
    public abstract float distanceAdjacent(int x1, int y1, int x2, int y2);

    public float distanceHeuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    protected boolean inOpenSet(int x, int y) {
        return openSet.getOrDefault(x, Collections.emptyMap()).containsKey(y);
    }

    protected boolean inClosedSet(int x, int y) {
        return closedSet.getOrDefault(x, Collections.emptySet()).contains(y);
    }

    /**
     * @param factor the y displacement per x displacement
     * @return the x displacement per 1 unit on the hypotenusa
     */
    public static float hypoLength(float factor) {
        return (float) Math.sqrt(1f / (factor * factor + 1));
    }

    private class ANode implements Comparable<ANode> {
        final int x;
        final int y;
        final float distToTarget;

        ANode source;
        float gScore;

        private ANode(int x, int y, ANode comeFrom, float distance) {
            this.x = x;
            this.y = y;
            this.gScore = comeFrom.gScore + distance;
            this.distToTarget = distanceHeuristic(x, y, target.x, target.y);
            this.source = comeFrom;
        }

        private ANode(Vector2ic sourcePosition) {
            this.x = sourcePosition.x();
            this.y = sourcePosition.y();
            this.gScore = 0;
            this.distToTarget = distanceHeuristic(x, y, target.x, target.y);
            this.source = null;
        }

        @Override
        public int compareTo(ANode other) {
            return Float.compare(getDist(), other.getDist());
        }

        public float getDist() {
            return distToTarget + gScore;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(getDist());
        }

        public boolean is(Vector2i target) {
            return x == target.x && y == target.y;
        }
    }

    private class Path extends AbstractCollection<Vector2i> {
        private List<ANode> path;

        public Path(List<ANode> path) {
            this.path = path;
        }

        @Override
        public int size() {
            return path.size();
        }

        @Override
        public Iterator<Vector2i> iterator() {
            return new Iterator<>() {
                private int i = path.size();

                @Override
                public boolean hasNext() {
                    return i > 0;
                }

                @Override
                public Vector2i next() {
                    i--;
                    ANode tgt = path.get(i); // exclusive path.size() and inclusive 0
                    return new Vector2i(tgt.x, tgt.y);
                }
            };
        }
    }

    ;
}
