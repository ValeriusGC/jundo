package model;

import undomodel.UndoGetter;
import undomodel.UndoSetter;
import undomodel.UndoCommand;

public class UndoPointCommand<V> extends UndoCommand<Point, V> {

    public UndoPointCommand() {
        super();
    }

    public UndoPointCommand(String text, UndoCommand parent, Point object, String fieldName, V newValue)
            throws NoSuchFieldException, IllegalAccessException {
        super(text, parent, object, fieldName, newValue);
    }

    public UndoPointCommand(String text, UndoCommand parent, Point object, UndoGetter<V> getter, UndoSetter<V> setter, V newValue)
            throws NoSuchFieldException, IllegalAccessException {
        super(text, parent, object, getter, setter, newValue);
    }

    @Override
    public void undo() throws IllegalAccessException {
        doUndo();
    }

    @Override
    public void redo() throws IllegalAccessException {
        doRedo();
    }
}
