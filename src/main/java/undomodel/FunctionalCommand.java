package undomodel;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 *
 * @param <V>
 */
public class FunctionalCommand<V extends java.io.Serializable> extends UndoCommand {

    private final Setter<V> setter;
    private final V oldValue;
    private final V newValue;

    public FunctionalCommand(String text, @NotNull Getter<V> getter, @NotNull Setter<V> setter, V newValue, UndoCommand parent)
            throws Exception {
        super(text, parent);
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
        FunctionalCommand<?> that = (FunctionalCommand<?>) o;
        return Objects.equals(oldValue, that.oldValue) &&
                Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public String toString() {
        return "FunctionalCommand{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
