package model;

import undomodel.UndoCommand;

import java.lang.reflect.Field;

public class UndoFieldCommand<T, V extends Object> extends UndoCommand {

    private final String fieldName;
    private final T object;
    private final Field f;
    private final V oldValue;
    private final V newValue;

    public UndoFieldCommand(UndoCommand parent, T object, String fieldName, V newValue) throws NoSuchFieldException, IllegalAccessException {
        super(parent);
        this.object = object;
        this.fieldName = fieldName;
        f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        this.newValue = newValue;
        this.oldValue = (V)f.get(object);
    }

    public UndoFieldCommand(String text, UndoCommand parent, T object, String fieldName, V newValue) throws NoSuchFieldException, IllegalAccessException {
        super(text, parent);
        this.object = object;
        this.fieldName = fieldName;
        f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        this.newValue = newValue;
        this.oldValue = (V)f.get(object);
    }

    public void undo() throws IllegalAccessException {
        f.set(object, oldValue);
    }

    public void redo() throws IllegalAccessException {
        f.set(object, newValue);
    }
}
