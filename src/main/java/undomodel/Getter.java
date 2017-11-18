package undomodel;

import java.io.Serializable;

@FunctionalInterface
public interface Getter<V> extends Serializable{
    V get();
}
