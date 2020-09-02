package NG.GUIMenu.Rendering;

import NG.DataStructures.Generic.Color4f;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Jorren & Geert van Ieperen
 */
public final class NVGOverlay {
    @SuppressWarnings("FieldCanBeLocal") /* fontbuffer MUST be a field */
    private final ByteBuffer[] fontBuffer = new ByteBuffer[NGFonts.values().length];
    private final Collection<Consumer<Painter>> drawBuffer = new ArrayList<>();
    private final Lock drawBufferLock = new ReentrantLock();
    private long vg;
    private NVGColor nvgColorBuffer;
    private NVGPaint paint;
    private Map<Path, Integer> imageBuffer = new HashMap<>();

    public enum Alignment {
        ALIGN_LEFT, ALIGN_RIGHT, ALIGN_TOP, ALIGN_BOTTOM
    }

    /**
     * @param antialiasLevel
     * @throws IOException If an error occurs during the setup of the Hud.
     */
    public void init(int antialiasLevel) throws IOException {
        if (antialiasLevel > 0) {
            vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        } else {
            vg = nvgCreate(NVG_STENCIL_STROKES);
        }

        if (vg == NULL) {
            throw new IOException("Could not initialize NanoVG");
        }

        NGFonts[] fonts = NGFonts.values();
        for (int i = 0; i < fonts.length; i++) {
            fontBuffer[i] = fonts[i].asByteBuffer();
            if (nvgCreateFontMem(vg, fonts[i].name, fontBuffer[i], 1) == -1) {
                Logger.ERROR.print("Could not create font " + fonts[i].name);
            }
        }

        nvgColorBuffer = NVGColor.create();
        paint = NVGPaint.create();
    }

    public void cleanup() {
        drawBufferLock.lock();
        try {
            drawBuffer.clear();
        } finally {
            drawBufferLock.unlock();
        }
    }

    public void addHudItem(Consumer<Painter> render) {
        if (render == null) return;

        drawBufferLock.lock();
        try {
            drawBuffer.add(render);
        } finally {
            drawBufferLock.unlock();
        }
    }

    public void removeHudItem(Consumer<Painter> render) {
        if (render == null) return;

        drawBufferLock.lock();
        try {
            drawBuffer.remove(render);
        } finally {
            drawBufferLock.unlock();
        }
    }

    /**
     * @param windowWidth    width of the current window drawn on
     * @param windowHeight   height of the current window
     * @param cameraPosition position of camera
     */
    public void draw(int windowWidth, int windowHeight, Vector3f cameraPosition) {
        Painter vanGogh = new Painter(windowWidth, windowHeight, cameraPosition, 5, 5, 24);
        draw(windowWidth, windowHeight, vanGogh);
    }

    /**
     * @param windowWidth  width of the current window drawn on
     * @param windowHeight height of the current window
     * @param xRoll        x position of the debug printroll screen
     * @param yRoll        y position of ''
     * @param rollSize     font size of ''
     */
    public void draw(int windowWidth, int windowHeight, int xRoll, int yRoll, int rollSize) {
        Painter bobRoss = new Painter(windowWidth, windowHeight, Vectors.Z, xRoll, yRoll, rollSize);
        draw(windowWidth, windowHeight, bobRoss);
    }

    /**
     * draw using the given painter
     */
    private synchronized void draw(int windowWidth, int windowHeight, Painter painter) {
        // this should be the case
        glViewport(0, 0, windowWidth, windowHeight);
        // Begin NanoVG frame
        nvgBeginFrame(vg, windowWidth, windowHeight, 1);

        // Draw the buffer elements
        drawBufferLock.lock();
        try {
            drawBuffer.forEach(m -> m.accept(painter));
        } finally {
            drawBufferLock.unlock();
        }

        // End NanoVG frame
        nvgEndFrame(vg);
    }

    public class Painter {
        public final int windowWidth;
        public final int windowHeight;
        public final Vector3fc cameraPosition;
        private final int printRollSize;
        private final int yPrintRoll;
        private final int xPrintRoll;
        private int printRollEntry = 0;
        private Color4f fillColor = Color4f.WHITE;
        private Color4f strokeColor;
        private int strokeWidth = 1;
        private Color4f textColor = Color4f.BLACK;

        /**
         * @param windowWidth    width of this hud display iteration
         * @param windowHeight   height of ''
         * @param cameraPosition renderposition of camera in worldspace
         * @param xPrintRoll     x position of where to start the printRoll
         * @param yPrintRoll     y position of where to start the printRoll
         * @param printRollSize  fontsize of printRoll
         */
        public Painter(
                int windowWidth, int windowHeight, Vector3fc cameraPosition,
                int xPrintRoll, int yPrintRoll, int printRollSize
        ) {
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
            this.cameraPosition = cameraPosition;
            this.printRollSize = printRollSize;
            this.yPrintRoll = printRollSize + yPrintRoll;
            this.xPrintRoll = xPrintRoll;
        }

        /**
         * Get an instance of NVGColor with the correct values. All nvgColorBuffer values are floating point numbers
         * supposed to be between 0f and 1f.
         * @param red   The red component.
         * @param green The green component.
         * @param blue  The blue component.
         * @param alpha The alpha component.
         * @return an instance of NVGColor.
         */
        private NVGColor toBuffer(float red, float green, float blue, float alpha) {
            nvgColorBuffer.r(red);
            nvgColorBuffer.g(green);
            nvgColorBuffer.b(blue);
            nvgColorBuffer.a(alpha);

            return nvgColorBuffer;
        }

        /** @see #toBuffer(float, float, float, float) */
        private NVGColor toBuffer(Color4f color) {
            return toBuffer(color.red, color.green, color.blue, color.alpha);
        }

        /**
         * sets the basic fill color of this painter to the given color
         * @param color a color, where the alpha value gives the opacity of the object
         */
        public void setFillColor(Color4f color) {
            this.fillColor = color;
            nvgFillColor(vg, toBuffer(color));
        }

        /**
         * sets the basic stroke syle of this painter
         * @param color the color of the stroke, if alpha is less than 1, the edge of the fill underneath will be
         *              visible
         * @param width the width of the stroke in pixels
         */
        public void setStroke(Color4f color, int width) {
            this.strokeWidth = width;
            this.strokeColor = color;
            nvgStrokeWidth(vg, width);
            nvgStrokeColor(vg, toBuffer(color));
        }

        /**
         * draws a rectangle using the basic fill and stroke style
         * @see #rectangle(int, int, int, int, Color4f, Color4f, int)
         */
        public void rectangle(int x, int y, int width, int height) {
            assert width >= 0 : "Negative width: " + width + " (height = " + height + ")";
            assert height >= 0 : "Negative height: " + height + " (width = " + width + ")";

            nvgBeginPath(vg);
            nvgRect(vg, x, y, width, height);

            nvgFill(vg);
            nvgStroke(vg);
        }

        /**
         * draws a rectangle on the given position with the given style. After this method call, the colors are reset to
         * the basic colors
         * @param x           the x position in pixels relative to the leftmost position on the GL frame
         * @param y           the y position in pixels relative to the topmost position on the GL frame
         * @param width       the width of this rectangle in pixels
         * @param height      the height of this rectangle in pixels
         * @param fillColor   the color used for the background of this rectangle
         * @param strokeColor the color used for the line around this rectangle
         * @param strokeWidth the width of the line around this rectangle
         */
        public void rectangle(
                int x, int y, int width, int height, Color4f fillColor, Color4f strokeColor, int strokeWidth
        ) {
            nvgFillColor(vg, toBuffer(fillColor));
            nvgStrokeColor(vg, toBuffer(strokeColor));
            nvgStrokeWidth(vg, strokeWidth);
            rectangle(x, y, width, height);
            restoreColors();
        }

        /**
         * resets the colors to the colors given using {@link #setFillColor(Color4f)} and {@link #setStroke(Color4f,
         * int)}
         */
        private void restoreColors() {
            nvgFillColor(vg, toBuffer(fillColor));
            nvgStrokeWidth(vg, strokeWidth);
            nvgStrokeColor(vg, toBuffer(strokeColor));
        }

        /**
         * draws a circle with the given middle and radius
         * @param x      the x coordinate of the middle of this circle
         * @param y      the y coordinate of the middle of this circle
         * @param radius the radius of this circle
         */
        public void circle(int x, int y, int radius) {
            nvgBeginPath(vg);
            nvgCircle(vg, x, y, radius);

            nvgFill(vg);
            nvgStroke(vg);
        }

        /**
         * Draws a polygon by drawing a line along the given points, connecting the last point with the first.After this
         * method call, the colors are reset to the basic colors
         * @param fillColor   the color used for the background of this polygon
         * @param strokeColor the color used for the line around this polygon
         * @param strokeWidth the width of the line around this polygon
         * @param points      the points used to draw this polygon
         */
        public void polygon(Color4f fillColor, Color4f strokeColor, int strokeWidth, Vector2i... points) {
            nvgFillColor(vg, toBuffer(fillColor));
            nvgStrokeColor(vg, toBuffer(strokeColor));
            nvgStrokeWidth(vg, strokeWidth);
            polygon(points);
            restoreColors();
        }

        /**
         * Draws a polygon by drawing a line along the given points, connecting the last point with the first.
         * @param points the points used to draw this polygon
         */
        public void polygon(Vector2i... points) {
            nvgBeginPath(vg);

            nvgMoveTo(vg, points[points.length - 1].x, points[points.length - 1].y);
            for (Vector2i point : points) {
                nvgLineTo(vg, point.x, point.y);
            }

            nvgFill(vg);
            nvgStroke(vg);
        }

        /**
         * draw a line along the given coordinates
         * @param points (x, y) pairs of screen coordinates
         */
        public void line(int strokeWidth, Color4f strokeColor, Vector2i... points) {
            nvgStrokeColor(vg, toBuffer(strokeColor));
            nvgStrokeWidth(vg, strokeWidth);
            nvgBeginPath(vg);
            nvgMoveTo(vg, points[0].x, points[0].y);

            for (int i = 1; i < points.length; i++) {
                nvgLineTo(vg, points[i].x, points[i].y);
            }

            nvgStroke(vg);
            restoreColors();
        }

        // non-shape functions

        /**
         * @param x         x coordinate of the top-left position of the text
         * @param y         y coordinate of the top-left position of the text
         * @param size      font size in pixels
         * @param font      the font to use
         * @param alignment the alignment, one of One of:<br><table><tr>
         *                  <td>{@link Alignment#ALIGN_LEFT}</td>
         *                  <td>{@link Alignment#ALIGN_RIGHT}</td>
         *                  <td>{@link Alignment#ALIGN_TOP}</td>
         *                  <td>{@link Alignment#ALIGN_BOTTOM}</td>
         *                  </tr></table>
         * @param color     the color of the text
         * @param text      the text to write
         * @param maxWidth
         */
        public void text(
                int x, int y, float size, NGFonts font, EnumSet<Alignment> alignment, Color4f color, String text,
                float maxWidth
        ) {
            int alignFlags = getAlignFlags(alignment);

            nvgFontSize(vg, size);
            nvgFontFace(vg, font.name);
            nvgTextAlign(vg, alignFlags);
            nvgFillColor(vg, toBuffer(color));
            nvgTextBox(vg, x, y, maxWidth, text);

            nvgFillColor(vg, toBuffer(fillColor));
        }

        private int getAlignFlags(EnumSet<Alignment> alignment) {
            int alignFlags = 0;

            if (alignment.contains(Alignment.ALIGN_TOP)) {
                alignFlags |= NVG_ALIGN_TOP;
            } else if (alignment.contains(Alignment.ALIGN_BOTTOM)) {
                alignFlags |= NVG_ALIGN_BOTTOM;
            } else {
                alignFlags |= NVG_ALIGN_MIDDLE;
            }

            if (alignment.contains(Alignment.ALIGN_LEFT)) {
                alignFlags |= NVG_ALIGN_LEFT;
            } else if (alignment.contains(Alignment.ALIGN_RIGHT)) {
                alignFlags |= NVG_ALIGN_RIGHT;
            } else {
                alignFlags |= NVG_ALIGN_CENTER;
            }

            return alignFlags;
        }

        /** for debugging purposes. Prints the given text in the upper left corner of the screen */
        public void printRoll(String text) {
            int y = yPrintRoll + ((printRollSize + 5) * printRollEntry);

            text(xPrintRoll, y, printRollSize, NGFonts.LUCIDA_CONSOLE, EnumSet.of(Alignment.ALIGN_LEFT), textColor, text, windowWidth - xPrintRoll);
            printRollEntry++;
        }

        /**
         * executes the action outside the GUI rendering context
         */
        public void render(Runnable action) {
            nvgEndFrame(vg);
            glEnable(GL_DEPTH_TEST);

            action.run();

            glDisable(GL_DEPTH_TEST);
            nvgBeginFrame(vg, windowWidth, windowHeight, 1);
            restoreColors();
        }

        /**
         * create an image based on a buffer with bytes in RGBA format
         * @param buffer the contents to write
         * @return the nvg id of the image
         */
        public int createImageFromBuffer(ByteBuffer buffer, int width, int height) {
            return nvgCreateImageRGBA(vg, width, height, NVG_IMAGE_NEAREST, buffer);
        }

        /**
         * updates the image from a buffer with bytes in RGBA format
         * @param nvgID  the nvg id of the image
         * @param buffer the contents to write
         */
        public void updateImageFromBuffer(int nvgID, ByteBuffer buffer) {
            nvgUpdateImage(vg, nvgID, buffer);
        }

        /**
         * create an image based on file location
         * @return the nvg id
         */
        public int createImage(Path filePath, int imageFlags) {
            return nvgCreateImage(vg, filePath.toString(), imageFlags);
        }

        /**
         * draws a previously created nvg image
         * @param imageID the id of the image to draw, obtained from either createImage or createImageFromTexture
         * @param angle   the rotation angle in radians
         * @param scale
         */
        public void drawImage(int imageID, int x, int y, int width, int height, float angle, float scale) {
            // note: the magic in this function is rather fragile
            // translate to middle of area-to-draw
            nvgTranslate(vg, x + width / 2f, y + height / 2f);
            // then rotate
            nvgRotate(vg, angle);
            // then draw pattern around (0, 0)
            NVGPaint p = nvgImagePattern(vg,
                    -width * 0.5f * scale, -height * 0.5f * scale,
                    width * scale, height * scale, 0, imageID, 1, paint
            );
            nvgFillPaint(vg, p);
            // reset before drawing
            nvgResetTransform(vg);

            nvgStrokeWidth(vg, 0);

            nvgBeginPath(vg);
            // set are to draw as usual
            nvgRect(vg, x, y, width, height);
            nvgFill(vg);

            restoreColors();
        }

        /**
         * @param text any string
         * @return the width of the text displayed in pixels
         */
        public int getTextWidth(String text, float size, NGFonts font) {
            nvgFontSize(vg, size);
            nvgFontFace(vg, font.name);
            return (int) nvgTextBounds(vg, 0, 0, text, (FloatBuffer) null);
        }

    }
}
