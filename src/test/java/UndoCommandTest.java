import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoStack;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;

import static org.junit.Assert.assertEquals;

public class UndoCommandTest {

    Point subj = null;
    UndoStack stack = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void prepare() {
        subj = new Point(1, 1);
        stack = new UndoStack(subj, null);
    }

    @Test
    public void testCtrException(){
        thrown.expect(NullPointerException.class);
        new UndoCommand(null, "", null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testMergeException(){
        thrown.expect(NullPointerException.class);
        new UndoCommand(stack,"", null).mergeWith(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testChild(){
        UndoCommand parent = new UndoCommand(stack, "", null);
        // for test coverage sake
        parent.undo();
        parent.redo();

        assertEquals(null, parent.child(0));
        assertEquals(null, parent.child(-1));
        assertEquals(null, parent.child(1));
        UndoCommand cmd1 = new RefCmd<>(stack, "1", subj::getX, subj::setX, 2, parent);
        assertEquals(cmd1, parent.child(0));
        UndoCommand cmd2 = new RefCmd<>(stack, "2", subj::getX, subj::setX, 3, parent);
        assertEquals(cmd2, parent.child(1));
    }

}
