package undomodel;

import com.sun.istack.internal.Nullable;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;


abstract public class UndoCommand<T, V> extends UndoBaseCommand{

    protected final T object;
    protected final Field f;
    protected final UndoGetter<V> getter;
    protected final UndoSetter<V> setter;
    protected final V oldValue;
    protected final V newValue;

    /**
     * For direct access to field.
     * @param text
     * @param parent
     * @param object
     * @param fieldName
     * @param newValue
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public UndoCommand(@Nullable String text, @Nullable UndoCommand parent, @NonNull T object,
                       @NonNull String fieldName, @Nullable V newValue) throws NoSuchFieldException, IllegalAccessException {
        super(text, parent);
        this.object = object;
        f = object.getClass().getDeclaredField(fieldName);
        this.getter = null;
        this.setter = null;
        this.newValue = newValue;
        this.oldValue = (V)f.get(object);
    }

    /**
     * For access to field via accessors.
     * @param text
     * @param parent
     * @param object
     * @param getter
     * @param setter
     * @param newValue
     * @throws NoSuchFieldException
     */
    public UndoCommand(@Nullable String text, @Nullable UndoCommand parent, @NonNull T object,
                       @NonNull UndoGetter<V> getter, @NonNull UndoSetter<V> setter, @Nullable V newValue) {
        super(text, parent);
        this.object = object;
        f = null;
        this.getter = getter;
        this.setter = setter;
        this.newValue = newValue;
        this.oldValue = getter.get();
    }

    abstract public void undo() throws IllegalAccessException;

    abstract public void redo() throws IllegalAccessException;

    protected void doUndo() throws IllegalAccessException {
        if(getter == null) {
            f.set(object, oldValue);
        }else {
            setter.set(oldValue);
        }
    }

    protected void doRedo() throws IllegalAccessException {
        if(setter == null) {
            f.set(object, newValue);
        }else {
            setter.set(newValue);
        }
    }

}
