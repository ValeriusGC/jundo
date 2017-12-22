import com.gdetotut.jundo.UndoSerializer;
import org.junit.Test;
import some.SimpleUndoWatcher;
import some.NonTrivialClass;
import com.gdetotut.jundo.UndoStack;

import static org.junit.Assert.assertEquals;

public class TestUndoSerializer {


    /**
     * Illustration for versioning.
     */
    static class NonTrivialClass_v2 extends NonTrivialClass {

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        private String title;

    }

    @Test
    public void serialize() throws Exception {

        NonTrivialClass ntc = new NonTrivialClass();
        UndoStack stack = new UndoStack(ntc, null);
        stack.setWatcher(new SimpleUndoWatcher());
        for(int i = 0; i < 1000; ++i){
            stack.push(new NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, ntc, null));
        }
        assertEquals(1000, ntc.items.size());
        assertEquals(1000, stack.count());
        for(int i = 0; i < 1000; ++i){
            stack.push(new NonTrivialClass.MovedCommand(ntc.items.get(i), 10, null));
        }
        assertEquals(1000, ntc.items.size());
        assertEquals(2000, stack.count());
        for(int i = 0; i < 1000; ++i){
            stack.push(new NonTrivialClass.DeleteCommand(ntc, null));
        }
        assertEquals(0, ntc.items.size());
        assertEquals(3000, stack.count());

        UndoSerializer managerBack = null;
        {
            // Make unzipped serialization
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String data = UndoSerializer.serialize(manager, false);
//            System.out.println("1: " + data.length());
            managerBack = UndoSerializer.deserialize(data, null);
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.ID, managerBack.ID);
            assertEquals(manager.VERSION, managerBack.VERSION);
            assertEquals(manager.getExtras(), managerBack.getExtras());
            assertEquals(manager.getStack().getSubject(), managerBack.getStack().getSubject());
            //~
            assertEquals(NonTrivialClass.class, manager.getStack().getSubject().getClass());
        }
        {
            // Make zipped serialization
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String z_data = UndoSerializer.serialize(manager, true);
//            System.out.println("zipped length : " + z_data.length());
            managerBack = UndoSerializer.deserialize(z_data, null);
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.VERSION, managerBack.VERSION);
            assertEquals(manager.getExtras(), managerBack.getExtras());
            assertEquals(manager.getStack().getSubject(), managerBack.getStack().getSubject());
            //~
            assertEquals(NonTrivialClass.class, manager.getStack().getSubject().getClass());
        }

        UndoStack stackBack = managerBack.getStack();
        NonTrivialClass ntcBack = (NonTrivialClass)stackBack.getSubject();
        stackBack.setWatcher(new SimpleUndoWatcher());
        // Check out
        for(int i = 0; i < 1000; ++i) {
            stackBack.undo();
        }
        assertEquals(1000, ntcBack.items.size());
        for(int i = 0; i < 1000; ++i) {
            stackBack.undo();
        }
        assertEquals(1000, ntcBack.items.size());
        for(int i = 0; i < 1000; ++i) {
            stackBack.undo();
        }
        assertEquals(0, ntcBack.items.size());

        //============================================================================
        // Illustrate versioning
        assertEquals(NonTrivialClass.class, ntcBack.getClass());
        NonTrivialClass_v2 v2 = new NonTrivialClass_v2();
        stackBack.setIndex(stackBack.count());
        v2.items.addAll(ntcBack.items);
        assertEquals(v2.items, ntcBack.items);

    }

}
