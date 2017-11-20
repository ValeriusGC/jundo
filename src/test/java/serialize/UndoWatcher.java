package serialize;

import undomodel.UndoEvents;

import java.io.Serializable;

/**
 * Tester for event subscribing.
 * {@link UndoEvents} has all the methods with default realization so client can realize just methods it wants.
 */
public class UndoWatcher implements UndoEvents, Serializable {

    private static int cnt;

    @Override
    public void indexChanged(int idx) {
        System.out.println("" + ++cnt + ": " + "UndoWatcher.indexChanged: " + idx);
    }

//    @Override
//    public void cleanChanged(boolean clean) {
//        System.out.println("" + ++cnt + ": " + "UndoWatcher.cleanChanged: " + clean);
//    }
//
//    @Override
//    public void canUndoChanged(boolean canUndo) {
//        System.out.println("" + ++cnt + ": " + "UndoWatcher.canUndoChanged: " + canUndo);
//    }
//
//    @Override
//    public void canRedoChanged(boolean canRedo) {
//        System.out.println("" + ++cnt + ": " + "UndoWatcher.canRedoChanged: " + canRedo);
//    }
//
//    @Override
//    public void undoTextChanged(String undoText) {
//        System.out.println("" + ++cnt + ": " + "UndoWatcher.undoTextChanged: " + undoText);
//    }
//
//    @Override
//    public void redoTextChanged(String redoText) {
//        System.out.println("" + ++cnt + ": " + "UndoWatcher.redoTextChanged: " + redoText);
//    }
}
