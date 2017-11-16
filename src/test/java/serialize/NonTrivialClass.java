package serialize;

import undomodel.UndoCommand;
import undomodel.UndoSubject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NonTrivialClass extends UndoSubject{

    /**
     * Inner Item for collection.
     */
    public static class Item implements Serializable {

        public enum Type {
            RECT,
            CIRCLE
        }

        public int x;

        public final Type type;

        public Item(Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "x=" + x +
                    ", type=" + type +
                    '}';
        }
    }

    /**
     * Command for Adding
     */
    public static class AddCommand extends UndoCommand {

        private final NonTrivialClass scene;
        private final Item item;
        private final int initialPos;

        public AddCommand(Item.Type type, NonTrivialClass scene) {
            super(null);
            this.scene = scene;
            item = new Item(type);
            initialPos = this.scene.items.size() * 2;
            setText(ConstForTest.CMD_ADD + " at " + initialPos);
        }

        @Override
        protected void doUndo() {
            scene.items.remove(item);
        }

        @Override
        protected void doRedo() {
            scene.items.add(item);
            item.x = initialPos;
        }
    }

    /**
     * Adding
     */
    public static class DeleteCommand extends UndoCommand {

        private final NonTrivialClass scene;
        private final Item item;

        public DeleteCommand(NonTrivialClass scene) {
            super(null);
            this.scene = scene;
            this.item = scene.items.size() > 0 ? scene.items.get(0) : null;
            setText(ConstForTest.CMD_DEL + " at " + item.x);
        }

        @Override
        protected void doUndo() {
            if(item != null){
                scene.items.add(item);
            }
        }

        @Override
        protected void doRedo() {
            if(item != null) {
                scene.items.remove(item);
            }

        }
    }

    /**
     *
     */
    public static class MovedCommand extends UndoCommand {

        private final Item item;
        private final int oldPos;
        private int newPos;

        public MovedCommand(Item item, int oldPos) {
            super(null);
            this.item = item;
            this.oldPos = oldPos;
            this.newPos = item.x;
            setText(ConstForTest.CMD_MOV + " to " + item.x);
        }

        @Override
        protected void doUndo() {
            item.x = oldPos;
        }

        @Override
        protected void doRedo() {
            item.x = newPos;
        }

        @Override
        public boolean mergeWith(UndoCommand cmd) {
            if(cmd instanceof MovedCommand){
                Item item = ((MovedCommand) cmd).item;
                if(item == this.item) {
                    newPos = item.x;
                    setText(ConstForTest.CMD_MOV + " to " + item.x);
                    return true;
                }
            }
            return false;
        }

        @Override
        public int id() {
            return 1234;
        }
    }

    //----------------------------------------------------------


    public final List<Item> items = new ArrayList<>();

    void addItem(Item item) {
        items.add(item);
    }

    void removeItem(Item item) {
        items.remove(item);
    }

    @Override
    public String toString() {
        return "NonTrivialClass{" +
                "items=" + items +
                '}';
    }
}
