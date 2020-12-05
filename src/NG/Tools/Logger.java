package NG.Tools;

import org.joml.*;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A logger class that has its levels reflected in enum constants.
 * @author Geert van Ieperen created on 2-6-2018.
 * @see #DEBUG
 */
public enum Logger {
    // The order of the constants reflect the order of level
    /** for any information about the internal functioning */
    DEBUG,
    /** for errors caused by a break of assumptions in internal, where the game still could continue */
    ASSERT,
    /** for general information about the game state */
    INFO,
    /** for errors that allow the game to continue */
    WARN,
    /** for errors that probably cause the game to crash at some point */
    ERROR;

    private static final long timeStart = System.currentTimeMillis();
    private final String codeName = String.format("[%-5s]", this);
    public static boolean doPrintCallsites = false;
    public static boolean doPrintTimeStamps = false;

    private static List<Supplier<String>> onlinePrints = new CopyOnWriteArrayList<>();
    private static Consumer<String> out = null;
    private static Consumer<String> err = null;
    /** prevents spamming the log */
    protected static Set<String> callerBlacklist = new HashSet<>();

    private boolean enabled = true;

    static {
        try {
            PrintStream out = new PrintStream("latest.log");
            setOutputReceiver(s -> {
                System.out.println(s);
                out.println(s);
            }, s -> {
                System.err.println(s);
                out.println(s);
                out.flush();
            });
        } catch (FileNotFoundException e) {
            setOutputReceiver(null, null);
            ERROR.print(e);
        }
    }

    /**
     * prints the result of {@link Object#toString()} of the given objects to the output, preceded with calling method.
     * Every unique callside will only be allowed to print once. For recursive calls, every level will be regarded as a
     * new level, thus print once for every unique depth
     * @param identifier the string that identifies this call as unique
     * @param s          the strings to print
     */
    public synchronized void printSpamless(String identifier, Object... s) {
        if (!callerBlacklist.contains(identifier)) {
            printFrom(2, s);
            callerBlacklist.add(identifier);
        }
    }

    /**
     * prints the toString method of the given objects to the debug output, preceded with the method caller specified by
     * the given call depth
     * @param depth 0 = this method, 1 = the calling method (yourself)
     */
    public synchronized void printFrom(int depth, Object... s) {
        if (!enabled) return;

        String prefix = codeName;
        if (doPrintTimeStamps) prefix = String.format("%-10d %s", System.currentTimeMillis() - timeStart, prefix);
        if (doPrintCallsites) prefix = getCallingMethod(depth) + prefix;

        switch (this) {
            case DEBUG:
            case INFO:
                out.accept(prefix + ": " + concatenate(s));
                break;
            case ASSERT:
            case WARN:
            case ERROR:
                err.accept(prefix + ": " + concatenate(s));

                if (this == ERROR) {
                    for (Object elt : s) {
                        if (elt instanceof Throwable) {
                            dumpException((Throwable) elt, err);
                        }
                    }
                }

                break;
        }
    }

    public void dumpException(Throwable e, Consumer<String> action) {
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            String s1 = "\t" + stackTraceElement;
            action.accept(s1);
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            err.accept("Caused by:");
            err.accept(cause.toString());
            dumpException(cause, action);
        }
    }

    /**
     * prints the toString method of the given objects to System.out, preceded with calling method
     */
    public void print(Object... s) {
        printFrom(2, s);
    }

    /**
     * prints the arguments according to the given format in Locale US.
     * @param format a format string
     * @see String#format(String, Object...)
     */
    public void printf(String format, Object... arguments) {
        printFrom(2, String.format(Locale.US, format, arguments));
    }

    /**
     * adds a newline if this logger is enabled
     */
    public void newLine() {
        if (enabled) out.accept("");
    }

    /**
     * @return the error logger as a printstream
     */
    public PrintStream getPrintStream() { // TODO make an outputstream that reroutes all traffic
        return new PrintStream(System.out, true) {
            @Override
            public void print(String s) {
                Logger.this.print(s);
            }
        };
    }

    private static String concatenate(Object[] x) {
        if (x.length == 0) return "";

        for (int i = 0; i < x.length; i++) {
            if (x[i] == null) {
                x[i] = "null";
            } else if (x[i] instanceof Vector3fc) {
                Vector3fc v = (Vector3fc) x[i];
                x[i] = Vectors.toString(v);
            } else if (x[i] instanceof Vector2fc) {
                Vector2fc v = (Vector2fc) x[i];
                x[i] = Vectors.toString(v);
            } else if (x[i] instanceof Vector2ic) {
                Vector2ic v = (Vector2ic) x[i];
                x[i] = Vectors.toString(v);
            } else if (x[i] instanceof Vector3ic) {
                Vector3ic v = (Vector3ic) x[i];
                x[i] = Vectors.toString(v);
            } else if (x[i] instanceof Vector4fc) {
                Vector4fc v = (Vector4fc) x[i];
                x[i] = Vectors.toString(v);
            }
        }

        StringBuilder s = new StringBuilder(x[0].toString());
        for (int i = 1; i < x.length; i++) {
            s.append(" | ").append(x[i]);
        }
        return s.toString();
    }

    /**
     * sets the debug output of the given print method to the specified output. If both regular and error is null, reset
     * to the default outputs
     * @param regular the new output
     * @param error   the error output
     */
    public static void setOutputReceiver(Consumer<String> regular, Consumer<String> error) {
        if (regular == null && error == null) {
            // default
            out = System.out::println;
            err = System.err::println;
            return;
        }

        if (regular != null) out = regular;
        if (error != null) err = error;
    }

    /**
     * Text resulting from the given supplier is presented somewhere on the screen of this program, likely a
     * debug-screen.
     * @param source a source of text, queried every frame
     */
    public static void printOnline(Supplier<String> source) {
        if (source == null) {
            Logger.ERROR.print("source is null");
        }
        onlinePrints.add(source);
    }

    /**
     * Writes all the online output strings to the given consumer
     * @param accepter a method that prints the given string, on the same position as a previous call to this method
     */
    public static void putOnlinePrint(Consumer<String> accepter) {
        for (Supplier<String> source : onlinePrints) {
            accepter.accept(source.get());
        }
    }

    /**
     * DEBUG method to get the calling method name
     * @param level the stack depth to receive. -1 = this method {@code getCallingMethod(int)} 0 = the calling method
     *              (yourself) 1 = the caller of the method this is called in
     * @return a string that completely describes the path to the file, the method and line number where this is called
     * If DEBUG == false, return an empty string
     */
    public static String getCallingMethod(int level) {
        StackTraceElement caller;
        Exception exception = new Exception();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        level++;
        do {
            caller = stackTrace[level++]; // level + 1
        } while (caller.isNativeMethod() && level < stackTrace.length);

        return String.format("%-100s ", caller);
    }

    /**
     * removes the specified updater off the debug screen
     * @param source an per-frame updated debug message that has previously added to the debug screen
     */
    public static void removeOnlinePrint(Supplier<String> source) {
        onlinePrints.remove(source);
    }

    public static Logger getLoggingLevel() {
        Logger[] values = values();
        for (Logger logger : values) {
            if (logger.enabled) return logger;
        }
        return null; // no logging is enabled
    }

    /**
     * Set the logger to display only the text supplied by loggers that came after the supplied logger. A call to {@code
     * setLoggerLevel(Logger.values()[0])} will enable all loggers
     * @param minimum the minimum logger level that IS displayed
     */
    public static void setLoggingLevel(Logger minimum) {
        Logger[] levels = values();
        for (int i = 0; i < levels.length; i++) {
            levels[i].enabled = (i >= minimum.ordinal());
        }
    }
}
