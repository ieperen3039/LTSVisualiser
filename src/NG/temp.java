package NG;

import java.util.Locale;

import static java.lang.StrictMath.PI;

/**
 * @author Geert van Ieperen created on 17-7-2020.
 */
public class temp {
    public static void main(String[] args) {
        int sides = 16;

        double factor = 2 * PI / sides;

        for (int i = 0; i < sides; i++) {
            System.out.printf(Locale.US, "(%1.3f, %1.3f),\n", Math.sin(factor * i), Math.cos(factor * i));
        }
    }
}
