package NG.Tools;

import NG.DataStructures.Generic.Pair;
import org.joml.Math;
import org.joml.*;

import java.util.Locale;

/**
 * A collection of utility functions for vectors, specifically for Vector2f
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public final class Vectors {
    public static final Vector3fc X = newXVector();
    public static final Vector3fc Y = new Vector3f(0, 1, 0);
    public static final Vector3fc Z = newZVector();
    public static final Vector3fc O = newZeroVector();
    private static final float PI = (float) Math.PI;
    private static final float VECTOR_EQUALITY_DIST_SQ = 1e-6f;

    public static Vector3f newZeroVector() {
        return new Vector3f(0, 0, 0);
    }

    public static Vector3f newZVector() {
        return new Vector3f(0, 0, 1);
    }

    public static Vector3f newXVector() {
        return new Vector3f(1, 0, 0);
    }

    public static String toString(Vector3fc v) {
        if (v == null) return "null";
        return String.format(Locale.US, "(%1.3f, %1.3f, %1.3f)", v.x(), v.y(), v.z());
    }

    public static String toString(Vector2fc v) {
        if (v == null) return "null";
        return String.format(Locale.US, "(%1.3f, %1.3f)", v.x(), v.y());
    }

    public static String toString(Vector3ic v) {
        if (v == null) return "null";
        return String.format("(%d, %d, %d)", v.x(), v.y(), v.z());
    }

    public static String toString(Vector2ic v) {
        if (v == null) return "null";
        return String.format("(%d, %d)", v.x(), v.y());
    }

    public static String toString(Vector4fc v) {
        if (v == null) return "null";
        return String.format(Locale.US, "(%1.3f, %1.3f, %1.3f, %1.3f)", v.x(), v.y(), v.z(), v.w());
    }

    public static String asVectorString(float x, float y) {
        return String.format("(%1.3f, %1.3f)", x, y);
    }

    /**
     * @return a vector with length < 1 that is universally distributed. Would form a solid sphere when created points
     */
    public static Vector3f randomOrb() {
        float phi = Toolbox.random.nextFloat() * 6.2832f;
        float costheta = (Toolbox.random.nextFloat() * 2) - 1;

        float theta = Math.acos(costheta);
        float r = (float) java.lang.Math.cbrt(Toolbox.random.nextFloat());

        float x = (r * Math.sin(theta) * Math.cos(phi));
        float y = (r * Math.sin(theta) * Math.sin(phi));
        float z = (r * Math.cos(theta));
        return new Vector3f(x, y, z);
    }

    /**
     * Rotates a two-dimensional vector on the z-axis in clockwise direction //TODO not counterclock?
     * @param target the vector to rotate
     * @param angle  the angle of rotation
     * @param dest   the vector to store the result
     * @return dest
     */
    public static Vector2f rotate(Vector2fc target, float angle, Vector2f dest) {
        float sin = Math.sin(angle), cos = Math.cos(angle);
        float tx = target.x();
        float ty = target.y();
        dest.x = tx * cos - ty * sin;
        dest.y = tx * sin + ty * cos;
        return dest;
    }

    /**
     * Rotates a two-dimensional vector on the z-axis in clockwise direction //TODO not counterclock?
     * @param target the vector to rotate
     * @param angle  the angle of rotation
     * @return the target vector holding the result
     */
    public static Vector2f rotate(Vector2f target, int angle) {
        float sin = Math.sin((float) angle), cos = Math.cos((float) angle);
        float tx = target.x;
        float ty = target.y;
        target.x = tx * cos - ty * sin;
        target.y = tx * sin + ty * cos;
        return target;
    }

    /**
     * Returns the angle <i>theta</i> from the conversion of rectangular coordinates ({@code x},&nbsp;{@code y}) to
     * polar coordinates (r,&nbsp;<i>theta</i>). This method computes the phase <i>theta</i> by computing an arc tangent
     * of {@code y/x} in the range of -<i>pi</i> to <i>pi</i>.
     * @param vector any vector
     * @return theta such that a vector with {@code x = {@code cos(float)}} and {@code y = {@code sin(float)}} gives
     * {@code vector}, normalized.
     * @see java.lang.Math#atan2(double, double)
     */
    public static float arcTan(Vector2fc vector) {
        return Math.atan2(vector.y(), vector.x());
    }

    /**
     * high(er)-precision calculation of the angle between two vectors.
     * @param v1 a vector
     * @param v2 another vector
     * @return the angle in randians between v1 and v2
     */
    public static float angle(Vector2fc v1, Vector2fc v2) {
        float x1 = v1.x();
        float y1 = v1.y();
        float x2 = v2.x();
        float y2 = v2.y();
        double length1Sqared = x1 * x1 + y1 * y1;
        double length2Sqared = x2 * x2 + y2 * y2;
        double dot = x1 * x2 + y1 * y2;
        double cos = dot / (java.lang.Math.sqrt(length1Sqared * length2Sqared));

        // Cull because sometimes cos goes above 1 or below -1 because of lost precision
        cos = cos < 1 ? cos : 1;
        cos = cos > -1 ? cos : -1;
        return (float) java.lang.Math.acos(cos);
    }

    /**
     * returns the point closest to the given position on the line between startCoord and endCoord
     * @param position   a point in space
     * @param startCoord one end of the line
     * @param endCoord   the other end of the line
     * @return the position p on the line from {@code startCoord} to {@code endCoord} such that the distance to the
     * given {@code position} is minimal
     */
    public static Vector2f getIntersectionPointLine(Vector2fc position, Vector2fc startCoord, Vector2fc endCoord) {
        float aX = startCoord.x();
        float aY = startCoord.y();
        float abX = endCoord.x() - aX;
        float abY = endCoord.y() - aY;
        float t = ((position.x() - aX) * abX + (position.y() - aY) * abY) / (abX * abX + abY * abY);
        if (t < 0.0f) t = 0.0f;
        if (t > 1.0f) t = 1.0f;
        return new Vector2f(aX + t * abX, aY + t * abY);
    }

    /**
     * Calculates the smallest orb around the given points
     * @param points a number of points, at least two
     * @return Left, the middle of the found orb. Right, the radius of the found orb
     */
    public static Pair<Vector3fc, Float> getMinimalOrb(Iterable<Vector3fc> points) {
        // determine furthest two point
        float duoMaxSq = 0;
        Vector3f aMax = new Vector3f();
        Vector3f bMax = new Vector3f();
        for (Vector3fc a : points) {
            for (Vector3fc b : points) {
                float dist = a.distanceSquared(b);
                if (dist > duoMaxSq) {
                    duoMaxSq = dist;
                    aMax.set(a);
                    bMax.set(b);
                }
            }
        }

        // determine point furthest from the middle
        Vector3f mid = aMax.lerp(bMax, 0.5f);
        Vector3f outer = new Vector3f();
        float tripleMaxSq = 0;
        for (Vector3fc vector : points) {
            float dist = mid.distanceSquared(vector);
            if (dist > tripleMaxSq) {
                outer.set(vector);
                tripleMaxSq = dist;
            }
        }

        // if the furthest point is none of the two previous points, determine the circumscribed circle
        // https://en.wikipedia.org/wiki/Circumscribed_circle
        if ((tripleMaxSq > (duoMaxSq / 4)) && !(outer.equals(aMax) || outer.equals(bMax))) {
            Vector3f temp2 = new Vector3f();
            Vector3f temp3 = new Vector3f();

            Vector3fc a = aMax.sub(outer, new Vector3f());
            Vector3fc b = bMax.sub(outer, new Vector3f());

            Vector3f dif = b.mul(a.lengthSquared(), temp2)
                    .sub(a.mul(b.lengthSquared(), temp3), temp2);
            float scalar = 2 * a.cross(b, temp3).lengthSquared();

            mid.set(
                    dif.cross(a.cross(b, new Vector3f())).div(scalar).add(outer)
            );
        }

        return new Pair<>(mid, mid.distance(aMax));
    }

    /**
     * evaluates a beziér curve defined by vectors. The given ABCD vectors are kept intact.
     * @param A starting point
     * @param B first control point
     * @param C second control point
     * @param D ending point
     * @param u fraction of the curve to be requested
     * @return vector to the point on the curve on fraction u
     */
    public static Vector3f bezierPoint(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D, double u) {
        Vector3f temp = new Vector3f();
        //A*(1−u)^3 + B*3u(1−u)^2 + C*3u^2(1−u) + D*u^3
        return new Vector3f(A).mul((float) ((1 - u) * (1 - u) * (1 - u)))
                .add(B.mul((float) (3 * u * (1 - u) * (1 - u)), temp))
                .add(C.mul((float) (3 * u * u * (1 - u)), temp))
                .add(D.mul((float) (u * u * u), temp));
    }

    /**
     * evaluates the derivative of a beziér curve on a point defined by u
     * @see #bezierPoint(Vector3fc, Vector3fc, Vector3fc, Vector3fc, double)
     */
    public static Vector3f bezierDerivative(Vector3fc A, Vector3fc B, Vector3fc C, Vector3fc D, double u) {
        Vector3f direction = new Vector3f();
        Vector3f temp = new Vector3f();
        final Vector3f point = new Vector3f();
        //(B-A)*3*(1-u)^2 + (C-B)*6*(1-u)*u + (D-C)*3*u^2
        (B.sub(A, point))
                .mul((float) (3 * (1 - u) * (1 - u)), point)
                .add(C.sub(B, temp).mul((float) (6 * (1 - u) * u), temp), direction)
                .add(D.sub(C, temp).mul((float) (3 * u * u), temp), direction);
        return direction;
    }

    /**
     * swaps the contents of a and b
     * @param a
     * @param b
     */
    public static void swap(Vector3f a, Vector3f b) {
        float t;
        t = a.x;
        a.x = b.x;
        b.x = t;
        t = a.y;
        a.y = b.y;
        b.y = t;
        t = a.z;
        a.z = b.z;
        b.z = t;
    }

    /**
     * @param vec any vector
     * @return true iff none of the scalars of the given vector is nan, and the length of the vector is not zero
     */
    public static boolean isScalable(Vector3fc vec) {
        return !((vec.x() == 0) && (vec.y() == 0) && (vec.z() == 0));
    }

    public static Vector3f getNormalVector(Vector3fc A, Vector3fc B, Vector3fc C) {
        Vector3f BC = new Vector3f(C).sub(B);
        Vector3f BA = new Vector3f(A).sub(B);
        return BA.cross(BC).normalize();
    }

    public static Vector2i toVector2i(Vector3ic coordinate) {
        return new Vector2i(coordinate.x(), coordinate.y());
    }

    public static Matrix4f toMatrix4f(float[] f) {
        assert f.length == 16;
        return new Matrix4f(
                f[0], f[1], f[2], f[3],
                f[4], f[5], f[6], f[7],
                f[8], f[9], f[10], f[11],
                f[12], f[13], f[14], f[15]
        );
    }

    public static Vector3f sizeOf(AABBf box) {
        return new Vector3f(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
    }

    public static boolean almostEqual(Vector3fc alpha, Vector3fc beta) {
        return alpha.distanceSquared(beta) < VECTOR_EQUALITY_DIST_SQ;
    }

    /**
     * answers the ray equation <i>origin + t * dir</i>
     * @param ray a ray with (origin, direction)
     * @param t   the value of t to fill in
     */
    public static Vector3f getFromRay(Rayf ray, float t) {
        return new Vector3f(ray.oX + ray.dX * t, ray.oY + ray.dY * t, ray.oZ + ray.dZ * t);
    }

    /** @return a rotation that maps the x-vector to the given direction, with up in direction of z */
    public static Quaternionf xTo(Vector3fc direction) {
        if (direction.y() == 0 && direction.z() == 0 && direction.x() < 0) {
            return new Quaternionf().rotateZ((float) java.lang.Math.PI);
        }

        float yawAngle = Math.atan2(direction.y(), direction.x());
        float hzMovement = Math.sqrt(direction.x() * direction.x() + direction.y() * direction.y());
        float pitchAngle = Math.atan2(direction.z(), hzMovement);

        return new Quaternionf()
                .rotateY(-pitchAngle)
                .rotateLocalZ(yawAngle);
    }

    public static boolean inSameDirection(Vector3fc a, Vector3fc b) {
        return a.dot(b) > 0;
    }

    public static void unionBoxTransformed(AABBf box, AABBf other, Matrix4fc transformation) {
        Vector3f point = new Vector3f();
        box.union(point.set(other.minX, other.minY, other.minZ).mulPosition(transformation));
        box.union(point.set(other.maxX, other.minY, other.minZ).mulPosition(transformation));
        box.union(point.set(other.minX, other.maxY, other.minZ).mulPosition(transformation));
        box.union(point.set(other.minX, other.minY, other.maxZ).mulPosition(transformation));
        box.union(point.set(other.maxX, other.maxY, other.minZ).mulPosition(transformation));
        box.union(point.set(other.maxX, other.minY, other.maxZ).mulPosition(transformation));
        box.union(point.set(other.minX, other.maxY, other.maxZ).mulPosition(transformation));
        box.union(point.set(other.maxX, other.maxY, other.maxZ).mulPosition(transformation));
    }

    public static boolean isNaN(Vector3fc v) {
        return Float.isNaN(v.x()) || Float.isNaN(v.y()) || Float.isNaN(v.z());
    }

    public static final class Scaling {
        public static final Vector3fc UNIFORM = new Vector3f(1, 1, 1);
        /** scaling that mirrors in the X direction */
        public static final org.joml.Vector3fc MIRROR_X = new Vector3f(-1, 1, 1);
        /** scaling that mirrors in the Y direction */
        public static final Vector3fc MIRROR_Y = new Vector3f(1, -1, 1);
        /** scaling that mirrors in the Z direction */
        public static final Vector3fc MIRROR_Z = new Vector3f(1, 1, -1);
    }
}
