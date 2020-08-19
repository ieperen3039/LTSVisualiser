package NG.Rendering.MeshLoading;

import NG.Rendering.Shaders.ShaderProgram;
import NG.Tools.Toolbox;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * A mesh with indexing, where the entire surface is smoothed. Allows textures but no colors.
 * @author Geert van Ieperen created on 1-2-2019.
 */
public class SmoothMesh extends AbstractMesh {

    public SmoothMesh(
            List<Vector3fc> vertices, List<Vector3fc> normals, List<Vector2fc> textureCoords, List<Face> faces
    ) {
        int nrOfVertices = vertices.size();

        // prepare empty normals
        List<Vector3f> secNormals = new ArrayList<>(nrOfVertices);
        for (int i = 0; i < nrOfVertices; i++) {
            secNormals.add(new Vector3f());
        }

        // prepare mapping attributes to integers
        HashMap<Attribute, Integer> attributes = new HashMap<>(nrOfVertices);
        int attributeSize = 0;

        // combination of triplets of face indices
        int[] indices = new int[faces.size() * 3];
        int faceAttributeIndex = 0;

        // collect all non-overlapping texture coordinates
        // average all normals
        for (Face face : faces) {
            assert face.size() == 3;

            for (int i = 0; i < 3; i++) {
                int posInd = face.vert[i];
                int texInd = face.tex[i];
                int normInd = face.norm[i];

                Vector3fc vertex = vertices.get(posInd);
                Vector3fc normal = normals.get(normInd);
                Vector2fc coord = textureCoords.get(texInd);

                // the MeshFile allows multiple normals per position vertex
                // we take the average normal of all normals of any one vertex
                Vector3f accNormal = secNormals.get(posInd).add(normal);

                Attribute att = new Attribute(vertex, accNormal, coord);
                Integer index = attributes.get(att);
                if (index == null) {
                    index = attributeSize++;
                    attributes.put(att, index);
                }

                indices[faceAttributeIndex++] = index;
            }
        }

        // flatten entries
        int nrOfAttributes = attributes.size();
        Vector3fc[] combinedVertices = new Vector3fc[nrOfAttributes];
        Vector3fc[] combinedNormals = new Vector3fc[nrOfAttributes];
        Vector2fc[] combinedTexCoords = new Vector2fc[nrOfAttributes];

        attributes.forEach((v, i) -> {
            combinedVertices[i] = v.vertex;
            combinedNormals[i] = v.normal.normalize();
            combinedTexCoords[i] = v.texCoord;
        });

        writeToGl(indices, combinedVertices, combinedNormals, combinedTexCoords);
    }

    private void writeToGl(
            int[] indices, Vector3fc[] combinedVertices, Vector3fc[] combinedNormals, Vector2fc[] combinedTexCoords
    ) {
        // flatten objects
        int nrOfElements = combinedVertices.length;
        float[] positions = new float[nrOfElements * 3];
        float[] normals = new float[nrOfElements * 3];
        float[] texCoords = new float[nrOfElements * 2];

        for (int i = 0; i < nrOfElements; i++) {
            // positions
            int vi = i * 3;
            Vector3fc vec = combinedVertices[i];
            positions[vi] = vec.x();
            positions[vi + 1] = vec.y();
            positions[vi + 2] = vec.z();
            // normals
            Vector3fc vec2 = combinedNormals[i];
            normals[vi] = vec2.x();
            normals[vi + 1] = vec2.y();
            normals[vi + 2] = vec2.z();
            // texture coordinates
            int ti = i * 2;
            Vector2fc coord = combinedTexCoords[i];
            texCoords[ti] = coord.x();
            texCoords[ti + 1] = coord.y();
        }

        initMesh();

        glBindVertexArray(getVAO());

        createIndexBuffer(indices);
        createVBO(positions, ShaderProgram.VERTEX_LOCATION, 3);
        createVBO(normals, ShaderProgram.NORMAL_LOCATION, 3);
        createVBO(texCoords, ShaderProgram.TEXTURE_LOCATION, 2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        Toolbox.checkGLError(toString());
    }

    private static class Attribute {
        Vector3fc vertex;
        Vector3f normal;
        Vector2fc texCoord;

        Attribute(Vector3fc vertex, Vector3f normal, Vector2fc texCoord) {
            this.vertex = vertex;
            this.normal = normal;
            this.texCoord = texCoord;
        }

        // this defines the uniqueness between attributes
        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != getClass()) return false;
            Attribute other = (Attribute) obj;

            return (vertex.equals(other.vertex) && texCoord.equals(other.texCoord));
        }

        @Override
        public int hashCode() {
            return vertex.hashCode() ^ texCoord.hashCode();
        }
    }
}

/*
 *HashMap<Integer, HashMap<Integer, Integer>> indices = new HashMap<>();
 *
 *        List<Face> facesList = model.faces;
 *        AtomicInteger accumulator = new AtomicInteger();
 *
 *        // collect all non-overlapping texture coordinates
 *        // average all normals
 *        for (Face face : facesList) {
 *            for (int i = 0; i < 3; i++) {
 *                int ind = face.vert[i];
 *                int texInd = face.tex[i];
 *                int normInd = face.norm[i];
 *
 *                List<Vector2fc> known = texCoords.get(ind);
 *                Vector2fc coord = model.textureCoords.get(texInd);
 *
 *                if (!known.contains(coord)) { // vecs is small
 *                    known.add(coord);
 *                }
 *                int newSubCoord = known.indexOf(coord);
 *
 *                // add all normal vectors of a given position vector
 *                Vector3fc normal = model.normals.get(normInd);
 *                normals.get(ind).add(normal);
 *
 *                // compute new index
 *                Map<Integer, Integer> vCollection = indices.computeIfAbsent(ind, (k) -> new HashMap<>());
 *                vCollection.computeIfAbsent(newSubCoord, (k) -> accumulator.getAndIncrement());
 *            }
 *        }
 *
 *    // flatten entries
 *    List<Vector3fc> combinedVertices = new ArrayList<>(nrOfVertices);
 *    List<Vector3fc> combinedNormals = new ArrayList<>(nrOfVertices);
 *    List<Vector2fc> combinedTexCoords = new ArrayList<>(nrOfVertices);
 *
 *        for (int vexInd = 0; vexInd < texCoords.size(); vexInd++) {
 *        Collection<Vector2fc> container = texCoords.get(vexInd);
 *        Vector3f normal = normals.get(vexInd);
 *        Vector3fc vertex = model.vertices.get(vexInd);
 *
 *        for (Vector2fc texCoord : container) {
 *        combinedVertices.add(vertex);
 *        combinedTexCoords.add(texCoord);
 *        combinedNormals.add(normal.normalize());
 *        }
 *        }
 *
 */
