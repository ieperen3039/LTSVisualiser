package NG.Graph.Rendering;

import NG.Camera.Camera;
import NG.Core.Root;
import NG.Rendering.GLFWWindow;
import NG.Rendering.MatrixStack.AbstractSGL;
import NG.Rendering.MeshLoading.Mesh;
import NG.Rendering.Shaders.ShaderException;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Tools.Directory;
import NG.Tools.Logger;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Path;

import static NG.Graph.Rendering.NodeShader.NODE_RADIUS;
import static NG.Rendering.Shaders.ShaderProgram.createShader;
import static NG.Rendering.Shaders.ShaderProgram.loadText;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * @author Geert van Ieperen created on 17-5-2018.
 */
public class EdgeShader implements ShaderProgram {
    private static final Path VERTEX_PATH = Directory.shaders.getPath("edges", "vertex.vert");
    private static final Path FRAGMENT_PATH = Directory.shaders.getPath("edges", "fragment.frag");
    private static final Path GEOMETRY_PATH = Directory.shaders.getPath("edges", "geometry.glsl");
    private static final float EDGE_SIZE = NODE_RADIUS * 1.5f;
    private static final float HEAD_SIZE = 0; //NODE_RADIUS * 2.0f

    private final int programID;
    private final int vertexShaderID;
    private final int fragmentShaderID;
    private final int geometryShaderID;

    private final int viewMatrixUID;
    private final int projectionMatrixUID;
    private final int radiusUID;
    private final int edgeSizeUID;
    private final int headSizeUID;

    public EdgeShader() throws IOException {
        programID = glCreateProgram();

        final String vertexCode = loadText(VERTEX_PATH);
        vertexShaderID = createShader(programID, GL_VERTEX_SHADER, vertexCode);

        final String fragmentCode = loadText(FRAGMENT_PATH);
        fragmentShaderID = createShader(programID, GL_FRAGMENT_SHADER, fragmentCode);

        final String geometryCode = loadText(GEOMETRY_PATH);
        geometryShaderID = createShader(programID, GL_GEOMETRY_SHADER, geometryCode);

        link();

        viewMatrixUID = glGetUniformLocation(programID, "viewMatrix");
        projectionMatrixUID = glGetUniformLocation(programID, "projectionMatrix");
        radiusUID = glGetUniformLocation(programID, "nodeRadius");
        edgeSizeUID = glGetUniformLocation(programID, "arrowSize");
        headSizeUID = glGetUniformLocation(programID, "headSize");
    }

    @Override
    public void initialize(Root root) {
        GLFWWindow window = root.window();
        float ratio = (float) window.getWidth() / window.getHeight();
        Camera camera = root.camera();

        Matrix4f projection = camera.getProjectionMatrix(ratio);
        writeMatrix(projection, projectionMatrixUID);

        // set the view
        Matrix4f view = new Matrix4f().setLookAt(
                camera.getEye(),
                camera.getFocus(),
                camera.getUpVector()
        );
        writeMatrix(view, viewMatrixUID);

        glUniform1f(radiusUID, NODE_RADIUS);
        glUniform1f(edgeSizeUID, EDGE_SIZE);
        glUniform1f(headSizeUID, HEAD_SIZE);
    }

    private void writeMatrix(Matrix4f view, int transformUID) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer buffer = stack.mallocFloat(16);
            view.get(buffer);
            glUniformMatrix4fv(transformUID, false, buffer);
        }
    }

    public void bind() {
        glUseProgram(programID);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programID != 0) {
            glDeleteProgram(programID);
        }
    }

    private void link() throws ShaderException {
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programID, 1024));
        }

        glDetachShader(programID, vertexShaderID);
        glDetachShader(programID, geometryShaderID);
        glDetachShader(programID, fragmentShaderID);

        glValidateProgram(programID);
        if (glGetProgrami(programID, GL_VALIDATE_STATUS) == 0) {
            Logger.ERROR.print("Warning validating Shader code: " + glGetProgramInfoLog(programID, 1024));
        }
    }

    public ParticleGL getGL(Root root) {
        return new ParticleGL();
    }

    public class ParticleGL extends AbstractSGL {
        public ParticleGL() {}

        @Override
        public void render(Mesh object) {
            object.render(LOCK);
        }

        @Override
        public ShaderProgram getShader() {
            return EdgeShader.this;
        }
    }
}
