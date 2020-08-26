package NG.Graph.Rendering;

import NG.DataStructures.Generic.Color4f;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.MeshLoading.Mesh;
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
public class EdgeMesh implements Mesh {
    public static final Color4f EDGE_BASE_COLOR = new Color4f(0, 0, 0, 0.5f);
    private final List<Edge> bulk = new ArrayList<>();
    private int vaoId = -1;
    private int aPositionVBO;
    private int handlePositionVBO;
    private int bPositionVBO;
    private int colorVBO;
    private boolean isLoaded = false;
    private int nrOfParticles = 0;

    public void addParticle(NodeMesh.Node a, NodeMesh.Node b, String label){
        addParticle(new Edge(a, b, label));
    }

    public void addParticle(Edge p) {
        bulk.add(p);
    }

    public void writeToGL() {
        nrOfParticles = bulk.size();

        FloatBuffer aPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer handleBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer bPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

        put(bulk, aPosBuffer, handleBuffer, bPosBuffer, colorBuffer);

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            aPositionVBO = loadToGL(aPosBuffer, 0, 3, GL_STREAM_DRAW); // position of start side of edge
            handlePositionVBO = loadToGL(handleBuffer, 1, 3, GL_STREAM_DRAW); // position of handle of edge
            bPositionVBO = loadToGL(bPosBuffer, 2, 3, GL_STREAM_DRAW); // position of end side of edge
            colorVBO = loadToGL(colorBuffer, 3, 4, GL_DYNAMIC_DRAW); // color of edge

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

    public List<Edge> edgeList() {
        return bulk;
    }

    public void reload(){
        if (isLoaded) {
            FloatBuffer aPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
            FloatBuffer handlePosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
            FloatBuffer bPosBuffer = MemoryUtil.memAllocFloat(3 * nrOfParticles);
            FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(4 * nrOfParticles);

            try {
                put(bulk, aPosBuffer, handlePosBuffer, bPosBuffer, colorBuffer);

                aPosBuffer.rewind();
                handlePosBuffer.rewind();

                glBindBuffer(GL_ARRAY_BUFFER, aPositionVBO);
                glBufferData(GL_ARRAY_BUFFER, aPosBuffer, GL_STREAM_DRAW);

                glBindBuffer(GL_ARRAY_BUFFER, handlePositionVBO);
                glBufferData(GL_ARRAY_BUFFER, handlePosBuffer, GL_STREAM_DRAW);

                glBindBuffer(GL_ARRAY_BUFFER, bPositionVBO);
                glBufferData(GL_ARRAY_BUFFER, bPosBuffer, GL_STREAM_DRAW);

                glBindBuffer(GL_ARRAY_BUFFER, colorVBO);
                glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);

                glBindBuffer(GL_ARRAY_BUFFER, 0);

            } finally {
                MemoryUtil.memFree(aPosBuffer);
                MemoryUtil.memFree(handlePosBuffer);
                MemoryUtil.memFree(bPosBuffer);
                MemoryUtil.memFree(colorBuffer);
            }

        }
    }

    /**
     * renders all particles. The particle-shader must be linked first, and writeToGl must be called
     */
    @Override
    public void render(SGL.Painter lock) {
        if (!isLoaded) writeToGL();

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

    private static void put(
            List<Edge> bulk, FloatBuffer aPosBuffer, FloatBuffer handleBuffer, FloatBuffer bPosBuffer,
            FloatBuffer colorBuffer
    ) {
        for (int i = 0; i < bulk.size(); i++) {
            Edge p = bulk.get(i);

            p.aPosition.get(i * 3, aPosBuffer);
            p.handle.get(i * 3, handleBuffer);
            p.bPosition.get(i * 3, bPosBuffer);
            p.color.put(colorBuffer);
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

    public static class Edge {
        public final NodeMesh.Node a;
        public final NodeMesh.Node b;
        public final String label;

        public final Vector3fc aPosition;
        public final Vector3fc bPosition;
        public final Vector3f handle;

        public Color4f color = EDGE_BASE_COLOR;

        public Edge(NodeMesh.Node a, NodeMesh.Node b, String label) {
            this.a = a;
            this.b = b;
            this.aPosition = a.position;
            this.bPosition = b.position;
            this.handle = new Vector3f();
            this.label = label;
        }
    }
}
