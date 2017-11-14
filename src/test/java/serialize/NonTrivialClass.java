package serialize;

import undomodel.UndoCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NonTrivialClass implements Serializable{

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
            this.scene = scene;
            item = new Item(type);
            initialPos = this.scene.items.size() * 2;
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
            this.scene = scene;
            this.item = scene.items.size() > 0 ? scene.items.get(0) : null;
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
    public static class MoveCommand extends UndoCommand {

        private final Item item;
        private final int oldPos;
        private final int newPos;

        public MoveCommand(Item item, int oldPos) {
            this.item = item;
            this.oldPos = oldPos;
            this.newPos = item.x;
        }

        @Override
        protected void doUndo() {
            item.x = oldPos;
        }

        @Override
        protected void doRedo() {
            item.x = newPos;
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
