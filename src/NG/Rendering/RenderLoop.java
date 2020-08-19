package NG.Rendering;

import NG.Core.AbstractGameLoop;
import NG.Core.Root;
import NG.Core.ToolElement;
import NG.GUIMenu.Rendering.NVGOverlay;
import NG.Rendering.MatrixStack.SGL;
import NG.Rendering.Shaders.ShaderProgram;
import NG.Settings.Settings;
import NG.Tools.Logger;
import NG.Tools.TimeObserver;
import NG.Tools.Toolbox;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Repeatedly renders a frame of the main camera of the game given by {@link #init(Root)}
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class RenderLoop extends AbstractGameLoop implements ToolElement {
    public final TimeObserver timer;
    private final NVGOverlay overlay;
    public boolean accurateTiming = false;
    private Root root;
    private Map<ShaderProgram, RenderBundle> renders;

    /**
     * creates a new, paused gameloop
     * @param targetFPS the target frames per second
     */
    public RenderLoop(int targetFPS) {
        super("Renderloop", targetFPS);
        overlay = new NVGOverlay();
        renders = new HashMap<>();

        timer = new TimeObserver((targetFPS / 4) + 1, true);
    }

    public void init(Root root) throws IOException {
        if (this.root != null) return;
        this.root = root;

        Settings settings = root.settings();
        accurateTiming = settings.DEBUG;

        overlay.init(settings.ANTIALIAS_LEVEL);
        overlay.addHudItem((hud) -> {
            if (root.settings().DEBUG) {
                Logger.putOnlinePrint(hud::printRoll);
            }
        });
    }

    /**
     * generates a new render bundle, which allows adding rendering actions which are executed in order on the given
     * shader. There is no guarantee on execution order between shaders
     * @param shader the shader used, or null to use a basic Phong shading
     * @return a bundle that allows adding rendering options.
     */
    public RenderBundle renderSequence(ShaderProgram shader) {
        return renders.computeIfAbsent(shader, RenderBundle::new);
    }

    @Override
    protected void update(float deltaTime) {
        Toolbox.checkGLError("Pre-loop");
        timer.startNewLoop();

        GLFWWindow window = root.window();
        if (window.getWidth() == 0 || window.getHeight() == 0) return;
// camera
        root.camera().updatePosition(deltaTime); // real-time deltatime

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glEnable(GL_LINE_SMOOTH);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        Toolbox.checkGLError(window.toString());

        for (RenderBundle renderBundle : renders.values()) {
            String identifier = renderBundle.shader.getClass().getSimpleName();
            if (accurateTiming) timer.startTiming(identifier);

            renderBundle.draw();

            if (accurateTiming) {
                glFinish();
                timer.endTiming(identifier);
            }
            Toolbox.checkGLError(identifier);
        }

        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        if (accurateTiming) timer.startTiming("GUI");
        root.settings();
        overlay.draw(windowWidth, windowHeight, 10, Settings.TOOL_BAR_HEIGHT + 10, 12);

        if (accurateTiming) {
            glFinish();
            timer.endTiming("GUI");
        }
        Toolbox.checkGLError(overlay.toString());

        timer.startTiming("GPU Update");
        // update window
        window.update();
        timer.endTiming("GPU Update");

        // loop clean
        Toolbox.checkGLError("Render loop");
        if (window.shouldClose()) stopLoop();

        timer.startTiming("Loop Overhead");
    }

    public void addHudItem(Consumer<NVGOverlay.Painter> draw) {
        overlay.addHudItem(draw);
    }

    @Override
    public void cleanup() {
        overlay.cleanup();
    }

    public class RenderBundle {
        private final ShaderProgram shader;
        private final List<BiConsumer<SGL, Root>> targets;

        public RenderBundle(ShaderProgram shader) {
            this.shader = shader;
            this.targets = new ArrayList<>();
        }

        /**
         * appends the given consumer to the end of the render sequence
         * @return this
         */
        public RenderBundle add(BiConsumer<SGL, Root> drawable) {
            targets.add(drawable);
            return this;
        }

        /**
         * executes the given drawables in order
         */
        public void draw() {
            shader.bind();
            {
                shader.initialize(root);

                // GL object
                SGL gl = shader.getGL(root);

                for (BiConsumer<SGL, Root> tgt : targets) {
                    tgt.accept(gl, root);

                    assert gl.getPosition(new Vector3f(1, 1, 1))
                            .equals(new Vector3f(1, 1, 1)) : "GL object has not been properly restored";
                }
            }
            shader.unbind();
        }
    }
}
