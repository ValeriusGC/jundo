package undomodel;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

abstract public class UndoCommand implements Serializable{

    private String text;
    //private final List<UndoCommand> childList = new ArrayList<>();


    /**
     * Constructs a UndoCommand object with the given text.
     *
     * @param text
     */
    public UndoCommand(String text) {
        setText(text);
    }

    /**
     * Returns the ID of this command.
     * <p>
     * A command ID is used in command compression. It must be an integer unique to this command's class,
     * or -1 if the command doesn't support compression.
     * <p>
     * If the command supports compression this function must be overridden in the derived class to return the correct ID.
     * The base implementation returns -1.
     * <p>
     * UndoStack.push() will only try to merge two commands if they have the same ID, and the ID is not -1.
     *
     * @return Integer unique to this command's class or -1 if the command doesn't support compression.
     */
    public int id() {
        return -1;
    }

    /**
     * Attempts to merge this command with cmd. Returns true on success; otherwise returns false.
     * <br/>
     * If this function returns true, calling this command's redo() must have the same effect as redoing
     * both this command and cmd.
     * <br/>
     * Similarly, calling this command's undo() must have the same effect as undoing cmd and this command.
     * <br/>
     * UndoStack will only try to merge two commands if they have the same id, and the id is not -1.
     * <br/>
     * The default implementation returns false.
     *
     * @param cmd Command to try merge with
     * @return True on success; otherwise returns false.
     */
    public boolean mergeWith(@NotNull UndoCommand cmd) {return false;}

    /**
     * Calls doRedo()  in derived classes.
     */
    public void redo() {
        doRedo();
    }

    /**
     * Calls doUndo()  in derived classes.
     */
    public void undo() { doUndo(); }

    /**
     * Returns a short text string describing what this command does.
     *
     * @return
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the command's text to be the \a text specified.
     * <br/>
     * The specified text should be a short user-readable string describing what this  command does.
     *
     * @param text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Applies a change to the document. This function must be implemented in the derived class.
     * Calling UndoStack.push(), UndoStack.undo() or UndoStack.redo() from this function leads to  undefined behavior.
     */
    abstract protected void doRedo();

    /**
     * Reverts a change to the document. After undo() is called, the state of the document should be the same
     * as before redo() was called. This function must be implemented in the derived class.
     * Calling UndoStack::push(), UndoStack::undo() or UndoStack::redo() from this function leads to undefined behavior.
     */
    abstract protected void doUndo();

    @Override
    public String toString() {
        return "UndoCommand{" +
                "text='" + text + '\'' +
                '}';
    }
}
