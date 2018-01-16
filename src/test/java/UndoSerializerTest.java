import com.gdetotut.jundo.UndoSerializer;
import org.junit.Test;
import some.SimpleUndoWatcher;
import some.NonTrivialClass;
import com.gdetotut.jundo.UndoStack;

import static org.junit.Assert.assertEquals;

public class UndoSerializerTest {


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

        int count = 20;

        NonTrivialClass ntc = new NonTrivialClass();
        UndoStack stack = new UndoStack(ntc, null);
        stack.setWatcher(new SimpleUndoWatcher());
        for(int i = 0; i < count; ++i){
            stack.push(new NonTrivialClass.AddCommand(stack, NonTrivialClass.Item.Type.CIRCLE, ntc, null));
        }
        assertEquals(count, ntc.items.size());
        assertEquals(count, stack.count());
        for(int i = 0; i < count; ++i){
            stack.push(new NonTrivialClass.MovedCommand(stack, ntc.items.get(i), 10, null));
        }
        assertEquals(count, ntc.items.size());
        assertEquals(count * 2, stack.count());
        for(int i = 0; i < count; ++i){
            stack.push(new NonTrivialClass.DeleteCommand(stack, ntc, null));
        }
        assertEquals(0, ntc.items.size());
        assertEquals(count * 3, stack.count());

        UndoSerializer managerBack = null;
        {
            // Make unzipped serialization
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String data = UndoSerializer.serialize(manager, false, null);
//            System.out.println("1: " + data.length());
            managerBack = UndoSerializer.deserialize(data, null);
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.subjInfo.id, managerBack.subjInfo.id);
            assertEquals(manager.subjInfo.version, managerBack.subjInfo.version);
            assertEquals(manager.subjInfo.extras, managerBack.subjInfo.extras);
            assertEquals(manager.getStack().getSubj(), managerBack.getStack().getSubj());
            //~
            assertEquals(NonTrivialClass.class, manager.getStack().getSubj().getClass());
        }
        {
            // Make zipped serialization
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String z_data = UndoSerializer.serialize(manager, true, null);
//            System.out.println("zipped length : " + z_data.length());
            managerBack = UndoSerializer.deserialize(z_data, null);
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.subjInfo.version, managerBack.subjInfo.version);
            assertEquals(manager.subjInfo.extras, managerBack.subjInfo.extras);
            assertEquals(manager.getStack().getSubj(), managerBack.getStack().getSubj());
            //~
            assertEquals(NonTrivialClass.class, manager.getStack().getSubj().getClass());
        }

        UndoStack stackBack = managerBack.getStack();
        NonTrivialClass ntcBack = (NonTrivialClass)stackBack.getSubj();
        stackBack.setWatcher(new SimpleUndoWatcher());
        // Check out
        for(int i = 0; i < count; ++i) {
            stackBack.undo();
        }
        assertEquals(count, ntcBack.items.size());
        for(int i = 0; i < count; ++i) {
            stackBack.undo();
        }
        assertEquals(count, ntcBack.items.size());
        for(int i = 0; i < count; ++i) {
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
