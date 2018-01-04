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
 *     <li>id allows to save an unique identifier of stack's subject</li>
 *     <li>version can be very useful when saved version and new version of object are not equal so migration needed.</li>
 *     <li>The map "extras" allows to save other extra parameters in the 'key-value' form</li>
 * </ul>
 */
public class UndoSerializer2 implements Serializable {

    // TODO: 04.01.18 Требует перевода!
    /**
     *   В случае объекта который не имеет маркера {@link Serializable}
     */
    public interface OnConvertSubj {
        String toStr(@NotNull Object subj);
    }

    public interface OnExtractSubj {
        Object toSubj(@NotNull String subjAsString);
    }


    public static class Info implements Serializable{
        public final String id;
        public final int version;
        public final Class clazz;
        public final Map<String, Serializable> extras = new TreeMap<>();

        public Info(String id, int version, Class clazz) {
            this.id = id;
            this.version = version;
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Info info = (Info) o;
            return version == info.version &&
                    Objects.equals(id, info.id) &&
                    Objects.equals(clazz, info.clazz) &&
                    Objects.equals(extras, info.extras);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version, clazz, extras);
        }
    }

    private static class InnerStruct implements Serializable{
        UndoStack2 stack;
        Serializable subj;
        Info info;
    }

    private transient UndoStack2 stack;

    public final Info info;

    /**
     * Serializes object to Base64 string.
     * @param obj object to serialize.
     * @param doZip flag for gzipping.
     * @return Object as base64 string.
     * @throws IOException when something goes wrong.
     */
    public static String serialize(@NotNull UndoSerializer2 obj, boolean doZip,
                                   OnConvertSubj onConvertSubj) throws IOException {

        InnerStruct innerStruct = new InnerStruct();
        innerStruct.stack = obj.stack;
        Object subj = innerStruct.stack.getSubj();
        if(!(subj instanceof Serializable)) {
            if(onConvertSubj == null) {
                throw new IOException("need Serializable");
            }else {
               innerStruct.subj = onConvertSubj.toStr(subj);
            }
        }else {
            innerStruct.subj = (Serializable) subj;
        }
        innerStruct.info = obj.info;
        innerStruct.stack.contexts.clear();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(innerStruct);
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
    public static UndoSerializer2 deserialize(@NotNull String base64,
                                              OnExtractSubj onExtractSubj) throws IOException, ClassNotFoundException {
        final byte[] data = Base64.getUrlDecoder().decode(base64);
        final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {

            InnerStruct struct = (InnerStruct)ois.readObject();
            Info info = struct.info;
            UndoStack2 stack = struct.stack;
            if(struct.subj.getClass() == info.clazz){
                stack.setSubj(struct.subj);
            } else if(struct.subj instanceof String && onExtractSubj != null) {
                Object subj = onExtractSubj.toSubj((String) struct.subj);
                stack.setSubj(subj);
            }
            UndoSerializer2 obj = new UndoSerializer2(info.id, info.version, stack);
            return obj;
        }
    }

    /**
     * Makes object with specific parameters.
     * @param id unique identifier allowing recognize subject on the deserializing side.
     * @param version version of subject for correct restoring in the possible case of object migration.
     * @param stack stack itself.
     */
    public UndoSerializer2(String id, int version, @NotNull UndoStack2 stack) {
        info = new Info(id, version, stack.getSubj().getClass());
        this.stack = stack;
    }

    /**
     * @return Saved stack.
     */
    public UndoStack2 getStack() {
        return stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoSerializer2 that = (UndoSerializer2) o;
        return Objects.equals(stack, that.stack) &&
                Objects.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, info);
    }
}