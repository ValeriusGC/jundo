package undomodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndoStackT<T extends UndoSubject> extends UndoStack {

    public UndoStackT(@NotNull T subject, UndoGroup group) {
        super(subject, group);
        System.out.println("UndoStackT: " + subject.getClass().getCanonicalName());
    }

    @SuppressWarnings("unchecked")
    public T getSubject() {
        System.out.println("getSubject: " + super.getSubject().getClass().getCanonicalName());
        return (T)super.getSubject();
    }

}
