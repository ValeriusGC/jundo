import model.Point;
import model.UndoPointCommand;
import org.junit.Test;
import undomodel.UndoStack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
                stackforField.push(new UndoPointCommand<>(null, null, point, "x", arrInt[i]));
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

            // Test that field value changed after pushing command to stack
            for (Integer i :
                    arrInt) {
                stackForMethod.push(new UndoPointCommand<>(null, null, point, point::getY, point::setY, arrInt[i]));
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

    @Test
    public void testStringUndo() throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException {

        final int max = 10;
        // Prepare data
        String[] arr = new String[max];
        for(int i = 0; i < max; i++) {
            arr[i] = i % 2 == 0 ?  "s: " + i : null;
        }

        // Init
        Point point = new Point();
        point.setLabel(null);

        UndoStack stack = new UndoStack(null);

        // Test that field value changed after pushing command to stack
        for(int i = 0; i < max; i++) {
            stack.push(new UndoPointCommand<>(null, null, point, point::getLabel, point::setLabel, arr[i]));
            assertEquals(arr[i], point.getLabel());
        }

        // All linked properties must be valid
        assertEquals(max, stack.count());
        assertEquals(stack.getIndex(), stack.count());

        // Walk here and there
        for(int i = max-1; i > 0; i--) {
            stack.undo();
            assertEquals(arr[i-1], point.getLabel());
        }
        for(int i = 1; i < max; i++) {
            stack.redo();
            assertEquals(arr[i], point.getLabel());
        }
        // ~Walk here and there

//        ObjectMapper mapper = new ObjectMapper();
//        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
//        String json = mapper.writeValueAsString(stack);
//        System.out.println(json);
//
//        {
//            UndoStack stackBack = mapper.readValue(json, UndoStack.class);
//            stackBack.setIndex(0, false);
//            for(int i = 1; i < max; i++) {
//                stackBack.redo();
//                assertEquals(arr[i], point.getLabel());
//            }
//        }

    }

}
