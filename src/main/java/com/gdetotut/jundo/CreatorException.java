package com.gdetotut.jundo;

/**
 * Special exception for situations that can occur at creating {@link UndoStack} in {@link UndoPacket}
 */
public class CreatorException extends Exception {

    public CreatorException(String message) {
        super(message);
    }

}
