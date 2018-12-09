package com.gdetotut.jundo;

public interface UndoCommandFactory2019<Subject> {
    BaseUndoCommand2019<Subject> makeBy(Object data, Subject subject);
}
