package serialize;

import serialize.sermod.BaseCommand;
import serialize.sermod.SerStack;
import serialize.sermod.SimpleCommand;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuncMain {

    static final String name = "my_objs";

    public static void main(String[] args) throws Exception {
        save();
        load();
    }

    private static void save() throws Exception {

        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File(name)));
        PointForSer point = new PointForSer();
        point.setY(100);
        point.setLabel("Start label");
        System.out.println("0: " + point);
        SerStack stack = new SerStack();
        stack.push(new SimpleCommand<>(point::getLabel, point::setLabel, "I changed Label #1"));
        System.out.println("1: " + point);
        stack.push(new SimpleCommand<>(point::getLabel, point::setLabel, "I changed Label #2"));
        System.out.println("2: " + point);
        stack.push(new SimpleCommand<>(point::getY, point::setY, 200));
        System.out.println("3: " + point);
        stack.push(new SimpleCommand<>(point::getY, point::setY, 300));
        System.out.println("4: " + point);
        stack.push(new SimpleCommand<PointForSer, List<PointForSer.Item>>(point::getItems, point::setItems,
                new ArrayList<>(Arrays.asList(new PointForSer.Item(222)))));
        System.out.println("5: " + point);
        System.out.println("----------");
        output.writeObject(point);
        output.writeObject(stack);
        output.close();
    }


    private static void load() throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(name)));
        PointForSer point = (PointForSer) input.readObject();
        SerStack stack = (SerStack) input.readObject();
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
