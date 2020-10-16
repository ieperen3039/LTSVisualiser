package NG.Graph.Rendering;

import NG.Graph.State;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.SGL;
import NG.Tools.Toolbox;
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
    private int borderVboID;

    private boolean isLoaded = false;
    private final List<State> bulk = new ArrayList<>();
    private int nrOfParticles = 0;
    private boolean doReload = false;

    public void addNode(State p) {
        bulk.add(p);
    }

    public void writeToGL() {
        nrOfParticles = bulk.size();

        FloatBuffer positionBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);
        FloatBuffer borderBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);
        put(bulk, positionBuffer, colorBuffer, borderBuffer);

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            posMidVboID = loadToGL(positionBuffer, 0, 3, GL_STREAM_DRAW);
            colorVboID = loadToGL(colorBuffer, 1, 4, GL_STREAM_DRAW);
            borderVboID = loadToGL(borderBuffer, 2, 4, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            isLoaded = true;

        } finally {
            MemoryUtil.memFree(positionBuffer);
            MemoryUtil.memFree(colorBuffer);
            MemoryUtil.memFree(borderBuffer);
        }

        Toolbox.checkGLError(toString());
    }

    private void reload() {
        FloatBuffer positionBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);
        FloatBuffer borderBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

        put(bulk, positionBuffer, colorBuffer, borderBuffer);

        try {
            glBindBuffer(GL_ARRAY_BUFFER, posMidVboID);
            glBufferData(GL_ARRAY_BUFFER, positionBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, colorVboID);
            glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, borderVboID);
            glBufferData(GL_ARRAY_BUFFER, borderBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);

        } finally {
            MemoryUtil.memFree(positionBuffer);
            MemoryUtil.memFree(colorBuffer);
            MemoryUtil.memFree(borderBuffer);
        }
    }

    public void scheduleReload() {
        doReload = true;
    }

    public List<State> nodeList() {
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
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glDrawArrays(GL_POINTS, 0, nrOfParticles);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void dispose() {
        if (!isLoaded) return;
        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(new int[]{posMidVboID, colorVboID, borderVboID});

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        vaoId = 0;
        Toolbox.checkGLError(toString());
    }

    private static void put(
            List<State> bulk, FloatBuffer positionBuffer, FloatBuffer colorBuffer, FloatBuffer borderBuffer
    ) {
        for (int i = 0; i < bulk.size(); i++) {
            State p = bulk.get(i);

            p.position.get(i * 3, positionBuffer);
            p.getColor().put(colorBuffer);
            p.border.put(borderBuffer);
        }
        colorBuffer.flip();
        borderBuffer.flip();
    }

    private static int loadToGL(FloatBuffer buffer, int index, int itemSize, int usage) {
        buffer.rewind();
        int vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        glVertexAttribPointer(index, itemSize, GL_FLOAT, false, 0, 0);
        return vboID;
    }

}
