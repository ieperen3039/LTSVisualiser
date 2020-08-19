package NG.Tools;

import java.io.*;

/**
 * An Object of this type can be written to file using a {@link DataOutputStream} and read from that file using a {@link
 * DataInputStream}. This object must have a public constructor that accepts a {@link DataInputStream} object as only
 * parameter. Implementing classes may not be inner classes (until a workaround has been found)
 * @author Geert van Ieperen. Created on 28-9-2018.
 */
public final class SerializationTools {
    /**
     * Writes this object to a file, which can be recovered with {@link #readFromFile(File)}
     * @param file   the file to write to, existing or not.
     * @param object
     */
    public static void writeToFile(File file, Serializable object) {
        try (OutputStream fileStream = new FileOutputStream(file)) {
            ObjectOutput out = new ObjectOutputStream(fileStream);
            Logger.DEBUG.print("Writing " + file, object);
            out.writeObject(object);

        } catch (IOException ex) {
            Logger.ERROR.print(ex);
        }
    }

    /**
     * reads an object from file, returning null if any exception occurs.
     * @param file the file to read
     * @return the object from the file, cast to T
     * @throws IOException if anything goes wrong while reading the file
     * @see #readFromFileRequired(File, Class)
     */
    public static Object readFromFile(File file) throws IOException, ClassNotFoundException {
        try (InputStream fileStream = new FileInputStream(file)) {
            ObjectInput in = new ObjectInputStream(fileStream);
            return in.readObject();
        }
    }

    /** a version of {@link #readFromFile(File)} that throws an Runtime exception when an error occurs */
    public static <T> T readFromFileRequired(File file, Class<T> expected) {
        try {
            return expected.cast(readFromFile(file));

        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException("Could not load required file", ex);
        }
    }

    /**
     * packs the given element in a storage box and writes it to the stream. This ensures that reading the stream is
     * protected against crashes, at the cost of temporary overhead while writing.
     * @param out     the output stream
     * @param element the element to write to the stream
     * @throws IOException signals that anything went wrong. The output stream is untouched when this happens.
     */
    public static void writeSafe(ObjectOutput out, Object element) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
        ObjectOutput wrapOut = new ObjectOutputStream(buffer);

        wrapOut.writeObject(element);
        wrapOut.close();

        out.writeInt(buffer.size());
        out.write(buffer.toByteArray());
    }

    /**
     * reads and returns the contents of a storage box from the output stream. Upon failure, return null and the stream
     * is skipped to the byte right after this box.
     * @param in       the input stream
     * @param expected the expected class of the contents
     * @return the contents of this storage box
     * @throws IOException only if the mandatory operations of this box fail. No guarantee can be made about the state
     *                     of the input stream when this is thrown.
     */
    public static <T> T readSafe(ObjectInput in, Class<T> expected) throws IOException {
        int nrOfBytes = in.readInt();
        // first try reading
        byte[] bytes = new byte[nrOfBytes];
        int nrRead = in.read(bytes);
        if (nrRead < nrOfBytes) throw new IOException("Input did not contain the entire box");

        try {
            // then parse the read bytes
            ObjectInputStream ins = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object o = ins.readObject();
            return expected.cast(o);

        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
