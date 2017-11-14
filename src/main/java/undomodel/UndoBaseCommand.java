package undomodel;

import com.sun.istack.internal.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;


public class UndoBaseCommand {

    /**
     *
     */
    private int id = -1;

    private final String text;
    private final UndoBaseCommand parent;

    public UndoBaseCommand() {
        this.text = null;
        this.parent = null;
    }


    public UndoBaseCommand(@Nullable String text, @Nullable UndoBaseCommand parent) {
        this.text = text;
        this.parent = parent;
    }

    public void undo() throws IllegalAccessException {

    }

    public void redo() throws IllegalAccessException {

    }

    public String text() {
        return text;
    }

    public UndoBaseCommand parent() {
        return parent;
    }

}
