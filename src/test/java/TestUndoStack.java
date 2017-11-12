import model.Point;
import model.UndoFieldCommand;
import model.UndoMethodCommand;
import org.junit.Test;
import undomodel.UndoStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

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
    public void undoInteger() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

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


        // Test for field
        {
            // Without parent means not in a group
            UndoStack stackforField = new UndoStack(null);

            // Test that field value changed after pushing command to stack
            for (Integer i :
                    arrInt) {
                stackforField.push(new UndoFieldCommand<Point, Integer>(null, point, "x", arrInt[i]));
                assertEquals(arrInt[i], point.x);
            }
            // All linked properties must be valid
            assertEquals(max, stackforField.count());
            assertEquals(stackforField.getIndex(), stackforField.count());

            // Walk here and there
            for(int i = max; i > 1; i--) {
                stackforField.undo();
                assertEquals((Integer) (arrInt[i-1] - 1), point.x);
            }

            for(int i = 1; i < max; i++) {
                stackforField.redo();
                assertEquals(arrInt[i], point.x);
            }
        }
        // ~Test for field

        // Test for method
        {
            // Without parent means not in a group
            UndoStack stackForMethod = new UndoStack(null);

//            Setter2<Integer> u = point::setY;

            // Test that field value changed after pushing command to stack
            for (Integer i :
                    arrInt) {
                stackForMethod.push(new UndoMethodCommand<>("", null, point, "x", point::getY, point::setY, arrInt[i]));
                assertEquals(arrInt[i], point.getY());
            }
            // All linked properties must be valid
            assertEquals(max, stackForMethod.count());
            assertEquals(stackForMethod.getIndex(), stackForMethod.count());

            // Walk here and there
            for(int i = max; i > 1; i--) {
                stackForMethod.undo();
                assertEquals((Integer) (arrInt[i-1] - 1), point.getY());
            }

            for(int i = 1; i < max; i++) {
                stackForMethod.redo();
                assertEquals(arrInt[i], point.getY());
            }
        }
        // ~Test for method
        
    }

}
