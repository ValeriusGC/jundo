package undomodel;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UndoStack {

    private int index;

    public List<UndoCommand> getCommandList() {
        return commandList;
    }

    private List<UndoCommand> commandList = new ArrayList<UndoCommand>();

    public UndoStack() {
    }

    public UndoStack(@Nullable UndoStack parent) {
    }

    public void push(@NotNull UndoCommand command) throws IllegalAccessException, InvocationTargetException {
        command.redo();
        commandList.add(command);
        setIndex(index + 1, false);
    }

    public void undo() throws IllegalAccessException, InvocationTargetException {
        if (index == 0) {
            return;
        }
        int idx = index - 1;
        commandList.get(idx).undo();
        setIndex(idx, false);
    }

    public void redo() throws IllegalAccessException, InvocationTargetException {
        if (index == commandList.size()) {
            return;
        }

        commandList.get(index).redo();
        setIndex(index + 1, false);

    }

    public int count() {
        return commandList.size();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index, boolean clean) {

        if (this.index != index) {
            this.index = index;
        }
    }

}
