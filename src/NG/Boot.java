package NG;

import NG.Core.Main;
import NG.DataStructures.Generic.PairList;
import NG.Settings.Settings;
import NG.Tools.Logger;
import org.lwjgl.system.Configuration;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * Tags the Flags, Boots the Roots
 * @author Geert van Ieperen. Created on 13-9-2018.
 */
public class Boot {
    public static void main(String[] args) throws Exception {
        Settings settings = new Settings();
//        Logger.setLoggingLevel(Logger.INFO);
        Logger.setLoggingLevel(Logger.DEBUG);

        new FlagManager()
                .addFlag("debug", () -> Logger.doPrintCallsites = true)
                .addFlag("quiet", () -> Logger.setLoggingLevel(Logger.INFO))
                .addFlag("silent", () -> Logger.setLoggingLevel(Logger.ERROR))
                .addExclusivity("debug", "quiet", "silent")

                .addFlag("lwjgl-debug", () -> Configuration.DEBUG.set(true))

                .addFlag("timed", () -> Logger.doPrintTimeStamps = true)

                .addFlag("loopTimingOverlay", () -> settings.PRINT_ROLL = true)
                .addFlag("advancedDragging", () -> settings.ADVANCED_MANIPULATION = true)

                .addParameterFlag("log", file -> {
                    PrintStream out = new PrintStream(file);
                    Logger.setOutputReceiver(s -> {
                        System.out.println(s);
                        out.println(s);
                    }, s -> {
                        System.err.println(s);
                        out.println(s);
                        out.flush();
                    });
                })
                .addFlag("noLogFile", () -> Logger.setOutputReceiver(null, null))
                .addExclusivity("log", "noLogFile")

                .addParameterFlag("maxIterationsPerSecond",
                        s -> settings.MAX_ITERATIONS_PER_SECOND = Integer.parseInt(s)
                )
                .addParameterFlag("numWorkerThreads",
                        s -> settings.NUM_WORKER_THREADS = Integer.parseInt(s)
                )

                .parse(args);

        new Main(settings).root();
    }

    private interface RunnableThr {
        void run() throws Exception;
    }

    private interface StringConsumerThr {
        void accept(String s) throws Exception;
    }

    private static class FlagManager {
        private final PairList<String, RunnableThr> flags = new PairList<>();
        private final PairList<String, StringConsumerThr> parameters = new PairList<>();
        private final Collection<Collection<String>> exclusives = new ArrayList<>();

        /** create a new flag manager with an automatic 'help' flag */
        public FlagManager() {
            flags.add("help", () -> {
                System.out.println("The following flags are accepted:");
                flags.forEach(x -> System.out.println("\t-" + x.left));
                System.out.println("The following parameters are accepted:");
                parameters.forEach(x -> System.out.println("\t-" + x.left + " [PARAMETER]"));

                System.exit(1);
            });
        }

        /**
         * if the given flag is found in the arguments, the {@code ifPresent} action will be executed
         */
        public FlagManager addFlag(String flag, RunnableThr ifPresent) {
            flags.add(flag, ifPresent);
            return this;
        }

        /**
         * if the given flag is found in the arguments, the {@code ifPresent} action receives the next element in the
         * argument list.
         */
        public FlagManager addParameterFlag(String flag, StringConsumerThr ifPresent) {
            parameters.add(flag, ifPresent);
            return this;
        }

        /**
         * makes the given flags and parameters mutually exclusive. Flags and parameters can be mixed. Correctness of
         * the flags is not checked.
         */
        public FlagManager addExclusivity(String... mutuallyExclusiveFlags) {
            exclusives.add(Arrays.asList(mutuallyExclusiveFlags));
            return this;
        }

        public void parse(String[] args) throws Exception {
            ArrayList<Collection<String>> exclusivesFound = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i].substring(1); // remove the initial dash
                // check whether this arg is mutually exclusive with an earlier arg
                for (Collection<String> set : exclusives) {
                    if (!set.contains(arg)) continue;

                    if (exclusivesFound.contains(set)) {
                        throwExclusionException(args[i], set);
                    }

                    exclusivesFound.add(set);
                }

                RunnableThr action = flags.right(arg);
                if (action != null) {
                    action.run();
                    continue;
                }

                StringConsumerThr pAction = parameters.right(arg);
                if (pAction != null) {
                    i++; // increment loop counter, skipping parameter
                    pAction.accept(args[i]);
                    continue;
                }

                throw new IllegalArgumentException("Unknown flag " + args[i]);
            }

            flags.clear();
            parameters.clear();
            exclusives.clear();
        }

        public void throwExclusionException(String arg, Collection<String> set) {
            StringJoiner acc = new StringJoiner(", ");

            for (String s : set) {
                if (!s.equals(arg)) {
                    acc.add(s);
                }
            }

            throw new IllegalArgumentException("Flag " + arg + " is mutually exclusive with " + acc.toString());
        }
    }
}
