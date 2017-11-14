package ser_by_java;

import java.io.Serializable;

public class SerializableData implements Serializable {

    private double t1;
    private double t2;

    public SerializableData(double t1, double t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public double getT1() {
        return t1;
    }

    public void setT1(double t1) {
        this.t1 = t1;
    }

    public double getT2() {
        return t2;
    }

    public void setT2(double t2) {
        this.t2 = t2;
    }

}
