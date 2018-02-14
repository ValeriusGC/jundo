package com.gdetotut.jundo;

import java.io.Serializable;

/**
 * Special wrapper for macro commands. Simplifies and clarifies macros handling in {@link UndoStack}.
 * <p>Since macro should be independent object (i.e. not linked with any stacks until it applies as {@link UndoCommand})
 * this wrapper automatically cleanses {@link UndoCommand#owner} property for entire command.
 * <p>Later one can apply this command via overloaded method {@link UndoStack#push(UndoMacro)}.
 */
public class UndoMacro implements Serializable {

    /**
     * Read-only inner command.
     */
    final UndoCommand cmd;

    /**
     * Creates object with command inside.
     * @param cmd wrapped command.
     */
    UndoMacro(UndoCommand cmd) {

        if(cmd == null) {
            throw new NullPointerException("cmd");
        }

        if(cmd.children != null) {
            for (UndoCommand child : cmd.children) {
                child.setOwner(null);
            }
        }
        cmd.setOwner(null);

        this.cmd = cmd;
    }

}
