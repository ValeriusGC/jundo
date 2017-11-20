package undomodel;

import java.io.Serializable;

@FunctionalInterface
public interface Getter<V extends java.io.Serializable> extends Serializable{
    V get();
}
