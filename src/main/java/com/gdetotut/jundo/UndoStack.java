package com.gdetotut.jundo;

import java.io.Serializable;
import java.util.*;

/**
 * <b>Main characteristic of {@link UndoStack} is that two different stacks should not share one subject.</b>
 * <p>Otherwise {@link UndoGroup} may not add them both, and there will be collision when undoing one subject
 * via different stacks.
 */
public class UndoStack implements Serializable{

    UndoGroup group;

    /**
     * Keeps the subject for whom {@link #commands} are behave.
     * <p>Required parameter.
     */
    private transient Object subj;
    private int idx;
    private int cleanIdx;
    private List<UndoCommand> commands;
    private List<UndoCommand> macroStack;
    private int undoLimit;
    private transient UndoWatcher watcher;

    private transient Map<String, Object> localContexts;

    // TODO: 07.01.18 Перевод!!!!!
    /**
     * Суспендер нужен для приостановки добавления команд в момент выполнения undo/redo.
     * В некоторых случаях только это может избавить от добавления "паразитных команд".
     */
    private boolean suspend = false;

    /**
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If group is not a null the stack is automatically added to the group.
     * @param subj for whom this stack was made. Can be null if no way to make it serializable. Required.
     * @param group possible group for this {@link UndoStack}.
     */
    public UndoStack(Object subj, UndoGroup group) {
        if (subj == null) {
            throw new NullPointerException("subj");
        } else {
            this.subj = subj;
            if (null != group) {
                group.add(this);
            }

        }
    }

    public Map<String, Object> getLocalContexts() {
        if(localContexts == null) {
            localContexts = new HashMap<>();
        }
        return localContexts;
    }

    /**
     * Clears the command stack by deleting all commands on it, and returns the stack to the clean state.
     * <p>Commands are not undone or redone; the state of the edited object remains  unchanged.
     * <p>This function is usually used when the contents of the document are abandoned.
     */
    public void clear() {
        if(commands == null || commands.isEmpty()) {
            return;
        }

        boolean wasClean = isClean();

        if(macroStack != null){
            macroStack.clear();
        }

        for (UndoCommand cmd : commands) {
            if(cmd.children != null) {
                cmd.children.clear();
            }
        }
        commands.clear();
        idx = 0;
        cleanIdx = 0;

        if(null != watcher){
            watcher.indexChanged(0);
            watcher.canUndoChanged(false);
            watcher.undoTextChanged("");
            watcher.canRedoChanged(false);
            watcher.redoTextChanged("");
            if(!wasClean){
                watcher.cleanChanged(true);
            }
        }
    }

    /**
     * Pushes cmd on the stack or merges it with the most recently executed command.
     * In either case, executes cmd by calling its {@link UndoCommand#redo} function.
     * <p>If cmd's id is not {@link UndoCommand#NO_MERGING}, and if the id is the same
     * as that of the most recently executed command, UndoStack will attempt to merge the two
     * commands by calling {@link UndoCommand#mergeWith} on the most recently executed
     * command. If {@link UndoCommand#mergeWith} returns true, cmd is deleted.
     * <p>In all other cases cmd is simply pushed on the stack.
     * <p>If commands were undone before cmd was pushed, the current command and
     * all commands above it are deleted. Hence cmd always ends up being the top-most on the stack.
     * <p>Once a command is pushed, the stack takes ownership of it.
     * There are no getters to return the command, since modifying it after it has
     * been executed will almost always lead to corruption of the document's state.
     * @param cmd new command to execute. Required.
     */
    public void push(UndoCommand cmd) {

        if (cmd == null) {
            throw new NullPointerException("cmd");
        } else if (!suspend) {
            cmd.redo();
            boolean macro = macroStack != null && !macroStack.isEmpty();
            if (commands == null) {
                commands = new ArrayList<>();
            }

            UndoCommand cur = null;
            UndoCommand macroCmd = null;
            if (macro) {
                macroCmd = macroStack.get(macroStack.size() - 1);
                if (macroCmd.children != null && !macroCmd.children.isEmpty()) {
                    cur = macroCmd.children.get(macroCmd.children.size() - 1);
                }
            } else {
                if (idx > 0) {
                    cur = commands.get(idx - 1);
                }
                while (idx < commands.size()) {
                    commands.remove(commands.size() - 1);
                }
                if (cleanIdx > idx) {
                    cleanIdx = -1;
                }
            }


            boolean canMerge = cur != null
                    && cur.id() != -1
                    && cur.id() == cmd.id()
                    && macro || idx != cleanIdx;

            if (canMerge && cur != null && cur.mergeWith(cmd)) {
                if (!macro && null != watcher) {
                    watcher.indexChanged(idx);
                    watcher.canUndoChanged(canUndo());
                    watcher.undoTextChanged(undoCaption());
                    watcher.canRedoChanged(canRedo());
                    watcher.redoTextChanged(redoCaption());
                }
            } else {
                if (macro) {
                    if (macroCmd.children == null) {
                        macroCmd.children = new ArrayList<>();
                    }
                    macroCmd.children.add(cmd);
                } else {
                    // And last actions
                    commands.add(cmd);
                    checkUndoLimit();
                    setIndex(idx + 1, false);
                }
            }
        }
    }

    /**
     * Marks the stack as clean and emits {@link UndoWatcher#cleanChanged} if the stack was not already clean.
     * <p>Whenever the stack returns to this state through the use of undo/redo commands,
     * it emits the signal {@link UndoWatcher#cleanChanged}.
     * This signal is also emitted when the stack leaves the clean state.
     */
    public void setClean() {
        if(macroStack != null && !macroStack.isEmpty()) {
            System.err.println("UndoStack.setClean(): cannot set clean in the middle of a macro");
            return;
        }
        setIndex(idx, true);
    }

    /**
     * @return If the stack is in the clean state, returns true; otherwise returns false.
     */
    public boolean isClean() {
        if(macroStack != null && !macroStack.isEmpty()) {
            return false;
        }
        return cleanIdx == idx;
    }

    /**
     * @return Returns the clean index. This is the index at which {@link #setClean} was called.
     * <p>A stack may not have a clean index. This happens if a document is saved, some commands are undone,
     * then a new command is pushed. Since {@link #push} deletes all the undone commands before pushing
     * the new command, the stack can't return to the clean state again.
     * In this case, this function returns -1.
     */
    public int getCleanIdx() {
        return cleanIdx;
    }

    /**
     * Undoes the command below the current command by calling {@link UndoCommand#undo}.
     * Decrements the current command index.
     * <p>If the stack is empty, or if the bottom command on the stack has already been undone,
     * this function does nothing.
     */
    public void undo() {
        if (commands == null || idx == 0) {
            return;
        }

        if(macroStack != null && !macroStack.isEmpty()) {
            System.err.println("UndoStack.undo(): cannot undo in the middle of a macro");
            return;
        }

        try{
            suspend = true;
            int idx = this.idx - 1;
            commands.get(idx).undo();
            setIndex(idx, false);
        }finally {
            suspend = false;
        }

    }

    /**
     * Redoes the current command by calling {@link UndoCommand#redo}. Increments the current command index.
     * <p>If the stack is empty, or if the top command on the stack has already been redone,
     * this function does nothing.
     */
    public void redo() {
        if (commands == null || idx == commands.size()) {
            return;
        }

        if(macroStack != null && !macroStack.isEmpty()) {
            System.err.println("UndoStack.redo(): cannot redo in the middle of a macro");
            return;
        }

        try{
            suspend = true;
            commands.get(idx).redo();
            setIndex(idx + 1, false);
        }finally {
            suspend = false;
        }
    }

    /**
     * @return Returns the number of commands on the stack.
     */
    public int count() {
        return commands == null ? 0 : commands.size();
    }

    /**
     * @return Returns the index of the current command.
     * This is the command that will be executed on the next call to {@link #redo}.
     * It is not always the top-most command on the stack, since a number of commands may have been undone.
     */
    public int getIdx() {
        return idx;
    }

    /**
     * Repeatedly calls {@link #undo} or {@link #redo} until the current command index reaches idx.
     * This function can be used to roll the state of the document forwards of backwards.
     * <p>{@link UndoWatcher#indexChanged} is emitted only once.
     * @param idx index to achieve.
     */
    public void setIndex(int idx) {

        if(macroStack != null && !macroStack.isEmpty()) {
            System.err.println("UndoStack.setIndex(): cannot set index in the middle of a macro");
            return;
        }

        if(commands == null) {
            return;
        }

        if(idx < 0) {
            idx = 0;
        }else if(idx > commands.size()){
            idx = commands.size();
        }

        int i = this.idx;
        while (i < idx) {
            commands.get(i++).redo();
        }
        while (i > idx){
            commands.get(--i).undo();
        }

        setIndex(idx, false);
    }

    /**
     * @return Returns true if there is a command available for undo; otherwise returns false.
     * <p>This function returns false if the stack is empty, or if the bottom command on the stack
     * has already been undone.
     * <p>Synonymous with {@link #getIdx()} == 0.
     */
    public boolean canUndo() {
        if(macroStack != null && !macroStack.isEmpty()) {
            return false;
        }
        return idx > 0;
    }

    /**
     * @return Returns true if there is a command available for redo; otherwise returns false.
     * <p>This function returns false if the stack is empty or if the top command on the stack
     * has already been redone.
     * <p>Synonymous with {@link #getIdx()}  == {@link #count()}).
     */
    public boolean canRedo() {
        if(macroStack != null && !macroStack.isEmpty()) {
            return false;
        }
        return commands != null && idx < commands.size();
    }

    /**
     * @return The caption of the command which will be undone in the next call to {@link #undo()}.
     */
    public String undoCaption() {
        if(macroStack != null && !macroStack.isEmpty()) {
            return "";
        }
        return (commands != null && idx > 0) ? commands.get(idx - 1).getCaption() : "";
    }

    /**
     * @return The caption of the command which will be redone in the next call to {@link #redo()}.
     */
    public String redoCaption() {
        if(macroStack != null && !macroStack.isEmpty()) {
            return "";
        }
        return (commands != null && idx < commands.size()) ? commands.get(idx).getCaption() : "";
    }

    /**
     * Begins composition of a macro command with the given description.
     * <p>An empty command described by the specified caption is pushed on the stack.
     * Any subsequent commands pushed on the stack will be appended to the empty command's children
     * until {@link #endMacro()} is called.
     * <p>Calls to beginMacro() and {@link #endMacro()} may be nested, but every call to beginMacro()
     * must have a matching call to {@link #endMacro()}.
     * <p>While a macro is composed, the stack is disabled. This means that:
     * <ul>
     *  <li>{@link UndoWatcher#indexChanged} and {@link UndoWatcher#cleanChanged} are not emitted,
     *  <li>{@link #canUndo()} and {@link #canRedo()} return false,
     *  <li>calling {@link #undo()} or {@link #redo()} has no effect,
     *  <li>the undo/redo actions are disabled
     * </ul>
     * <p>The stack becomes enabled and appropriate signals are emitted when {@link #endMacro()} is called
     * for the outermost macro.
     * @param caption description for this macro.
     */
    public void beginMacro(String caption) {

        final UndoCommand cmd = new UndoCommand(this, caption, null);

        if(macroStack == null) {
            macroStack = new ArrayList<>();
        }

        if(macroStack.isEmpty()) {

            while (idx < commands.size()) {
                commands.remove(commands.size() - 1);
            }
            if(cleanIdx > idx) {
                cleanIdx = -1;
            }
            commands.add(cmd);

        }else {
            macroStack.get(macroStack.size() - 1).children.add(cmd);
        }

        macroStack.add(cmd);

        if(macroStack.size() == 1) {
            if(watcher != null) {
                watcher.canUndoChanged(false);
                watcher.undoTextChanged("");
                watcher.canRedoChanged(false);
                watcher.redoTextChanged("");
            }
        }
    }

    /**
     * Ends composition of a macro command.
     * <p>If this is the outermost macro in a set nested macros, this function
     * emits {@link UndoWatcher#indexChanged} once for the entire macro command.
     */
    public void endMacro() {
        if(macroStack == null || macroStack.isEmpty()) {
            System.err.println("UndoStack.endMacro(): no matching beginMacro()");
        }
        macroStack.remove(macroStack.size()-1);

        if(macroStack.isEmpty()){
            checkUndoLimit();
            setIndex(idx + 1, false);
        }
    }

    /**
     * Returns a reference to the command at idx.
     * <p>Be aware to modify it because modifying a command, once it has been pushed onto the stack and executed,
     * almost always causes corruption of the state of the document, if the command is later undone or redone.
     * @param idx the index of command has been retrieved.
     * @return Command or null.
     */
    public UndoCommand getCommand(int idx) {
        if(commands == null || idx < 0 || idx >= commands.size()) {
            return null;
        }
        return commands.get(idx);
    }

    /**
     * Returns the caption of the command at index idx.
     * @param idx the index of command's caption has been retrieved.
     * @return Text or empty string.
     */
    public String caption(int idx) {
        if(commands == null || idx < 0 || idx >= commands.size()) {
            return "";
        }
        return commands.get(idx).getCaption();
    }

    /**
     * When the number of commands on a stack exceeds the stack's {@link #undoLimit}, commands are deleted
     * from the bottom of the stack. The default value is 0, which means that there is no limit.
     * <p>This property may only be set when the undo stack is empty, since setting it on a non-empty stack
     * might delete the command at the current index. Calling {@link #setUndoLimit} on a non-empty stack
     * prints a warning and does nothing.
     * @param value new undo limit.
     */
    public void setUndoLimit(int value) {

        if(commands != null && commands.size() > 0) {
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
     * the one associated with the currently active document. If the stack belongs to a {@link UndoGroup},
     * calls to {@link UndoGroup#undo()} or {@link UndoGroup#redo()} will be forwarded to this stack
     * when it is active.
     * If the stack does not belong to a {@link UndoGroup}, making it active has no effect.
     * <p>It is the programmer's responsibility to specify which stack is active by calling setActive(),
     * usually when the associated document window receives focus.
     *
     * @param active setting this UndoStack active in its group if it exists.
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
     * @return Subject via calling descendants real object.
     */
    public Object getSubj() {
        return subj;
    }

    public void setSubj(Object value) {
        if (value == null) {
            throw new NullPointerException("value");
        } else {
            if (subj == null) {
                subj = value;
            }

        }
    }

    /**
     * @return The subscribed wather if it exists or null.
     */
    public UndoWatcher getWatcher() {
        return watcher;
    }

    /**
     * Sets the watcher for signals emitted.
     * @param watcher subscriber for signals. Setting parameter to null unsubscribe it.
     */
    public void setWatcher(UndoWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoStack stack = (UndoStack) o;
        return idx == stack.idx
                && subj.equals(stack.subj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx, subj, commands);
    }

    /**
     * Sets the current index to idx, emitting appropriate signals. If clean is true,
     * makes idx the clean index as well.
     *
     * @param index index to achieve.
     * @param clean flag to set/unset clean state.
     */
    private void setIndex(int index, boolean clean) {

        final boolean wasClean = idx == cleanIdx;

        if (this.idx != index) {
            this.idx = index;
            if(null != watcher){
                watcher.indexChanged(idx);
                watcher.canUndoChanged(canUndo());
                watcher.undoTextChanged(undoCaption());
                watcher.canRedoChanged(canRedo());
                watcher.redoTextChanged(redoCaption());
            }
        }

        if(clean) {
            cleanIdx = idx;
        }

        final boolean isClean = idx == cleanIdx;
        if(isClean != wasClean && null != watcher) {
            watcher.cleanChanged(isClean);
        }
    }

    /**
     * If the number of commands on the stack exceeds the undo limit, deletes commands
     * from the bottom of the stack.
     */
    private void checkUndoLimit() {

        if( undoLimit <= 0
                || (commands == null)
                || undoLimit >= commands.size()
                || (macroStack != null && macroStack.size() > 0) ) {
            return;
        }

        int delCnt = commands.size() - undoLimit;
        for(int i = 0; i < delCnt; ++i) {
            commands.remove(0);
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
