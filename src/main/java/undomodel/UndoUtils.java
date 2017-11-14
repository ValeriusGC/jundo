package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;

public class UndoUtils {

    public static <T extends Serializable> String serialize(T object) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            return Base64.getUrlEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new Error(e);
        }
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

    public static <T extends Serializable> T deserialize(String data) throws ClassNotFoundException, IOException {
        byte[] dataBytes = Base64.getUrlDecoder().decode(data);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataBytes);
        final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        @SuppressWarnings({"unchecked"}) final T obj = (T) objectInputStream.readObject();

        objectInputStream.close();
        return obj;
    }

}
