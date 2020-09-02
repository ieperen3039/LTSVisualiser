package NG.Rendering.Shaders;

import NG.Core.Main;
import NG.Rendering.MeshLoading.Mesh;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Scanner;

import static org.lwjgl.opengl.GL20.*;

/**
 * @author Geert van Ieperen created on 7-1-2018.
 */
public interface ShaderProgram {

    /** shaders and meshes must use these shader locations */
    int VERTEX_LOCATION = 0;
    int NORMAL_LOCATION = 1;
    int COLOR_LOCATION = 2;
    int TEXTURE_LOCATION = 3;

    /**
     * Bind the renderer to the current rendering state
     */
    void bind();

    /**
     * Unbind the renderer from the current rendering state
     */
    void unbind();

    /**
     * Cleanup the renderer to a state of disposal
     */
    void cleanup();

    /**
     * initialize the uniforms for this shader. Must be called before rendering.
     * @param root the source of information
     */
    void initialize(Main root);

    SGL getGL(Main root);

    void setClickShading(boolean setTrue);

    class BaseSGL implements SGL {
        private static final Painter LOCK = new Painter();
        private final ShaderProgram shader;

        public BaseSGL(ShaderProgram shader) {
            this.shader = shader;
        }

        @Override
        public void render(Mesh object) {
            object.render(LOCK);
        }

        @Override
        public ShaderProgram getShader() {
            return shader;
        }
    }

    /**
     * Create a new shader and return the id of the newly created shader.
     * @param programId
     * @param shaderType The type of shader, e.g. GL_VERTEX_SHADER.
     * @param shaderCode The shaderCode as a String.
     * @return The id of the newly created shader.
     * @throws ShaderException If an error occurs during the creation of a shader.
     */
    static int createShader(int programId, int shaderType, String shaderCode) throws ShaderException {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new ShaderException("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new ShaderException("Error compiling Shader code:\n" + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    /**
     * loads a textfile and returns the text as a string
     * @param path a path to an UTF-8 text file.
     * @return a string representation of the requested resource
     * @throws IOException if the path does not point to a valid, readable file
     */
    static String loadText(Path path) throws IOException {
        String result;
        try (
                InputStream in = new FileInputStream(path.toFile());
                Scanner scanner = new Scanner(in, "UTF8")
        ) {
            result = scanner.useDelimiter("\\A").next();

        } catch (FileNotFoundException e) {
            throw new IOException("Resource not found: " + path);
        }
        return result;
    }
}
