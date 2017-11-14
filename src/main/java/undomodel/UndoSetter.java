package undomodel;

import java.io.Serializable;

@FunctionalInterface
public interface UndoSetter<V> extends Serializable{
    void set(V v);
}
