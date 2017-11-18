package serialize;

import undomodel.UndoEvents;

public class UndoWather implements UndoEvents {

    private static int cnt;

    @Override
    public void indexChanged(int idx) {
        System.out.println("" + ++cnt + ": " + "UndoWather.indexChanged: " + idx);
    }

    @Override
    public void cleanChanged(boolean clean) {
        System.out.println("" + ++cnt + ": " + "UndoWather.cleanChanged: " + clean);
    }

    @Override
    public void canUndoChanged(boolean canUndo) {
        System.out.println("" + ++cnt + ": " + "UndoWather.canUndoChanged: " + canUndo);
    }

    @Override
    public void canRedoChanged(boolean canRedo) {
        System.out.println("" + ++cnt + ": " + "UndoWather.canRedoChanged: " + canRedo);
    }

    @Override
    public void undoTextChanged(String undoText) {
        System.out.println("" + ++cnt + ": " + "UndoWather.undoTextChanged: " + undoText);
    }

    @Override
    public void redoTextChanged(String redoText) {
        System.out.println("" + ++cnt + ": " + "UndoWather.redoTextChanged: " + redoText);
    }
}
