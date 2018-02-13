package com.gdetotut.jundo;

import java.io.Serializable;

public class UndoMacro implements Serializable {

    final UndoCommand cmd;

    public UndoMacro(UndoCommand cmd) {

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
