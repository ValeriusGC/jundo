package undomodel;

import com.sun.istack.internal.Nullable;
import lombok.NonNull;

import java.lang.reflect.InvocationTargetException;


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

    abstract public void undo() throws IllegalAccessException, InvocationTargetException;

    abstract public void redo() throws IllegalAccessException, InvocationTargetException;

    public String text() {
        return "";
    }


}
