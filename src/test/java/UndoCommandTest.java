import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoStack;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;
import some.UndoCmdStub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UndoCommandTest {

    Point subj = null;
    UndoStack stack = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void prepare() {
        subj = new Point(1, 1);
        stack = new UndoStack(subj);
    }

    @Test
    public void testMergeException() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoStack stack = new UndoStack("");
        stack.push(new UndoCmdStub("")).getCommand(0).mergeWith(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testChild(){
        UndoCommand parent = new UndoCmdStub( "");
        // for test coverage sake
        parent.undo();
        parent.redo();

        assertEquals(null, parent.child(0));
        assertEquals(null, parent.child(-1));
        assertEquals(null, parent.child(1));
        UndoCommand cmd1 = new RefCmd<>("1", subj::getX, subj::setX, 2);
        parent.addChild(cmd1);
        assertEquals(cmd1, parent.child(0));
        UndoCommand cmd2 = new RefCmd<>( "2", subj::getX, subj::setX, 3);
        parent.addChild(cmd2);
        assertEquals(cmd2, parent.child(1));
    }

    @Test
    public void testAddChild() {
        UndoCommand parent = new UndoCmdStub("");
        assertEquals(0, parent.childCount());
        parent.addChild(new UndoCmdStub(""));
        assertEquals(1, parent.childCount());


    }

}
