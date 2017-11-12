package undomodel;

import com.sun.istack.internal.Nullable;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;


abstract public class UndoBaseCommand {

    /**
     *
     */
    private int id = -1;

    private final String text;
    private final UndoBaseCommand parent;

    public UndoBaseCommand(@Nullable String text, @Nullable UndoBaseCommand parent) {
        this.text = text;
        this.parent = parent;
    }

    abstract public void undo() throws IllegalAccessException;

    abstract public void redo() throws IllegalAccessException;

    public String text() {
        return text;
    }

    public UndoBaseCommand parent() {
        return parent;
    }

}
