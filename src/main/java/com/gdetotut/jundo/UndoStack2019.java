package com.gdetotut.jundo;

import java.util.*;

/**
 * Стек по новым правилам.
 * Хранит список экземпляров классов-данных, а не команд.
 * Применяет к ним команды из фАБРИКИ
 */
public class UndoStack2019<Subject> {

    protected Map<Class, BaseUndoCommand2019<Subject> > map = new TreeMap<>();

    List<Object> data = new ArrayList<>();

    int cursor = 0;

    public Subject subject;

    final UndoCommandFactory2019<Subject> factory;

    public UndoStack2019(UndoCommandFactory2019<Subject> factory) {
        this.factory = factory;
    }

    public void push(Object data) {
        BaseUndoCommand2019<Subject> cmd = factory.makeBy(data, subject);
        if(cmd != null) {
            cmd.doRedo();
            this.data.add(data);
            cursor++;
        }
    }

    public void redo() {
        if(cursor < data.size() + 1) {
            Object data = this.data.get(cursor);
            BaseUndoCommand2019<Subject> cmd = factory.makeBy(data, subject);
            if(cmd != null) {
                cmd.doRedo();
                cursor++;
            }
        }
    }

    public void undo() {
        if(cursor > -1) {
            Object data = this.data.get(cursor - 1);
            BaseUndoCommand2019<Subject> cmd = factory.makeBy(data, subject);
            if(cmd != null) {
                cmd.doUndo();
                cursor--;
            }
        }
    }

}
