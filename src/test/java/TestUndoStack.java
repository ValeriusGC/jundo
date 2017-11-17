import org.junit.Test;
import serialize.NonTrivialClass;
import serialize.SimpleClass;
import undomodel.*;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestUndoStack {

    UndoStack stack;
    Object[] arr;
    UndoSubject subj;

    /**
     * Create {@link UndoStack} with or without groups.
     */
    @Test
    public void creation() {

        {
            // Create without group
            UndoSubject subj = new SimpleClass<Integer>();
            UndoStack stack = new UndoStackT<>(subj, null);
            assertEquals(true, stack.isClean());
            assertEquals(false, stack.canRedo());
            assertEquals(false, stack.canUndo());
            assertEquals(0, stack.count());
            assertEquals(true, stack.isActive());
            assertEquals(subj, stack.getSubject());
            assertEquals("", stack.redoText());
            assertEquals("", stack.undoText());
            assertEquals(0, stack.getCleanIdx());
            assertEquals(0, stack.getIdx());
            assertEquals(0, stack.getUndoLimit());
        }

        {
            // Create with group
            // Checks:
            //  - setActive()
            //  - active()
            UndoSubject subjA = new SimpleClass<Integer>();
            UndoSubject subjB = new SimpleClass<Integer>();
            UndoGroup group = new UndoGroup();
            assertEquals(0, group.getStacks().size());

            UndoStack stackA = new UndoStackT<>(subjA, group);
            assertEquals(1, group.getStacks().size());
            assertEquals(null, group.getActive());
            assertEquals(false, stackA.isActive());

            // Set active thru UndoStack
            stackA.setActive(true);
            assertEquals(stackA, group.getActive());
            assertEquals(true, stackA.isActive());
            //
            stackA.setActive(false);
            assertEquals(null, group.getActive());
            assertEquals(false, stackA.isActive());

            // Set active thru UndoGroup
            group.setActive(stackA);
            assertEquals(stackA, group.getActive());
            assertEquals(true, stackA.isActive());
            //
            group.setActive(null);
            assertEquals(null, group.getActive());
            assertEquals(false, stackA.isActive());

            // Second stack. Do the same
            UndoStack stackB = new UndoStackT<>(subjB, group);
            assertEquals(2, group.getStacks().size());
            assertEquals(null, group.getActive());
            assertEquals(false, stackA.isActive());
            assertEquals(false, stackB.isActive());

            group.setActive(stackB);
            assertEquals(stackB, group.getActive());
            assertEquals(false, stackA.isActive());
            assertEquals(true, stackB.isActive());

            group.setActive(stackA);
            assertEquals(stackA, group.getActive());
            assertEquals(true, stackA.isActive());
            assertEquals(false, stackB.isActive());
        }

    }

    /**
     * Adding and clearing
     */
    @Test
    public void addAndClear() throws Exception {

        NonTrivialClass scene = new NonTrivialClass();
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStackT<>(scene, group);
        group.setActive(stack);

        stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, scene));
        assertEquals(1, stack.count());
        assertEquals(1, stack.getIdx());
        stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, scene));
        assertEquals(2, stack.count());
        assertEquals(2, stack.getIdx());
        stack.clear();
        assertEquals(0, stack.count());
        assertEquals(0, stack.getIdx());

    }

    /**
     * Set and check limits:
     *  - undoLimit
     *  - setIndex
     */
    @Test
    public void limits() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<Integer>();
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStackT<>(subj, group);
        stack.setUndoLimit(5);
        for (int i = 0; i < 10; ++i) {
            stack.push(new UndoCommandT<>(String.valueOf(i), subj::getValue, subj::setValue, i));
        }
        assertEquals(5, stack.count());
        stack.setIndex(0);
        assertEquals((Integer) 4, subj.getValue());
        stack.setIndex(stack.count());
        assertEquals((Integer) 9, subj.getValue());
    }

    /**
     * Set and check clean:
     *  - setClean
     *  - isClean
     *  - getCleanIdx
     */
    @Test
    public void clean() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<Integer>();
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStackT<>(subj, group);
        for (int i = 0; i < 10; ++i) {
            stack.push(new UndoCommandT<>(String.valueOf(i), subj::getValue, subj::setValue, i));
        }
        assertEquals(10, stack.count());
        stack.setIndex(5);
        assertEquals((Integer) 4, subj.getValue());
        stack.setClean();
        assertEquals(5, stack.getCleanIdx());
        assertEquals(true, stack.isClean());
        stack.undo();
        assertEquals(false, stack.isClean());
        stack.redo();
        assertEquals(true, stack.isClean());
        stack.redo();
        assertEquals(false, stack.isClean());
        stack.clear();
        assertEquals(0, stack.getCleanIdx());

        // Now set limit, set clean, and go out of it
        stack.setUndoLimit(5);
        for (int i = 0; i < 5; ++i) {
            stack.push(new UndoCommandT<>(String.valueOf(i), subj::getValue, subj::setValue, i));
        }
        assertEquals(5, stack.count());
        stack.setIndex(2);
        stack.setClean();
        assertEquals(2, stack.getCleanIdx());
        stack.setIndex(0);
        assertEquals(2, stack.getCleanIdx());
        stack.push(new UndoCommandT<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(-1, stack.getCleanIdx());
        assertEquals(false, stack.isClean());
    }

    /**
     *  - canUndo
     *  - canRedo
     *  - undoText
     *  - redoText
     */
    @Test
    public void auxProps() throws Exception {
        SimpleClass<Integer> subj = new SimpleClass<Integer>();
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStackT<>(subj, group);
        group.setActive(stack);
        assertEquals(false, stack.canUndo());
        assertEquals(false, stack.canRedo());
        assertEquals("", stack.undoText());
        assertEquals("", stack.redoText());
        assertEquals("", group.undoText());
        assertEquals("", group.redoText());
        assertEquals(false, group.canUndo());
        assertEquals(false, group.canRedo());

        for (int i = 0; i < 3; ++i) {
            stack.push(new UndoCommandT<>(String.valueOf(i), subj::getValue, subj::setValue, i));
        }
        assertEquals(true, stack.canUndo());
        assertEquals(false, stack.canRedo());
        assertEquals("2", stack.undoText());
        assertEquals("", stack.redoText());
        assertEquals("2", group.undoText());
        assertEquals("", group.redoText());
        assertEquals(true, group.canUndo());
        assertEquals(false, group.canRedo());

        group.undo();
        assertEquals(true, stack.canUndo());
        assertEquals(true, stack.canRedo());
        assertEquals("1", stack.undoText());
        assertEquals("2", stack.redoText());
        assertEquals("1", group.undoText());
        assertEquals("2", group.redoText());
        assertEquals(true, group.canUndo());
        assertEquals(true, group.canRedo());

        group.getActive().setIndex(0);
        assertEquals(false, stack.canUndo());
        assertEquals(true, stack.canRedo());
        assertEquals("", stack.undoText());
        assertEquals("0", stack.redoText());
        assertEquals("", group.undoText());
        assertEquals("0", group.redoText());
        assertEquals(false, group.canUndo());
        assertEquals(true, group.canRedo());
    }

    /**
     * Undo props like {@link Integer}, {@link String}, etc
     */
    @Test
    public void testSimpleUndo() throws Exception {

//        initSimple(new String[]{"one", null, "two"});
//        testSimple();

//        initSimple(new Integer[]{1, 2, 3, null, 8});
//        testSimple();

        initSimple(new Long[]{11L, 12L, 13L, 14L, 15L});
        testSimple();

        initSimple(new Double[]{1.1, 2.2, 3.222});
        testSimple();

        initSimple(new Boolean[]{true, false, true, null});
        testSimple();

    }

    @Test
    public void testNonTrivial() throws Exception {
        NonTrivialClass ntc = new NonTrivialClass();
        UndoStackT<NonTrivialClass> stack = new UndoStackT<>(ntc, null);
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

            NonTrivialClass.Item item = ((NonTrivialClass) stack.getSubject()).items.get(0);
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
            while (bf.ready()) {
                str2.append(bf.readLine());
            }
            String outStr = str2.toString();
            System.out.println("Output String : " + outStr);
            assertEquals(str, outStr);

        }


    }


    //------ Private section -------------------------------------------------------------------------------------------

    private <V> void initSimple(V[] array) throws Exception {
        System.out.println("initSimple: " + array.getClass().getCanonicalName());
        arr = array;
        subj = new SimpleClass<V>();
        stack = new UndoStackT<>(subj, null);
        for (V i : array) {
            stack.push(new UndoCommandT<>(null, ((SimpleClass<V>) subj)::getValue,
                    ((SimpleClass<V>) subj)::setValue, i));
        }
    }

    private <V> void testSimple() throws IOException, ClassNotFoundException {

        UndoStackT<SimpleClass<V>> stackBack = UndoUtils.deserialize(UndoUtils.serialize(stack));
        assertEquals(stack, stackBack);
        SimpleClass<V> objBack = stackBack.getSubject();
        assertEquals(subj, objBack);
        objBack.getValue();

        // Walk here and there
        for (int i = arr.length - 1; i > 0; i--) {
            stackBack.undo();
            System.out.println(objBack.getValue());
            assertEquals((arr[i - 1]), objBack.getValue());
        }
        for (int i = 1; i < arr.length; i++) {
            stackBack.redo();
            assertEquals(arr[i], objBack.getValue());
        }
    }

}
