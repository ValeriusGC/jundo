import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoStack;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;
import some.SimpleUndoWatcher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * We must make it serializable because some inner classes are subject to serialize.
 */
public class UndoPacketTest3 implements Serializable {

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
    public void testStackEx() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoPacket.make(null, "", 1)
                .store();
        thrown = ExpectedException.none();
    }

    @Test
    public void testExtraEx() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoPacket.make(stack, "", 1)
                .extra(null, "")
                .store();
        thrown = ExpectedException.none();
    }

    // for 100% test coverage
    @Test
    public void testExtra() throws Exception {
        UndoPacket.make(stack, "", 1)
                .extra("a", "b")
                .zipped(true)
                .store();
    }

    @Test
    public void testSubjEx() throws Exception {
        thrown.expect(Exception.class);
        stack.setSubj(new Object());
        UndoPacket.make(stack, "", 1)
                .store();
        thrown = ExpectedException.none();
    }

    @Test
    public void testCandidateEx() throws Exception {
        thrown.expect(Exception.class);
        UndoPacket.peek(null, null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testCandidateEx2() throws Exception {
        thrown.expect(Exception.class);
        UndoPacket.peek("too short", null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testHandlerEx() throws Exception {
        String s = UndoPacket.make(stack, "", 1)
                .onStore(subj -> "")
                .store();

        thrown.expect(Exception.class);
        // Need subj handler, cause was onStore
        UndoPacket.peek(s, null)
                .restore(null)
                .stack(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testHandlerEx2() throws Exception {
        String s = UndoPacket.make(stack, "", 1)
                .onStore(subj -> "")
                .store();

        // for 100% test coverage
        UndoPacket.peek(s, null)
                .restore((processedSubj, subjInfo) -> null)
                .stack(null);
    }

}
