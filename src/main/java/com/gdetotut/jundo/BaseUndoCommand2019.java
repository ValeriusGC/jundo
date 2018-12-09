package com.gdetotut.jundo;

/**
 * Новый подход к командам.
 */
public abstract class BaseUndoCommand2019<Subject>{

    protected Subject subject;

    protected BaseUndoCommand2019(Subject subject) {
        this.subject = subject;
    }

    public abstract void doUndo();

    public abstract void doRedo();

}
