package NG;

import NG.Core.Main;
import NG.Tools.Logger;
import org.lwjgl.system.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Boots the Root
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Boot {
    public static void main(String[] argArray) throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList(argArray));

        if (args.contains("-debug")) {
            Logger.setLoggingLevel(Logger.DEBUG);

        } else if (args.contains("-quiet")) {
            Logger.setLoggingLevel(Logger.ERROR);

        } else {
            Logger.setLoggingLevel(Logger.INFO);
        }

        if (args.contains("-lwjgl-debug")) {
            Configuration.DEBUG.set(true);
        }

        new Main().root();
    }
}
