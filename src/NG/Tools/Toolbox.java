package NG.Tools;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;
import static org.lwjgl.opengl.GL45.GL_CONTEXT_LOST;

/**
 * Created by Geert van Ieperen on 31-1-2017. a class with various tools
 */
public final class Toolbox {

    // universal random to be used everywhere
    public static final Random random = new Random();
    public static final double PHI = 1.6180339887498948;
    public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    public static final Pattern PERIOD_MATCHER = Pattern.compile("\\.");

    private static final float ROUNDINGERROR = 1E-6F;

    // a set of possible titles for error messages
    private static final String[] ERROR_MESSAGES = new String[]{
            "I Blame Menno", "You're holding it wrong", "This title is at random",
            "You can't blame me for this", "Something Happened", "Oops!", "stuff's broke lol",
            "Look at what you have done", "Please ignore the following message", "Congratulations!"
    };

    public static void checkGLError(String name) {
        int error;
        int i = 0;

        while ((error = glGetError()) != GL_NO_ERROR) {
            Logger.ERROR.printFrom(2, name + ": " + asHex(error) + " " + getMessage(error));
            if (++i == 20) throw new IllegalStateException("Context is probably not current for this thread");
        }
    }

    private static String getMessage(int error) {
        switch (error) {
            case GL_INVALID_ENUM:
                return "Invalid Enum";
            case GL_INVALID_VALUE:
                return "Invalid Value";
            case GL_INVALID_OPERATION:
                return "Invalid Operation";
            case GL_STACK_OVERFLOW:
                return "Stack Overflow";
            case GL_STACK_UNDERFLOW:
                return "Stack Underflow";
            case GL_OUT_OF_MEMORY:
                return "Out of Memory";
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return "Invalid Framebuffer Operation";
            case GL_CONTEXT_LOST:
                return "Context Lost";

        }
        return "Unknown Error";
    }

    public static String asHex(int decimal) {
        return "0x" + Integer.toHexString(decimal).toUpperCase();
    }

    /**
     * call System.exit and tells who did it
     */
    public static void exitJava() {
        try {
            Logger.ERROR.newLine();
            Logger.DEBUG.printFrom(2, "Ending JVM");
            Thread.sleep(10);
            Thread.dumpStack();
            System.exit(-1);
        } catch (InterruptedException e) {
            System.exit(-1);
        }
    }

    public static boolean almostZero(float number) {
        return (((number + ROUNDINGERROR) >= 0.0f) && ((number - ROUNDINGERROR) <= 0.0f));
    }

    /**
     * performs an incremental insertion-sort on (preferably nearly-sorted) the given array. modifies items
     * @param items the array to sort
     * @param map   maps a moving source to the value to be sorted upon
     */
    public static <Type> void insertionSort(Type[] items, Function<Type, Float> map) {
        // iterate incrementally over the array
        for (int head = 1; head < items.length; head++) {
            Type subject = items[head];

            // decrement for the right position
            int empty = head;

            while (empty > 0) {
                Type target = items[empty - 1];

                if (map.apply(target) > map.apply(subject)) {
                    items[empty] = target;
                    empty--;
                } else {
                    break;
                }
            }
            items[empty] = subject;
        }
    }

    /** returns a uniformly distributed random value between val1 and val2 */
    public static float randomBetween(float val1, float val2) {
        return val1 + ((val2 - val1) * random.nextFloat());
    }

    /**
     * transforms a floating point value to an integer value, by drawing a random variable for the remainder.
     * @return an int i such that for float f, we have (f - 1 < i < f + 1) and the average return value is f.
     */
    public static int randomToInt(float value) {
        int floor = (int) value;
        if (floor == value) return floor;
        return random.nextFloat() > (value - floor) ? floor : floor + 1;
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * merges a joining array into this array
     * @param host the sorted largest non-empty of the arrays to merge, entities in this array will be checked for
     *             relevance.
     * @param join the sorted other non-empty array to merge
     * @param map  maps a moving source to the value to be sorted upon
     * @return a sorted array of living entities from both host and join combined.
     */
    public static <Type> Type[] mergeArrays(Type[] host, Type[] join, Function<Type, Float> map) {
        int hLength = host.length;
        int jLength = join.length;

        Type[] results = Arrays.copyOf(host, hLength + jLength);
        // current indices
        int hIndex = 0;
        int jIndex = 0;

        for (int i = 0; i < results.length; i++) {
            if (jIndex >= jLength) {
                results[i] = host[hIndex];
                hIndex++;

            } else if (hIndex >= hLength) {
                results[i] = join[jIndex];
                jIndex++;

            } else {
                Type hostItem = host[hIndex];
                Type joinItem = join[jIndex];

                // select the smallest
                if (map.apply(hostItem) < map.apply(joinItem)) {
                    results[i] = hostItem;
                    hIndex++;

                } else {
                    results[i] = joinItem;
                    jIndex++;
                }
            }
        }

        // loop automatically ends after at most (i = alpha.length + beta.length) iterations
        return results;
    }

    public static <Type> int binarySearch(Type[] array, Function<Type, Float> map, float value) {
        int low = 0;
        int high = array.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Type e = array[mid];

            float cmp = map.apply(e);
            if (cmp < value) {
                low = mid + 1;
            } else if (cmp > value) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found.
    }

    public static boolean isValidQuaternion(Quaternionf rotation) {
        return !(Float.isNaN(rotation.x) || Float.isNaN(rotation.y) || Float.isNaN(rotation.z) || Float.isNaN(rotation.w));
    }

    public static String[] toStringArray(Object[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toString();
        }
        return result;
    }

    public static <T> T findClosest(String target, T[] options) {
        int max = 0;
        int lengthOfMax = Integer.MAX_VALUE;
        T best = null;

        for (T candidate : options) {
            String asString = candidate.toString();
            int wordLength = Math.abs(asString.length() - target.length());
            int dist = hammingDistance(target, asString);

            if (dist > max || (dist == max && wordLength < lengthOfMax)) {
                max = dist;
                lengthOfMax = wordLength;
                best = candidate;
            }
        }

        return best;
    }

    /**
     * computes the longest common substring of string a and b
     */
    // LCSLength(X[1..m], Y[1..n])
    //  C = array(0..m, 0..n)
    //  for i := 1..m
    //      for j := 1..n
    //          if X[i] = Y[j]
    //              C[i,j] := C[i-1,j-1] + 1
    //          else
    //              C[i,j] := max(C[i,j-1], C[i-1,j])
    //  return C[m,n]
    public static int hammingDistance(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[][] cMat = new int[m + 1][n + 1]; // initialized at 0

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char ca = a.charAt(i - 1);
                char cb = b.charAt(j - 1);
                if (ca == cb) {
                    cMat[i][j] = cMat[i - 1][j - 1] + 1;
                } else {
                    cMat[i][j] = Math.max(cMat[i][j - 1], cMat[i - 1][j]);
                }
            }
        }

        return cMat[m][n];
    }

    public static ByteBuffer toByteBuffer(Path path) throws IOException {
        ByteBuffer buffer;

        try (SeekableByteChannel fc = Files.newByteChannel(path)) {
            buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
            while (fc.read(buffer) != -1) ;
        }

        buffer.flip();
        return buffer;
    }

    /**
     * @return the greatest common integer diviser of a and b.
     */
    public static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static float interpolate(float a, float b, float fraction) {
        return ((b - a) * fraction) + a;
    }

    public static void display(Exception e) {
        Logger.ERROR.print(e);
        int rng = random.nextInt(ERROR_MESSAGES.length);

        JOptionPane.showMessageDialog(null, e.getClass() + ":\n" + e.getMessage(), ERROR_MESSAGES[rng], JOptionPane.ERROR_MESSAGE);
    }

    public static <T> Iterator<T> singletonIterator(T action) {
        // from Collections.singletonIterator
        return new Iterator<T>() {
            private boolean hasNext = true;

            public boolean hasNext() {
                return hasNext;
            }

            public T next() {
                hasNext = false;
                return action;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> element) {
                Objects.requireNonNull(element);
                if (hasNext) {
                    hasNext = false;
                    element.accept(action);
                }
            }
        };
    }

    public static <T> List<T> combinedList(List<T> a, List<T> b) {
        return new AbstractList<T>() {
            final List<T> aList = a;
            final List<T> bList = b;

            @Override
            public T get(int index) {
                int aSize = aList.size();
                if (index > aSize) return bList.get(index - aSize);
                return aList.get(index);
            }

            @Override
            public int size() {
                return aList.size() + bList.size();
            }

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Iterator<T> aItr = aList.iterator();
                    final Iterator<T> bItr = bList.iterator();

                    @Override
                    public boolean hasNext() {
                        return bItr.hasNext() || aItr.hasNext();
                    }

                    @Override
                    public T next() {
                        return aItr.hasNext() ? aItr.next() : bItr.next();
                    }
                };
            }
        };
    }

    /** @return f such that interpolate(a, b, f) = target */
    public static float getFraction(float a, float b, float target) {
//        target = ((b - a) * f) + a;
//        target - a = (b - a) * f;
//        (target - a) / (b - a) = f;
        return (target - a) / (b - a);
    }

    public static <T> Map<T, Integer> getIntersection(Map<T, Integer> a, Map<T, Integer> b) {
        Map<T, Integer> intersection = new HashMap<>();
        for (T type : a.keySet()) {
            if (b.containsKey(type)) {
                int amount = Math.min(a.get(type), b.get(type));
                if (amount > 0) {
                    intersection.put(type, amount);
                }
            }
        }
        return intersection;
    }

    public Vector3f bezier(Vector3fc A, Vector3fc B, Vector3fc C, float u) {
        Vector3f temp = new Vector3f();
        final float uinv = 1 - u;
        // A * uinv2 + 2B*uinv*u + C * u2
        return new Vector3f(A).mul(uinv * uinv)
                .add(B.mul(2 * u * uinv, temp))
                .add(C.mul(u * u, temp));
    }
}
