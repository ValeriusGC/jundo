package serialize;

import undomodel.UndoSubject;

import java.io.Serializable;

public class SimpleClass<V> extends UndoSubject{

    @Override
    public String toString() {
        return "SimpleClass{" +
                "value=" + value +
                '}';
    }

    private V value;

    public V getValue() {
        if(value != null) {
            System.out.println("getV: " + value.getClass().getCanonicalName());
        }
        return value;
    }

    public void setValue(V value) {
        this.value = value;
        if(value != null) {
            System.out.println("setV: " + value.getClass().getCanonicalName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleClass<?> that = (SimpleClass<?>) o;

        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
