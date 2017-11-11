package undomodel;

import com.sun.istack.internal.Nullable;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class UndoStack {

    private int index;
    private List<UndoCommand> commandList = new ArrayList<UndoCommand>();

    public UndoStack(@Nullable UndoStack parent) {
    }

    public void push(@NonNull UndoCommand command) throws IllegalAccessException {
        command.redo();
        commandList.add(command);
        setIndex(index + 1, false);
    }

    public void undo() throws IllegalAccessException {
        if (index == 0) {
            return;
        }
        int idx = index - 1;
        commandList.get(idx).undo();
        setIndex(idx, false);
    }

    public void redo() throws IllegalAccessException {
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
