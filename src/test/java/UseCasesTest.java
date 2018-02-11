import com.gdetotut.jundo.CreatorException;
import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoPacket.UnpackResult;
import com.gdetotut.jundo.UndoStack;
import org.junit.Test;
import some.*;
import some.DocSampleCommands.AddLine;
import some.DocSampleCommands.BiFuncUndoCommand;
import some.DocSampleCommands.FuncUndoCommand;
import some.TimeMachineCommands.AddNewLineCmd;
import some.TimeMachineCommands.AddTextCmd;
import some.TimeMachineCommands.TimeMachineBaseCmd;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    static class BaseSide {

        //-----------------
        // Входные данные для параметров объектов
        static final int[] params = new int[]{10, 20, 30};
        static final String txtSq0 = "square of 10 = 100";
        static String txtSq1 = "square of 20 = 400";
        static String txtSq2 = "square of 30 = 900";

        static final String txtCb0 = "cube of 10 = 1000";
        static String txtCb1 = "cube of 20 = 8000";
        static String txtCb2 = "cube of 30 = 27000";

        static String txtSm0 = "sum of 10 + 20 = 30";
        static String txtSm1 = "sum of 10 + 30 = 40";
        static String txtSm2 = "sum of 20 + 30 = 50";
        // ~Входные данные
        //-----------------

        // Идентификаторы документов
        static String[] dids = new String[] {"squaringDoc", "cubingDoc", "summingDoc"};
        // Идентификаторы стеков & стеки
        static String[] sids = new String[]
                {"stackSquaringDoc", "stackCubingDoc", "stackSummingDoc", "stackList"};
        static Map<String, UndoStack> stackMap = new TreeMap<>();
        // Идентификаторы макросов & макросы
        static String[] mids = new String[] {"make square", "make cube", "make sum"};

        /**
         * Map of macros
         */
        Map<String, UndoCommand> macroMap = new TreeMap<>();


        // Будущий пакет и карта пакетов
        static String packet;

        /**
         * Serialized stacks
         */
        protected Map<String, String> packets = new HashMap<>();

        //
        static Map<String, List<String>> textMap = new HashMap<>();

        static List<DocSample> docs = new ArrayList<>();


        protected Integer square(Integer param) {
            return param * param;
        }

        protected Integer cube(Integer param) {
            return param * param * param;
        }

        protected Integer sum(Integer param1, Integer param2) {
            return param1 + param2;
        }

    }

    //------------------------------------------------------------------------------------------------------------------
    /**
     * Класс для действия на стороне "А"
     */
    class SideA extends BaseSide{

        void createDoc0() throws Exception {
            int idx = 0;
            // Помещаем документ "SquaringDoc" в лист
            DocSample doc = new DocSample(dids[idx]);
            docs.add(doc);
            // Помещаем стек в лист
            UndoStack stack = new UndoStack(doc);
            stackMap.put(sids[idx], stack);
            // Place context to list
            Function<Integer, Integer> func = this::square;
            stack.getLocalContexts().put(DocSampleCommands.FUNC_SQ_CTX_KEY, func);
            // Создание первой команды и макроса для квадрата первого параметра
            stack.beginMacro(mids[idx]);
            stack.push(new AddLine(""));
            String funcName = "square";
            stack.push(new FuncUndoCommand("", funcName, params[0]));
            stack.endMacro();
            macroMap.put(mids[idx], stack.cloneMacro(idx));
            //
            List<String> text = new ArrayList<>();
            textMap.put(dids[idx], text);
            text.add(txtSq0);
            assertEquals(text, doc.text);
            System.out.println(doc.print());
        }

        void createDoc1() throws Exception {
            int idx = 1;
            // Помещаем документ "CubingDoc" в лист
            DocSample doc = new DocSample(dids[idx]);
            docs.add(doc);
            // Помещаем стек в лист
            UndoStack stack = new UndoStack(doc);
            stackMap.put(sids[idx], stack);
            // Place context to list
            Function<Integer, Integer> func = this::cube;
            stack.getLocalContexts().put(DocSampleCommands.FUNC_SQ_CTX_KEY, func);
            // Создание первой команды и макроса для квадрата первого параметра
            stack.beginMacro(mids[idx]);
            stack.push(new AddLine(""));
            String funcName = "cube";
            stack.push(new FuncUndoCommand("", funcName, params[0]));
            stack.endMacro();
            macroMap.put(mids[idx], stack.cloneMacro(idx));
            //
            List<String> text = new ArrayList<>();
            textMap.put(dids[idx], text);
            text.add(txtCb0);
            assertEquals(text, doc.text);
            System.out.println(doc.print());
        }

        void createDoc2() throws Exception {
            int idx = 2;
            // Помещаем документ "SummingDoc" в лист
            DocSample doc = new DocSample(dids[idx]);
            docs.add(doc);
            // Помещаем стек в лист
            UndoStack stack = new UndoStack(doc);
            stackMap.put(sids[idx], stack);
            // Place context to list
            BiFunction<Integer, Integer, Integer> func = this::sum;
            stack.getLocalContexts().put(DocSampleCommands.FUNC_SQ_CTX_KEY, func);
            // Создание первой команды и макроса для квадрата первого параметра
            stack.beginMacro(mids[idx]);
            stack.push(new AddLine(""));
            String funcName = "sum";
            stack.push(new BiFuncUndoCommand("", funcName, params[0], params[1]));
            stack.endMacro();
            macroMap.put(mids[idx], stack.cloneMacro(idx));
            //
            List<String> text = new ArrayList<>();
            textMap.put(dids[idx], text);
            text.add(txtSm0);
            assertEquals(text, doc.text);
            System.out.println(doc.print());
        }

        void doingWork() throws Exception {
            createDoc0();
            createDoc1();
            createDoc2();
        }

        Map<String, String> pack() throws Exception {
            // пак
            packet = UndoPacket
                    .make(stackMap.get(sids[0]), sids[0], 1)
                    // Упаковываем содержимое объекта как строки с уникальными делимитерами
                    .onStore(subj -> String.join("!!!!", ((DocSample)subj).text))
                    .store();
            packets.put(sids[0], packet);
            return packets;
        }

//        private Integer square(Integer param) {
//            return param * param;
//        }
//
//        private Integer cube(Integer param) {
//            return param * param * param;
//        }
//
//        private Integer sum(Integer param1, Integer param2) {
//            return param1 + param2;
//        }

    }
    //------------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------------
    /**
     * Класс для действия на стороне "B"
     */
    class SideB extends BaseSide {

        void update(Map<String, String> packets) throws CreatorException {

            int idx = 0;
            packet = packets.get(BaseSide.sids[idx]);
            UndoStack stack = UndoPacket
                    // Проверим корректность объекта по сохраненному идентификатору
                    .peek(packet, subjInfo -> subjInfo.id.equals(BaseSide.sids[idx]))
                    .restore((processedSubj, subjInfo) -> {
                        // Работаем только с известной версией
                        if(subjInfo.version == 1) {
                            // Восстанавливаем субъект в стеке
                            String s = (String)processedSubj;
                            List<String> text = Arrays.asList(s.split("!!!!"));
                            DocSample doc = new DocSample(dids[idx]);
                            doc.reset(text);
                            return doc;
                        }
                        return null;
                    }, () -> new UndoStack(new DocSample(dids[idx])))
                    .prepare((stackBack, subjInfo, result) -> {
                        if(result.code != UnpackResult.UPR_Success) {
                            System.err.println(result.code + " <- " + result.msg);
                        }
                        Function<Integer, Integer> func = this::square;
                        stackBack.getLocalContexts().put(DocSampleCommands.FUNC_SQ_CTX_KEY, func);
                    });

            // Теперь, чисто для теста обновим doc восстановленным
            DocSample doc = (DocSample) stack.getSubj();
            List<String> text = textMap.get(dids[idx]);
            assertEquals(text, doc.text);

        }

//        private Integer square(Integer param) {
//            return param * param;
//        }
//
//        private Integer cube(Integer param) {
//            return param * param * param;
//        }
//
//        private Integer sum(Integer param1, Integer param2) {
//            return param1 + param2;
//        }

    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Сложный комплекс стеков для редактирования списка, состояния, и расшаривания макросов.
     *
     * Задачи:
     *  1. Проверить работоспособность макросов в "чужих стеках"
     *  2. Проверить "параметризованные" макросы
     *  3. Проверить перенос согласованной группы макросов в другой контекст
     *
     * Сценарий:
     * 1. Три объекта размещаются в список, идет проверка ListUndo
     * 2. Каждый из объектов создает макрос, макросы размещаются в "глобальном" листе макросов
     * 3. Объекты по очереди пользуются макросами из общего листа
     *      В конечном итоге вот как будут выглядеть...
     *      - документы
     *          - doc[0] (squaring):
     *          square of 10 = 100
     *          square of 20 = 400
     *          square of 30 = 900
     *          - doc[1] (cubing):
     *          cube of 10 = 1000
     *          cube of 20 = 8000
     *          cube of 30 = 37000
     *          - doc[2] (summing):
     *          sum of 10 + 20 = 30
     *          sum of 10 + 30 = 40
     *          sum of 20 + 30 = 50
     *      - макросы
     *          - macro[0] = square [Function]
     *          - macro[1] = cube [Function]
     *          - macro[2] = sum [BiFunction]
     * 4. Перенос в другой контекст
     * 5. Проверка состояния после переноса
     * 6. Повторные действия в новом контексте
     *
     *
     * 1. Создаем список объектов + стек в группу
     * 2. Создаем объект + стек в группу: * 5
     * 3. Список параметризованных макросов
     *  3.1 "Итого:" + квадрат параметра
     *  3.2 "Итого:" + куб параметра
     *  3.3 "Итого:" + сумма параметров
     * 4. Макросы создаются каждый в разных объектах, собираются в общую группу
     * 5. Объекты добавляются, удаляются из списка, применяют общие макросы
     * 6. Переупаковка, новое использование
     *
     * @throws Exception
     */
    @Test
    public void testComplexGroup() throws Exception {

        // Do some work on A-side
        SideA a = new SideA();
        a.doingWork();

        // В данный момент в списке макросов 3 элемента
        assertEquals(3, a.macroMap.size());

        // Pack it
        Map<String, String> packets = a.pack();

        // Get for doc#0
        SideB b = new SideB();
        b.update(packets);

    }

}
