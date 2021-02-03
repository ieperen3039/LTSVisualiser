package NG.DataStructures.Generic;

import NG.Tools.Toolbox;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * An immutable representation of colors. Includes an opacity value (the alpha value) and a set of predefined colors.
 * @author Geert van Ieperen created on 21-11-2017.
 */
public class Color4f implements Serializable {
    public static final Color4f BLACK = new Color4f(0, 0, 0);
    public static final Color4f GREY = new Color4f(0.5f, 0.5f, 0.5f);
    public static final Color4f LIGHT_GREY = new Color4f(0.8f, 0.8f, 0.8f);
    public static final Color4f WHITE = new Color4f(1, 1, 1);
    public static final Color4f RED = new Color4f(1, 0, 0);
    public static final Color4f GREEN = new Color4f(0, 1, 0);
    public static final Color4f BLUE = new Color4f(0, 0, 1);
    public static final Color4f YELLOW = new Color4f(1, 1, 0);
    public static final Color4f MAGENTA = new Color4f(1, 0, 1);
    public static final Color4f CYAN = new Color4f(0, 1, 1);
    public static final Color4f ORANGE = new Color4f(1, 0.5f, 0);

    /** a color with alpha == 0, thus (technically) not visible */
    public static final Color4f INVISIBLE = new Color4f(0, 0, 0, 0);
    public static final Color4f TRANSPARENT_GREY = new Color4f(0.5f, 0.5f, 0.5f, 0.5f);

    public final float red;
    public final float green;
    public final float blue;
    public final float alpha;

    public Color4f(java.awt.Color target) {
        this(target.getRed(), target.getGreen(), target.getBlue(), target.getAlpha());
    }

    /**
     * @param red   the red intensity [0...1]
     * @param green the green intensity [0...1]
     * @param blue  the blue intensity [0...1]
     * @param alpha the opacity of the color
     */
    public Color4f(float red, float green, float blue, float alpha) {
        this.red = cap(red);
        this.green = cap(green);
        this.blue = cap(blue);
        this.alpha = cap(alpha);
    }

    /**
     * @param vec a color described as a vector with values [0, 1]
     */
    public Color4f(Vector4f vec) {
        this(vec.x, vec.y, vec.z, vec.w);
    }

    /**
     * @param color     a color described as a vector with values [0, 1]
     * @param intensity the alpha value
     */
    public Color4f(Vector3f color, float intensity) {
        this(color.x, color.y, color.z, intensity);
    }

    public Color4f(float red, float green, float blue) {
        this(red, green, blue, 1f);
    }

    /**
     * create a new color based on another color with new intensity. if (intensity == source.alpha), then
     * (this.equals(source))
     * @param source a source color, of which the red green and blue values are taken
     * @param alpha  the new alpha value
     */
    public Color4f(Color4f source, float alpha) {
        red = source.red;
        green = source.green;
        blue = source.blue;
        this.alpha = cap(alpha);
    }

    /**
     * flat add this color with the given other color
     * @param other another color
     * @return each color aspect added to the other, with alpha inverse multiplied
     * @see #overlay(Color4f)
     */
    public Color4f add(Color4f other) {
        return new Color4f(
                cap(red + other.red),
                cap(green + other.green),
                cap(blue + other.blue),
                inverseMul(alpha, other.alpha)
        );
    }

    /**
     * adds color as combined light, using inverse multiplication
     * @param other another color
     * @return a lighter mix of these colors, using inverse multiplication of every color aspect including alpha
     */
    public Color4f overlay(Color4f other) {
        return new Color4f(
                inverseMul(red, other.red),
                inverseMul(green, other.green),
                inverseMul(blue, other.blue),
                alpha * other.alpha
        );
    }

    /**
     * multiply this color with the given color. Alpha values are inverse multiplied
     * @param other another color
     * @return a darker mix of the color
     */
    public Color4f multiply(Color4f other) {
        return new Color4f(
                red * other.red,
                green * other.green,
                blue * other.blue,
                inverseMul(alpha, other.alpha)
        );
    }

    /** culls parameter to [0, 1] */
    private float cap(float input) {
        return (input < 0) ? 0 : ((input > 1) ? 1 : input);
    }

    /**
     * intensifies this color by adding a white light to it. does not affect alpha, and is not the inverse of {@link
     * #darken(float)}
     * @param scalar a factor in [0, 1], where 0 gives no change, and 1 makes this color effectively white
     * @return the new color, brighted up
     * @see #darken(float)
     */
    public Color4f intensify(float scalar) {
        return new Color4f(
                inverseMul(red, scalar),
                inverseMul(green, scalar),
                inverseMul(blue, scalar),
                alpha
        );
    }

    /**
     * darken this color by linearly fading it to black. Does not affect alpha, and is not the inverse of {@link
     * #intensify(float)}
     * @param scalar a factor in [0, 1], where 0 gives no change, and 1 makes this color effectively black
     * @return the new color, darkened
     * @see #intensify(float)
     */
    public Color4f darken(float scalar) {
        return new Color4f(
                red * (1 - scalar),
                green * (1 - scalar),
                blue * (1 - scalar),
                alpha
        );
    }

    /**
     * replaces the transparency with white, creating an opaque variation of this color
     * @return the color if it was on a white background.
     */
    public Color4f opaque() {
        return new Color4f(
                inverseMul(red, alpha),
                inverseMul(green, alpha),
                inverseMul(blue, alpha),
                1
        );
    }

    /**
     * @return 1 - ((1 - alpha) * (1 - beta))
     */
    private float inverseMul(float alpha, float beta) {
        return 1f - ((1f - alpha) * (1f - beta));
    }

    /**
     * @return the vector that would represent this color, when the color is multiplied with the alpha value
     */
    public Vector3f toVector3f() {
        return new Vector3f(red * alpha, green * alpha, blue * alpha);
    }

    public Vector4f toVector4f() {
        return new Vector4f(red, green, blue, alpha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;

        Color4f other = (Color4f) o;
        if (other.red != red) return false;
        if (other.green != green) return false;
        if (other.blue != blue) return false;
        return (other.alpha == alpha);
    }

    @Override
    public int hashCode() {
        int result = ((red != +0.0f) ? Float.floatToIntBits(red) : 0);
        result = (31 * result) + ((green != +0.0f) ? Float.floatToIntBits(green) : 0);
        result = (31 * result) + ((blue != +0.0f) ? Float.floatToIntBits(blue) : 0);
        result = (31 * result) + ((alpha != +0.0f) ? Float.floatToIntBits(alpha) : 0);
        return result;
    }

    /**
     * @return the red, green and blue value of this color, ignoring alpha
     */
    public Vector3f rawVector3f() {
        return new Vector3f(red, green, blue);
    }

    public FloatBuffer toFloatBuffer() {
        return FloatBuffer.wrap(toArray());
    }

    public float[] toArray() {
        return new float[]{red, green, blue, alpha};
    }

    public Color4f interpolateTo(Color4f other, float value) {
        return new Color4f(
                red + ((other.red - red) * value),
                green + ((other.green - green) * value),
                blue + ((other.blue - blue) * value)
        );
    }

    /**
     * puts this color on the buffer and increase the position by 4
     * @param colorBuffer the buffer to write to
     */
    public void put(FloatBuffer colorBuffer) {
        colorBuffer.put(red);
        colorBuffer.put(green);
        colorBuffer.put(blue);
        colorBuffer.put(alpha);
    }

    public void put(ByteBuffer colorBuffer) {
        colorBuffer.put((byte) (red * 255));
        colorBuffer.put((byte) (green * 255));
        colorBuffer.put((byte) (blue * 255));
        colorBuffer.put((byte) (alpha * 255));
    }

    @Override
    public String toString() {
        return "Color (" +
                (int) (red * 255) + ", " +
                (int) (green * 255) + ", " +
                (int) (blue * 255) + ")" +
                (alpha < 1 ? String.format(" %5.03f", alpha) : "")
                ;
    }

    /**
     * create a color using integer arguments [0 - 255] with 0 = none and 255 = max
     */
    public static Color4f rgb(int ired, int igreen, int iblue, float alpha) {
        return new Color4f(ired / 255f, igreen / 255f, iblue / 255f, alpha);
    }

    /**
     * create a color using integer arguments [0 - 255] with 0 = none and 255 = max
     */
    public static Color4f rgb(int ired, int igreen, int iblue) {
        return rgb(ired, igreen, iblue, 1f);
    }

    public static Color4f randomBetween(Color4f color1, Color4f color2) {
        float red = Toolbox.randomBetween(color1.red, color2.red);
        float green = Toolbox.randomBetween(color1.green, color2.green);
        float blue = Toolbox.randomBetween(color1.blue, color2.blue);
        float alpha = Toolbox.randomBetween(color1.alpha, color2.alpha);
        return new Color4f(red, green, blue, alpha);
    }

    public static Color4f parse(String asVector4f) {
        String[] numbers = asVector4f
                .replaceFirst("\\(", "")
                .replaceFirst("\\)", "")
                .split(", ");

        return new Color4f(
                Float.parseFloat(numbers[0]),
                Float.parseFloat(numbers[1]),
                Float.parseFloat(numbers[2]),
                Float.parseFloat(numbers[3])
        );
    }
}
