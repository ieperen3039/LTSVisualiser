package NG;

import java.util.*;

/**
 * @author Geert van Ieperen created on 27-1-2021.
 */
class FlagManager {
    private final Map<String, Flag> flags = new HashMap<>();
    private final Map<String, Parameter> parameters = new HashMap<>();
    private final Map<String, String> defaultParameters = new HashMap<>();
    private final Collection<Collection<String>> exclusives = new ArrayList<>();

    /**
     * create a new flag manager with an automatic 'help' flag which shows the available parameters, and force quits the
     * application
     */
    public FlagManager() {
        String help = "help";
        flags.put(help, new Flag(help, "shows this text, then quits",
                () -> {
                    System.out.println("The following flags are accepted:");
                    for (Flag f : flags.values()) {
                        System.out.println("\t-" + f.name);
                        System.out.println("\t\t" + f.description);
                    }

                    System.out.println("The following parameters are accepted:");
                    for (Parameter p : parameters.values()) {
                        System.out.println("\t-" + p.name + " [PARAMETER]");
                        System.out.println("\t\t" + p.description);
                    }

                    System.exit(1);
                }
        ));
    }

    /**
     * if the given flag is found in the arguments, the {@code ifPresent} action will be executed
     */
    public FlagManager addFlag(String flag, RunnableThr ifPresent, String description) {
        flags.put(flag, new Flag(flag, description, ifPresent));
        return this;
    }

    /**
     * if the given flag is found in the arguments, the {@code ifPresent} action receives the next element in the
     * argument list.
     */
    public FlagManager addParameterFlag(String flag, StringConsumerThr ifPresent, String description) {
        parameters.put(flag, new Parameter(flag, description, ifPresent));
        return this;
    }

    /**
     * if the given flag is found in the arguments, the {@code ifPresent} action receives the next element in the
     * argument list. If not, the {@code ifPresent} action receives the default value
     */
    public FlagManager addParameterFlag(
            String flag, String defaultValue, StringConsumerThr ifPresent, String description
    ) {
        addParameterFlag(flag, ifPresent, description);
        addParameterDefault(flag, defaultValue);
        return this;
    }

    /**
     * If the given flag is NOT found in the arguments, the {@code ifPresent} action is executed with the given default
     * value
     */
    public FlagManager addParameterDefault(String flag, String defaultValue) {
        defaultParameters.put(flag, defaultValue);
        return this;
    }

    /**
     * makes the given flags and parameters mutually exclusive. Flags and parameters can be mixed. Correctness of the
     * flags is not checked.
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

            Flag flag = flags.get(arg);
            if (flag != null) {
                flag.action.run();
                continue;
            }

            Parameter parameter = parameters.get(arg);
            if (parameter != null) {
                i++; // increment loop counter, skipping parameter
                parameter.action.accept(args[i]);
                // prevent default values from activating
                defaultParameters.remove(arg);
                continue;
            }

            throw new IllegalArgumentException("Unknown flag " + args[i]);
        }

        // all found parameters are removed from the default map
        for (String flag : defaultParameters.keySet()) {
            Parameter parameter = parameters.get(flag);
            String value = defaultParameters.get(flag);
            parameter.action.accept(value);
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

    public interface RunnableThr {
        void run() throws Exception;
    }

    public interface StringConsumerThr {
        void accept(String s) throws Exception;
    }

    private static class Flag {
        String name;
        String description;
        RunnableThr action;

        public Flag(String name, String description, RunnableThr action) {
            this.name = name;
            this.description = description;
            this.action = action;
        }
    }

    private static class Parameter {
        String name;
        String description;
        StringConsumerThr action;

        public Parameter(String name, String description, StringConsumerThr action) {
            this.name = name;
            this.description = description;
            this.action = action;
        }
    }
}
