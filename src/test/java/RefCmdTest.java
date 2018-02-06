import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoStack;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;

public class RefCmdTest {

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
    public void testGetterNullEx(){
        thrown.expect(NullPointerException.class);
        new RefCmd<>( "", null, subj::setX, 1);
        thrown = ExpectedException.none();
    }

    @Test
    public void testSetterNullEx(){
        thrown.expect(NullPointerException.class);
        new RefCmd<>("", subj::getX, null, 1);
        thrown = ExpectedException.none();
    }

    @Test
    public void testCompare(){
        UndoCommand cmd1 = new RefCmd<>( "", subj::getX, subj::setX, 1);
        UndoCommand cmd2 = cmd1;
        Assert.assertEquals(cmd1, cmd2);
        UndoCommand cmd3 = new RefCmd<>("", subj::getX, subj::setX, 1);
        Assert.assertNotEquals(cmd1, cmd3);
    }


}
