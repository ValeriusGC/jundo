import com.gdetotut.jundo.*;
import org.junit.Test;
import some.*;
import some.SimpleUndoWatcher;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static some.NonTrivialClass.Item.Type.CIRCLE;
import static some.NonTrivialClass.Item.Type.RECT;

public class UndoStackTest implements Serializable {

    UndoStack stack;
    Object[] arr;
    Serializable subj;

    @SuppressWarnings("unchecked")
    public <V extends Serializable> void initSimple(Class<V> type, V[] array) throws Exception {
        arr = array;
        subj = new SimpleClass<V>(type);
        stack = new UndoStack(subj, null);
        stack.setWatcher(new SimpleUndoWatcher());
        for (V i : array) {
            stack.push(new RefCmd<>(stack, "", ((SimpleClass<V>) subj)::getValue,
                    ((SimpleClass<V>) subj)::setValue, i, null));
        }
    }

    @SuppressWarnings("unchecked")
    public <V extends Serializable> void testSimple() throws IOException, ClassNotFoundException {

        UndoSerializer manager = new UndoSerializer(null, 333, stack);
        UndoSerializer managerBack = UndoSerializer.deserialize(
                UndoSerializer.serialize(manager, false, null),
                null);
        UndoStack stackBack = managerBack.getStack();
        // Here we can not compare stacks themselves 'cause of stack's comparison principle
        assertEquals(stack.getSubj(), stackBack.getSubj());
        SimpleClass<V> objBack = (SimpleClass<V>) stackBack.getSubj();
        assertEquals(subj, objBack);

        // Walk here and there
        for (int i = arr.length - 1; i > 0; i--) {
            stackBack.undo();
//            System.out.println(objBack.getValue());
            assertEquals((arr[i - 1]), objBack.getValue());
        }
        for (int i = 1; i < arr.length; i++) {
            stackBack.redo();
            assertEquals(arr[i], objBack.getValue());
        }
    }


    /**
     * Simply shows how elegant {@link RefCmd} works
     */
    @Test
    public void testIntegerClass() throws Exception {

        Point pt = new Point(-30, -40);
        UndoStack stack = new UndoStack(pt, null);
        UndoCommand undoCommand = new UndoCommand(stack, "Move point", null);
        new RefCmd<>(stack, "Change x", pt::getX, pt::setX, 10, undoCommand);
        new RefCmd<>(stack, "Change y", pt::getY, pt::setY, 20, undoCommand);
        stack.push(undoCommand);
        assertEquals(1, stack.count());
        assertEquals(10, pt.getX());
        assertEquals(20, pt.getY());
        stack.undo();
        assertEquals(-30, pt.getX());
        assertEquals(-40, pt.getY());
        assertEquals(0, stack.getIdx());

        UndoSerializer manager = new UndoSerializer(null, 4, stack);
        manager = UndoSerializer.deserialize(UndoSerializer.serialize(manager, true, null), null);
        UndoStack stackBack = manager.getStack();
        Point ptBack = (Point) stackBack.getSubj();
        assertEquals(pt, ptBack);
        assertEquals(-30, ptBack.getX());
        assertEquals(-40, ptBack.getY());
        assertEquals(1, stackBack.count());
        assertEquals(0, stackBack.getIdx());

        stackBack.redo();
        // ))
        stackBack.redo();
        stackBack.redo();
        assertEquals(10, ptBack.getX());
        assertEquals(20, ptBack.getY());


    }

    /**
     * Create {@link UndoStack} with or without groups.
     */
    @Test
    public void creation() {

        {
            // Create without group
            Serializable subj = new SimpleClass<>(Integer.class);
            UndoStack stack = new UndoStack(subj, null);
            stack.setWatcher(new SimpleUndoWatcher());
            assertEquals(true, stack.isClean());
            assertEquals(false, stack.canRedo());
            assertEquals(false, stack.canUndo());
            assertEquals(0, stack.count());
            assertEquals(true, stack.isActive());
            assertEquals(subj, stack.getSubj());
            assertEquals("", stack.redoCaption());
            assertEquals("", stack.undoCaption());
            assertEquals(0, stack.getCleanIdx());
            assertEquals(0, stack.getIdx());
            assertEquals(0, stack.getUndoLimit());
        }

        {
            // Create with group
            // Checks:
            //  - setActive()
            //  - active()
            Serializable subjA = new SimpleClass<>(Integer.class);
            Serializable subjB = new SimpleClass<>(String.class);
            assertNotEquals(subjA, subjB);
            UndoGroup group = new UndoGroup();
            assertEquals(0, group.getStacks().size());

            UndoStack stackA = new UndoStack(subjA, group);
            stackA.setWatcher(new SimpleUndoWatcher());
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
            UndoStack stackB = new UndoStack(subjB, group);
            stackB.setWatcher(new SimpleUndoWatcher());
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
        UndoStack stack = new UndoStack(scene, group);
        stack.setWatcher(new SimpleUndoWatcher());
        group.setActive(stack);

        stack.push(new NonTrivialClass.AddCommand(stack, CIRCLE, scene, null));
        assertEquals(1, stack.count());
        assertEquals(1, stack.getIdx());
        stack.push(new NonTrivialClass.AddCommand(stack, CIRCLE, scene, null));
        assertEquals(2, stack.count());
        assertEquals(2, stack.getIdx());
        stack.clear();
        assertEquals(0, stack.count());
        assertEquals(0, stack.getIdx());

    }

    /**
     * Set and check limits:
     * - undoLimit
     * - setIndex
     */
    @Test
    public void limits() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());
        stack.setUndoLimit(5);
        for (int i = 0; i < 10; ++i) {
            stack.push(new RefCmd<>(stack, String.valueOf(i), subj::getValue, subj::setValue, i, null));
        }
        assertEquals(5, stack.count());
        stack.setIndex(0);
        assertEquals(0, stack.getIdx());
        assertEquals((Integer) 4, subj.getValue());
        stack.setIndex(stack.count());
        assertEquals(stack.count(), stack.getIdx());
        assertEquals((Integer) 9, subj.getValue());
    }

    /**
     * Set and check clean:
     * - setClean
     * - isClean
     * - getCleanIdx
     */
    @Test
    public void clean() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());
        for (int i = 0; i < 10; ++i) {
            stack.push(new RefCmd<>(stack, String.valueOf(i), subj::getValue, subj::setValue, i, null));
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
            stack.push(new RefCmd<>(stack, String.valueOf(i), subj::getValue, subj::setValue, i, null));
        }
        assertEquals(5, stack.count());
        stack.setIndex(2);
        stack.setClean();
        assertEquals(2, stack.getCleanIdx());
        stack.setIndex(0);
        assertEquals(2, stack.getCleanIdx());
        stack.push(new RefCmd<>(stack, String.valueOf(10), subj::getValue, subj::setValue, 10, null));
        assertEquals(-1, stack.getCleanIdx());
        assertEquals(false, stack.isClean());
    }

    /**
     * - canUndo
     * - canRedo
     * - undoCaption
     * - redoCaption
     */
    @Test
    public void auxProps() throws Exception {
        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());
        group.setActive(stack);
        assertEquals(false, stack.canUndo());
        assertEquals(false, stack.canRedo());
        assertEquals("", stack.undoCaption());
        assertEquals("", stack.redoCaption());
        assertEquals("", group.undoCaption());
        assertEquals("", group.redoCaption());
        assertEquals(false, group.canUndo());
        assertEquals(false, group.canRedo());

        for (int i = 0; i < 3; ++i) {
            stack.push(new RefCmd<>(stack, String.valueOf(i), subj::getValue, subj::setValue, i, null));
        }
        assertEquals(true, stack.canUndo());
        assertEquals(false, stack.canRedo());
        assertEquals("2", stack.undoCaption());
        assertEquals("", stack.redoCaption());
        assertEquals("2", group.undoCaption());
        assertEquals("", group.redoCaption());
        assertEquals(true, group.canUndo());
        assertEquals(false, group.canRedo());

        group.undo();
        assertEquals(true, stack.canUndo());
        assertEquals(true, stack.canRedo());
        assertEquals("1", stack.undoCaption());
        assertEquals("2", stack.redoCaption());
        assertEquals("1", group.undoCaption());
        assertEquals("2", group.redoCaption());
        assertEquals(true, group.canUndo());
        assertEquals(true, group.canRedo());

        group.getActive().setIndex(0);
        assertEquals(false, stack.canUndo());
        assertEquals(true, stack.canRedo());
        assertEquals("", stack.undoCaption());
        assertEquals("0", stack.redoCaption());
        assertEquals("", group.undoCaption());
        assertEquals("0", group.redoCaption());
        assertEquals(false, group.canUndo());
        assertEquals(true, group.canRedo());
    }

    /**
     * Undo props like {@link Integer}, {@link String}, etc
     */
    @Test
    public void testSimpleUndo() throws Exception {

        initSimple(String.class, new String[]{"one", null, "two"});
        testSimple();

        initSimple(Integer.class, new Integer[]{1, 2, 3, null, 8});
        testSimple();

        initSimple(Long.class, new Long[]{11L, 12L, 13L, 14L, null});
        testSimple();

        initSimple(Double.class, new Double[]{1.1, 2.2, 3.222});
        testSimple();

        initSimple(Boolean.class, new Boolean[]{true, false, true, null});
        testSimple();

    }

    @Test
    public void testNonTrivial() throws Exception {
        NonTrivialClass ntc = new NonTrivialClass();
        UndoStack stack = new UndoStack(ntc, null);
        stack.setWatcher(new SimpleUndoWatcher());
        assertEquals(0, ntc.items.size());

        {
            stack.push(new NonTrivialClass.AddCommand(stack, CIRCLE, ntc, null));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);

            stack.push(new NonTrivialClass.AddCommand(stack, RECT, ntc, null));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(2, ntc.items.size());
//            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
//            System.out.println(ntc);

            UndoSerializer manager = new UndoSerializer(null, 333, stack);
            UndoSerializer managerBack =
                    UndoSerializer.deserialize(UndoSerializer.serialize(manager, false, null), null);
            UndoStack stackBack = managerBack.getStack();
//            assertEquals(stack, stackBack);
            NonTrivialClass objBack = (NonTrivialClass) stackBack.getSubj();
//            assertEquals(subj, objBack);

//            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count());
            assertEquals(0, stackBack.getIdx());
            assertEquals(0, objBack.items.size());
//            System.out.println(objBack);

            stackBack.redo();
            assertEquals(1, objBack.items.size());
//            System.out.println(objBack);

            stackBack.redo();
            assertEquals(2, objBack.items.size());
//            System.out.println(objBack);
        }


        {
//            System.out.println("--- Add/Del ---");
            stack.push(new NonTrivialClass.AddCommand(stack, CIRCLE, ntc, null));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);
            stack.push(new NonTrivialClass.DeleteCommand(stack, ntc, null));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(0, ntc.items.size());
//            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
//            System.out.println(ntc);

            stack.undo();
            assertEquals(2, stack.count());
            assertEquals(0, stack.getIdx());
            assertEquals(0, ntc.items.size());
//            System.out.println(ntc);

        }

        {
//            System.out.println("--- Add/Del/Move ---");
            stack.redo();
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());

            NonTrivialClass.Item item = ((NonTrivialClass) stack.getSubj()).items.get(0);
            int newPos = 100;
            int oldPos = item.x;
            item.x = newPos; // Moved
            stack.push(new NonTrivialClass.MovedCommand(stack, item, oldPos, null));
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
            stack.push(new NonTrivialClass.MovedCommand(stack, item, item.x, null));
            assertEquals(2, stack.count());
            assertEquals(2, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println("4: " + stack);


            // Back
            stack.undo();
            assertEquals(oldPos, item.x);
            assertEquals(2, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());

            // Serialize
            UndoSerializer manager = new UndoSerializer(null, 333, stack);
            UndoSerializer managerBack =
                    UndoSerializer.deserialize(UndoSerializer.serialize(manager, false, null), null);
            UndoStack stackBack = managerBack.getStack();
            NonTrivialClass objBack = (NonTrivialClass) stackBack.getSubj();

//            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count());
            assertEquals(1, stackBack.getIdx());
            assertEquals(1, objBack.items.size());
//            System.out.println(objBack);

            stackBack.redo();
            assertEquals(1, objBack.items.size());
//            System.out.println(objBack);

        }

        {

            UndoSerializer manager = new UndoSerializer(null, 333, stack);
            String str = UndoSerializer.serialize(manager, false, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(str.getBytes("UTF-8"));
            gzip.close();
//            System.out.println("Output String : " + str);
//            System.out.println("Unzipped length : " + str.length());
//            System.out.println("Zipped length : " + baos.size());
//            System.out.println("Zip : " + new String(baos.toByteArray()));

            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
            BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
            StringBuilder str2 = new StringBuilder();
            while (bf.ready()) {
                str2.append(bf.readLine());
            }
            String outStr = str2.toString();
//            System.out.println("Output String : " + outStr);
            assertEquals(str, outStr);

        }


    }

//    @Test
//    public void context() throws Exception {
//
//        // class Cmd
//        //
//        // Keep in mind that class of Command should be Serializable (and Superclass UndoStackTest too)
//        class Cmd extends UndoCommand {
//            int oldV, newV;
//
//            public Cmd(@NonNull String caption, int v) {
//                super(caption, null);
//                newV = v;
//            }
//
//            @Override
//            protected void doRedo() {
//                if (context != null && context instanceof MyContext) {
//                    MyContext ctx = (MyContext) context;
//                    oldV = ctx.getValue();
//                    ctx.setValue(newV);
//                }
//            }
//
//            @Override
//            protected <Context> void doUndo(Context context) {
//                if (context != null && context instanceof MyContext) {
//                    MyContext ctx = (MyContext) context;
//                    ctx.setValue(oldV);
//                }
//            }
//        }
//        //~class Cmd
//
//        // When making undo for non-serializable class
//        //
//        // Nonserializable class
//        MyContext ctx = new MyContext();
//        int start = ctx.getValue();
//
//        UndoStack stack = new UndoStack(null, null);
//        stack.setWatcher(new SimpleUndoWatcher());
//        stack.setContext(ctx);
//        stack.push(new Cmd("Change X to 10", 10));
//
//        assertEquals(10, ctx.getValue());
//        stack.push(new Cmd("Change X to 20", 20));
//        assertEquals(20, ctx.getValue());
//        stack.undo();
//        assertEquals(10, ctx.getValue());
//        stack.undo();
//        assertEquals(start, ctx.getValue());
//        stack.setIndex(stack.count());
//        assertEquals(20, ctx.getValue());
//
//
//        // Suppose that we are forced to save/restore UndoStack and process it again.
//        // We know MyContext state and want restore undo-chain.
//
//        UndoSerializer manager = new UndoSerializer(null, 4, stack);
//        UndoSerializer.serialize(manager, true);
//        manager = UndoSerializer.deserialize(UndoSerializer.serialize(manager, true), ctx);
//        UndoStack stackBack = manager.getStack();
//
//        stackBack.undo();
//        assertEquals(10, ctx.getValue());
//        stackBack.undo();
//        assertEquals(start, ctx.getValue());
//
//        // ~When making undo for non-serializable class
//
//    }

    /**
     * Test for command's chain
     * <p>Makes command chain without using macrocommands.
     */
    @Test
    public void chain() throws Exception {

        // TODO Make the same in KUndo

        {
            // Independently
            final int x = 10;
            final int y = 20;
            Point subj = new Point(x, y);
            UndoStack stack = new UndoStack(subj, null);
            UndoCommand parentCmd = new UndoCommand(stack, "parent", null);
            new RefCmd<>(stack, "move 1", subj::getY, subj::setY, 50, parentCmd);
            new RefCmd<>(stack, "move 2", subj::getX, subj::setX, 35, parentCmd);
            new RefCmd<>(stack, "move 3", subj::getY, subj::setY, 55, parentCmd);
            new RefCmd<>(stack, "move 4", subj::getX, subj::setX, 39, parentCmd);
            parentCmd.redo();
            assertEquals(39, subj.getX());
            assertEquals(55, subj.getY());

            parentCmd.undo();
            assertEquals(x, subj.getX());
            assertEquals(y, subj.getY());
        }

        {
            // In stack
            final int x = 10;
            final int y = 20;
            Point subj = new Point(x, y);
            UndoStack stack = new UndoStack(subj, null);
            UndoCommand parentCmd = new UndoCommand(stack, "parent", null);
            new RefCmd<>(stack, "move 1", subj::getY, subj::setY, 50, parentCmd);
            new RefCmd<>(stack, "move 2", subj::getX, subj::setX, 35, parentCmd);
            new RefCmd<>(stack, "move 3", subj::getY, subj::setY, 55, parentCmd);
            new RefCmd<>(stack, "move 4", subj::getX, subj::setX, 39, parentCmd);
            stack.push(parentCmd);
            assertEquals(39, subj.getX());
            assertEquals(55, subj.getY());

            stack.undo();
            assertEquals(x, subj.getX());
            assertEquals(y, subj.getY());
            stack.undo();
            assertEquals(x, subj.getX());
            assertEquals(y, subj.getY());


            stack.redo();
            assertEquals(39, subj.getX());
            assertEquals(55, subj.getY());
            stack.redo();
            assertEquals(39, subj.getX());
            assertEquals(55, subj.getY());
        }
    }


}
