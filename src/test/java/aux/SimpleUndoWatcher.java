package aux;

import com.gdetotut.jundo.UndoWatcher;

/**
 * Tester for event subscribing.
 * {@link UndoWatcher} has all the methods with default realization so client can realize just methods it wants.
 */
public class SimpleUndoWatcher implements UndoWatcher {

    private static int cnt;

//    @Override
//    public void indexChanged(int idx) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.indexChanged: " + idx);
//    }

//    @Override
//    public void cleanChanged(boolean clean) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.cleanChanged: " + clean);
//    }
//
//    @Override
//    public void canUndoChanged(boolean canUndo) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.canUndoChanged: " + canUndo);
//    }
//
//    @Override
//    public void canRedoChanged(boolean canRedo) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.canRedoChanged: " + canRedo);
//    }
//
//    @Override
//    public void undoTextChanged(String undoText) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.undoTextChanged: " + undoText);
//    }
//
//    @Override
//    public void redoTextChanged(String redoText) {
//        System.out.println("" + ++cnt + ": " + "SimpleUndoWatcher.redoTextChanged: " + redoText);
//    }
}
