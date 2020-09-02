package NG.Rendering.Shaders;

import NG.Rendering.MeshLoading.Mesh;

/**
 * This resembles the {@link org.lwjgl.opengl.GL} object.
 * @author Geert van Ieperen created on 15-11-2017.
 */
public interface SGL {

    /**
     * instructs the graphical card to render the specified mesh
     * @param object A Mesh that has not been disposed.
     */
    void render(Mesh object);

    /** @return the shader that is used for rendering. */
    ShaderProgram getShader();

    /**
     * Objects should use GPU calls only in their render method. To prevent invalid uses of the {@link
     * Mesh#render(Painter)} object, a Painter object is required to call that render method.
     */
    class Painter {
        /**
         * Objects should call GPU calls only in their render method. This render method may only be called by a GL2
         * object, to prevent drawing calls while the GPU is not initialized. For this reason, the Painter constructor
         * is protected.
         */
        protected Painter() {
        }
    }

}
