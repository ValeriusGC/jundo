package com.gdetotut.jundo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UndoCommand class is the base class of all commands stored on an {@link UndoStack}.
 */
public class UndoCommand implements Serializable {

    /**
     * Default value for {@link #id}.
     */
    static int NO_MERGING = -1;

    /**
     * Command's caption. Identifies command in the list of ones. Optional.
     */
    private String caption;

    /**
     * Possible list of child commands for macros. Optional.
     */
    List<UndoCommand> children;

    /**
     * This command's owner. Via this reference the command can use local contexts.
     */
    private UndoStack owner = null;

    /**
     * Constructs an UndoCommand object with the given caption.
     * <p>
     * Not should be created standalone!
     *
     * @param caption a short string describing what this command does. Optional.
     */
    protected UndoCommand(String caption) {
        setCaption(caption);
    }

    /**
     * Setting an owner to the command.
     * <p>
     * Macros should not have an owner, while simple commands should.
     *
     * @param owner stack as an owner.
     * @return Object itself.
     */
    UndoCommand setOwner(UndoStack owner) {
        this.owner = owner;
        return this;
    }

    /**
     * @return The owner of object.
     */
    public UndoStack getOwner() {
        return owner;
    }

    /**
     * Returns the id of this command.
     * <p>A command id is used in the "command merging" concept. It must be an integer value
     * unique to this command's class, or {@link #NO_MERGING} if the command doesn't support merging.
     * <p>If the command supports merging this function must be overridden in the derived class
     * to return the correct id.
     * The base implementation returns {@link #NO_MERGING}.
     * <p>{@link UndoStack#push} will only try to merge two commands if they have the same id,
     * and the id is not {@link #NO_MERGING}.
     *
     * @return Integer unique value to this command's class or {@link #NO_MERGING}
     * if the command doesn't support merging.
     */
    public int id() {
        return NO_MERGING;
    }

    /**
     * Attempts to merge this command with cmd. Returns true on success; otherwise returns false.
     * <p>If this function returns true, calling this command's {@link #redo()} must have the same effect
     * as redoing both this command and cmd.
     * <p>Similarly, calling this command's {@link #undo()} must have the same effect
     * as undoing cmd and this command.
     * <p>UndoStack will only try to merge two commands if they have the same {@link #id}, and the id
     * is not {@link #NO_MERGING}.
     * <p>The default implementation returns false.
     *
     * @param cmd command to try merge with. Required.
     * @return True on success; otherwise returns false.
     */
    public boolean mergeWith(UndoCommand cmd) {
        if (cmd == null) {
            throw new NullPointerException("cmd");
        } else {
            return false;
        }
    }

    /**
     * @return If child commands exist returns their count; otherwise returns zero.
     */
    public int childCount() {
        return children != null ? children.size() : 0;
    }

    /**
     * Returns command by its index. If the index is invalid, returns null.
     *
     * @param idx index of desired command.
     * @return Command if index is valid; otherwise null.
     */
    public UndoCommand child(int idx) {
        if (idx < 0 || idx >= childCount()) {
            return null;
        }
        return children.get(idx);
    }

    /**
     * Adds cmd as a child in the children list.
     *
     * @param cmd command to be added. Required.
     */
    public void addChild(UndoCommand cmd) {
        if (cmd == null) {
            throw new NullPointerException("cmd");
        }

        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(cmd);
    }

    /**
     * If command has children calls their redo consistently; otherwise calls {@link #doRedo}.
     */
    public final void redo() {
        if (null != children && children.size() > 0) {
            for (UndoCommand cmd : children) {
                cmd.redo();
            }
        } else {
            doRedo();
        }
    }

    /**
     * If command has children calls their redo consistently; otherwise calls {@link #doUndo}.
     */
    public final void undo() {
        if (null != children && children.size() > 0) {
            for (int i = children.size() - 1; i > -1; --i) {
                children.get(i).undo();
            }
        } else {
            doUndo();
        }
    }

    /**
     * @return A short string describing what this command does.
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the command's caption.
     * <p>Specified caption should be a short user-readable string describing what this  command does.
     *
     * @param caption a short caption string describing what this command does. Optional.
     */
    public final void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Applies a change to the document. This function can be implemented in the derived class.
     * <p>Calling {@link UndoStack#push}, {@link UndoStack#undo} or {@link UndoStack#redo} from this function
     * leads to  undefined behavior.
     */
    protected void doRedo() {
    }

    /**
     * Reverts a change to the document. After undo() is called, the state of the document should be the same
     * as before {@link #redo} was called. This function can be implemented in the derived class.
     * <p>Calling {@link UndoStack#push}, {@link UndoStack#undo} or {@link UndoStack#redo} from this function
     * leads to  undefined behavior.
     */
    protected void doUndo() {
    }

}
