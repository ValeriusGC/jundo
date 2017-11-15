package undomodel;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

abstract public class UndoCommand implements Serializable{

    private String text;
    //private final List<UndoCommand> childList = new ArrayList<>();


    public UndoCommand(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int id() {
        return -1;
    }

    public boolean mergeWith(@NotNull UndoCommand cmd) {return false;}

    public void undo() { doUndo(); }

    public void redo() {
        doRedo();
    }

    abstract protected void doUndo();

    abstract protected void doRedo();

    @Override
    public String toString() {
        return "UndoCommand{" +
                "text='" + text + '\'' +
                '}';
    }
}
