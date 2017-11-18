package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class UndoStackT<T extends Serializable> extends UndoStack {

    public UndoStackT(@NotNull T subject, UndoGroup group) {
        super(subject, group);

    }

    @SuppressWarnings("unchecked")
    public T getSubject() {
        return (T)super.getSubject();
    }

}
