package NG.Core;

import NG.Graph.Layout.SpringLayout;
import NG.Settings.Settings;
import NG.Tools.Logger;
import NG.Tools.Toolbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Geert van Ieperen created on 27-1-2021.
 */
public class Auto extends Thread {
    public static final float TARGET_SPEED = 1f / (1 << 8);
    private static final int RUN_TIME_MS = 60_000;
    private static final int RANDOM_REPETITIONS = 1;
    private final Main root;
    private final Path directory;
    private final String[] graphs = {"alma.aut"};

    public Auto(Main root, Path directory) {
        super("auto thread");
        this.root = root;
        this.directory = directory;
    }

    @Override
    public void run() {
        SpringLayout layout = root.getSpringLayout();
        Settings settings = root.settings();

        // all files in directory
        for (File graphFile : Objects.requireNonNull(directory.toFile().listFiles())) {
            String name = graphFile.getName();

//        // only files of 'graphs'
//        for (String name : graphs) {
//            File graphFile = directory.resolve(name).toFile();

            boolean isGraph = name.contains(".aut");
            if (!isGraph) continue;

            String fileName = name.replaceFirst("\\.aut", "");

            {
                File resultFile = new File(fileName + "_hde.csv");
                PrintWriter out = getWriter(resultFile);

                layout.defer(() -> {
                    Logger.INFO.print("Testing HDE with " + graphFile);
                    out.println("\"Net Force\";\"Tension\"");
                    layout.setTensionReader((n, t) -> out.printf(Locale.US, "%6.03f;%6.03f\n", n, t));

                    settings.RANDOM_LAYOUT = false;
                    root.setGraph(graphFile);
                    layout.setSpeed(TARGET_SPEED);
                });

                Toolbox.sleep(RUN_TIME_MS);

                out.close();
            }


            for (int i = 0; i < RANDOM_REPETITIONS; i++) {
                File resultFile = new File(fileName + "_rand_" + i + ".csv");
                PrintWriter out = getWriter(resultFile);

                layout.defer(() -> {
                    Logger.INFO.print("Testing Random with " + graphFile);

                    out.println("\"Net Force\";\"Tension\"");
                    layout.setTensionReader((n, t) -> out.printf(Locale.US, "%6.03f,%6.03f\n", n, t));

                    settings.RANDOM_LAYOUT = true;
                    root.setGraph(graphFile);
                    layout.setSpeed(TARGET_SPEED);
                });

                Toolbox.sleep(RUN_TIME_MS);
                out.close();
            }

            root.window().shouldClose();
        }
    }

    private PrintWriter getWriter(File resultFile) {
        try {
            return new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
