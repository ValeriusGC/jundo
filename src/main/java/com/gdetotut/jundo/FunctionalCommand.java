package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @param <V> generic type for this class.
 */
public class FunctionalCommand<V extends Serializable> extends UndoCommand {

    private final Setter<V> setter;
    private final V oldValue;
    private final V newValue;

    public FunctionalCommand(String text, @NotNull Getter<V> getter, @NotNull Setter<V> setter, V newValue, UndoCommand parent)
            throws Exception {
        super(text, parent);
        this.setter = setter;
        this.oldValue = getter.get();
        this.newValue = newValue;
    }

    @Override
    public <Context> void doUndo(final Context context) {
        setter.set(oldValue);
    }

    @Override
    public <Context> void doRedo(final Context context) {
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
