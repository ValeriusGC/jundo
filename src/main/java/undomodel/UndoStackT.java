package undomodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndoStackT<T> extends UndoStack {

    private final T object;

    public UndoStackT(@Nullable String id, @NotNull T object) {
        super(id);
        this.object = object;
    }

    public T getObject() {
        return object;
    }

}
