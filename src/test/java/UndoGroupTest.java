import org.junit.Test;
import some.SimpleClass;
import com.gdetotut.jundo.UndoGroup;
import com.gdetotut.jundo.UndoStack;

import static org.junit.Assert.assertEquals;

public class UndoGroupTest {

    /**
     * Group should not contain two or more stacks with one object.
     */
    @Test
    public void testOneGroupOneObject() {

        SimpleClass<Integer> subj = new SimpleClass<>(Integer.class);
        UndoGroup group = new UndoGroup();
        new UndoStack(subj, group);
        assertEquals(1, group.getStacks().size());
        new UndoStack(subj, group);
        // See that 2nd stack with the same subject is not added to the group...
        assertEquals(1, group.getStacks().size());

        SimpleClass<Integer> subj2 = new SimpleClass<>(Integer.class);
        // ...And with another one is added.
        new UndoStack(subj2, group);
        assertEquals(2, group.getStacks().size());

    }

}
