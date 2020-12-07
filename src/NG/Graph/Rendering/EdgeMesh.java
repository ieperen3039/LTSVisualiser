package NG.Graph.Rendering;

import NG.DataStructures.Generic.Color4f;
import NG.Graph.State;
import NG.Graph.Transition;
import NG.Rendering.Mesh;
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
public class EdgeMesh implements Mesh {
    public static final Color4f BASE_COLOR = new Color4f(0, 0, 0, 0.5f);
    private final List<Transition> bulk = new ArrayList<>();
    private int vaoId = -1;
    private int aPositionVBO;
    private int handlePositionVBO;
    private int bPositionVBO;
    private int colorVBO;
    private boolean isLoaded = false;
    private int nrOfParticles = 0;
    private boolean doPositionReload = false;
    private boolean doColorReload = false;

    public void addParticle(State a, State b, String label) {
        addParticle(new Transition(a, b, label));
    }

    public void addParticle(Transition p) {
        bulk.add(p);
    }

    public void writeToGL() {
        nrOfParticles = bulk.size();

        FloatBuffer aPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer handleBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer bPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

        for (int i = 0; i < bulk.size(); i++) {
            Transition p = bulk.get(i);

            p.fromPosition.get(i * 3, aPosBuffer);
            p.handlePos.get(i * 3, handleBuffer);
            p.toPosition.get(i * 3, bPosBuffer);
            p.getColor().put(colorBuffer);
        }

        colorBuffer.flip();

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            aPositionVBO = loadToGL(aPosBuffer, 0, 3, GL_STREAM_DRAW); // position of start side of edge
            handlePositionVBO = loadToGL(handleBuffer, 1, 3, GL_STREAM_DRAW); // position of handle of edge
            bPositionVBO = loadToGL(bPosBuffer, 2, 3, GL_STREAM_DRAW); // position of end side of edge
            colorVBO = loadToGL(colorBuffer, 3, 4, GL_STREAM_DRAW); // color of edge

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            isLoaded = true;

        } finally {
            MemoryUtil.memFree(aPosBuffer);
            MemoryUtil.memFree(handleBuffer);
            MemoryUtil.memFree(bPosBuffer);
            MemoryUtil.memFree(colorBuffer);
        }

        Toolbox.checkGLError(toString());
    }

    private void reloadPositions() {
        FloatBuffer aPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer handlePosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer bPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);

        try {
            for (int i = 0; i < bulk.size(); i++) {
                Transition p = bulk.get(i);

                p.fromPosition.get(i * 3, aPosBuffer);
                p.handlePos.get(i * 3, handlePosBuffer);
                p.toPosition.get(i * 3, bPosBuffer);
            }

            aPosBuffer.rewind();
            handlePosBuffer.rewind();

            glBindBuffer(GL_ARRAY_BUFFER, aPositionVBO);
            glBufferData(GL_ARRAY_BUFFER, aPosBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, handlePositionVBO);
            glBufferData(GL_ARRAY_BUFFER, handlePosBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, bPositionVBO);
            glBufferData(GL_ARRAY_BUFFER, bPosBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);

        } finally {
            MemoryUtil.memFree(aPosBuffer);
            MemoryUtil.memFree(handlePosBuffer);
            MemoryUtil.memFree(bPosBuffer);
        }
    }

    private void reloadColors() {
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

        try {
            for (Transition p : bulk) {
                p.getColor().put(colorBuffer);
            }

            colorBuffer.flip();

            glBindBuffer(GL_ARRAY_BUFFER, colorVBO);
            glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STREAM_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);

        } finally {
            MemoryUtil.memFree(colorBuffer);
        }
    }

    public void schedulePositionReload() {
        doPositionReload = true;
    }

    public void scheduleColorReload() {
        doColorReload = true;
    }

    public List<Transition> edgeList() {
        return bulk;
    }

    /**
     * renders all particles. The particle-shader must be linked first, and writeToGl must be called
     */
    @Override
    public void render(SGL.Painter lock) {
        if (!isLoaded) {
            writeToGL();

        } else {
            if (doPositionReload) {
                reloadPositions();
                doPositionReload = false;
            }
            if (doColorReload) {
                reloadColors();
                doColorReload = false;
            }
        }

        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0); // a pos
        glEnableVertexAttribArray(1); // h pos
        glEnableVertexAttribArray(2); // b pos
        glEnableVertexAttribArray(3); // color

        glDrawArrays(GL_POINTS, 0, nrOfParticles);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glBindVertexArray(0);
    }

    public void dispose() {
        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(new int[]{aPositionVBO, handlePositionVBO, bPositionVBO, colorVBO});

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        vaoId = 0;
        Toolbox.checkGLError(toString());
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
