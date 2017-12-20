package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The UndoManager class is responsible for correct serialization and deserialization of entire UndoStack.
 * <p>Stack encodes to Base64 using the <a href="#url">URL and Filename safe</a> type base64 encoding scheme.</p>
 * <p>UndoManager has a number of useful properties to restore stack correctly:</p>
 * <ul>
 *     <li>ID alows to save an unique identifier of stack's subject</li>
 *     <li>VERSION can be very useful when saved version and new version of object are not equal so migration needed.</li>
 *     <li>The map "extras" allows to save other extra parameters in the key-value form</li>
 * </ul>
 */
public class UndoManager implements Serializable {

    public final String ID;
    public final int VERSION;
    private final UndoStack stack;
    private final Map<String, String> extras = new TreeMap<>();

    /**
     * Serializes manager to Base64 string.
     * @param manager manager to aux
     * @param doZip flag for gzipping
     * @return manager as base64 string
     * @throws IOException when something goes wrong
     */
    public static String serialize(@NotNull UndoManager manager, boolean doZip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(manager);
        }

        if (doZip) {
            final ByteArrayOutputStream zippedBaos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(zippedBaos)) {
                gzip.write(baos.toByteArray());
            }
            baos = zippedBaos;
        }
        return Base64Copy.getUrlEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Deserialize base64 string back to manager
     * @param base64 base64 string
     * @return manager
     * @throws IOException when something goes wrong
     * @throws ClassNotFoundException when something goes wrong
     */
    public static <Context> UndoManager deserialize(@NotNull String base64, Context context) throws IOException, ClassNotFoundException {

        final byte[] data = Base64Copy.getUrlDecoder().decode(base64);
        final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {
            final UndoManager um = (UndoManager) ois.readObject();
            um.getStack().setContext(context);
            return um;
        }
    }

    /**
     *  Makes object with specific parameters.
     * @param id unique identifier allowing recognize subject on the deserializing side.
     * @param version version of subject for correct restoring in the possible case of object migration.
     * @param stack stack itself.
     */
    public UndoManager(String id, int version, @NotNull UndoStack stack) {
        this.ID = id;
        this.VERSION = version;
        this.stack = stack;
    }

    /**
     * @return saved stack.
     */
    public UndoStack getStack() {
        return stack;
    }

    /**
     * @return extra parameters in the form of key-value.
     */
    public Map<String, String> getExtras() {
        return extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoManager that = (UndoManager) o;
        return VERSION == that.VERSION &&
                Objects.equals(ID, that.ID) &&
                Objects.equals(getStack(), that.getStack()) &&
                Objects.equals(getExtras(), that.getExtras());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, VERSION, getStack(), getExtras());
    }
}
