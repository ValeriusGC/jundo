package some;

import com.gdetotut.jundo.UndoWatcher;

/**
 * Tester for event subscribing.
 * {@link UndoWatcher} has all the methods with default realization so client can realize just methods it wants.
 */
public class SimpleUndoWatcher implements UndoWatcher {

    //private static int cnt;

    private final String title;

    public SimpleUndoWatcher(String title) {
        this.title = title;
    }

    @Override
    public void indexChanged(int from, int to) {
        System.out.println(String.format("%s.indexChanged: %d -> %d", title, from, to));
    }

    @Override
    public void cleanChanged(boolean clean) {
        System.out.println(String.format("%s.cleanChanged: %s", title, clean));
    }

    @Override
    public void canUndoChanged(boolean canUndo) {
        System.out.println(String.format("%s.canUndoChanged: %s", title, canUndo));
    }

    @Override
    public void canRedoChanged(boolean canRedo) {
        System.out.println(String.format("%s.canRedoChanged: %s", title, canRedo));
    }

    @Override
    public void undoTextChanged(String undoCaption) {
        System.out.println(String.format("%s.undoCaptionChanged: %s", title, undoCaption));
    }

    @Override
    public void redoTextChanged(String redoCaption) {
        System.out.println(String.format("%s.redoCaptionChanged: %s", title, redoCaption));
    }

    @Override
    public void macroChanged(boolean on) {
        System.out.println(String.format("%s.macroChanged: %s", title, on));
    }
}
