package ser_by_java;

import java.io.*;

public class Main {

    public static void main(String[] args) throws Exception {
        saveLambda();
        loadLambda();
    }

    public static void saveLambda() throws IOException {
        Factory factory = new Factory(3);
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File("lambdas")));
        output.writeObject(factory.createLambda(4,new SerializableData(3.3,4)));
        output.writeObject(factory.createLambda(5,new SerializableData(5.3,-4)));
        output.writeObject(factory.createLambda(3,new SerializableData(10.3,+80e-5)));
        output.close();
    }

    public static void loadLambda() throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File("lambdas")));
        DoubleToString operator1 = (DoubleToString) input.readObject();
        DoubleToString operator2 = (DoubleToString) input.readObject();
        DoubleToString operator3 = (DoubleToString) input.readObject();
        System.out.println(operator1.get(5));
        System.out.println(operator2.get(5));
        System.out.println(operator3.get(5));
    }

}
