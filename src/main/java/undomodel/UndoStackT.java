package undomodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndoStackT<T extends UndoSubject> extends UndoStack {

    public UndoStackT(@NotNull T subject, UndoGroup group) {
        super(subject, group);

    }

    @SuppressWarnings("unchecked")
    public T getSubject() {
        return (T)super.getSubject();
    }

}
