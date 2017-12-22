package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Simple command with access via getter/setter references.
 * @param <V> the type of the referenced value.
 */
public final class RefCmd<V extends Serializable> extends UndoCommand {

    private final Setter<V> setter;
    private final V oldValue;
    private final V newValue;

    /**
     * Constructs object.
     * @param caption caption for the command.
     * @param getter a reference to getter-method for this value. Getter shouldn't has parameters
     *               and should return value of the V type.
     * @param setter a reference to setter-method for this value. Getter should has parameter
     *               of the V type and shouldn't return value.
     * @param newValue the value to set to.
     * @param parent command's parent. Used in the concept of 'command-chain'.  Optional.
     */
    public RefCmd(String caption, @NotNull Getter<V> getter, @NotNull Setter<V> setter, V newValue,
                  UndoCommand parent){
        super(caption, parent);
        this.setter = setter;
        this.oldValue = getter.get();
        this.newValue = newValue;
    }

    @Override
    protected void doUndo() {
        setter.set(oldValue);
    }

    @Override
    protected void doRedo() {
        setter.set(newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefCmd<?> that = (RefCmd<?>) o;
        return Objects.equals(oldValue, that.oldValue) &&
                Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public String toString() {
        return "RefCmd{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
