package serialize;

import undomodel.UndoStack;
import undomodel.UndoCommandT;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FuncMain {

    static final String name = "my_objs";

    public static void main(String[] args) throws Exception {
        save();
        load();

    }

    private static void save() throws Exception {

        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File(name)));
        ComplexClass point = new ComplexClass();
        point.setY(100);
        point.setLabel("Start label");
        System.out.println("0: " + point);
        UndoStack stack = new UndoStack("ComplexClass");
        stack.push(new UndoCommandT<>(point::getLabel, point::setLabel, "I changed Label #1"));
        System.out.println("1: " + point);
        stack.push(new UndoCommandT<>(point::getLabel, point::setLabel, "I changed Label #2"));
        System.out.println("2: " + point);
        stack.push(new UndoCommandT<>(point::getY, point::setY, 200));
        System.out.println("3: " + point);
        stack.push(new UndoCommandT<>(point::getY, point::setY, 300));
        System.out.println("4: " + point);
        stack.push(new UndoCommandT<>(point::getItems, point::setItems, new ArrayList<>(Arrays.asList(new ComplexClass.Item(222)))));
        System.out.println("5: " + point);
        System.out.println("----------");
        output.writeObject(point);
        output.writeObject(stack);
        output.close();
    }

    private static void load() throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(name)));
        ComplexClass point = (ComplexClass) input.readObject();
        UndoStack stack = (UndoStack) input.readObject();
        System.out.println(" : " + point);
        System.out.println("----------");
        while (stack.getIndex() > 0) {
            stack.undo();
            System.out.println(" : " + point);
        }
        System.out.println("----------");
        while (stack.getIndex() < stack.count()) {
            stack.redo();
            System.out.println(" : " + point);
        }
        System.out.println("--------");
    }

}
