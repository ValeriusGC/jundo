package undomodel;

import java.io.Serializable;

@FunctionalInterface
public interface Setter<V> extends Serializable{
    void set(V v);
}
