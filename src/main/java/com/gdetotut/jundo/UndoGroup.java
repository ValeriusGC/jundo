package com.gdetotut.jundo;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UndoGroup class is a group of UndoStack objects.
 * <p>An application often has multiple undo stacks, one for each opened document. At the same time,
 * an application usually has one undo action and one redo action, which triggers undo or redo in the active document.</p>
 * <p>UndoGroup is a group of UndoStack objects, one of which may be active. It has an undo() and redo() methods,
 * which calls UndoStack::undo() and UndoStack::redo() for the active stack.</p>
 * <p>Stacks are added to a group with add() and removed with remove(). A stack is implicitly added to a group
 * when it is created with the group as its parent.</p>
 * <p><b>UndoGroup doesn't allow to add 2 stacks with the same subject (compares by address) because
 * it is logical violation.</b></p>
 * <p>It is the programmer's responsibility to specify which stack is active by calling UndoStack::setActive(),
 * usually when the associated document window receives focus. The active stack may also be set with setActiveStack(),
 * and is returned by getActive().</p>
 */
public class UndoGroup implements Serializable {

    private UndoStack active;
    private final List<UndoStack> stacks = new ArrayList<>();

    /**
     * Use this method instead of destructor.
     * Ensure all UndoStacks no longer refer to this group when it's time to do it.
     */
    public void clear() {
        for (UndoStack stack : stacks) {
            stack.group = null;
        }
        stacks.clear();
    }

    /**
     * Adds {@link UndoStack} to this group.
     *
     * @param stack stack to be added
     */
    public void add(@NotNull UndoStack stack) {
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
     *
     * @param stack stack to be removed
     */
    public void remove(@NotNull UndoStack stack) {
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
     *
     * @return stack list.
     */
    public List<UndoStack> getStacks() {
        return stacks;
    }

    /**
     * Sets the active stack of this group to stack.
     * <p>If the stack is not a member of this group, this function does nothing.
     * Synonymous with calling UndoStack.setActive() on stack.
     *
     * @param stack stack to make active or null
     */
    public void setActive(UndoStack stack) {
        if (active == stack) {
            return;
        }
        active = stack;
    }

    /**
     * Returns the active stack of this group.
     * <p>If none of the stacks are active, or if the group is empty, this function returns null.
     *
     * @return active stack or null.
     */
    public UndoStack getActive() {
        return active;
    }

    /**
     * Calls UndoStack.undo() on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void undo() {
        if (active != null) {
            active.undo();
        }
    }

    /**
     * Calls UndoStack.redo() on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void redo() {
        if (active != null) {
            active.redo();
        }
    }

    /**
     * Returns the value of the active stack's UndoStack.canUndo().
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     *
     * @return UndoStack.canUndo() for active stack or false.
     */
    public boolean canUndo() {
        return active != null && active.canUndo();
    }

    /**
     * Returns the value of the active stack's UndoStack.canRedo().
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     *
     * @return UndoStack.canRedo() for active stack or false.
     */
    public boolean canRedo() {
        return active != null && active.canRedo();
    }

    /**
     * Returns the value of the active stack's UndoStack.undoText().
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     *
     * @return the value of the active stack's UndoStack.undoText() or empty string.
     */
    public String undoText() {
        return active != null ? active.undoText() : "";
    }

    /**
     * Returns the value of the active stack's UndoStack.redoText().
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     *
     * @return the value of the active stack's UndoStack.redoText() or empty string.
     */
    public String redoText() {
        return active != null ? active.redoText() : "";
    }

    /**
     * Returns the value of the active stack's UndoStack.isClean().
     * <p>If none of the stacks are active, or if the group is empty, this function returns true.
     *
     * @return the value of the active stack's UndoStack.isClean() or true.
     */
    public boolean isClean() {
        return active == null || active.isClean();
    }

}
