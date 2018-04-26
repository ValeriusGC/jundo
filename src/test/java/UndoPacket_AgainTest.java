import com.gdetotut.jundo.CreatorException;
import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoPacket.SubjInfo;
import com.gdetotut.jundo.UndoStack;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;

import static com.gdetotut.jundo.UndoPacket.Result.Code.*;
import static org.junit.Assert.*;

public class UndoPacket_AgainTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Point subj = null;
    UndoStack stack = null;

    @Before
    public void prepare() {
        subj = new Point(1, 1);
        stack = new UndoStack(subj);
    }

    @Test
    public void testStackEx() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoPacket.prepare(null, "", 1)
                .store();
        thrown = ExpectedException.none();
    }

    @Test
    public void testExtraEx() throws Exception {
        thrown.expect(NullPointerException.class);
        UndoPacket.Builder builder = UndoPacket.prepare(stack, "", 1);
        builder.addExtra(null, "");
        thrown = ExpectedException.none();
    }

    // for 100% test coverage
    @Test
    public void testExtra() throws Exception {
        UndoPacket.Builder builder = UndoPacket.prepare(stack, "", 1);
        UndoPacket.prepare(stack, "", 1);
        assertNotNull(builder.addExtra("a", "b"));
        assertNotNull(builder.zip());
    }

    @Test
    public void testSubjEx() throws Exception {
        thrown.expect(Exception.class);
        stack.setSubj(new Object());
        UndoPacket
                .prepare(stack, "", 1)
                .store();
        thrown = ExpectedException.none();
    }

    @Test
    public void testCreateNewEx() throws CreatorException {
        thrown.expect(CreatorException.class);
        UndoPacket
                .peek("", null)
                .restore(null, () -> null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testNullCandidate() {
        UndoPacket.Peeker peeker = UndoPacket.peek(null, null);
        assertEquals(peeker.result.code, RC_WrongCandidate);
        assertEquals(peeker.result.msg, "is null");
    }

    @Test
    public void testShortCandidate() {
        UndoPacket.Peeker peeker = UndoPacket.peek("short", null);
        assertEquals(peeker.result.code, RC_WrongCandidate);
        assertEquals(peeker.result.msg, "too small size");
    }

    @Test
    public void testBadSubjInfo() {
        UndoPacket.Peeker peeker = UndoPacket.peek("0ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ", null);
        assertEquals(peeker.result.code, RC_WrongCandidate);
    }

    @Test
    public void testPeeker() throws Exception {
        String s = UndoPacket.prepare(stack, "stack", 1).store();
        UndoPacket.Peeker peeker = UndoPacket.peek(s, null);
        assertEquals(peeker.result.code, RC_Success);
        assertEquals(peeker.result.msg, null);

        peeker = UndoPacket.peek(s, it -> false);
        assertEquals(peeker.result.code, RC_PeekRefused);
        assertEquals(peeker.result.msg, null);
    }

    @Test
    public void testHandlerEx() throws Exception {
        String s = UndoPacket.prepare(stack, "", 1)
                .onStoreManually(subj -> "")
                .store();

        thrown.expect(Exception.class);
        // Need subj handler, cause was onStoreManually
        UndoPacket.peek(s, null)
                .restore(null, null)
                .process(null);
        thrown = ExpectedException.none();
    }

    @Test
    public void testHandlerEx2() throws Exception {
        String s = UndoPacket.prepare(stack, "", 1)
                .onStoreManually(subj -> "")
                .store();

        UndoPacket packet = UndoPacket
                .peek(s, null)
                .restore((processedSubj, subjInfo) -> null, () -> stack);

        assertNull(packet.subjInfo);
        assertEquals(RC_NewStack, packet.result.code);
    }


    /**
     * Simple storing of Serializable object
     *
     * @throws Exception
     */
    @Test
    public void testSimpleStore() throws Exception {

        Point pt = new Point(-30, -40);
        assertEquals(-30, pt.getX());
        assertEquals(-40, pt.getY());

        UndoStack stack = new UndoStack(pt);
        stack.push(new RefCmd<>("Change x", pt::getX, pt::setX, 10));
        stack.push(new RefCmd<>("Change y", pt::getY, pt::setY, 20));

        // After 2 commands applied
        assertEquals(2, stack.count());
        assertEquals(2, stack.getIdx());
        assertEquals(10, pt.getX());
        assertEquals(20, pt.getY());

        // One step back
        stack.undo();
        assertEquals(10, pt.getX());
        assertEquals(-40, pt.getY());
        assertEquals(1, stack.getIdx());

        // After 2 commands applied again
        stack.redo();
        assertEquals(10, pt.getX());
        assertEquals(20, pt.getY());
        assertEquals(2, stack.getIdx());

        // One step back
        stack.undo();
        assertEquals(1, stack.getIdx());

        String packAsString = UndoPacket.prepare(stack, "some.Point", 1)
                .addExtra("key_int", 20)
                .addExtra("key_int", 30) // rewrited
                .addExtra("key_str", "value")
                .zip()
                .store();

        System.out.println(packAsString.length());

        // Распаковка информации и проверка соответствия стека ожидаемому.
        UndoPacket packet = UndoPacket
                .peek(packAsString, null)
                .restore(null, null);

        SubjInfo subjInfo = packet.subjInfo;
        assertEquals("some.Point", subjInfo.id);
        assertEquals(1, subjInfo.version);
        assertEquals(30, subjInfo.extras.get("key_int"));
        assertEquals("value", subjInfo.extras.get("key_str"));
        assertEquals(2, subjInfo.extras.size());

        // Раз стек ожидаемого типа, можно смело распаковывать остальное
        UndoStack stack1 = packet.process(null);
        // Это логично - стек, воссозданный заново ничего общего не имеет с текущим по адресному пространству
        assertNotEquals(stack, stack1);
        // Зато это должно быть одинаково
        assertEquals(stack.getIdx(), stack1.getIdx());
        assertEquals(stack.getCleanIdx(), stack1.getCleanIdx());
        assertEquals(stack.getUndoLimit(), stack1.getUndoLimit());

        // We shoul understand that after deserialization it is new instance of subj.
        Point pt1 = (Point) stack1.getSubj();
        assertEquals(pt, pt1);

        stack1.redo();
        // After 2 commands applied
        assertEquals(2, stack1.count());
        assertEquals(10, pt1.getX());
        assertEquals(20, pt1.getY());

        stack1.undo();
        stack1.undo();
        assertEquals(-30, pt1.getX());
        assertEquals(-40, pt1.getY());

    }

    @Test
    public void testEmptyExtras() throws Exception {

    }


    /**
     * Tests exception
     *
     * @throws Exception
     */
    @Test
    public void testNonSerializableException() throws Exception {

        Color color = Color.RED;
        UndoStack stack = new UndoStack(color);
        thrown.expect(Exception.class);
        UndoPacket
                .prepare(stack, "", 1)
                .store();
        thrown = ExpectedException.none();

    }

    /**
     * Simplified version for non-serializable with {@link UndoPacket.OnRestoreManually}
     */
    @Test
    public void testWithHandlers1() throws Exception {

        {
            Color color = Color.RED;
            UndoStack stack = new UndoStack(color);
            String str = UndoPacket
                    .prepare(stack, "", 1)
                    .onStoreManually(subj -> "RED")
                    .store();
            // We need handler here 'cause we store with handler
            UndoPacket
                    .peek(str, null)
                    .restore((processedSubj, subjInfo) -> Color.RED, null);
        }

    }

    /**
     * Tests exception
     * Simplified version for non-serializable without {@link UndoPacket.OnRestoreManually}
     */
    @Test
    public void testWithHandlers2() throws Exception {

        {
            Color color = Color.RED;
            UndoStack stack = new UndoStack(color);
            String str = UndoPacket
                    .prepare(stack, "", 1)
                    .onStoreManually(new UndoPacket.OnStoreManually() {
                        @Override
                        public String store(Object subj) {
                            return "RED";
                        }
                    })
                    .store();
            thrown.expect(Exception.class);
            // We need handler here 'cause we store with handler
            UndoPacket.peek(str, null).restore(null, null);
            thrown = ExpectedException.none();
        }

    }

    /**
     * Tests exception
     * - Simplified version for serializable without {@link UndoPacket.OnRestoreManually}
     */
    @Test
    public void testWithHandlers3() throws Exception {

        {
            Point pt = new Point(1, 2);
            UndoStack stack = new UndoStack(pt);
            // Here handler is redundant, only for illustrating pair OnStoreManually/OnRestoreManually
            String str = UndoPacket
                    .prepare(stack, "", 1)
                    .onStoreManually(new UndoPacket.OnStoreManually() {
                        @Override
                        public String store(Object subj) {
                            return "Point";
                        }
                    })
                    .store();
            thrown.expect(Exception.class);
            // We need handler here 'cause we store with handler
            UndoPacket.peek(str, null).restore(null, null);
            thrown = ExpectedException.none();
        }

    }

    @Test
    public void testNew() throws Exception {

        Point pt = new Point(1, 2);
        UndoStack stack = new UndoStack(pt);
        // Here handler is redundant, only for illustrating pair OnStoreManually/OnRestoreManually
        String str = UndoPacket
                .prepare(stack, "", 1)
                .onStoreManually(subj -> "Point")
                .store();

        SubjInfo subjInfo = UndoPacket.peek(str, p -> p.id.equals("abc")).subjInfo;
        UndoPacket packet = UndoPacket
                .peek(str, it -> it.id.equals("abc"))
                .restore((processedSubj, it) -> "", () -> new UndoStack(pt));

        UndoStack stack1 = UndoPacket
                .peek(str, it -> it.id.equals("abc"))
                .restore((processedSubj, it) -> "", () -> new UndoStack(pt))
                .process((s, si, r) -> {
                    if (si != null && si.version == 2) {
                        s.getLocalContexts().put("a", "aa");
                        s.getLocalContexts().put("b", "bb");
                    }
                });

    }

}
