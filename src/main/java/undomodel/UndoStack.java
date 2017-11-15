package undomodel;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract public class UndoStack implements Serializable{

    /**
     * Not public 'cause is hidden.
     */
    UndoGroup group;

    private int idx;
    private int cleanIdx;
    private List<UndoCommand> cmdLst = new ArrayList<>();

    public UndoStack(UndoGroup group) {
        if(null != group) {
            group.add(this);
        }
    }

    public void setActive(boolean active) {
        if(group != null) {
            if(active) {
                group.setActive(this);
            }else if(group.getActive() == this) {
                group.setActive(null);
            }
        }
    }

    public boolean isActive() {
        return group == null || group.getActive() == this;
    }

    public boolean isClean() {
        return cleanIdx == idx;
    }

    public int getCleanIdx() {
        return cleanIdx;
    }

    public void setClean() {
        setIndex(idx, true);
    }

    public void clear() {
        if(cmdLst.isEmpty()) {
            return;
        }

    }

    /**
     * Used to get concrete object via base class
     * @return
     */
    public Object getObject() {
        return doGetObject();
    }

    protected abstract Object doGetObject();

    public void push(@NotNull UndoCommand cmd) throws Exception {
        if(cmd == null) {
            throw new Exception("command must be not null");
        }

        // FIA
        cmd.redo();

        UndoCommand cur = null;
        if(idx > 0) {
            cur = cmdLst.get(idx - 1);
        }
        while (idx < cmdLst.size()) {
            cmdLst.remove(cmdLst.size() - 1);
        }

        boolean canMerge = cur != null
                && cur.id() != -1
                && cur.id() == cmd.id();
        if(!(canMerge && cur.mergeWith(cmd))){
            // And last actions
            cmdLst.add(cmd);
            setIndex(idx + 1, false);
        }
    }

    public void setIndex(int index, boolean clean) {

        boolean wasClean = idx == cleanIdx;

        if (this.idx != index) {
            this.idx = index;
        }

        if(clean) {
            cleanIdx = idx;
        }

        boolean isClean = idx == cleanIdx;

    }

    public void undo() {
        if (idx == 0) {
            return;
        }
        int idx = this.idx - 1;
        cmdLst.get(idx).undo();
        setIndex(idx, false);
    }

    public void redo() {
        if (idx == cmdLst.size()) {
            return;
        }

        cmdLst.get(idx).redo();
        setIndex(idx + 1, false);

    }

    public int count() {
        return cmdLst.size();
    }

    public int getIdx() {
        return idx;
    }

    public boolean canUndo() {
        return idx > 0;
    }

    public boolean canRedo() {
        return idx < cmdLst.size();
    }

    public String undoText() {
        return idx > 0 ? cmdLst.get(idx - 1).getText() : "";
    }

    public String redoText() {
        return idx < cmdLst.size() ? cmdLst.get(idx).getText() : "";
    }

    @Override
    public String toString() {
        return "UndoStack{" +
                "idx=" + idx +
                ", cmdLst=" + cmdLst +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoStack stack = (UndoStack) o;
        return idx == stack.idx &&
                Objects.equals(cmdLst, stack.cmdLst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx, cmdLst);
    }
}
