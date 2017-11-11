package undomodel;

import com.sun.istack.internal.Nullable;
import lombok.NonNull;

abstract public class UndoCommand {

    /**
     *
     */
    private int id = -1;

    /**
     * @param parent for grouping
     */
    public UndoCommand(@Nullable UndoCommand parent) {
    }

    public UndoCommand(@NonNull String text, @Nullable UndoCommand parent) {
    }

    abstract public void undo() throws IllegalAccessException;

    abstract public void redo() throws IllegalAccessException;

    public String text() {
        return "";
    }


}
