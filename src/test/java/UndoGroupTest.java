import com.gdetotut.jundo.RefCmd;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;
import some.SimpleClass;
import com.gdetotut.jundo.UndoGroup;
import com.gdetotut.jundo.UndoStack;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UndoGroupTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Group should not contain two or more stacks with one object.
     */
    @Test
    public void testOneGroupOneObject() {
        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        new UndoStack(subj, group);
        assertEquals(1, group.getStacks().size());
        new UndoStack(subj, group);
        // See that 2nd stack with the same subject is not added to the group...
        assertEquals(1, group.getStacks().size());

        SimpleClass<Integer> subj2 = new SimpleClass<>(Integer.class);
        // ...And with another one is added.
        new UndoStack(subj2, group);
        assertEquals(2, group.getStacks().size());
    }

    @Test
    public void testClear() {
        UndoGroup group = new UndoGroup();
        new UndoStack(new Point(1,1), group);
        new UndoStack(new Point(1,1), group);
        assertEquals(2, group.getStacks().size());
        group.clear();
        assertEquals(0, group.getStacks().size());
    }

    @Test
    public void testAdd() {
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(new Point(1,1), null);
        group.add(stack);
        group.add(new UndoStack(new Point(1,1), null));
        assertEquals(2, group.getStacks().size());

        UndoGroup group2 = new UndoGroup();
        group2.add(stack);
        assertEquals(1, group.getStacks().size());
        assertEquals(1, group2.getStacks().size());

        thrown.expect(NullPointerException.class);
        group.add(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testRemove() {
        UndoGroup group = new UndoGroup();
        UndoStack stack1 = new UndoStack(new Point(1,1), group);
        UndoStack stack2 = new UndoStack(new Point(1,1), group);
        group.remove(stack1);
        assertEquals(1, group.getStacks().size());
        group.remove(stack2);
        assertEquals(0, group.getStacks().size());

        thrown.expect(NullPointerException.class);
        group.remove(null);
        thrown = ExpectedException.none();

    }

    @Test
    public void testStacks() {
        UndoGroup group = new UndoGroup();
        UndoStack stack1 = new UndoStack(new Point(1,1), group);
        UndoStack stack2 = new UndoStack(new Point(1,1), group);
        List<UndoStack> stacks = group.getStacks();
        assertEquals(2, stacks.size());
        assertEquals(stack1, stacks.get(0));
        assertEquals(stack2, stacks.get(1));
    }

    @Test
    public void testActive() {
        UndoGroup group = new UndoGroup();
        UndoStack stack1 = new UndoStack(new Point(1,1), group);
        UndoStack stack2 = new UndoStack(new Point(1,1), group);
        assertEquals(null, group.getActive());
        group.setActive(stack1);
        assertEquals(stack1, group.getActive());
        group.setActive(stack2);
        assertEquals(stack2, group.getActive());
        // repeat for 100% test coverage
        group.setActive(stack2);
        assertEquals(stack2, group.getActive());


        group.remove(stack2);
        assertEquals(null, group.getActive());
    }

    @Test
    public void testUndoRedo() {
        Point pt = new Point(1, 1);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(pt, group);
        stack.push(new RefCmd<>(stack, "1", pt::getY, pt::setY, 2, null));
        stack.push(new RefCmd<>(stack, "2", pt::getY, pt::setY, 4, null));

        // before active
        group.undo();
        assertEquals(4, pt.getY());
        group.redo();
        assertEquals(4, pt.getY());

        group.setActive(stack);
        group.undo();
        assertEquals(2, pt.getY());
        group.redo();
        assertEquals(4, pt.getY());
        group.setActive(null);
        group.undo();
        assertEquals(4, pt.getY());
    }

    @Test
    public void testCanUndoRedo() {
        Point pt = new Point(1, 1);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(pt, group);
        stack.push(new RefCmd<>(stack, "1", pt::getY, pt::setY, 2, null));
        stack.push(new RefCmd<>(stack, "2", pt::getY, pt::setY, 4, null));
        assertEquals(false, group.canUndo());
        assertEquals(false, group.canRedo());
        group.setActive(stack);
        assertEquals(true, group.canUndo());
        assertEquals(false, group.canRedo());
        group.undo();
        assertEquals(true, group.canUndo());
        assertEquals(true, group.canRedo());
        group.undo();
        assertEquals(false, group.canUndo());
        assertEquals(true, group.canRedo());
    }

    @Test
    public void testCaption() {
        Point pt = new Point(1, 1);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(pt, group);
        stack.push(new RefCmd<>(stack, "1", pt::getY, pt::setY, 2, null));
        stack.push(new RefCmd<>(stack, "2", pt::getY, pt::setY, 4, null));
        assertEquals("", group.undoCaption());
        assertEquals("", group.redoCaption());
        group.setActive(stack);
        assertEquals("2", group.undoCaption());
        assertEquals("", group.redoCaption());
        group.undo();
        assertEquals("1", group.undoCaption());
        assertEquals("2", group.redoCaption());
        group.undo();
        assertEquals("", group.undoCaption());
        assertEquals("1", group.redoCaption());
    }

    @Test
    public void testIsClean() {
        Point pt = new Point(1, 1);
        UndoGroup group = new UndoGroup();
        UndoStack stack = new UndoStack(pt, group);
        stack.push(new RefCmd<>(stack, "1", pt::getY, pt::setY, 2, null));
        stack.push(new RefCmd<>(stack, "2", pt::getY, pt::setY, 4, null));
        assertEquals(true, group.isClean());
        group.setActive(stack);
        assertEquals(false, group.isClean());
        stack.setClean();
        assertEquals(true, group.isClean());
    }



}
