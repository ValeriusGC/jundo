package com.gdetotut.jundo;

/**
 * Methods for subscribers.
 */
public interface UndoWatcher {

    /**
     * This event fires when {@link UndoStack#idx} is changed.
     *
     * @param idx new index of current command.
     */
    default void indexChanged(int idx) {
    }

    /**
     * This event fires when undo/redo operation cross {@link UndoStack#cleanIdx} point.
     *
     * @param clean true when we go into clean state; false when out.
     */
    default void cleanChanged(boolean clean) {
    }

    /**
     * This event fires once when we have no undo anymore (false); we again have them (true).
     *
     * @param canUndo true when stack has command to undo; false otherwise.
     */
    default void canUndoChanged(boolean canUndo) {
    }

    /**
     * This event fires once when we have no redo anymore (false); we again have them (true).
     *
     * @param canRedo true when stack has command to redo; false otherwise.
     */
    default void canRedoChanged(boolean canRedo) {
    }

    /**
     * This event fires after every undo/redo. shows next undoCaption.
     *
     * @param undoCaption caption for next undo command.
     */
    default void undoTextChanged(String undoCaption) {
    }

    /**
     * This event fires after every undo/redo. shows next redoCaption.
     *
     * @param redoCaption caption for next redo command.
     */
    default void redoTextChanged(String redoCaption) {
    }

    /**
     * This event fires once when macro creation starts and stops.
     *
     * @param on true if starts; otherwise false.
     */
    default void macroChanged(boolean on) {
    }

}
