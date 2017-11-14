package ser_by_java;

import java.io.Serializable;

public class Factory implements Serializable {

    private int c;

    public Factory(int c) {
        this.c = c;
    }

    public DoubleToString createLambda(int i, SerializableData data) {
        return t -> "int value: " + Integer.toString(i) +
                "\nclass int value: " + String.valueOf(c) +
                "\nfirst double value: " + String.valueOf(data.getT1()) +
                "\nsecond double value: " + String.valueOf(data.getT2()) +
                "\nparameter: " + String.valueOf(t);
    }

}
