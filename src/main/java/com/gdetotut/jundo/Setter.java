package com.gdetotut.jundo;

import java.io.Serializable;

@FunctionalInterface
public interface Setter<V extends java.io.Serializable> extends Serializable{
    void set(V v);
}
