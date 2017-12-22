package com.gdetotut.jundo;

import java.io.Serializable;

@FunctionalInterface
public interface Getter<V extends Serializable> extends Serializable{
    V get();
}
