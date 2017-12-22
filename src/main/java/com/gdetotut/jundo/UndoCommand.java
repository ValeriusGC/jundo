package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UndoCommand class is the base class of all commands stored on an UndoStack.
 */
public class UndoCommand implements Serializable {

    private String caption;
    List<UndoCommand> childLst;

    /**
     * Constructs an UndoCommand object with the given text.
     * @param caption caption for the command.
     * @param parent possible parent.
     */
    public UndoCommand(@NotNull String caption, UndoCommand parent) {
        setCaption(caption);
        if(parent != null) {
            if(parent.childLst == null) {
                parent.childLst = new ArrayList<>();
            }
            parent.childLst.add(this);
        }
    }

    /**
     * Returns the ID of this command.
     * <p>A command ID is used in command compression. It must be an integer unique to this command's class,
     * or -1 if the command doesn't support compression.
     * <p>If the command supports compression this function must be overridden in the derived class to return the correct ID.
     * The base implementation returns -1.
     * <p>{@link UndoStack#push}  will only try to merge two commands if they have the same ID, and the ID is not -1.
     *
     * @return Integer unique value to this command's class or -1 if the command doesn't support compression.
     */
    public int id() {
        return -1;
    }

    /**
     * Attempts to merge this command with cmd. Returns true on success; otherwise returns false.
     * <p>If this function returns true, calling this command's redo() must have the same effect as redoing
     * both this command and cmd.
     * <p>Similarly, calling this command's undo() must have the same effect as undoing cmd and this command.
     * <p>UndoStack will only try to merge two commands if they have the same id, and the id is not -1.
     * <p>The default implementation returns false.
     *
     * @param cmd command to try merge with.
     * @return True on success; otherwise returns false.
     */
    public boolean mergeWith(@NotNull UndoCommand cmd) {
        return false;
    }

    /**
     * @return If child commands exist returns their count otherwise returns zero.
     */
    public int childCount() {
        return childLst != null ? childLst.size() : 0;
    }

    /**
     *
     * @param idx Index of desired command.
     * @return Command if index is valid or null.
     */
    public UndoCommand child(int idx) {
        if(idx < 0 || idx >= childCount()) {
            return null;
        }
        return childLst.get(idx);
    }

    /**
     * Calls {@link #doRedo} in derived classes.
     */
    public final void redo() {
        if(null != childLst && childLst.size() > 0) {
            for (UndoCommand cmd : childLst) {
                cmd.redo();
            }
        }else {
            doRedo();
        }
    }

    /**
     * Calls {@link #doUndo}  in derived classes.
     */
    public final void undo() {
        if(null != childLst && childLst.size() > 0) {
            for (UndoCommand cmd : childLst) {
                cmd.undo();
            }
        }else {
            doUndo();
        }
    }

    /**
     * @return A short text string describing what this command does.
     */
    @NotNull
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the command's text to be the {@link #caption}  specified.
     * <p>The specified text should be a short user-readable string describing what this  command does.
     * @param caption A short text string describing what this command does.
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Applies a change to the document. This function must be implemented in the derived class.
     * <p>Calling {@link UndoStack#push}, {@link UndoStack#undo} or {@link UndoStack#redo} from this function
     * leads to  undefined behavior.
     */
    protected void doRedo() { }

    /**
     * Reverts a change to the document. After undo() is called, the state of the document should be the same
     * as before {@link #redo} was called. This function must be implemented in the derived class.
     * <p>Calling {@link UndoStack#push}, {@link UndoStack#undo} or {@link UndoStack#redo} from this function
     * leads to  undefined behavior.
     */
    protected void doUndo() { }

    @Override
    public String toString() {
        return "UndoCommand{" +
                "text='" + caption + '\'' +
                '}';
    }
}
