import org.junit.Test;
import serialize.NonTrivialClass;
import serialize.SimpleClass;
import undomodel.*;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestUndoStack {

    UndoStack stack;
    Object[] arr;
    Object object;

    public <V> void initSimple(V[] array) throws Exception {
        arr = array;
        object = new SimpleClass<V>();
        stack = new UndoStackT<>(null, object);
        for (V i : array) {
            stack.push(new UndoCommandT<>(((SimpleClass<V>)object)::getValue, ((SimpleClass<V>)object)::setValue, i));
//            System.out.println(object);
        }
    }

    public <V> void testSimple() throws IOException, ClassNotFoundException {

        UndoStackT<SimpleClass<V>> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
        assertEquals(stack, stackBack);
        SimpleClass<V> objBack = stackBack.getObject();
        assertEquals(object, objBack);

//        System.out.println("-----------------");
        // Walk here and there
        for(int i = arr.length - 1; i > 0; i--) {
            stackBack.undo();
//            System.out.println(objBack);
            assertEquals((arr[i-1]), objBack.getValue());
        }
//        System.out.println("-----------------");
        for(int i = 1; i < arr.length; i++) {
            stackBack.redo();
//            System.out.println(objBack);
            assertEquals(arr[i], objBack.getValue());
        }
//        System.out.println("=================");
    }
    /**
     * Undo props like {@link Integer}, {@link String}, etc
     */
    @Test
    public void testSimpleUndo() throws Exception {

        initSimple(new Integer[]{1,2,3,null, 8});
        testSimple();

        initSimple(new Long[]{1L,2L,3L, null, 5L});
        testSimple();

        initSimple(new String[]{"one", null, "two"});
        testSimple();

        initSimple(new Double[]{1.1,2.2,3.222});
        testSimple();

        initSimple(new Boolean[]{true, false, true, null});
        testSimple();

    }

    @Test
    public void testNonTrivial() throws Exception {
        NonTrivialClass aClass = new NonTrivialClass();
        UndoStack stack = new UndoStackT<>(null, aClass);
        assertEquals(0, aClass.items.size());

        {
            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, aClass));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIndex());
            assertEquals(1, aClass.items.size());
            System.out.println(aClass);

            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.RECT, aClass));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIndex());
            assertEquals(2, aClass.items.size());
            System.out.println(aClass);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIndex());
            assertEquals(1, aClass.items.size());
            System.out.println(aClass);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIndex());
            assertEquals(0, aClass.items.size());
            System.out.println(aClass);

            UndoStackT<NonTrivialClass> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
//            assertEquals(stack, stackBack);
            NonTrivialClass objBack = stackBack.getObject();
//            assertEquals(object, objBack);

            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count());
            assertEquals(0, stackBack.getIndex());
            assertEquals(0, objBack.items.size());
            System.out.println(objBack);

            stackBack.redo();
            assertEquals(1, objBack.items.size());
            System.out.println(objBack);

            stackBack.redo();
            assertEquals(2, objBack.items.size());
            System.out.println(objBack);
        }


        {
            System.out.println("--- Add/Del ---");
            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, aClass));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIndex());
            assertEquals(1, aClass.items.size());
            System.out.println(aClass);
            stack.push(new NonTrivialClass.DeleteCommand(aClass));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIndex());
            assertEquals(0, aClass.items.size());
            System.out.println(aClass);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIndex());
            assertEquals(1, aClass.items.size());
            System.out.println(aClass);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIndex());
            assertEquals(0, aClass.items.size());
            System.out.println(aClass);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIndex());
            assertEquals(0, aClass.items.size());
            System.out.println(aClass);

        }

        {
            System.out.println("--- Add/Del/Move ---");
            // TODO: 14.11.17 Продолжать доделывать команды и тестировать
        }


    }

}
