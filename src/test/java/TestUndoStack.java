import model.Point;
import model.UndoFieldCommand;
import org.junit.Test;
import undomodel.UndoStack;

import static org.junit.Assert.assertEquals;

public class TestUndoStack {

    @Test
    public void simpleTest() {
        assertEquals(true, true);
    }

    /**
     * Tests simple undo chain
     */
    @Test
    public void undoInteger() throws NoSuchFieldException, IllegalAccessException {

        final int max = 10;
        // Prepare data
        Integer[] arrInt = new Integer[max];
        for(int i = 0; i < max; i++) {
            arrInt[i] = i;
        }


        // Init
        Point point = new Point();
        point.x = arrInt[0];
        point.setY(arrInt[0]);

        // Without parent means not in a group
        UndoStack stack = new UndoStack(null);

        // Test that field value changed after pushing command to stack
        for (Integer i :
                arrInt) {
            stack.push(new UndoFieldCommand<Point, Integer>(null, point, "x", arrInt[i]));
            assertEquals(arrInt[i], point.x);
        }
        // All linked properties must be valid
        assertEquals(max, stack.count());
        assertEquals(stack.getIndex(), stack.count());

        // Walk here and there
        for(int i = max; i > 1; i--) {
            stack.undo();
            assertEquals((Integer) (arrInt[i-1] - 1), point.x);
        }

        for(int i = 1; i < max; i++) {
            stack.redo();
            assertEquals(arrInt[i], point.x);
        }


    }

}
