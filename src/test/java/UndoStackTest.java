import com.gdetotut.jundo.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static some.NonTrivialClass.Item.Type.CIRCLE;
import static some.NonTrivialClass.Item.Type.RECT;
import static some.TextSampleCommands.SUBJ_ID;

public class UndoStackTest implements Serializable {

    UndoStack stack;
    Object[] arr;
    Serializable subj;

    static class Macros implements Serializable {
        final List<UndoMacro> items = new ArrayList<>();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper function.
     */
    @SuppressWarnings("unchecked")
    public <V extends Serializable> void initSimple(Class<V> type, V[] array) throws Exception {
        arr = array;
        subj = new SimpleClass<V>(type);
        stack = new UndoStack(subj);
        stack.setWatcher(new SimpleUndoWatcher());
        for (V i : array) {
            stack.push(new RefCmd<>("", ((SimpleClass<V>) subj)::getValue,
                    ((SimpleClass<V>) subj)::setValue, i));
        }
    }

    /**
     * Helper function.
     */
    @SuppressWarnings("unchecked")
    public <V extends Serializable> void makeTestSimple() throws Exception {

        String store = UndoPacket
                .make(stack, "not_used", 333)
                .store();
        UndoStack stackBack = UndoPacket
                .peek(store, null)
                .restore(null, null)
                .prepare(null);

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


    @Test
    public void testGetLocalContexts() {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        assertNotNull(stack);
    }

    @Test
    public void testGetCommand() {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        assertEquals(null, stack.getCommand(0));
        assertEquals(null, stack.getCommand(-1));
        assertEquals(null, stack.getCommand(1000));
    }

    @Test
    public void testStackCompare() {
        UndoStack stack1 = new UndoStack(new Point(1, 1));
        UndoStack stack2 = new UndoStack(new Point(1, 1));
        assertNotEquals(stack1, stack2);
    }

    @Test
    public void testAddChildEx() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoCommand parent = new UndoCmdStub("");
        parent.addChild(null);
        thrown = ExpectedException.none();
    }


    @Test
    public void testClear() throws Exception {

        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);

        // for 100% test coverage
        stack.clear();

        stack.push(new RefCmd<>( "", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 4));
        stack.clear();
        assertEquals(0, stack.count());
        assertEquals(0, stack.getIdx());

        // children
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 2));
        assertEquals(3, stack.count());

        UndoCommand parent = stack.getCommand(0);
        parent.addChild(new RefCmd<>("", pt::getX, pt::setX, 20));
        parent.addChild(new RefCmd<>( "", pt::getX, pt::setX, 30));
        assertEquals(2, parent.childCount());
        assertEquals(3, stack.count());
        stack.clear();
        assertEquals(0, stack.count());
        assertEquals(0, parent.childCount());
    }

    @Test
    public void testSetClean() throws Exception {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 4));
        stack.setClean();
        assertEquals(2, stack.getCleanIdx());
    }

    @Test
    public void testIsClean() throws Exception {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 4));
        stack.setClean();
        stack.push(new RefCmd<>("", pt::getX, pt::setX, 3));
        assertEquals(false, stack.isClean());
        stack.undo();
        assertEquals(true, stack.isClean());
    }

    @Test
    public void testUndoCaption() throws Exception {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        stack.push(new RefCmd<>("1", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("2", pt::getX, pt::setX, 4));
        stack.push(new RefCmd<>("3", pt::getX, pt::setX, 4));
        assertEquals("3", stack.undoCaption());
        stack.undo();
        assertEquals("2", stack.undoCaption());
        stack.setIndex(0);
        assertEquals("", stack.undoCaption());
    }

    @Test
    public void testRedoCaption() throws Exception {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        stack.push(new RefCmd<>("1", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("2", pt::getX, pt::setX, 4));
        stack.push(new RefCmd<>("3", pt::getX, pt::setX, 4));
        assertEquals("", stack.redoCaption());
        stack.undo();
        assertEquals("3", stack.redoCaption());
        stack.setIndex(0);
        assertEquals("1", stack.redoCaption());
    }

    @Test
    public void testCaption() throws Exception {
        Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        assertEquals("", stack.caption(0));

        stack.push(new RefCmd<>("1", pt::getX, pt::setX, 2));
        stack.push(new RefCmd<>("2", pt::getX, pt::setX, 4));
        stack.push(new RefCmd<>("3", pt::getX, pt::setX, 4));
        assertEquals("1", stack.caption(0));
        assertEquals("2", stack.caption(1));
        assertEquals("3", stack.caption(2));

        assertEquals("", stack.caption(-1));
        assertEquals("", stack.caption(1000));

    }

    /**
     * Simply shows how elegant {@link RefCmd} works
     */
    @Test
    public void testIntegerClass() throws Exception {

        Point pt = new Point(-30, -40);
        UndoStack stack = new UndoStack(pt);

        // for 100% test coverage
        stack.undo();
        stack.redo();

        stack.push(new RefCmd<>("Change x", pt::getX, pt::setX, 10));
        stack.push(new RefCmd<>("Change y", pt::getY, pt::setY, 20));
        assertEquals(2, stack.count());
        assertEquals(10, pt.getX());
        assertEquals(20, pt.getY());
        // One step back
        stack.undo();
        assertEquals(10, pt.getX());
        assertEquals(-40, pt.getY());
        assertEquals(1, stack.getIdx());

        String store = UndoPacket
                // It is a good idea always store smth like subject class identifier.
                .make(stack, "sample.Point", 1)
                .store();
        UndoStack stackBack = UndoPacket
                // When we have no handler, we need to specify it explicitly.
                .peek(store, null)
                .restore(null, null)
                .prepare(null);

        Point ptBack = (Point) stackBack.getSubj();
        assertEquals(pt, ptBack);
        assertEquals(10, ptBack.getX());
        assertEquals(-40, ptBack.getY());
        assertEquals(2, stackBack.count());
        assertEquals(1, stackBack.getIdx());

        // One step forward
        stackBack.redo();
        // Just testing robust )
        stackBack.redo();
        stackBack.redo();
        assertEquals(10, ptBack.getX());
        assertEquals(20, ptBack.getY());
    }

    // null subject
    @Test
    public void testException1() {
        thrown.expect(NullPointerException.class);
        new UndoStack(null);
        thrown = ExpectedException.none();
    }

    // Pushing null command
    @Test
    public void testException2() throws Exception {
        thrown.expect(NullPointerException.class);
        new UndoStack(new Point(1, 1)).push((UndoMacro) null);
        thrown = ExpectedException.none();
    }

    // Pushing null command
    @Test
    public void testException3() throws Exception {
        thrown.expect(NullPointerException.class);
        new UndoStack(new Point(1, 1)).push((UndoCommand) null);
        thrown = ExpectedException.none();
    }

    // null subject 2
    @Test
    public void testException4() {
        UndoStack stack = new UndoStack(new Point(0, 0));
        thrown.expect(NullPointerException.class);
        stack.setSubj(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testException5() {
        thrown.expect(NullPointerException.class);
        new UndoStack(null);
        thrown = ExpectedException.none();
    }

    /**
     * Create {@link UndoStack} with or without groups.
     */
    @Test
    public void testCreation() {

        {
            // Create without group
            Serializable subj = new SimpleClass<>(Integer.class);
            UndoStack stack = new UndoStack(subj);
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
    public void testAddAndClear() throws Exception {

        NonTrivialClass scene = new NonTrivialClass();
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(scene, group);
        stack.setWatcher(new SimpleUndoWatcher());
        group.setActive(stack);

        stack.push(new NonTrivialClass.AddCommand(CIRCLE, scene));
        assertEquals(1, stack.count());
        assertEquals(1, stack.getIdx());
        stack.push(new NonTrivialClass.AddCommand(CIRCLE, scene));
        assertEquals(2, stack.count());
        assertEquals(2, stack.getIdx());
        stack.clear();
        assertEquals(0, stack.count());
        assertEquals(0, stack.getIdx());

    }

    @Test
    public void testSetIndex() throws Exception {
        NonTrivialClass scene = new NonTrivialClass();
        UndoStack stack = new UndoStack(scene);
        stack.push(new NonTrivialClass.AddCommand(CIRCLE, scene));
        assertEquals(1, stack.getIdx());
        stack.setIndex(-1);
        assertEquals(0, stack.getIdx());
    }

    /**
     * Set and check limits:
     * - undoLimit
     * - setIndex
     */
    @Test
    public void testLimits() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());
        stack.setUndoLimit(5);
        // for 100% test cover
        stack.setUndoLimit(5);
        for (int i = 0; i < 10; ++i) {
            stack.push(new RefCmd<>(String.valueOf(i), subj::getValue, subj::setValue, i));
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
    public void testClean() throws Exception {

        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());

        // Without commands not affected
        assertEquals(0, stack.getIdx());
        stack.setIndex(2);
        assertEquals(0, stack.getIdx());

        for (int i = 0; i < 10; ++i) {
            stack.push(new RefCmd<>(String.valueOf(i), subj::getValue, subj::setValue, i));
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
            stack.push(new RefCmd<>(String.valueOf(i), subj::getValue, subj::setValue, i));
        }
        assertEquals(5, stack.count());
        stack.setIndex(2);
        stack.setClean();
        assertEquals(2, stack.getCleanIdx());
        stack.setIndex(0);
        assertEquals(2, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(-1, stack.getCleanIdx());
        assertEquals(false, stack.isClean());

        // It should change cleanIdx
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        stack.push(new RefCmd<>( String.valueOf(10), subj::getValue, subj::setValue, 10));
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        stack.setClean();
        assertEquals(5, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(4, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(3, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(2, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(1, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(0, stack.getCleanIdx());
        stack.push(new RefCmd<>(String.valueOf(10), subj::getValue, subj::setValue, 10));
        assertEquals(-1, stack.getCleanIdx());


    }

    /**
     * - canUndo
     * - canRedo
     * - undoCaption
     * - redoCaption
     */
    @Test
    public void testAuxProps() throws Exception {
        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(subj, group);
        stack.setWatcher(new SimpleUndoWatcher());
        assertNotNull(stack.getWatcher());
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
            stack.push(new RefCmd<>(String.valueOf(i), subj::getValue, subj::setValue, i));
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
        makeTestSimple();

        initSimple(Integer.class, new Integer[]{1, 2, 3, null, 8});
        makeTestSimple();

        initSimple(Long.class, new Long[]{11L, 12L, 13L, 14L, null});
        makeTestSimple();

        initSimple(Double.class, new Double[]{1.1, 2.2, 3.222});
        makeTestSimple();

        initSimple(Boolean.class, new Boolean[]{true, false, true, null});
        makeTestSimple();

    }

    @Test
    public void testNonTrivial() throws Exception {
        NonTrivialClass ntc = new NonTrivialClass();
        UndoStack stack = new UndoStack(ntc);
        stack.setWatcher(new SimpleUndoWatcher());
        assertEquals(0, ntc.items.size());

        {
            stack.push(new NonTrivialClass.AddCommand(CIRCLE, ntc));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);

            stack.push(new NonTrivialClass.AddCommand(RECT, ntc));
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

            String store = UndoPacket
                    // It's a good practice always specify id.
                    .make(stack, "NonTrivialClass", 1)
                    .store();
            UndoStack stackBack = UndoPacket
                    // When we have no handlers, we still need to specify it explicitly.
                    .peek(store, null)
                    .restore(null, null)
                    .prepare(null);


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
            stack.push(new NonTrivialClass.AddCommand(CIRCLE, ntc));
            assertEquals(1, stack.count());
            assertEquals(1, stack.getIdx());
            assertEquals(1, ntc.items.size());
//            System.out.println(ntc);
            stack.push(new NonTrivialClass.DeleteCommand(ntc));
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
            stack.push(new NonTrivialClass.MovedCommand(item, oldPos));
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
            String store = UndoPacket
                    // It's a good practice always specify id.
                    .make(stack, "some.NonTrivialClass", 1)
                    .store();
            UndoStack stackBack = UndoPacket
                    // When we have no handlers, we still need to specify it explicitly.
                    .peek(store, null)
                    .restore(null, null)
                    .prepare(null);

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

    }

    /**
     * Test for command's chain
     * <p>Makes command chain without using macrocommands.
     */
    @Test
    public void testChain() throws Exception {

        {
            // Independently
            final int x = 10;
            final int y = 20;
            Point subj = new Point(x, y);
            UndoStack stack = new UndoStack(subj);
            UndoCommand parentCmd = new UndoCmdStub("parent");
            parentCmd.addChild(new RefCmd<>("move 1", subj::getY, subj::setY, 50));
            parentCmd.addChild(new RefCmd<>("move 2", subj::getX, subj::setX, 35));
            parentCmd.addChild(new RefCmd<>("move 3", subj::getY, subj::setY, 55));
            parentCmd.addChild(new RefCmd<>("move 4", subj::getX, subj::setX, 39));
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
            UndoStack stack = new UndoStack(subj);
            UndoCommand parentCmd = new UndoCmdStub("parent");
            parentCmd.addChild(new RefCmd<>("move 1", subj::getY, subj::setY, 50));
            parentCmd.addChild(new RefCmd<>("move 2", subj::getX, subj::setX, 35));
            parentCmd.addChild(new RefCmd<>("move 3", subj::getY, subj::setY, 55));
            parentCmd.addChild(new RefCmd<>("move 4", subj::getX, subj::setX, 39));
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

    /**
     * Real macros is a standalone macro-command that may be applied to our subject and added to our stack.
     */
    @Test
    public void testRealMacros() throws Exception {

        final TextSample subj = new TextSample();
        UndoStack stack = new UndoStack(new ArrayList<String>());
        stack.getLocalContexts().put(TextSampleCommands.TEXT_CTX_KEY, subj);

        String s = "start: ";
        // pos.1: Two commands up
        stack.push(new TextSampleCommands.AddLine("new line"));
        stack.push(new TextSampleCommands.AddString("new string", s));
        TextSample testText = new TextSample();
        testText.addLine();
        testText.add(s);
        assertEquals(subj, testText);
        System.out.println(subj.print());
        assertEquals(2, stack.count());

        stack.undo();
        testText.remove(s);
        assertEquals(subj, testText);
        System.out.println(subj.print());

        stack.undo();
        testText.removeLine();
        assertEquals(subj, testText);
        System.out.println(subj.print());

        stack.redo();
        stack.redo();
        testText.addLine();
        testText.add(s);
        assertEquals(subj, testText);
        System.out.println(subj.print());

        // pos.2: One command up and macros is created
        stack.beginMacro("macro 1");
        stack.push(new TextSampleCommands.AddString("new string", "Hello"));
        stack.push(new TextSampleCommands.AddString("new string", ", "));
        stack.push(new TextSampleCommands.AddString("new string", "world!"));

        // some method inside macros shouldn't work (for 100% test coverage)
        stack.setClean();
        stack.isClean();
        stack.undo();
        stack.redo();
        stack.setIndex(0);
        stack.setUndoLimit(4);
        assertEquals(false, stack.canUndo());
        assertEquals(false, stack.canRedo());
        assertEquals("", stack.undoCaption());
        assertEquals("", stack.redoCaption());
        stack.beginMacro("another macro");
        // ~

        stack.endMacro();
        String s2 = "Hello, world!";
        testText.add(s2);
        assertEquals(subj, testText);
        System.out.println(subj.print());

        stack.undo();
        testText.remove(s2);
        assertEquals(subj, testText);
        System.out.println(subj.print());

        stack.redo();
        testText.add(s2);
        assertEquals(subj, testText);
        System.out.println(subj.print());

        // pos.3: Use macro
        stack.push(new TextSampleCommands.AddLine("new line"));
        testText.addLine();
        assertEquals(subj, testText);
        UndoMacro macro = stack.getMacro(0);
        stack.push(macro);
        testText.add(s2);
        assertEquals(subj, testText);
        System.out.println(subj.print());
        //
        stack.undo();
        testText.remove(s2);
        assertEquals(subj, testText);
        assertEquals(5, stack.count());
        assertEquals(4, stack.getIdx());

        System.out.println(subj.print());

        //-----------------------------------------------------------------
        // store/restore

        Macros macros = new Macros();
        for (int i = 0; i < stack.getMacroCount(); ++i) {
            macros.items.add(stack.getMacro(i));
        }

        String pack = UndoPacket
                .make(stack, SUBJ_ID, 1)
//                .onStore(o -> {
//                    // Here it is redundant
//                    return (String)o;
//                })
                .extra("macros", macros)
                .store();

        // Let's emulate new local context
        TextSample subj1 = new TextSample();


        final Macros[] macros1 = new Macros[1];
        UndoStack stack1 = UndoPacket
                .peek(pack, null)
                .restore((processedSubj, subjInfo) -> {
                    // Always return null for unexpected code.
                    return SUBJ_ID.equals(subjInfo.id) ? (ArrayList<String>) processedSubj : null;
                }, null)
                .prepare((stack2, subjInfo, result) -> {
                    macros1[0] = (Macros) subjInfo.extras.get("macros");
                    stack2.getLocalContexts().put(TextSampleCommands.TEXT_CTX_KEY, subj1);
                    subj1.clear();
                    subj1.text.addAll((ArrayList<String>) stack2.getSubj());
                });

        assertEquals(5, stack1.count());
        assertEquals(4, stack1.getIdx());
        assertEquals(1, macros1[0].items.size());

        // pos.0
        stack1.setIndex(0);
        System.out.println(subj1.print());

        // repeat pos.1
        stack1.redo();
        stack1.redo();
        testText = new TextSample();
        testText.addLine();
        testText.add(s);
        assertEquals(subj1, testText);
        System.out.println(subj1.print());
        assertEquals(2, stack1.getIdx());

        // repeat pos.2
        stack1.redo();
        testText.add(s2);
        assertEquals(subj1, testText);
        System.out.println(subj1.print());

        // repeat pos.3: It shows that macros restored correctly
        stack1.push(new TextSampleCommands.AddLine("new line"));
        testText.addLine();
        assertEquals(subj1, testText);
        UndoMacro macro1 = stack1.getMacro(0);
        stack1.push(macro1);
        testText.add(s2);
        assertEquals(subj1, testText);
        System.out.println(subj1.print());

        // Use macros from zero subj
        stack1.setIndex(0);
        stack1.push(new TextSampleCommands.AddLine("new line"));
        stack1.push(macro1);
        testText.clear();
        testText.addLine();
        testText.add(s2);
        assertEquals(subj1, testText);
        System.out.println(subj1.print());
        assertEquals(2, stack1.count());

        // Use macros from list
        stack1.setIndex(0);
        stack1.push(new TextSampleCommands.AddLine("new line"));
        UndoMacro macro2 = macros1[0].items.get(0);
        stack1.push(macro2);
        testText.clear();
        testText.addLine();
        testText.add(s2);
        assertEquals(subj1, testText);
        System.out.println(subj1.print());
        assertEquals(2, stack1.count());


    }

    // for 100% test coverage
    @Test
    public void testMacros2() throws Exception {
        final Point pt = new Point(1, 1);
        UndoStack stack = new UndoStack(pt);
        stack.setWatcher(new SimpleUndoWatcher());
        assertEquals(null, stack.getMacro(0));
        stack.beginMacro("macro 1");
        stack.endMacro();
        assertEquals(1, stack.getMacroCount());

        // for 100% code coverage
        stack.endMacro();
        stack.dropMacro();
        new RefCmd<>("1", pt::getX, pt::setX, 2);
        new RefCmd<>("2", pt::getX, pt::setX, 3);
        stack.setClean();
        stack.setIndex(0);
        stack.beginMacro("macro 2");
        stack.endMacro();
        //~

    }

    @Test
    public void hash() {
        HashMap<UndoStack, Integer> map = new HashMap<>();
        map.put(new UndoStack(new Point(1, 2)), null);
        map.put(new UndoStack(new Point(1, 2)), null);
        assertEquals(2, map.size());
    }


}
