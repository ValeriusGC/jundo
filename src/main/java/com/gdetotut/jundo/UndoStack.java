package com.gdetotut.jundo;

import java.io.*;
import java.util.*;

/**
 * Stack of entire {@link UndoCommand} chain for subject.
 * <b>Main characteristic of {@link UndoStack} is that two different stacks should not share one subject.</b>
 * <p>Otherwise {@link UndoGroup} may not add them both, and there will be collision when undoing one subject
 * via different stacks.
 */
public class UndoStack implements Serializable {

    /**
     * Group of stacks that owns this stack. Optional.
     */
    UndoGroup group;

    /**
     * Keeps the subject for whom {@link #commands} are behave. Required.
     */
    private transient Object subj;

    /**
     * Index of current command that redo.
     */
    private int idx;

    /**
     * Index of clean state command.
     */
    private int cleanIdx;

    /**
     * List of commands.
     */
    private List<UndoCommand> commands;

    /**
     * Macro that is been building at this moment.
     */
    private UndoCommand macroCmd;

    /**
     * List of macros.
     */
    private List<UndoCommand> macros;

    /**
     * Limit for command's stack.
     */
    private int undoLimit;

    /**
     * Client that watching events. Optional.
     */
    private transient UndoWatcher watcher;

    /**
     * List of local contexts. Optional.
     */
    private transient Map<String, Object> localContexts;

    /**
     * Flag for get rid of parasite commands.
     */
    private boolean suspend = false;

    /**
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If group is not a null the stack is automatically added to the group.
     *
     * @param subj  for whom this stack was made. Can be null if no way to make it serializable. Required.
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

    /**
     * @return List of local contexts.
     */
    public Map<String, Object> getLocalContexts() {
        if (localContexts == null) {
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
        if (commands == null || commands.isEmpty()) {
            return;
        }

        boolean wasClean = isClean();
        for (UndoCommand cmd : commands) {
            if (cmd.children != null) {
                cmd.children.clear();
            }
        }
        commands.clear();
        idx = 0;
        cleanIdx = 0;

        if (null != watcher) {
            watcher.indexChanged(0);
            watcher.canUndoChanged(false);
            watcher.undoTextChanged("");
            watcher.canRedoChanged(false);
            watcher.redoTextChanged("");
            if (!wasClean) {
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
     *
     * @param cmd new command to execute. Required.
     */
    public void push(UndoCommand cmd) throws Exception {

        if (cmd == null) {
            throw new NullPointerException("cmd");
        } else if (!suspend) {

            UndoCommand copy = clone(cmd);

            cmd.redo();

            boolean onMacro = null != macroCmd;

            if (commands == null) {
                commands = new ArrayList<>();
            }

            UndoCommand cur = idx > 0 ? commands.get(idx - 1) : null;

            while (idx < commands.size()) {
                commands.remove(commands.size() - 1);
            }

            if (cleanIdx > idx) {
                cleanIdx = -1;
            }

            boolean canMerge = cur != null
                    && cur.id() != -1
                    && cur.id() == cmd.id()
                    && onMacro || idx != cleanIdx;

            if (canMerge && cur != null && cur.mergeWith(cmd)) {
                if (!onMacro && null != watcher) {
                    watcher.indexChanged(idx);
                    watcher.canUndoChanged(canUndo());
                    watcher.undoTextChanged(undoCaption());
                    watcher.canRedoChanged(canRedo());
                    watcher.redoTextChanged(redoCaption());
                }
            } else {
                if (onMacro) {
                    if (null != copy) {
                        if (macroCmd.children == null) {
                            macroCmd.children = new ArrayList<>();
                        }
                        macroCmd.children.add(copy);
                    } else {
                        dropMacro();
                    }

                    if (null == cur.children) {
                        cur.children = new ArrayList<>();
                    }
                    cur.children.add(cmd);

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
        if (null != macroCmd) {
            System.err.println("UndoStack.setClean(): cannot set clean in the middle of a macro");
            return;
        }
        setIndex(idx, true);
    }

    /**
     * @return If the stack is in the clean state, returns true; otherwise returns false.
     */
    public boolean isClean() {
        if (null != macroCmd) {
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

        if (null != macroCmd) {
            System.err.println("UndoStack.undo(): cannot undo in the middle of a macro");
            return;
        }

        try {
            suspend = true;
            int idx = this.idx - 1;
            commands.get(idx).undo();
            setIndex(idx, false);
        } finally {
            suspend = false;
        }

    }

    /**
     * Redoes the current command by calling {@link UndoCommand#redo}. Increments the current command index.
     * <p>If the stack is empty, or if the top command on the stack has already been redone,
     * this function does nothing.
     */
    public void redo() {
        if (macroCmd != null) {
            System.err.println("UndoStack.redo(): cannot redo in the middle of a macro");
            return;
        }

        if (commands == null || idx == commands.size()) {
            return;
        }

        try {
            suspend = true;
            commands.get(idx).redo();
            setIndex(idx + 1, false);
        } finally {
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
     *
     * @param idx index to achieve.
     */
    public void setIndex(int idx) {

        if (null != macroCmd) {
            System.err.println("UndoStack.setIndex(): cannot set index in the middle of a macro");
            return;
        }

        if (commands == null) {
            return;
        }

        if (idx < 0) {
            idx = 0;
        } else if (idx > commands.size()) {
            idx = commands.size();
        }

        int i = this.idx;
        while (i < idx) {
            commands.get(i++).redo();
        }
        while (i > idx) {
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
        if (null != macroCmd) {
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
        if (null != macroCmd) {
            return false;
        }
        return commands != null && idx < commands.size();
    }

    /**
     * @return The caption of the command which will be undone in the next call to {@link #undo()}.
     */
    public String undoCaption() {
        if (null != macroCmd) {
            return "";
        }
        return (commands != null && idx > 0) ? commands.get(idx - 1).getCaption() : "";
    }

    /**
     * @return The caption of the command which will be redone in the next call to {@link #redo()}.
     */
    public String redoCaption() {
        if (null != macroCmd) {
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
     * <li>{@link UndoWatcher#indexChanged} and {@link UndoWatcher#cleanChanged} are not emitted,
     * <li>{@link #canUndo()} and {@link #canRedo()} return false,
     * <li>calling {@link #undo()} or {@link #redo()} has no effect,
     * <li>the undo/redo actions are disabled
     * </ul>
     * <p>The stack becomes enabled and appropriate signals are emitted when {@link #endMacro()} is called
     * for the outermost macro.
     *
     * @param caption description for this macro. Optional.
     */
    public void beginMacro(String caption) {

        if (null != macroCmd) {
            System.err.println("UndoStack.beginMacro(): cannot set new beginMacro in the middle of a macro");
            return;
        }

        UndoCommand startMacro = new UndoCommand(this, caption, null);

        try {
            macroCmd = clone(startMacro);
        } catch (Exception  e) {
            System.err.println(e.getLocalizedMessage());
            endMacro();
        }

        if (null == commands) {
            commands = new ArrayList<>();
        }

        while (idx < commands.size()) {
            commands.remove(commands.size() - 1);
        }
        if (cleanIdx > idx) {
            cleanIdx = -1;
        }
        commands.add(startMacro);
        checkUndoLimit();
        setIndex(idx + 1, false);

        if (watcher != null) {
            watcher.macroChanged(true);
            watcher.canUndoChanged(false);
            watcher.undoTextChanged("");
            watcher.canRedoChanged(false);
            watcher.redoTextChanged("");
        }
    }

    /**
     * Ends composition of a macro command.
     * <p>If this is the outermost macro in a set nested macros, this function
     * emits {@link UndoWatcher#indexChanged} once for the entire macro command.
     */
    public void endMacro() {
        if (null == macroCmd) {
            System.err.println("UndoStack.endMacro(): no matching beginMacro()");
        }
        if (null == macros) {
            macros = new ArrayList<>();
        }
        macros.add(macroCmd);
        macroCmd = null;
        if (null != watcher) {
            watcher.macroChanged(false);
        }
    }

    /**
     * Drops macro creation.
     */
    public void dropMacro() {
        macroCmd = null;
        if (null != watcher) {
            watcher.macroChanged(false);
        }
    }

    /**
     * Returns a reference to the command at idx.
     * <p>Be aware to modify it because modifying a command, once it has been pushed onto the stack and executed,
     * almost always causes corruption of the state of the document, if the command is later undone or redone.
     *
     * @param idx the index of command has been retrieved.
     * @return Command or null.
     */
    public UndoCommand getCommand(int idx) {
        if (commands == null || idx < 0 || idx >= commands.size()) {
            return null;
        }
        return commands.get(idx);
    }

    /**
     * Returns the caption of the command at index idx.
     *
     * @param idx the index of command's caption has been retrieved.
     * @return Text or empty string.
     */
    public String caption(int idx) {
        if (commands == null || idx < 0 || idx >= commands.size()) {
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
     *
     * @param value new undo limit.
     */
    public void setUndoLimit(int value) {

        if (commands != null && commands.size() > 0) {
            System.err.println("UndoStack.setUndoLimit(): an undo limit can only be set when the stack is empty");
            return;
        }

        if (value == undoLimit) {
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
        if (group != null) {
            if (active) {
                group.setActive(this);
            } else if (group.getActive() == this) {
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

    /**
     * Reset subj. Used in {@link UndoPacket}.
     * <p><b>Do not use it!</b>
     *
     * @param value new subject. Required.
     */
    public void setSubj(Object value) {
        if (null == value) {
            throw new NullPointerException("value");
        } else {
            subj = value;
        }
    }

    /**
     * @return List of macros.
     */
    public List<UndoCommand> getMacros() {
        return macros;
    }

    /**
     * @return The subscribed watcher if it exists or null.
     */
    public UndoWatcher getWatcher() {
        return watcher;
    }

    /**
     * Sets the watcher for signals emitted.
     *
     * @param watcher subscriber for signals. Setting parameter to null unsubscribe it.
     */
    public void setWatcher(UndoWatcher watcher) {
        this.watcher = watcher;
    }

    /**
     * Clones command. Use it for clone macro only!
     *
     * @param cmd macro for clone.
     * @return Cloned command.
     * @throws Exception If something goes wrong.
     */
    public UndoCommand clone(UndoCommand cmd) throws Exception {

        if (null == cmd) {
            throw new NullPointerException("cmd");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(cmd);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            UndoCommand cmdClone = (UndoCommand) ois.readObject();
            cmdClone.owner = cmd.owner;
            if (null != cmdClone.children) {
                for (UndoCommand uc : cmdClone.children) {
                    uc.owner = cmd.owner;
                }
            }
            return cmdClone;
        }
    }

    /**
     * We use <b>getSubj() == stack.getSubj()</b> instead of <b>Objects.equals(getSubj(), stack.getSubj())</b>
     * because semantic of <b>2 stack differs when they have different addresses in memory.</b>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoStack stack = (UndoStack) o;

        return getIdx() == stack.getIdx() &&
                getCleanIdx() == stack.getCleanIdx() &&
                getUndoLimit() == stack.getUndoLimit() &&
                suspend == stack.suspend &&
                getSubj() == stack.getSubj() &&
                Objects.equals(commands, stack.commands) &&
                Objects.equals(macroCmd, stack.macroCmd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubj());
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
            if (null != watcher) {
                watcher.indexChanged(idx);
                watcher.canUndoChanged(canUndo());
                watcher.undoTextChanged(undoCaption());
                watcher.canRedoChanged(canRedo());
                watcher.redoTextChanged(redoCaption());
            }
        }

        if (clean) {
            cleanIdx = idx;
        }

        final boolean isClean = idx == cleanIdx;
        if (isClean != wasClean && null != watcher) {
            watcher.cleanChanged(isClean);
        }
    }

    /**
     * If the number of commands on the stack exceeds the undo limit, deletes commands
     * from the bottom of the stack.
     */
    private void checkUndoLimit() {

        if (undoLimit <= 0
                || (commands == null)
                || undoLimit >= commands.size()
                || (null != macroCmd)) {
            return;
        }

        int delCnt = commands.size() - undoLimit;
        for (int i = 0; i < delCnt; ++i) {
            commands.remove(0);
        }

        idx -= delCnt;
        if (cleanIdx != -1) {
            if (cleanIdx < delCnt) {
                cleanIdx = -1;
            } else {
                cleanIdx -= delCnt;
            }
        }
    }

}
