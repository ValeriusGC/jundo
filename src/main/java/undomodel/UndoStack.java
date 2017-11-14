package undomodel;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UndoStack implements Serializable{

    /**
     * Can be used for linking to appropriate object
     */
    private final String id;
    private int index;
    private List<UndoCommand> commandList = new ArrayList<>();

    public UndoStack(@Nullable String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void push(@NotNull UndoCommand command) throws Exception {
        if(command == null) {
            throw new Exception("command must be not null");
        }

        // FIA
        command.redo();

        UndoCommand cur = null;
        if(index > 0) {
            cur = commandList.get(index - 1);
        }
        while (index < commandList.size()) {
            commandList.remove(commandList.size() - 1);
        }

        boolean canMerge = cur != null
                && cur.id() != -1
                && cur.id() == command.id();

        // And last actions
        commandList.add(command);
        setIndex(index + 1, false);
    }

    public void setIndex(int index, boolean clean) {
        if (this.index != index) {
            this.index = index;
        }
    }

    public void undo() {
        if (index == 0) {
            return;
        }
        int idx = index - 1;
        commandList.get(idx).undo();
        setIndex(idx, false);
    }

    public void redo() {
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

    @Override
    public String toString() {
        return "UndoStack{" +
                "id='" + id + '\'' +
                ", index=" + index +
                ", commandList=" + commandList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoStack stack = (UndoStack) o;
        return index == stack.index &&
                Objects.equals(id, stack.id) &&
                Objects.equals(commandList, stack.commandList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, index, commandList);
    }
}
