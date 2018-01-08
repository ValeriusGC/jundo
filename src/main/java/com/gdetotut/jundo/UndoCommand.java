package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// TODO: 08.01.18 Правильные аннотации (javax.annotation?) https://habrahabr.ru/post/204518/ 

/**
 * The UndoCommand class is the base class of all commands stored on an {@link UndoStack}.
 */
public class UndoCommand implements Serializable {

    // TODO: 25.12.17 Убрать все toString() перед продакшном

    // TODO: 25.12.17 Покрытие тестами улучшить


    public static int NO_MERGING = -1;
    private String caption;
    List<UndoCommand> children;
    protected final UndoStack owner;

    /**
     * Constructs an UndoCommand object with the given caption.
     * @param caption a short string describing what this command does. Optional.
     * @param parent command's parent. Used in the concept of 'command-chain'.  Optional.
     */
    public UndoCommand(@NotNull UndoStack owner, String caption, UndoCommand parent) {

        this.owner = owner;

        setCaption(caption);
        if(null != parent) {
            if(null == parent.children) {
                parent.children = new ArrayList<>();
            }
            parent.children.add(this);
        }
    }

    /**
     * Returns the id of this command.
     * <p>A command id is used in the "command compression" concept. It must be an integer value
     * unique to this command's class, or {@link #NO_MERGING} if the command
     * doesn't support compression.
     * <p>If the command supports compression this function must be overridden in the derived class
     * to return the correct id.
     * The base implementation returns {@link #NO_MERGING}.
     * <p>{@link UndoStack#push} will only try to merge two commands if they have the same id,
     * and the id is not {@link #NO_MERGING}.
     *
     * @return Integer unique value to this command's class or {@link #NO_MERGING}
     * if the command doesn't support compression.
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
     * @param cmd command to try merge with.
     * @return True on success; otherwise returns false.
     */
    public boolean mergeWith(@NotNull UndoCommand cmd) {
        return false;
    }

    /**
     * @return If child commands exist returns their count; otherwise returns zero.
     */
    public int childCount() {
        return children != null ? children.size() : 0;
    }

    /**
     * Returns command by its index. If the index is invalid, returns null.
     * @param idx index of desired command.
     * @return Command if index is valid; otherwise null.
     */
    public UndoCommand child(int idx) {
        if(idx < 0 || idx >= childCount()) {
            return null;
        }
        return children.get(idx);
    }

    /**
     * Calls {@link #doRedo} in the derived classes.
     */
    public final void redo() {
        if(null != children && children.size() > 0) {
            for (UndoCommand cmd : children) {
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
        if(null != children && children.size() > 0) {
            for (UndoCommand cmd : children) {
                cmd.undo();
            }
        }else {
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
     * @param caption a short caption string describing what this command does.
     */
    public final void setCaption(String caption) {
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

}
