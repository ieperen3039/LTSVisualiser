package NG;

import NG.Core.Main;
import NG.Settings.Settings;
import NG.Tools.Logger;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;

/**
 * Tags the Flags, Boots the Roots
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Boot {
    public static void main(String[] args) throws Exception {
        Settings settings = new Settings();
//        Logger.setLoggingLevel(Logger.INFO);
        Logger.setLoggingLevel(Logger.DEBUG);

        File defaultLog = new File("output1.log");
        for (int i = 2; defaultLog.exists(); i++) {
            defaultLog = new File("output" + i + ".log");
        }

        new FlagManager()
                .addFlag("debug", () -> Logger.doPrintCallsites = true,
                        "Sets logging to DEBUG level")
                .addFlag("quiet", () -> Logger.setLoggingLevel(Logger.INFO),
                        "Sets logging to INFO level")
                .addFlag("silent", () -> Logger.setLoggingLevel(Logger.ERROR),
                        "Sets logging to ERROR level")
                .addExclusivity("debug", "quiet", "silent")

                .addParameterFlag("log", defaultLog.getName(),
                        file -> Logger.setOutputStream(new FileOutputStream(file)),
                        "Sets logging to write to the file with the given name. If the file exists, it is overwritten. " +
                                "By default, it writes to a generated new file"
                )

                .addFlag("lwjgl-debug", () -> Configuration.DEBUG.set(true),
                        "Activates logging of underlying libraries")
                .addFlag("untimed", () -> Logger.doPrintTimeStamps = false,
                        "Removes timestamps from logging")
                .addFlag("loopTimingOverlay", () -> settings.PRINT_ROLL = true,
                        "Display real-time timing results in the graph area")
                .addFlag("advancedDragging", () -> settings.ADVANCED_MANIPULATION = true,
                        "Activates an experimental dragging mode. " +
                                "This mode pulls the neighbours of targeted nodes along"
                )
                .addFlag("randomLayout", () -> settings.RANDOM_LAYOUT = true,
                        "Layout will be initialized at random, rather than using HDE")

                .addParameterFlag("maxIterationsPerSecond",
                        s -> settings.MAX_ITERATIONS_PER_SECOND = Integer.parseInt(s),
                        "Maximum iterations executed each second by the layout algorithm. " +
                                "default = " + settings.MAX_ITERATIONS_PER_SECOND
                )
                .addParameterFlag("numWorkerThreads",
                        s -> settings.NUM_WORKER_THREADS = Integer.parseInt(s),
                        "Number of worker threads used to parallelize the layout algorithm. " +
                                "default = " + settings.NUM_WORKER_THREADS
                )
                .addParameterFlag("startAutoTester",
                        s -> settings.DATA_COLLECTION_PATH = Paths.get(s),
                        "Run an automatic layout speed data generation on the graphs of the given path, then quit")

                .parse(args);

        new Main(settings).root();
    }
}
