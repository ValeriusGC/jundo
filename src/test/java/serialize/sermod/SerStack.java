package serialize.sermod;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SerStack implements Serializable{

    private int index;
//    public List<Object> objects = new ArrayList<>();
    public List<BaseCommand> commandList = new ArrayList<>();

    public void push(@NotNull BaseCommand command) throws Exception {
        if(command == null) {
            throw new Exception("command must be not null");
        }

        command.redo();
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

}
