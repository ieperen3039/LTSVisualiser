package NG.Graph;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Geert van Ieperen created on 25-9-2020.
 */
public class BarnesHutTree {
    private final InternalNode root;
    private BiFunction<Vector3fc, Vector3fc, Vector3f> function;
    private float maxTheta;
    private int maxDepth = 8;

    public BarnesHutTree(float universeSize) {
        root = new InternalNode(new Vector3f(), universeSize, 0);
        setForceComputation((a, b) -> new Vector3f(a).sub(b).div(a.distanceSquared(b)));
        setMaxTheta(0.5f);
    }

    public void setForceComputation(BiFunction<Vector3fc, Vector3fc, Vector3f> function) {
        this.function = function;
    }

    public void setMaxTheta(float maxTheta) {
        this.maxTheta = maxTheta;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void add(Vector3f position) {
        // limit node position to tree area
        float maxValue = root.voxelSize / 2;
        if (position.x > maxValue) position.x = maxValue;
        if (position.x < -maxValue) position.x = -maxValue;
        if (position.y > maxValue) position.y = maxValue;
        if (position.y < -maxValue) position.y = -maxValue;
        if (position.z > maxValue) position.z = maxValue;
        if (position.z < -maxValue) position.z = -maxValue;

        root.add(position);
    }

    /**
     * Computes the net force of an element at the given position. If element is not null, the given element is assumed
     * to be the target element and is ignored.
     * @param position the position of the element to compute
     * @return the net force exerted on an element on the given position
     */
    public Vector3f getForceOn(Vector3fc position) {
        return root.getForce(position);
    }

    interface Node {
        /**
         * @param position the position of the element to compute
         * @return the cumulative force that this node exerts on the given element.
         */
        Vector3f getForce(Vector3fc position);

        void add(Vector3fc position);
    }

    private class InternalNode implements Node {
        private final List<Node> children;
        private final Vector3f middle;
        private final float voxelSize;
        private final Vector3f centerOfMass;
        private final int depth;
        private int nrOfElements;

        public InternalNode(Vector3f middle, float voxelSize, int depth) {
            this.children = new ArrayList<>(8);
            this.middle = middle;
            this.voxelSize = voxelSize;
            this.depth = depth;
            centerOfMass = new Vector3f();

            for (int i = 0; i < 8; i++) {
                children.add(null);
            }
        }

        public void add(Vector3fc position) {
            float x = position.x();
            float y = position.y();
            float z = position.z();

            assert x >= middle.x - voxelSize / 2 && x <= middle.x + voxelSize / 2;
            assert y >= middle.y - voxelSize / 2 && y <= middle.y + voxelSize / 2;
            assert z >= middle.z - voxelSize / 2 && z <= middle.z + voxelSize / 2;

            int i = 0;
            if (x < middle.x) i += 4;
            if (y < middle.y) i += 2;
            if (z < middle.z) i += 1;
            Node elt = children.get(i);

            if (elt == null) {
                elt = new ExternalNode(position);
                children.set(i, elt);

            } else {
                if (elt instanceof ExternalNode && depth < maxDepth) {
                    // expand node
                    Vector3f newMiddle = new Vector3f(middle);
                    float quarter = voxelSize / 4;
                    newMiddle.x += (x < middle.x) ? -quarter : quarter;
                    newMiddle.y += (y < middle.y) ? -quarter : quarter;
                    newMiddle.z += (z < middle.z) ? -quarter : quarter;

                    // transform to internal
                    ExternalNode oldElt = (ExternalNode) elt;
                    InternalNode newNode = new InternalNode(newMiddle, voxelSize / 2, depth + 1);
                    newNode.placeExternal(oldElt);

                    elt = newNode;
                    children.set(i, elt);
                }

                elt.add(position);
            }

            centerOfMass.mul(nrOfElements).add(position).div(nrOfElements + 1);
            nrOfElements++;
        }

        private void placeExternal(ExternalNode elt) {
            for (Vector3fc pos : elt.positions) {
                add(pos);
            }
        }

        @Override
        public Vector3f getForce(Vector3fc position) {
            // To determine if a node is sufficiently far away, compute the quotient s / d, where s is the width of the
            // region represented by the internal node, and d is the distance between the body and the node’s center-of-mass
            float theta = voxelSize / position.distance(centerOfMass);

            if (theta < maxTheta) {
                // treat as single body
                Vector3f force = function.apply(position, centerOfMass);
                return force.mul(nrOfElements);

            } else {
                Vector3f total = new Vector3f();
                for (Node node : children) {
                    if (node == null) continue;
                    Vector3f force = node.getForce(position);
                    total = total.add(force);
                }
                return total;
            }
        }
    }

    private class ExternalNode implements Node {
        public final List<Vector3fc> positions;

        public ExternalNode(Vector3fc position) {
            this.positions = new ArrayList<>();
            positions.add(position);
        }

        @Override
        public void add(Vector3fc position) {
            positions.add(position);
        }

        @Override
        public Vector3f getForce(Vector3fc position) {
            Vector3f combinedForce = new Vector3f();

            boolean foundEqual = false;
            for (Vector3fc other : positions) {
                // the requested position occurs exactly once in this node already, we ignore it.
                if (position.equals(other) && !foundEqual) {
                    foundEqual = true;
                    continue;
                }

                Vector3f force = function.apply(position, other);
                combinedForce.add(force);
            }

            return combinedForce;
        }
    }
}
