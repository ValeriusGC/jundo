package serialize;

import java.io.Serializable;

public class SimpleClass<V> implements Serializable {

    private V value;
    // Local technique to diff one Type from another in equal()
    private Class<V> type;

    public SimpleClass(Class<V> type) {
        this.type = type;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SimpleClass{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleClass<?> that = (SimpleClass<?>) o;

        return value != null ? value.equals(that.value) : that.value == null
                && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }


}
