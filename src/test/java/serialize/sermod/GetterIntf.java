package serialize.sermod;

import java.io.Serializable;

@FunctionalInterface
public interface GetterIntf<V> extends Serializable{
    V get();
}
