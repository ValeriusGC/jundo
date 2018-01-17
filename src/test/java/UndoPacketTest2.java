import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoPacket.SubjInfo;
import com.gdetotut.jundo.UndoStack;
import javafx.scene.paint.Color;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import some.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UndoPacketTest2 {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

        UndoStack stack = new UndoStack(pt, null);
        stack.push(new RefCmd<>(stack, "Change x", pt::getX, pt::setX, 10, null));
        stack.push(new RefCmd<>(stack, "Change y", pt::getY, pt::setY, 20, null));

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

        String packAsString = UndoPacket.make(stack, "some.Point", 1)
                .extra("key_int", 20)
                .extra("key_int", 30) // rewrited
                .extra("key_str", "value")
                .zipped(true)
                .store();

        System.out.println(packAsString.length());

        // Распаковка информации и проверка соответствия стека ожидаемому.
        UndoPacket packet = UndoPacket
                .peek(packAsString, null)
                .restore(null);

        SubjInfo subjInfo = packet.subjInfo;
        assertEquals("some.Point", subjInfo.id);
        assertEquals(1, subjInfo.version);
        assertEquals(30, subjInfo.extras.get("key_int"));
        assertEquals("value", subjInfo.extras.get("key_str"));
        assertEquals(2, subjInfo.extras.size());

        // Раз стек ожидаемого типа, можно смело распаковывать остальное
        UndoStack stack1 = packet.stack(null);
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
        UndoStack stack = new UndoStack(color, null);
        thrown.expect(Exception.class);
        UndoPacket
                .make(stack, "", 1)
                .store();
        thrown = ExpectedException.none();

    }

    /**
     * Simplified version for non-serializable with {@link com.gdetotut.jundo.UndoPacket.OnRestore}
     */
    @Test
    public void testWithHandlers1() throws Exception {

        {
            Color color = Color.RED;
            UndoStack stack = new UndoStack(color, null);
            String str = UndoPacket
                    .make(stack, "", 1)
                    .onStore(subj -> "RED")
                    .store();
            // We need handler here 'cause we store with handler
            UndoPacket
                    .peek(str, null)
                    .restore((processedSubj, subjInfo) -> Color.RED);
        }

    }

    /**
     * Tests exception
     * Simplified version for non-serializable without {@link com.gdetotut.jundo.UndoPacket.OnRestore}
     */
    @Test
    public void testWithHandlers2() throws Exception {

        {
            Color color = Color.RED;
            UndoStack stack = new UndoStack(color, null);
            String str = UndoPacket
                    .make(stack, "", 1)
                    .onStore(new UndoPacket.OnStore() {
                        @Override
                        public String handle(Object subj) {
                            return "RED";
                        }
                    })
                    .store();
            thrown.expect(Exception.class);
            // We need handler here 'cause we store with handler
            UndoPacket.peek(str, null).restore(null);
            thrown = ExpectedException.none();
        }

    }

    /**
     * Tests exception
     * - Simplified version for serializable without {@link com.gdetotut.jundo.UndoPacket.OnRestore}
     */
    @Test
    public void testWithHandlers3() throws Exception {

        {
            Point pt = new Point(1, 2);
            UndoStack stack = new UndoStack(pt, null);
            // Here handler is redundant, only for illustrating pair OnStore/OnRestore
            String str = UndoPacket
                    .make(stack, "", 1)
                    .onStore(new UndoPacket.OnStore() {
                        @Override
                        public String handle(Object subj) {
                            return "Point";
                        }
                    })
                    .store();
            thrown.expect(Exception.class);
            // We need handler here 'cause we store with handler
            UndoPacket.peek(str, null).restore(null);
            thrown = ExpectedException.none();
        }

    }

    @Test
    public void testNew() throws Exception {

        Point pt = new Point(1, 2);
        UndoStack stack = new UndoStack(pt, null);
        // Here handler is redundant, only for illustrating pair OnStore/OnRestore
        String str = UndoPacket
                .make(stack, "", 1)
                .onStore(subj -> "Point")
                .store();

        SubjInfo subjInfo = UndoPacket.peek(str, p -> p.id.equals("abc")).subjInfo;
        UndoPacket packet = UndoPacket
                .peek(str, it -> it.id.equals("abc"))
                .restore((processedSubj, it) -> "");
        UndoStack stack1 = UndoPacket
                .peek(str, it -> it.id.equals("abc"))
                .restore((processedSubj, it) -> "")
                .stack((s, si) -> {
                    if (si.version == 2) {
                        s.getLocalContexts().put("a", "aa");
                        s.getLocalContexts().put("b", "bb");
                    }
                });

    }

}
