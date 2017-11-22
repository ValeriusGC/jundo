import org.junit.Test;
import serialize.UndoWatcher;
import com.gdetotut.jundo.UndoManager;
import serialize.NonTrivialClass;
import com.gdetotut.jundo.UndoStack;

import static org.junit.Assert.assertEquals;

public class TestUndoManager {

    @Test
    public void serialize() throws Exception {

        NonTrivialClass ntc = new NonTrivialClass();
        UndoStack stack = new UndoStack(ntc, null);
        stack.setSubscriber(new UndoWatcher());
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

        UndoManager managerBack = null;
        {
            // Make unzipped serialization
            UndoManager manager = new UndoManager(null,2, stack);
            String data = UndoManager.serialize(manager, false);
//            System.out.println("1: " + data.length());
            managerBack = UndoManager.deserialize(data);
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.VERSION, managerBack.VERSION);
            assertEquals(manager.getExtras(), managerBack.getExtras());
            assertEquals(manager.getStack().getSubject(), managerBack.getStack().getSubject());
            //~
            assertEquals(NonTrivialClass.class, manager.getStack().getSubject().getClass());
        }
        {
            // Make zipped serialization
            UndoManager manager = new UndoManager(null,2, stack);
            String z_data = UndoManager.serialize(manager, true);
//            System.out.println("zipped length : " + z_data.length());
            managerBack = UndoManager.deserialize(z_data);
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
        stackBack.setSubscriber(new UndoWatcher());
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

    }

}
