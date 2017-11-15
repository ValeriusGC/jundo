package undomodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndoStackT<T> extends UndoStack {

    private final T object;

    public UndoStackT(@NotNull T object, UndoGroup group) {
        super(group);
        this.object = object;
    }

    protected T doGetObject() {
        return object;
    }

}
