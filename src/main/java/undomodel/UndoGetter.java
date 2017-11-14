package undomodel;

import java.io.Serializable;

@FunctionalInterface
public interface UndoGetter<V> extends Serializable{
    V get();
}
