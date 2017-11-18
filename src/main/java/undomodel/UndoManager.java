package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UndoManager implements Serializable {

    public final int VER = 1;
    public final int DATA_VER;
    private final UndoStack stack;
    private final Map<String, String> extras = new TreeMap<>();

    /**
     * 1. Object -> byte[]
     * 1.1. if(zip): byte[] -> byte[] zipped
     * 2. byte[] -> Base64 string
     *
     * @param manager
     * @param doZip
     * @return
     * @throws IOException
     */
    public static String serialize(@NotNull UndoManager manager, boolean doZip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(manager);
        }

        if (doZip) {
            ByteArrayOutputStream z_baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(z_baos)) {
                gzip.write(baos.toByteArray());
            }
            baos = z_baos;
        }
        return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * 1. Base64 string -> byte[]
     * 1.1. if(zip): byte[] -> byte[] unzipped
     * 2. byte[] -> Object
     *
     * @param str
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static UndoManager deserialize(@NotNull String str) throws IOException, ClassNotFoundException {

        byte[] data = Base64.getUrlDecoder().decode(str);
        boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (UndoManager) ois.readObject();
        }
    }

    public UndoManager(int dataVersion, @NotNull UndoStack stack) {
        this.DATA_VER = dataVersion;
        this.stack = stack;
    }

    public Serializable getStack() {
        return stack;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoManager manager = (UndoManager) o;
        return VER == manager.VER &&
                DATA_VER == manager.DATA_VER &&
                Objects.equals(stack, manager.stack) &&
                Objects.equals(extras, manager.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(VER, DATA_VER, stack, extras);
    }
}
