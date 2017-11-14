package serialize.sermod;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

abstract public class BaseCommand implements Serializable{

//    private SerStack stack;
//    private final transient Object object;
//    private int objectIdx = -1;

//    public BaseCommand(@NotNull Object object) throws Exception {
//        if(object == null) {
//            throw new Exception("null parameters!");
//        }
//        this.object = object;
//    }

//    public SerStack getStack() {
//        return stack;
//    }

//    public void setStack(SerStack stack) throws Exception {
//        if(stack == null) {
//            throw new Exception("stack must exists");
//        }
//
//        if(this.stack != null && this.stack != stack) {
//            throw new Exception("command already in another stack");
//        }
//
//        this.stack = stack;
//        if(!stack.commandList.contains(this)){
//            this.stack.commandList.add(this);
//        }
//        if(!stack.objects.contains(object)){
//            this.stack.objects.add(object);
//        }
//        objectIdx = stack.objects.indexOf(object);
//    }

    public void undo() {
        doUndo();
    }

    public void redo() {
        doRedo();
    }

    abstract public void doUndo();

    abstract public void doRedo();

}
