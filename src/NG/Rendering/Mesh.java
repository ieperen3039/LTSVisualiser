package NG.Rendering;

import NG.Rendering.Shaders.SGL;
import NG.Resources.LazyInit;

import java.util.Arrays;

/**
 * @author Geert van Ieperen created on 17-11-2017.
 */
public interface Mesh {
    Mesh EMPTY_MESH = new Mesh() {
        @Override
        public void render(SGL.Painter lock) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public String toString() {
            return "EMPTY_MESH";
        }
    };

    /**
     * draws the object on the gl buffer. This method may only be called by a class that implements SGL. To draw a mesh,
     * use {@link SGL#render(Mesh)}
     * @param lock a non-null object that can only be generated by a SGL object.
     */
    void render(SGL.Painter lock);

    /**
     * removes this mesh from the GPU. This method must be called exactly once before it is disposed
     */
    void dispose();

    static LazyInit<Mesh> emptyMesh() {
        return new LazyInit<>(() -> EMPTY_MESH, null);
    }

    /**
     * a record class to describe a plane by indices. This object is not robust, thus one may not assume it is
     * immutable.
     */
    class Face {
        /** indices of the vertices of this face */
        public final int[] vert;
        /** indices of the normals of this face */
        public final int[] norm;
        /** indices of the texture coordinates of this face */
        public final int[] tex;
        /** indices of the colors of this face */
        public final int[] col;

        public Face(int[] vertexIndices, int[] normalIndices, int[] textureIndices, int[] colors) {
            int size = vertexIndices.length;
            assert (normalIndices.length == size && textureIndices.length == size);

            this.vert = vertexIndices;
            this.norm = normalIndices;
            this.tex = textureIndices;
            this.col = colors;
        }

        /**
         * a description of a plane, with the indices of the vertices and normals given. The indices should refer to a
         * list of vertices and normals that belong to a list of faces where this face is part of.
         */
        public Face(int[] vertices, int[] normals) {
            int size = vertices.length;
            assert (normals.length == size);

            this.vert = vertices;
            this.norm = normals;
            this.tex = null;
            this.col = null;
        }

        /**
         * a description of a plane, with the indices of the vertices and normals given. The indices should refer to a
         * list of vertices and normals that belong to a list of faces where this face is part of.
         */
        public Face(int[] vertices, int nInd) {
            this(vertices, new int[vertices.length]);
            Arrays.fill(norm, nInd);
        }

        public int size() {
            return vert.length; // vertices is always non-null
        }

        /**
         * parses a face object from a given line of an OBJ formatted file
         * @param tokens a line of a face, split on whitespaces, with 'f' on position 0.
         */
        public static Face parseOBJ(String[] tokens) {
            assert tokens[0].equals("f") : Arrays.toString(tokens);

            int nrOfVerices = tokens.length - 1;

            int[] vert = new int[nrOfVerices];
            int[] norm = new int[nrOfVerices];
            int[] tex = new int[nrOfVerices];

            for (int i = 0; i < nrOfVerices; i++) {
                String[] indices = tokens[i + 1].split("/");
                vert[i] = readSymbol(indices[0]);
                tex[i] = readSymbol(indices[1]);
                norm[i] = readSymbol(indices[2]);
            }

            return new Face(vert, norm, tex, null);
        }

        /**
         * parses a face object from a given line of an PLY formatted file
         * @param tokens a line describing the index of a face
         */
        public static Face parsePLY(String[] tokens) {
            int nrOfVertices = tokens.length - 1;
            int[] indices = new int[nrOfVertices];

            for (int i = 0; i < nrOfVertices; i++) {
                indices[i] = Integer.parseInt(tokens[i + 1]);
            }

            return new Face(indices, indices, indices, indices);
        }

        private static int readSymbol(String index) {
            return index.isEmpty() ? -1 : Integer.parseInt(index) - 1;
        }

    }
}