package some;

/**
 * No serializable class
 */
public class MyContext {

    private int value = -4;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MyContext{" +
                "value=" + value +
                '}';
    }

}
