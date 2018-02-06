import com.gdetotut.jundo.UndoStack;
import org.junit.Test;
import some.TextSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Realistic use cases.
 */
public class UseCasesTest {


    // TODO: 01.02.18  Наваять ряд автоматических Историй Пользователей

    /**
     * This test emulates 'Time Machine' behaviour, i.e. we can do moving across commands by their time periods.
     *
     * Every command has 'time label' property and we manipulate them.
     *
     * 1. At first the document will look like that:
     * <ul>
     *     <li>100 msec
     *     <li>200 msec
     *     <li>...
     *     <li>900 msec
     *     <li>1000 msec
     * </ul>
     *
     * 2. Then we'll go back, say, 500 milliseconds. The document will be then
     * <ul>
     *     <li>100 msec
     *     <li>200 msec
     *     <li>300 msec
     *     <li>400 msec
     *     <li>500 msec
     * </ul>
     *
     * 3. When we start again and make, say, 3 steps the document will turn to
     * <ul>
     *     <li>100 msec
     *     <li>200 msec
     *     <li>300 msec
     *     <li>400 msec
     *     <li>500 msec
     *     <li>1100 msec
     *     <li>1200 msec
     *     <li>1300 msec
     * </ul>
     * because of time never stops.
     *
     * 4. Then if we wanna go back for 500 milliseconds we will got of course
     * <ul>
     *     <li>100 msec
     *     <li>200 msec
     *     <li>300 msec
     *     <li>400 msec
     *     <li>500 msec
     * </ul>
     * again!
     *
     * Let's check. And for fun will mix it with repacking.
     */
    @Test
    public void testTimeMachine() {

        TextSample doc = new TextSample();
        UndoStack stack = new UndoStack(doc);
        List<String> testStrings = new ArrayList<>();




    }

}
