package NG.Graph.Layout;

/**
 * Class for performing matrix calculations specific to PCA.
 * @author Kushal Ranjan
 * @version 051413
 */
class ArrayMatrix {

    /**
     * Returns the transpose of the input matrix.
     * @param matrix double[][] matrix of values
     * @return the matrix transpose of matrix
     */
    static double[][] transpose(double[][] matrix) {
        double[][] out = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[0].length; j++) {
                out[i][j] = matrix[j][i];
            }
        }
        return out;
    }

    /**
     * Returns the difference of a and b.
     * @param a double[][] matrix of values
     * @param b double[][] matrix of values
     * @return the matrix difference a - b
     */
    static double[][] subtract(double[][] a, double[][] b) {
        assert  (a.length == b.length);
        assert (a[0].length == b[0].length);

        double[][] out = new double[a.length][a[0].length];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[0].length; j++) {
                out[i][j] = a[i][j] - b[i][j];
            }
        }
        return out;
    }

    /**
     * Returns the matrix product of a and b; if the horizontal length of a is not equal to the vertical length of b,
     * throws an exception.
     * @param a double[][] matrix of values
     * @param b double[][] matrix of values
     * @return the matrix product ab
     */
    static double[][] multiply(double[][] a, double[][] b) {
        assert (a.length == b[0].length);
        double[][] out = new double[b.length][a[0].length];
        for(int i = 0; i < out.length; i++) {
            for(int j = 0; j < out[0].length; j++) {
                double[] row = getRow(a, j);
                double[] column = getColumn(b, i);
                out[i][j] = dot(row, column);
            }
        }
        return out;
    }

    /**
     * returns a * b
     */
    static double[] transform(double[][] a, double[] b) {
        assert (a.length == b.length);
        double[] out = new double[a[0].length];

        for (int i = 0; i < a[0].length; i++) {
            out[i] = 0;

            for (int j = 0; j < a.length; j++) {
                out[i] += b[j] * a[j][i];
            }
        }

        return out;
    }


    /**
     * Returns a version of mat scaled by a constant.
     * @param mat   input matrix
     * @param coeff constant by which to scale
     * @return mat scaled by coeff
     */
    static double[][] scale(double[][] mat, double coeff) {
        double[][] out = new double[mat.length][mat[0].length];
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[0].length; j++) {
                out[i][j] = mat[i][j] * coeff;
            }
        }
        return out;
    }

    /**
     * Takes the dot product of two vectors, {a[0]b[0], ..., a[n]b[n]}.
     * @param a double[] of values
     * @param b double[] of values
     * @return the dot product of a with b
     */
    static double dot(double[] a, double[] b) {
        assert (a.length == b.length);
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Returns a copy of the input matrix.
     * @param input double[][] to be copied
     */
    static double[][] copy(double[][] input) {
        double[][] copy = new double[input.length][input[0].length];
        for (int i = 0; i < copy.length; i++) {
            System.arraycopy(input[i], 0, copy[i], 0, copy[i].length);
        }
        return copy;
    }

    /**
     * Returns the ith column of the input matrix.
     */
    static double[] getColumn(double[][] matrix, int i) {
        return matrix[i];
    }

    /**
     * Returns the ith row of the input matrix.
     */
    static double[] getRow(double[][] matrix, int i) {
        double[] vals = new double[matrix.length];
        for (int j = 0; j < vals.length; j++) {
            vals[j] = matrix[j][i];
        }
        return vals;
    }

    /**
     * Returns a normalized version of the input vector, i.e. vec scaled such that ||vec|| = 1.
     * @return vec/||vec||
     */
    static double[] normalize(double[] vec) {
        double[] newVec = new double[vec.length];
        double norm = length(vec);
        for (int i = 0; i < vec.length; i++) {
            newVec[i] = vec[i] / norm;
        }
        return newVec;
    }

    /**
     * Computes the norm of the input vector
     * @return ||vec||
     */
    static double length(double[] vec) {
        return Math.sqrt(dot(vec, vec));
    }

    static double[][] getCovariance(double[][] matrix) {

        int ySize = matrix[0].length;

        double[][] out = new double[ySize][ySize];
        for(int i = 0; i < ySize; i++) {
            for(int j = 0; j < ySize; j++) {
                double sum = 0;
                for (double[] doubles : matrix) {
                    sum += doubles[j] * doubles[i];
                }
                out[i][j] = sum;
            }
        }

        return scale(out, 1f / ySize);
    }
}
