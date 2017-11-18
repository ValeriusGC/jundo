package undomodel;

public interface UndoEvents {

    default void indexChanged(int idx) {}
    default void cleanChanged(boolean clean) {}
    default void canUndoChanged(boolean canUndo) {}
    default void canRedoChanged(boolean canRedo) {}
    default void undoTextChanged(String undoText) {}
    default void redoTextChanged(String redoText) {}

}
