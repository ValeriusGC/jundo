package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The UndoSerializer class is responsible for correct serialization and deserialization of entire {@link UndoStack}.
 * <p>Stack encodes to Base64 using the <a href="#url">URL and Filename safe</a> type base64 encoding scheme.
 * <p>UndoSerializer has a number of useful properties to restore stack correctly:
 * <ul>
 *     <li>ID allows to save an unique identifier of stack's subject</li>
 *     <li>VERSION can be very useful when saved version and new version of object are not equal so migration needed.</li>
 *     <li>The map "extras" allows to save other extra parameters in the 'key-value' form</li>
 * </ul>
 */
public class UndoSerializer implements Serializable {

    public final String ID;
    public final int VERSION;
    private final UndoStack stack;
    private final Map<String, Serializable> extras = new TreeMap<>();

    /**
     * Serializes object to Base64 string.
     * @param obj object to serialize.
     * @param doZip flag for gzipping.
     * @return Object as base64 string.
     * @throws IOException when something goes wrong.
     */
    public static String serialize(@NotNull UndoSerializer obj, boolean doZip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }

        if (doZip) {
            final ByteArrayOutputStream zippedBaos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(zippedBaos)) {
                gzip.write(baos.toByteArray());
            }
            baos = zippedBaos;
        }
        return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Deserializes base64 string back to object.
     * @param base64 base64 string.
     * @return Object.
     * @throws IOException when something goes wrong.
     * @throws ClassNotFoundException when something goes wrong.
     */
    public static UndoSerializer deserialize(@NotNull String base64) throws IOException, ClassNotFoundException {
        final byte[] data = Base64.getUrlDecoder().decode(base64);
        final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (UndoSerializer) ois.readObject();
        }
    }

    /**
     * Makes object with specific parameters.
     * @param id unique identifier allowing recognize subject on the deserializing side.
     * @param version version of subject for correct restoring in the possible case of object migration.
     * @param stack stack itself.
     */
    public UndoSerializer(String id, int version, @NotNull UndoStack stack) {
        this.ID = id;
        this.VERSION = version;
        this.stack = stack;
    }

    /**
     * @return Saved stack.
     */
    public UndoStack getStack() {
        return stack;
    }

    /**
     * @return Extra parameters in the 'key-value' form.
     */
    public Map<String, Serializable> getExtras() {
        return extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoSerializer that = (UndoSerializer) o;
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
