import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoPacket.UnpackResult;
import com.gdetotut.jundo.UndoStack;
import org.junit.Test;
import some.TextSample;
import some.TimeMachineCommands;
import some.TimeMachineCommands.AddNewLineCmd;
import some.TimeMachineCommands.AddTextCmd;
import some.TimeMachineCommands.TimeMachineBaseCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Realistic use cases.
 *
 * <p>testTimeMachine shows mechanizm to turn back for some time.
 *
 * <p>@see SNAPSHOT on <a href="https://github.com/ValeriusGC/jundo/tree/feat_ver2">https://github.com/ValeriusGC/jundo/tree/feat_ver2</a>
 */
public class UseCasesTest {

    /**
     * This test emulates 'Time Machine' behaviour, i.e. we can do moving across commands by their time periods.
     * <p>Every command has 'time label' property and we manipulate them.
     * <p>1. At first the document will look like that:
     * <ul>
     * <li>100 msec
     * <li>200 msec
     * <li>...
     * <li>900 msec
     * <li>1000 msec
     * </ul>
     * <p>2. Then we'll go back, say, 500 milliseconds. The document will be then
     * <ul>
     * <li>100 msec
     * <li>200 msec
     * <li>300 msec
     * <li>400 msec
     * <li>500 msec
     * </ul>
     * <p>3. When we start again and make, say, 3 steps the document will turn to
     * <ul>
     * <li>100 msec
     * <li>200 msec
     * <li>300 msec
     * <li>400 msec
     * <li>500 msec
     * <li>1100 msec
     * <li>1200 msec
     * <li>1300 msec
     * </ul>
     * because of time never stops.
     * <p>4. Then if we wanna go back for 500 milliseconds we will got of course
     * <ul>
     * <li>100 msec
     * <li>200 msec
     * <li>300 msec
     * <li>400 msec
     * <li>500 msec
     * </ul>
     * again!
     * <p>Let's check. And for fun will mix it with repacking.
     */
    @Test
    public void testTimeMachine() throws Exception {

        TextSample doc = new TextSample();
        UndoStack stack = new UndoStack(doc);
        List<String> testDoc = new ArrayList<>();

        //--------------------------
        // Do step [1] and test list
        for (int i = 0; i < 10; ++i) {
            long time = (i + 1) * 100L;
            String str = String.format("%d msec", time);
            stack.push(new AddNewLineCmd("step " + i, time));
            stack.push(new AddTextCmd("step " + i, time, str));
            // This is because TextSample' behaviour on adding (see TextSample.add method)
            testDoc.add("" + (i + 1) + ": " + str);
        }
        // Emulate passing 1000 msec
        Long currTime = 1000L;
        // Test
        assertEquals(testDoc, doc.text);
        // Show
        System.out.println("testTimeMachine. Step 1:");
        System.out.println(doc.print());

        //--------------------------
        // Step 2. Removing 'last 500 msec'
        Long needTime = currTime - 500;
        while (((TimeMachineBaseCmd) stack.getCommand(stack.getIdx() - 1)).getTime() > needTime) {
            stack.undo();
        }
        testDoc = testDoc.subList(0, 5);
        // Test
        assertEquals(testDoc, doc.text);
        // Show
        System.out.println("testTimeMachine. Step 2:");
        System.out.println(doc.print());


        //----------------
        // Make repacking for fun
        String pack = UndoPacket
                .make(stack, TimeMachineCommands.SUBJ_ID, 1)
                // As TextSample is not serializable, we do it by hands.
                .onStore(subj -> String.join("!!!!", ((TextSample)subj).text))
                .store();
        UndoStack stack1 = UndoPacket
                .peek(pack, si -> si.id.equals(TimeMachineCommands.SUBJ_ID))
                // As TextSample was serialized by hands do it back the same way.
                .restore((processedSubj, subjInfo) -> {
                    String s = (String)processedSubj;
                    List<String> text = Arrays.asList(s.split("!!!!"));
                    TextSample textSample = new TextSample();
                    textSample.reset(text);
                    return textSample;
                }, () -> new UndoStack(new TextSample())) // Very recommend to provide default UndoStack creator!
                // Good practice to check code and make smth when it is error.
                .prepare((stack2, subjInfo, result) -> {
                    if(result.code != UnpackResult.UPR_Success) {
                        System.err.println(result.msg);
                    }
                });
        // As subject in restored stack is not the same we need to replace it.
        doc = (TextSample) stack1.getSubj();
        // ~Make repacking for fun

        //--------------------------
        // Step 3. Add another 3 steps from 'current time'
        for (int i = 10; i < 13; ++i) {
            long time = (i + 1) * 100L;
            String str = String.format("%d msec", time);
            stack1.push(new AddNewLineCmd("step " + i, time));
            stack1.push(new AddTextCmd("step " + i, time, str));
            // This is because TextSample' behaviour on adding (see TextSample.add method)
            testDoc.add("" + (i + 1 - 5) + ": " + str);
        }
        // Emulate passing 1300 msec
        currTime = 1300L;
        // Test
        assertEquals(testDoc, doc.text);
        // Show
        System.out.println("testTimeMachine. Step 3:");
        System.out.println(doc.print());

        //--------------------------
        // Step 4. Removing 'last 500 msec' again
        needTime = currTime - 500;
        while (((TimeMachineBaseCmd) stack1.getCommand(stack1.getIdx() - 1)).getTime() > needTime) {
            stack1.undo();
        }
        testDoc = testDoc.subList(0, 5);
        // Test
        assertEquals(testDoc, doc.text);
        // Show
        System.out.println("testTimeMachine. Step 4:");
        System.out.println(doc.print());
    }

}
