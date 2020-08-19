package NG.Core;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public class Version implements Comparable<Version>, Serializable {
    private static final Pattern PATTERN = Pattern.compile("v \\d.\\d");

    private final int major;
    private final int minor;

    public Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public Version(String versionString) {
        Matcher matcher = PATTERN.matcher(versionString);
        major = Integer.parseInt(matcher.group(1));
        minor = Integer.parseInt(matcher.group(2));
    }

    public int major() {
        return major;
    }

    @Override
    public int compareTo(Version o) {
        return (major == o.major) ? Integer.compare(minor, o.minor) : Integer.compare(major, o.major);
    }

    public boolean isLessThan(int major, int minor) {
        return this.major == major ? this.minor < minor : this.major < major;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) return false;
        Version other = (Version) obj;

        return major == other.major && minor == other.minor;
    }

    /**
     * checks whether this version is less than the given major and minor version, throwing an VersionMisMatch exception
     * if this is the case
     * @param major the major version required
     * @param minor the optional minor version, 0 if it doesn't matter
     * @throws MisMatchException if {@link this#isLessThan(int, int)}
     */
    public void requireAtLeast(int major, int minor) throws MisMatchException {
        if (this.isLessThan(major, minor)) {
            throw new MisMatchException(new Version(major, minor), this);
        }
    }

    @Override
    public String toString() {
        return "v " + major + "." + minor;
    }

    public static class MisMatchException extends Exception {
        public MisMatchException(Version required, Version current) {
            super("Version mismatch: required " + required + ", got " + current);
        }
    }
}
