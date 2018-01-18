package com.gdetotut.jundo;

/**
 * Methods for subscribers.
 */
public interface UndoWatcher {

    /**
     * Fires when {@link UndoStack@idx} is changed.
     * @param idx
     */
    default void indexChanged(int idx) {
    }

    /**
     * Fires when undo/redo operation cross {@link UndoStack#cleanIdx} point.
     * @param clean
     */
    default void cleanChanged(boolean clean) {
    }

    /**
     * Fires once when we have no undo anymore (false); we again have them (true).
     */
    default void canUndoChanged(boolean canUndo) {
    }

    /**
     * Fires once when we have no redo anymore (false); we again have them (true).
     */
    default void canRedoChanged(boolean canRedo) {
    }

    /**
     * Fires after every undo/redo. shows next undoText.
     */
    default void undoTextChanged(String undoText) {
    }

    /**
     * Fires after every undo/redo. shows next redoText.
     */
    default void redoTextChanged(String redoText) {
    }

    /**
     * Fires once when macro creation starts and stops.
     * @param on true if starts; otherwise false.
     */
    default void macroChanged(boolean on) {
    }

}
