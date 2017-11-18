package undomodel;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>Main characteristic of {@link UndoStack} is that two different stack should not share one subject.</b>
 * <br/>
 *  Otherwise {@link UndoGroup} may not add them both, and there will be collision when undoing one subject
 *  via different stacks.
 */
public class UndoStack implements Serializable{

    UndoGroup group;
    private final Serializable subject;
    private int idx;
    private int cleanIdx;
    private List<UndoCommand> cmdLst = new ArrayList<>();
    private int undoLimit;

    /**
     *
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If parent is not a null the stack is automatically added to the group.
     * @param group possible group for this {@link UndoStack}
     */
    public UndoStack(@NotNull Serializable subject, UndoGroup group) {
        this.subject = subject;
        if(null != group) {
            group.add(this);
        }
    }

    /**
     * Clears the command stack by deleting all commands on it, and returns the stack to the clean state.
     * <p></p>
     * Commands are not undone or redone; the state of the edited object remains  unchanged.
     * <p></p>
     * This function is usually used when the contents of the document are abandoned.
     */
    public void clear() {
        if(cmdLst.isEmpty()) {
            return;
        }

        boolean wasClean = isClean();
        cmdLst.clear();
        idx = 0;
        cleanIdx = 0;
    }

    /**
     * Pushes cmd on the stack or merges it with the most recently executed command.
     * In either case, executes cmd by calling its redo() function.
     * <p></p>
     * If cmd's id is not -1, and if the id is the same as that of the
     * most recently executed command, UndoStack will attempt to merge the two
     * commands by calling UndoCommand.mergeWith() on the most recently executed
     * command. If UndoCommand.mergeWith() returns true, cmd is deleted.
     * <p>
     * In all other cases cmd is simply pushed on the stack.
     * <p>
     * If commands were undone before cmd was pushed, the current command and
     * all commands above it are deleted. Hence cmd always ends up being the top-most on the stack.
     * <p>
     * Once a command is pushed, the stack takes ownership of it. There
     * are no getters to return the command, since modifying it after it has
     * been executed will almost always lead to corruption of the document's state.
     * @param cmd new command to execute.
     * @throws Exception
     */
    public void push(@NotNull UndoCommand cmd) throws Exception {

        // FOA
        cmd.redo();

        UndoCommand cur = null;
        if(idx > 0) {
            cur = cmdLst.get(idx - 1);
        }
        while (idx < cmdLst.size()) {
            cmdLst.remove(cmdLst.size() - 1);
        }
        if(cleanIdx > idx){
            cleanIdx = -1;
        }

        boolean canMerge = cur != null
                && cur.id() != -1
                && cur.id() == cmd.id();
        if(!(canMerge && cur.mergeWith(cmd))){
            // And last actions
            cmdLst.add(cmd);
            checkUndoLimit();
            setIndex(idx + 1, false);
        }
    }

    /**
     * Marks the stack as clean and emits cleanChanged() if the stack was not already clean.
     * <p>
     * Whenever the stack returns to this state through the use of undo/redo commands,
     * it emits the signal cleanChanged(). This signal is also emitted when the stack leaves the clean state.
     */
    public void setClean() {
        setIndex(idx, true);
    }

    /**
     * If the stack is in the clean state, returns true; otherwise returns false.
     * @return Clean state.
     */
    public boolean isClean() {
        return cleanIdx == idx;
    }

    /**
     * Returns the clean index. This is the index at which setClean() was called.
     * <p>
     * A stack may not have a clean index. This happens if a document is saved, some commands are undone,
     * then a new command is pushed. Since push() deletes all the undone commands before pushing
     * the new command, the stack can't return to the clean state again.
     * In this case, this function returns -1.
     *
     * @return Clean index
     */
    public int getCleanIdx() {
        return cleanIdx;
    }

    /**
     * Undoes the command below the current command by calling UndoCommand.undo().
     * Decrements the current command index.
     * <p>
     * If the stack is empty, or if the bottom command on the stack has already been undone, this function does nothing.
     */
    public void undo() {
        if (idx == 0) {
            return;
        }
        int idx = this.idx - 1;
        cmdLst.get(idx).undo();
        setIndex(idx, false);
    }

    /**
     * Redoes the current command by calling UndoCommand.redo(). Increments the current command index.
     * <p>
     * If the stack is empty, or if the top command on the stack has already been redone, this function does nothing.
     */
    public void redo() {
        if (idx == cmdLst.size()) {
            return;
        }

        cmdLst.get(idx).redo();
        setIndex(idx + 1, false);
    }

    /**
     * Returns the number of commands on the stack.
     * @return The number of commands on the stack.
     */
    public int count() {
        return cmdLst.size();
    }

    /**
     * Returns the index of the current command. This is the command that will be executed on the next call to redo().
     * It is not always the top-most command on the stack, since a number of commands may have been undone.
     * @return The index of the current command.
     */
    public int getIdx() {
        return idx;
    }

    /**
     * Repeatedly calls undo() or redo() until the current command index reaches idx.
     * This function can be used to roll the state of the document forwards of backwards.
     * <p>
     * indexChanged() is emitted only once.
     * @param idx Index to achieve.
     */
    public void setIndex(int idx) {
        if(idx < 0) {
            idx = 0;
        }else if(idx > cmdLst.size()){
            idx = cmdLst.size();
        }

        int i = this.idx;
        while (i < idx) {
            cmdLst.get(i++).redo();
        }
        while (i > idx){
            cmdLst.get(--i).undo();
        }

        setIndex(idx, false);
    }

    /**
     * Returns true if there is a command available for undo; otherwise returns false.
     * <p>
     * This function returns false if the stack is empty, or if the bottom command on the stack has already been undone.
     * <p>
     * Synonymous with index() == 0.
     *
     * @return If there is a command available for undo.
     */
    public boolean canUndo() {
        return idx > 0;
    }

    /**
     * Returns true if there is a command available for redo; otherwise returns false.
     * <p>
     * This function returns false if the stack is empty or if the top command on the stack has already been redone.
     * <p>
     * Synonymous with index() == count().
     *
     * @return If there is a command available for redo.
     */
    public boolean canRedo() {
        return idx < cmdLst.size();
    }

    /**
     * Returns the text of the command which will be undone in the next call to undo().
     *
     * @return The text of the command which will be undone in the next call to undo().
     */
    public String undoText() {
        return idx > 0 ? cmdLst.get(idx - 1).getText() : "";
    }

    /**
     * Returns the text of the command which will be redone in the next call to redo().
     *
     * @return The text of the command which will be redone in the next call to redo().
     */
    public String redoText() {
        return idx < cmdLst.size() ? cmdLst.get(idx).getText() : "";
    }

    /**
     * Returns a reference to the command at idx.
     * <p>
     * Be aware to modify it because modifying a command, once it has been pushed onto the stack and executed,
     * almost always causes corruption of the state of the document, if the command is later undone or redone.
     *
     * @param idx The index of command has been retrieved.
     * @return Command or null.
     */
    public UndoCommand getCommand(int idx) {
        if(idx < 0 || idx >= cmdLst.size()) {
            return null;
        }
        return cmdLst.get(idx);
    }

    /**
     * Returns the text of the command at index idx.
     *
     * @param idx The index of command's text has been retrieved.
     * @return Text or empty string.
     */
    public String text(int idx) {
        if(idx < 0 || idx >= cmdLst.size()) {
            return "";
        }
        return cmdLst.get(idx).getText();
    }

    /**
     * When the number of commands on a stack exceedes the stack's undoLimit, commands are deleted from the bottom
     * of the stack. The default value is 0, which means that there is no limit.
     * <p>
     * This property may only be set when the undo stack is empty, since setting it on a non-empty stack
     * might delete the command at the current index. Calling setUndoLimit()on a non-empty stack prints a warning
     * and does nothing.
     *
     * @param value New undo limit.
     */
    public void setUndoLimit(int value) {
        if(cmdLst.size() > 0) {
            System.err.println("UndoStack.setUndoLimit(): an undo limit can only be set when the stack is empty");
        }

        if(value == undoLimit) {
            return;
        }
        undoLimit = value;
        checkUndoLimit();
    }

    /**
     * @return Undo limit.
     */
    public int getUndoLimit() {
        return undoLimit;
    }

    /**
     * @return True if this UndoStack not in group or active in group, otherwise false.
     */
    public boolean isActive() {
        return group == null || group.getActive() == this;
    }

    /**
     * An application often has multiple undo stacks, one for each opened document. The active stack is
     * the one associated with the currently active document. If the stack belongs to a UndoGroup,
     * calls to UndoGroup.undo() or UndoGroup.redo() will be forwarded to this stack when it is active.
     * If the stack does not belong to a UndoGroup, making it active has no effect.
     * <p>
     * It is the programmer's responsibility to specify which stack is active by calling setActive(),
     * usually when the associated document window receives focus.
     *
     * @param active Setting this UndoStack active in its group if it exists.
     */
    public void setActive(boolean active) {
        if(group != null) {
            if(active) {
                group.setActive(this);
            }else if(group.getActive() == this) {
                group.setActive(null);
            }
        }
    }

    /**
     * Used to get object via its base class
     *
     * @return Object via calling descendants real object.
     */
    public Serializable getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "UndoStack{" +
                "idx=" + idx +
                ", cmdLst=" + cmdLst +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoStack stack = (UndoStack) o;
        return idx == stack.idx &&
                subject.equals(((UndoStack) o).subject) &&
                Objects.equals(cmdLst, stack.cmdLst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx, subject, cmdLst);
    }

    /**
     * Sets the current index to idx, emitting appropriate signals. If clean is true,
     * makes idx the clean index as well.
     *
     * @param index Index to achieve.
     * @param clean Flag to set/unset clean state
     */
    private void setIndex(int index, boolean clean) {

        boolean wasClean = idx == cleanIdx;

        if (this.idx != index) {
            this.idx = index;
        }

        if(clean) {
            cleanIdx = idx;
        }

        boolean isClean = idx == cleanIdx;
    }

    /**
     * If the number of commands on the stack exceedes the undo limit, deletes commands from the bottom of the stack.
     */
    private void checkUndoLimit() {

        if(undoLimit <= 0 || undoLimit >= cmdLst.size()) {
            return;
        }

        int delCnt = cmdLst.size() - undoLimit;
        for(int i = 0; i < delCnt; ++i) {
            cmdLst.remove(0);
        }

        idx -= delCnt;
        if(cleanIdx != -1) {
            if(cleanIdx < delCnt) {
                cleanIdx = -1;
            }else {
                cleanIdx -= delCnt;
            }
        }
    }

}
