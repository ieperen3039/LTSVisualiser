package NG.Rendering.Shaders;

import NG.Core.Main;
import NG.Rendering.GLFWWindow;
import NG.Tools.Toolbox;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_DEPTH_COMPONENT16;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author Geert van Ieperen created on 7-1-2019.
 */
@SuppressWarnings("Duplicates")
public class ClickShader {
    private final int frameBuffer;
    private final int colorBuffer;
    private final int depthBuffer;

    private int windowWidth = 0;
    private int windowHeight = 0;

    private Vector2i mousePosition;

    public ClickShader() {
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        // color buffer to write to
        colorBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorBuffer);

        // depth buffer to use for depth testing
        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        Toolbox.checkGLError(this.toString());

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void init(Main game) {
        // TODO only render where the mouse is

        GLFWWindow window = game.window();
        mousePosition = window.getMousePosition();

        // if the screen size changed, resize buffers to match the new dimensions
        int newWidth = window.getWidth();
        int newHeight = window.getHeight();
        if (newWidth != windowWidth || newHeight != windowHeight) {
            windowWidth = newWidth;
            windowHeight = newHeight;
            glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, newWidth, newHeight);
            glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, newWidth, newHeight);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new ShaderException("ClickShader could not init FrameBuffer : error " + Toolbox.asHex(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glClearColor(0f, 0f, 0f, 0f); // black
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        Toolbox.checkGLError(this.toString());
    }

    public int getValue() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        ByteBuffer buffer = BufferUtils.createByteBuffer(3); // one for each color
        glReadPixels(mousePosition.x, windowHeight - mousePosition.y, 1, 1, GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = Byte.toUnsignedInt(buffer.get(0));
        int g = Byte.toUnsignedInt(buffer.get(1));
        int b = Byte.toUnsignedInt(buffer.get(2));
        assert !(r < 0 || g < 0 || b < 0) : String.format("got (%d, %d, %d)", r, g, b);
        buffer.clear();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // convert to local
        return colorToNumber(new Vector3i(r, g, b));
    }

    public void cleanup() {
        glDeleteFramebuffers(frameBuffer);
        glDeleteRenderbuffers(colorBuffer);
        glDeleteRenderbuffers(depthBuffer);
    }

    private static Vector3i numberToColor(int i) {
        assert i < (1 << 18);
        final int bitSize = (1 << 6);
        int r = (i % bitSize) << 2;
        int g = (((i >> 6) % bitSize) << 2);
        int b = (((i >> 12) % bitSize) << 2);

        return new Vector3i(r, g, b);
    }

    private static int colorToNumber(Vector3i value) {
        int i = 0;
        i += nearest(value.x) >> 2;
        i += nearest(value.y) << 4;
        i += nearest(value.z) << 10;

//        Logger.DEBUG.printf("%s -> %d", Vectors.toString(value), i);
        return i;
    }

    /**
     * if the number is not divisible by 4, move the number up or down such that it is
     * @param i a number
     * @return the closest value divisible by 4
     */
    private static int nearest(int i) {
        int mod = i % 4;
        if (mod == 1) {
            i -= 1;
        } else if (mod == 3) {
            i += 1;
        } else if (mod == 2) {
            i -= 2;
        }
        return i;
    }
}
