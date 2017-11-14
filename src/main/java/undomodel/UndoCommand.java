package undomodel;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

abstract public class UndoCommand implements Serializable{

    public void undo() {
        doUndo();
    }

    public void redo() {
        doRedo();
    }

    abstract protected void doUndo();

    abstract protected void doRedo();

    public int id() {
        return -1;
    }

}
