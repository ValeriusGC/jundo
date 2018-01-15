import com.gdetotut.jundo.RefCmd;
import com.gdetotut.jundo.UndoPacke;
import com.gdetotut.jundo.UndoStack;
import org.junit.Assert;
import org.junit.Test;
import some.Point;

import static org.junit.Assert.assertEquals;

public class UndoPackeTest {

    @Test
    public void testStore() throws Exception {

        Point pt = new Point(-30, -40);
        UndoStack stack = new UndoStack(pt, null);
        stack.push(new RefCmd<>(stack, "Change x", pt::getX, pt::setX, 10, null));
        stack.push(new RefCmd<>(stack, "Change y", pt::getY, pt::setY, 20, null));
        assertEquals(2, stack.count());
        assertEquals(10, pt.getX());
        assertEquals(20, pt.getY());
        // One step back
        stack.undo();
        assertEquals(10, pt.getX());
        assertEquals(-40, pt.getY());
        assertEquals(1, stack.getIdx());

        String pack = UndoPacke.builder(stack, "some.Point", 1)
                .extra("key_int", 20)
                .extra("key_int", 30) // rewrited
                .extra("key_str", "value")
                .store();

        // TODO: 15.01.18 Проверить, почему размер имени класса влияет на ошибку - UndoPacket is OK, UndoPacke & UndoStore is FALSE 

        System.out.println(pack.length());


        UndoPacke.SubjInfo subjInfo = UndoPacke.peek(pack);
        Assert.assertEquals("some.Point", subjInfo.id);
        Assert.assertEquals(1, subjInfo.version);
        Assert.assertEquals(30, subjInfo.extras.get("key_int"));
        Assert.assertEquals("value", subjInfo.extras.get("key_str"));
        Assert.assertEquals(2, subjInfo.extras.size());

    }

}
