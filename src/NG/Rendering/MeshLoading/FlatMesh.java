package NG.Rendering.MeshLoading;

import NG.DataStructures.Generic.Color4f;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Resources.GeneratorResource;
import NG.Resources.Resource;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * A mesh that supports (only) flat shading. Allows colors, but no textures
 * @author Geert van Ieperen created on 17-11-2017.
 */
public class FlatMesh extends AbstractMesh {

    /**
     * Creates a mesh from the given data. This may only be called on the main thread. VERY IMPORTANT that you have
     * first called {@link GL#createCapabilities()} (or similar) for openGL 3 or higher.
     * @param posList   a list of vertices
     * @param normList  a list of normal vectors
     * @param facesList a list of faces, where each face refers to indices from posList and normList
     */
    public FlatMesh(
            List<? extends Vector3fc> posList, List<? extends Vector3fc> normList, List<Color4f> colorList,
            List<Mesh.Face> facesList
    ) {
        if (facesList.isEmpty()) return;

        // Create position array in the order it has been declared. faces have (nOfEdges) vertices of 3 indices
        int nrOfVecElements = facesList.size() * 3;
        setElementCount(nrOfVecElements);
        float[] posArr = new float[nrOfVecElements * 3];
        float[] normArr = new float[nrOfVecElements * 3];
        float[] colorArr = new float[nrOfVecElements * 4];

        // all opaque white
        if (colorList == null) Arrays.fill(colorArr, 1.0f);

        for (int i = 0; i < facesList.size(); i++) {
            Mesh.Face face = facesList.get(i);
            readFaceVertex(face, posList, i, posArr);
            readFaceNormals(face, normList, i, normArr);

            if (colorList != null) {
                readFaceColors(face, colorList, i, colorArr);
            }
        }

        writeToGL(posArr, normArr, colorArr);
    }

    /**
     * Creates a mesh from the given data. This may only be called on the main thread. VERY IMPORTANT that you have
     * first called {@link GL#createCapabilities()} (or similar) for openGL 3 or higher.
     * @param posList   a list of vertices
     * @param normList  a list of normal vectors
     * @param facesList a list of faces, where each face refers to indices from posList and normList
     */
    public FlatMesh(
            List<? extends Vector3fc> posList, List<? extends Vector3fc> normList, List<Mesh.Face> facesList
    ) {
        this(posList, normList, null, facesList);
    }

    /**
     * create a mesh and store it to the GL. For both lists it holds that the ith vertex has the ith normal vector
     * @param positions the vertices, concatenated in groups of 3
     * @param normals   the normals, concatenated in groups of 3
     * @param colors    the vertex colors, concatenated in groups of 4. May be null.
     * @throws IllegalArgumentException if positions or normals has length not divisible by 3, or when colors has length
     *                                  not divisible by 4.
     * @throws IllegalArgumentException if the arrays are of unequal length
     * @throws IllegalStateException    if the mesh is already loaded
     */
    private void writeToGL(float[] positions, float[] normals, float[] colors) {
        if (getVAO() != 0) throw new IllegalStateException("Tried loading a mesh that was already loaded");
//        assert testAssumptions(positions, normals, colors);

        initMesh();
        // all 0's

        glBindVertexArray(getVAO());

        // Position VBO
        createVBO(positions, ShaderProgram.VERTEX_LOCATION, 3);

        // Vertex normals VBO
        createVBO(normals, ShaderProgram.NORMAL_LOCATION, 3);

        // Vertex color VBO
        createVBO(colors, ShaderProgram.COLOR_LOCATION, 4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        Toolbox.checkGLError(toString());
    }

    /**
     * test that the given arrays correctly represent a coherent set of vertices, normals, colors and coordinates.
     * @return always true
     * @throws IllegalArgumentException if any of the assumptions are invalid
     */
    private boolean testAssumptions(float[] positions, float[] normals, float[] colors) {
        if (((positions.length % 3) != 0) || (positions.length == 0)) {
            throw new IllegalArgumentException("received invalid position array of length " + positions.length + ".");

        } else if (normals.length != positions.length) {
            throw new IllegalArgumentException("received a normals array that is not as long as vertices array: " +
                    positions.length + " position values and " + normals.length + " normal values");

        } else if (colors != null && (colors.length * 3) != (positions.length * 4)) {
            throw new IllegalArgumentException("received a color array that is not of the required size: " +
                    "expected " + ((positions.length * 4) / 3) + " color values but got " + colors.length + " color values");
        }

        return true;
    }

    /**
     * creates a Mesh of a section of the given heightmap. Note that the xEnd value should not be larger than
     * (heightmap.length - 1), same for yEnd. The returned supplier must be activated on the current GL context, this
     * function does not have to be called there.
     * @param heightmap the heightmap, giving the height of a virtual (x, y) coordinate
     * @param xStart    the lowest x index to consider, inclusive
     * @param xEnd      the the highest x index to consider, inclusive.
     * @param yStart    the lowest y index to consider, inclusive
     * @param yEnd      the the highest y index to consider, inclusive.
     * @param edgeSize  the distance between two vertices in real coordinates. Multiplying a virtual coordinate with
     *                  this value gives the real coordinate.
     * @return a mesh of the heightmap, using quads, positioned in absolute coordinates. (no transformation is needed)
     */
    public static Resource<Mesh> meshFromHeightmap(
            float[][] heightmap, int xStart, int xEnd, int yStart, int yEnd, float edgeSize
    ) {
        int nOfXFaces = xEnd - xStart;
        int nOfYFaces = yEnd - yStart;
        int nOfVertices = (nOfXFaces + 1) * (nOfYFaces + 1);

        // vertices and normals
        List<Vector3fc> vertices = new ArrayList<>(nOfVertices);
        List<Vector3fc> normals = new ArrayList<>(nOfVertices);

        for (int y = yStart; y <= yEnd; y++) {
            for (int x = xStart; x <= xEnd; x++) {
                // vertex
                float height = heightmap[x][y];
                Vector3f vertex = new Vector3f(
                        x * edgeSize,
                        y * edgeSize,
                        height * edgeSize
                );
                vertices.add(vertex);

                // normal
                Vector3f normal = new Vector3f(0, 0, 1);
                if ((x - 1 >= 0) && (y - 1 >= 0) && (x + 1 < heightmap.length) && (y + 1 < heightmap[x].length)) {
                    float dx = heightmap[x - 1][y] - heightmap[x + 1][y];
                    float dy = heightmap[x][y - 1] - heightmap[x][y + 1];
                    normal.x = dx / 2;
                    normal.y = dy / 2;
                }

                // no need for normalisation
                normals.add(normal);
            }
        }

        // faces
        int nOfQuads = nOfXFaces * nOfYFaces;
        int arrayXSize = xEnd - xStart + 1;
        List<Mesh.Face> faces = new ArrayList<>(nOfQuads);

        for (int y = 0; y < nOfYFaces; y++) {
            for (int x = 0; x < nOfXFaces; x++) {
                int left = y * arrayXSize + x;
                int right = (y + 1) * arrayXSize + x;

                faces.add(new Mesh.Face(
                        new int[]{left, right + 1, left + 1},
                        new int[]{left, right + 1, left + 1}
                ));
                faces.add(new Mesh.Face(
                        new int[]{left, right, right + 1},
                        new int[]{left, right, right + 1}
                ));
            }
        }

        return new GeneratorResource<>(() -> new FlatMesh(vertices, normals, faces), Mesh::dispose);
    }

    private static void readFaceVertex(
            Mesh.Face face, List<? extends Vector3fc> posList, int faceNumber, float[] posArr
    ) {
        assert face.size() == 3 : "Face is not a triangle";

        int vectorIndex = faceNumber * 3;
        for (int i = 0; i < 3; i++) {
            readVector(vectorIndex + i, posList, posArr, face.vert[i]);
        }
    }

    private static void readFaceNormals(
            Mesh.Face face, List<? extends Vector3fc> normList, int faceNumber, float[] normArr
    ) {
        assert face.size() == 3 : "Face is not a triangle";

        int vectorIndex = faceNumber * 3;
        for (int i = 0; i < 3; i++) {
            readVector(vectorIndex + i, normList, normArr, face.norm[i]);
        }
    }

    private static void readFaceColors(Mesh.Face face, List<Color4f> colorList, int faceNumber, float[] colorArr) {
        assert face.size() == 3 : "Face is not a triangle";

        int vectorIndex = faceNumber * 3;
        for (int i = 0; i < 3; i++) {
            Color4f color = colorList.get(face.col[i]);
            int offset = (vectorIndex + i) * 4;

            colorArr[offset] = color.red;
            colorArr[offset + 1] = color.green;
            colorArr[offset + 2] = color.blue;
            colorArr[offset + 3] = color.alpha;
        }
    }

    private static void readVector(
            int vectorNumber, List<? extends Vector3fc> sourceList, float[] targetArray, int index
    ) {
        Vector3fc vertex = sourceList.get(index);
        int offset = vectorNumber * 3;
        targetArray[offset] = vertex.x();
        targetArray[offset + 1] = vertex.y();
        targetArray[offset + 2] = vertex.z();
    }

}
