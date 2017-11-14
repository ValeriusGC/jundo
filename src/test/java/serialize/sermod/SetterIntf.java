package serialize.sermod;

import java.io.Serializable;

@FunctionalInterface
public interface SetterIntf<V> extends Serializable{
    void set(V v);
}
