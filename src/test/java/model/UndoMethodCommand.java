package model;

import jdk.nashorn.internal.objects.annotations.Getter;
import undomodel.Getter2;
import undomodel.Setter2;
import undomodel.UndoCommand;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class UndoMethodCommand<T, V> extends UndoCommand {

    private T object;
    private Field f;
    private Getter2<V> getter;
    private Setter2<V> setter;
    private V oldValue;
    private V newValue;


//    public UndoMethodCommand(UndoCommand parent, T object, String getterName, String setterName, V newValue)
//            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        super(parent);
//        this.object = object;
//        getter = object.getClass().getMethod(getterName);
//        setter = object.getClass().getMethod(setterName, newValue.getClass());
//        this.newValue = newValue;
//        this.oldValue = (V) getter.invoke(object);
//    }

//    public UndoMethodCommand(String text, UndoCommand parent, T object, String getterName, String setterName, V newValue)
//            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        super(text, parent);
//        this.object = object;
//        getter = object.getClass().getMethod(getterName);
//        setter = object.getClass().getMethod(setterName, newValue.getClass());
//        this.newValue = newValue;
//        this.oldValue = (V) getter.invoke(object);
//    }

    public UndoMethodCommand(String text, UndoCommand parent, T object, String fieldName, Getter2<V> getter, Setter2<V> setter, V newValue)
            throws NoSuchFieldException {
        super(text, parent);
        init(object, fieldName, getter, setter, newValue);
    }

    private void init(T object, String fieldName, Getter2<V> getter, Setter2<V> setter, V newValue) throws NoSuchFieldException {
        this.object = object;
        f = object.getClass().getDeclaredField(fieldName);
        this.getter = getter;
        this.setter = setter;
        this.newValue = newValue;
        this.oldValue = getter.get();
    }


    public void undo() throws IllegalAccessException, InvocationTargetException {
        setter.set(oldValue);
    }

    public void redo() throws IllegalAccessException, InvocationTargetException {
        setter.set(newValue);
    }
}
