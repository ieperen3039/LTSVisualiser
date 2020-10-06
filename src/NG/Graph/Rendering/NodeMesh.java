package NG.Graph.Rendering;

import NG.DataStructures.Generic.Color4f;
import NG.Graph.GraphElement;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.SGL;
import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * a group of particles, each rendered around the same time
 * @author Geert van Ieperen created on 16-5-2018.
 */
public class NodeMesh implements Mesh {
    private int vaoId = -1;
    private int posMidVboID;
    private int colorVboID;

    private boolean isLoaded = false;
    private final List<Node> bulk = new ArrayList<>();
    private int nrOfParticles = 0;
    private boolean doReload = false;

    /**
     * @param position   position of the middle of the particle
     * @param label
     * @param classIndex
     */
    public void addParticle(Vector3fc position, String label, int classIndex) {
        addParticle(new Node(position, label, classIndex));
    }

    public void addParticle(Node p) {
        bulk.add(p);
    }

    public void writeToGL() {
        nrOfParticles = bulk.size();

        FloatBuffer positionBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);
        put(bulk, positionBuffer, colorBuffer);

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            posMidVboID = loadToGL(positionBuffer, 0, 3, GL_STREAM_DRAW);
            colorVboID = loadToGL(colorBuffer, 1, 4, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            isLoaded = true;

        } finally {
            MemoryUtil.memFree(positionBuffer);
            MemoryUtil.memFree(colorBuffer);
        }

        Toolbox.checkGLError(toString());
    }

    private void reload() {
        FloatBuffer positionBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

        put(bulk, positionBuffer, colorBuffer);

        try {
            glBindBuffer(GL_ARRAY_BUFFER, posMidVboID);
            glBufferData(GL_ARRAY_BUFFER, positionBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, colorVboID);
            glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);

        } finally {
            MemoryUtil.memFree(positionBuffer);
            MemoryUtil.memFree(colorBuffer);
        }
    }

    public void scheduleReload() {
        doReload = true;
    }

    public List<Node> nodeList() {
        return bulk;
    }

    /**
     * renders all particles. The particle-shader must be linked first, and writeToGl must be called
     */
    @Override
    public void render(SGL.Painter lock) {
        if (!isLoaded) {
            writeToGL();
        } else if (doReload) reload();

        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0); // Position of triangle middle VBO
        glEnableVertexAttribArray(1); // Position of triangle middle VBO

        glDrawArrays(GL_POINTS, 0, nrOfParticles);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void dispose() {
        if (!isLoaded) return;
        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(new int[]{posMidVboID, colorVboID});

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        vaoId = 0;
        Toolbox.checkGLError(toString());
    }

    private static void put(List<Node> bulk, FloatBuffer positionBuffer, FloatBuffer colorBuffer) {
        for (int i = 0; i < bulk.size(); i++) {
            Node p = bulk.get(i);

            p.position.get(i * 3, positionBuffer);
            p.getColor().put(colorBuffer);
        }
        colorBuffer.flip();
    }

    private static int loadToGL(FloatBuffer buffer, int index, int itemSize, int usage) {
        buffer.rewind();
        int vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        glVertexAttribPointer(index, itemSize, GL_FLOAT, false, 0, 0);
        return vboID;
    }

    public static class Node extends GraphElement {
        public static final Color4f BASE_COLOR = Color4f.WHITE;

        public final Vector3f position;
        public String label; // the node label, if any
        public int classIndex;

        public boolean isFixed = false;
        public boolean stayFixed = false;

        public Node(Vector3fc position, String label, int classIndex) {
            this.position = new Vector3f(position);
            this.label = label;
            this.classIndex = classIndex;
            colors.add(GraphElement.Priority.BASE, BASE_COLOR);
        }

        public Node(Node other, String newLabel) {
            this.position = other.position;
            this.label = newLabel;
            this.classIndex = other.classIndex;
            colors.add(GraphElement.Priority.BASE, BASE_COLOR);
        }

        @Override
        public String toString() {
            return "Node " + label;
        }
    }
}
