import org.junit.Test;
import serialize.NonTrivialClass;
import serialize.SimpleClass;
import undomodel.*;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;

public class TestUndoStack {

    UndoStack stack;
    Object[] arr;
    UndoSubject subj;

    public <V> void initSimple(V[] array) throws Exception {
        arr = array;
        subj = new SimpleClass<V>();
        stack = new UndoStackT<>(subj, null);
        for (V i : array) {
            stack.push(new UndoCommandT<>(null, ((SimpleClass<V>) subj)::getValue,
                    ((SimpleClass<V>) subj)::setValue, i));
        }
    }

    public <V extends UndoSubject> void testSimple() throws IOException, ClassNotFoundException {

        UndoStackT<SimpleClass<V>> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
        assertEquals(stack, stackBack);
        SimpleClass<V> objBack = stackBack.getSubject();
        assertEquals(subj, objBack);

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
        NonTrivialClass ntc = new NonTrivialClass();
        UndoStackT<NonTrivialClass> stack = new UndoStackT<>(ntc,null);
        assertEquals(0, ntc.items.size());

        {
            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, ntc));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println(ntc);

            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.RECT, ntc));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(2, ntc.items.size());
            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
            System.out.println(ntc);

            UndoStackT<NonTrivialClass> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
//            assertEquals(stack, stackBack);
            NonTrivialClass objBack = (NonTrivialClass) stackBack.getSubject();
//            assertEquals(subj, objBack);

            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count());
            assertEquals(0, stackBack.getIdx());
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
            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, ntc));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println(ntc);
            stack.push(new NonTrivialClass.DeleteCommand(ntc));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(0, ntc.items.size());
            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
            System.out.println(ntc);

        }

        {
            System.out.println("--- Add/Del/Move ---");
            stack.redo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());

            NonTrivialClass.Item item = ((NonTrivialClass)stack.getSubject()).items.get(0);
            int newPos = 100;
            int oldPos = item.x;
            item.x = newPos; // Moved
            System.out.println("1: " + item);
            stack.push(new NonTrivialClass.MovedCommand(item, oldPos));
            System.out.println("2: " + item);
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(1, ntc.items.size());

            assertEquals(newPos, item.x);
            stack.undo();
            assertEquals(oldPos, item.x);
            stack.redo();
            assertEquals(newPos, item.x);

            // Merge
            newPos = 200;
            item.x = newPos; // Moved again
            stack.push(new NonTrivialClass.MovedCommand(item, item.x));
            System.out.println("3: " + item);
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println("4: " + stack);


            // Back
            stack.undo();
            assertEquals(oldPos, item.x);
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
            System.out.println("4: " + item);

            // Serialize
            UndoStackT<NonTrivialClass> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
            NonTrivialClass objBack = (NonTrivialClass) stackBack.getSubject();

            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count());
            assertEquals(1, stackBack.getIdx());
            assertEquals(1, objBack.items.size());
            System.out.println(objBack);

            stackBack.redo();
            assertEquals(1, objBack.items.size());
            System.out.println(objBack);

        }

        {

            String str = UndoUtils.serialize(stack);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(str.getBytes("UTF-8"));
            gzip.close();
            System.out.println("Output String : " + str);
            System.out.println("Unzipped length : " + str.length());
            System.out.println("Zipped length : " + baos.size());
            System.out.println("Zip : " + new String(baos.toByteArray()));

            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
            BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
            StringBuilder str2 = new StringBuilder();
            while (bf.ready()){
                str2.append(bf.readLine());
            }
            String outStr = str2.toString();
            System.out.println("Output String : " + outStr);
            assertEquals(str, outStr);

        }


    }

}
