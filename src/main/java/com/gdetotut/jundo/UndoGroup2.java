package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UndoGroup class is a group of {@link UndoStack} objects.
 * <p>An application often has multiple undo stacks, one for each opened document. At the same time,
 * an application usually has one undo action and one redo action, which triggers undo or redo in the active document.</p>
 * <p>UndoGroup is a group of {@link UndoStack} objects, one of which may be active.
 * It has an {@link #undo} and {@link #redo} methods, which calls {@link UndoStack#undo} and {@link UndoStack#redo}
 * for the active stack.
 * <p>Stacks are added to a group with {@link #add} and removed with {@link #remove}.
 * A stack is implicitly added to a group when it is created with the group as its parent.
 * <p><b>UndoGroup doesn't allow to add 2 stacks with the same subject (compares by address) because
 * it is a logical violation.</b>
 * <p>It is the programmer's responsibility to specify which stack is active by calling {@link UndoStack#setActive},
 * usually when the associated document window receives focus. The active stack may also be set with {@link #setActive},
 * and is returned by {@link #getActive}.
 */
public class UndoGroup2 implements Serializable {

    private UndoStack2 active;
    private final List<UndoStack2> stacks = new ArrayList<>();

    /**
     * Use this method instead of destructor.
     * <p>Ensure all UndoStacks no longer refer to this group when it's time to do it.
     */
    public void clear() {
        for (UndoStack2 stack : stacks) {
            stack.group = null;
        }
        stacks.clear();
    }

    /**
     * Adds {@link UndoStack} to this group.
     * @param stack stack to be added.
     */
    public void add(@NotNull UndoStack2 stack) {
        if (stacks.contains(stack)) {
            return;
        }

        stacks.add(stack);
        if (null != stack.group) {
            stack.group.remove(stack);
        }
        stack.group = this;

    }

    /**
     * Removes stack from this group. If the stack was the active stack in the group,
     * the active stack becomes null.
     * @param stack stack to be removed.
     */
    public void remove(@NotNull UndoStack2 stack) {
        if (!stacks.remove(stack)) {
            return;
        }

        if (stack == active) {
            setActive(null);
        }
        stack.group = null;
    }

    /**
     * Returns a list of stacks in this group.
     * @return Stack list.
     */
    public List<UndoStack2> getStacks() {
        return stacks;
    }

    /**
     * Sets the active stack of this group to stack.
     * <p>If the stack is not a member of this group, this function does nothing.
     * Synonymous with calling {@link UndoStack#setActive} on stack.
     * @param stack stack to make active or null.
     */
    public void setActive(UndoStack2 stack) {
        if (active == stack) {
            return;
        }
        active = stack;
    }

    /**
     * Returns the active stack of this group.
     * <p>If none of the stacks are active, or if the group is empty, this function returns null.
     * @return active stack or null.
     */
    public UndoStack2 getActive() {
        return active;
    }

    /**
     * Calls {@link UndoStack#undo} on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void undo() {
        if (active != null) {
            active.undo();
        }
    }

    /**
     * Calls {@link UndoStack#redo} on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void redo() {
        if (active != null) {
            active.redo();
        }
    }

    /**
     * @return The value of the active stack's {@link UndoStack#canUndo}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     */
    public boolean canUndo() {
        return active != null && active.canUndo();
    }

    /**
     * @return The value of the active stack's {@link UndoStack#canRedo}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     */
    public boolean canRedo() {
        return active != null && active.canRedo();
    }

    /**
     * @return The value of the active stack's {@link UndoStack#undoCaption}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     */
    public String undoCaption() {
        return active != null ? active.undoCaption() : "";
    }

    /**
     * @return The value of the active stack's {@link UndoStack#redoCaption}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     */
    public String redoCaption() {
        return active != null ? active.redoCaption() : "";
    }

    /**
     * @return The value of the active stack's {@link UndoStack#isClean}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns true.
     */
    public boolean isClean() {
        return active == null || active.isClean();
    }

}
