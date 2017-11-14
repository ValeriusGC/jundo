package serialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AnotherClass<V> implements Serializable {

    private List<SimpleClass<V>> simples = new ArrayList<>();



}
