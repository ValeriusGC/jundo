package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * @param <V> generic type of value.
 */
public class FuncCommand<V extends Serializable> extends UndoCommand {

    private final Setter<V> setter;
    private final V oldValue;
    private final V newValue;

    public FuncCommand(String text, @NotNull Getter<V> getter, @NotNull Setter<V> setter, V newValue,
                       UndoCommand parent){
        super(text, parent);
        this.setter = setter;
        this.oldValue = getter.get();
        this.newValue = newValue;
    }

    @Override
    public void doUndo() {
        setter.set(oldValue);
    }

    @Override
    public void doRedo() {
        setter.set(newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncCommand<?> that = (FuncCommand<?>) o;
        return Objects.equals(oldValue, that.oldValue) &&
                Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public String toString() {
        return "FuncCommand{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
