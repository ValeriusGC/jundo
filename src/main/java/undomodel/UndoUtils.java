package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class UndoUtils {

    public static <T extends Serializable> String serialize(T object) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        }
    }


    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }


//    @NotNull
//    public static ObjectOutputStream streamForObjects() throws IOException {
//        return new ObjectOutputStream(new ByteArrayOutputStream());
//    }
//
//    public static <T extends Serializable> String serialize(@NotNull byte[] bytes) {
//        return Base64.getUrlEncoder().encodeToString(bytes);
//    }
//
//    @NotNull
//    public static ObjectInputStream streamWithObjects(@NotNull String data) throws IOException {
//        byte[] dataBytes = Base64.getUrlDecoder().decode(data);
//        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataBytes);
//        final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
//        return objectInputStream;
//    }

    @SuppressWarnings({"unchecked"})
    public static <T extends Serializable> T deserialize(String data) throws ClassNotFoundException, IOException {
        byte[] dataBytes = Base64.getUrlDecoder().decode(data);
        try(final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataBytes))) {
            return (T) ois.readObject();
        }
    }

}
