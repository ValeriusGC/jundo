package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class UndoOutputStream implements AutoCloseable {

    private ObjectOutputStream oos;
    private ByteArrayOutputStream baos;

    public UndoOutputStream() throws IOException {
        baos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(baos);
    }

    public void write(@NotNull Object object, @NotNull UndoStack undoStack) throws IOException {
        oos.reset();
        oos.writeObject(object);
        oos.writeObject(undoStack);
    }

    @Override
    public void close() throws Exception {
        oos.close();
    }
}
