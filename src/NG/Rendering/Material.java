package NG.Rendering;

import NG.DataStructures.Generic.Color4f;

/**
 * A collection representing material properties, for entities.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public enum Material {
    ROUGH(Color4f.GREY, Color4f.INVISIBLE, 1),
    METAL(Color4f.WHITE, Color4f.WHITE, 20),
    PLASTIC(Color4f.WHITE, Color4f.GREY, 2),

    PEARL(Color4f.WHITE, Color4f.MAGENTA, 100),
    SILVER(new Color4f(0.8f, 0.8f, 0.8f), new Color4f(0.9f, 0.9f, 1f), 40);

    public final Color4f baseColor;
    public final Color4f specular;
    public final float reflectance;

    Material(Color4f base, Color4f specular, float reflectance) {
        this.baseColor = base;
        this.specular = specular;
        this.reflectance = reflectance;
    }
}
