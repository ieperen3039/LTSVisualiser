package NG.Core;

import NG.Graph.Layout.SpringLayout;
import NG.Settings.Settings;
import NG.Tools.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Geert van Ieperen created on 27-1-2021.
 */
public class Auto extends Thread {
    public static final float TARGET_SPEED = 1f / (1 << 8);
    private static final int RUN_TIME_MS = 60_000;
    private final Main root;
    private final Path directory;

    private final BlockingQueue<Float> queue = new ArrayBlockingQueue<>(8);
    private final AtomicInteger iteration = new AtomicInteger(0);
    private final List<String> blacklist = Arrays.asList("alma_2.aut", "lift3-init.aut");

    public Auto(Main root, Path directory) {
        super("auto thread");
        this.root = root;
        this.directory = directory;
    }

    @Override
    public void run() {
        SpringLayout layout = root.getSpringLayout();
        Settings settings = root.settings();
        root.window().setMinimized(true);

        File resultFile = new File("barnes_comparison.csv");
        PrintWriter out = getWriter(resultFile);
        out.println("\"Files\";\"Plain 1\";\"Plain 10\";\"Plain 100\";\"Plain 1000\";\"Barnes 1\";\"Barnes 10\";\"Barnes 100\";\"Barnes 1000\"");

        // all files in directory
        File[] files = directory.toFile().listFiles();
        for (File graphFile : Objects.requireNonNull(files)) {
            String name = graphFile.getName();

            try {
                boolean isGraph = name.contains(".aut");
                if (!isGraph) continue;
                if (blacklist.contains(name)) continue;
                out.printf("\"%s\";", name);

                { // plain
                    layout.defer(() -> {
                        Logger.INFO.print("Testing plain with " + graphFile);

                        root.setGraph(graphFile);
                        layout.setSpeed(TARGET_SPEED);
                        layout.setBarnesHutTheta(0);
                        iteration.set(0);
                    });

                    for (int i = 0; i < 4; i++) {
                        Float value = queue.take();
                        out.printf(Locale.US, "%.06f;", value);
                    }
                }

                { // barnes-hut
                    layout.defer(() -> {
                        Logger.INFO.print("Testing barnes-hut with " + graphFile);

                        root.setGraph(graphFile);
                        layout.setSpeed(TARGET_SPEED);
                        layout.setBarnesHutTheta(1);
                        iteration.set(0);
                    });

                    for (int i = 0; i < 4; i++) {
                        Float value = queue.take();
                        out.printf(Locale.US, "%.06f;", value);
                    }
                }

                out.println();
                out.flush();

            } catch (InterruptedException e) {
                Logger.ERROR.print(e);
            }
        }

        out.close();
        root.getSpringLayout().stopLoop();
        root.window().close();
    }

    private PrintWriter getWriter(File resultFile) {
        try {
            return new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void onLayoutUpdate() {
        int i = iteration.incrementAndGet();

        if (i == 1 || i == 10 || i == 100 || i == 1000) {
            Logger.DEBUG.print("iteration " + i);
            float time = root.getSpringLayout().timer.secondsSinceLoopStart();
            queue.add(time);
        }
    }
}
