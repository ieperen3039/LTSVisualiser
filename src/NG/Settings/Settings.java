package NG.Settings;

/**
 * A class that collects a number of settings. It is the only class whose fields are always initialized upon creation.
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Settings {
    public static final String GAME_NAME = "TEST"; // laaaaame
    public static final float CLICK_BOX_WIDTH = 1.5f;
    public static final float CLICK_BOX_HEIGHT = 0.1f;
    public static final float CLICK_BOX_RESOLUTION = 1f;
    public static float Z_NEAR = 0.01f;
    public static float Z_FAR = 1000;
    public static float FOV = (float) Math.toRadians(40);
    // UI settings
    public static int TOOL_BAR_HEIGHT = 80;
    public boolean DEBUG = true;
    // video settings
    public int TARGET_FPS = 60;
    public boolean V_SYNC = false;
    public int WINDOW_WIDTH = 1200;
    public int WINDOW_HEIGHT = 800;
    public int ANTIALIAS_LEVEL = 1;
    // camera settings
    public boolean ISOMETRIC_VIEW = true;
    public float CAMERA_ZOOM_SPEED = 0.1f;
    public float MAX_CAMERA_DIST = Z_FAR / 2f;
    public float MIN_CAMERA_DIST = Z_NEAR * 2f;
}
