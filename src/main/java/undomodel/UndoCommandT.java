package undomodel;

import com.sun.istack.internal.NotNull;

import java.util.Objects;

public class UndoCommandT<V> extends UndoCommand {

    private final UndoSetter<V> setter;
    private final V oldValue;
    private final V newValue;

    public UndoCommandT(@NotNull UndoGetter<V> getter, @NotNull UndoSetter<V> setter, V newValue)
            throws Exception {
        if(getter == null || setter == null) {
            throw new Exception("null parameters!");
        }
        this.setter = setter;
        this.oldValue = getter.get();
        this.newValue = newValue;
    }

    public void doUndo() {
        setter.set(oldValue);
    }

    public void doRedo() {
        setter.set(newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoCommandT<?> that = (UndoCommandT<?>) o;
        return Objects.equals(oldValue, that.oldValue) &&
                Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public String toString() {
        return "UndoCommandT{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
