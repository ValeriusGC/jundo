package serialize.sermod;

import com.sun.istack.internal.NotNull;

import java.lang.reflect.Field;

public class SimpleCommand<T, V> extends BaseCommand {

    private final SetterIntf<V> setter;
    private final V oldValue;
    private final V newValue;

    public SimpleCommand(@NotNull GetterIntf<V> getter, @NotNull SetterIntf<V> setter, V newValue)
            throws Exception {
        if(getter == null || setter == null) {
            throw new Exception("null parameters!");
        }
        this.setter = setter;
        this.oldValue = getter.get();
        this.newValue = newValue;
    }

    public void doUndo() {
        setter.set(oldValue);
    }

    public void doRedo() {
        setter.set(newValue);
    }

}
