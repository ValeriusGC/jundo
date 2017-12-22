package com.gdetotut.jundo;

import java.io.Serializable;

/**
 * Interface for value's setter. Used in {@link RefCmd}
 * @param <V> the type of this value.
 */
@FunctionalInterface
public interface Setter<V extends Serializable> extends Serializable{
    void set(V v);
}
