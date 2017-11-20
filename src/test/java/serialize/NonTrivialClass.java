package serialize;

import undomodel.UndoCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NonTrivialClass implements Serializable {

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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return x == item.x &&
                    type == item.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, type);
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

        public AddCommand(Item.Type type, NonTrivialClass scene, UndoCommand parent) {
            super(null, parent);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddCommand that = (AddCommand) o;
            return initialPos == that.initialPos &&
                    Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, initialPos);
        }
    }

    /**
     * Adding
     */
    public static class DeleteCommand extends UndoCommand {

        private final NonTrivialClass scene;
        private final Item item;

        public DeleteCommand(NonTrivialClass scene, UndoCommand parent) {
            super(null, parent);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeleteCommand that = (DeleteCommand) o;
            return Objects.equals(scene, that.scene) &&
                    Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scene, item);
        }
    }

    /**
     *
     */
    public static class MovedCommand extends UndoCommand {

        private final Item item;
        private final int oldPos;
        private int newPos;

        public MovedCommand(Item item, int oldPos, UndoCommand parent) {
            super(null, parent);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MovedCommand that = (MovedCommand) o;
            return oldPos == that.oldPos &&
                    newPos == that.newPos &&
                    Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, oldPos, newPos);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonTrivialClass aClass = (NonTrivialClass) o;
        return Objects.equals(items, aClass.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return "NonTrivialClass{" +
                "items=" + items +
                '}';
    }
}
