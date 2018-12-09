import com.gdetotut.jundo.*;
import org.junit.Assert;
import org.junit.Test;
import some.Point;
import some.TextSample;
import some.TimeMachineCommands;
import some.TimeMachineCommands.AddNewLineCmd;
import some.TimeMachineCommands.AddTextCmd;
import some.TimeMachineCommands.TimeMachineBaseCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gdetotut.jundo.UndoPacket.Result.Code.RC_Success;
import static org.junit.Assert.assertEquals;

public class Undo2019Test {

    @Test
    public void testClassType() {
        Integer i = 20;
        Assert.assertEquals(Integer.class, i.getClass());
    }

    @Test
    public void testDataPacket() {
        DataPacket2019 dataPacket2019 = new DataPacket2019(Integer.class, "20");
        Assert.assertEquals(Integer.class, dataPacket2019.clazz);
    }

    @Test
    public void testStack() {
        UndoStack2019Point stack = new UndoStack2019Point();
        stack.subject = new Point(30, 30);

        stack.push(new UndoCommandData2019.Add(10));
        Assert.assertEquals((Integer) 40, (Integer) stack.subject.getX());

        stack.push(new UndoCommandData2019.Sub(20));
        Assert.assertEquals((Integer) 20, (Integer) stack.subject.getX());

        stack.undo();
        Assert.assertEquals((Integer) 40, (Integer) stack.subject.getX());
        stack.undo();
        Assert.assertEquals((Integer) 30, (Integer) stack.subject.getX());
        stack.redo();
        Assert.assertEquals((Integer) 40, (Integer) stack.subject.getX());
        stack.redo();
        Assert.assertEquals((Integer) 20, (Integer) stack.subject.getX());
    }


    static class UndoStack2019Point extends UndoStack2019<Point>{

        public UndoStack2019Point() {
            super(new UndoCommandFactoryPoint2019());
        }
    }

    interface UndoCommandData2019 {

        public class Add {
            public final int value;
            public Add(int value) {
                this.value = value;
            }
        }

        public class Sub {
            public final int value;
            public Sub(int value) {
                this.value = value;
            }
        }

    }

    public interface UndoCommand2019 {

        class CmdAdd extends BaseUndoCommand2019<Point> {

            private final UndoCommandData2019.Add data;

            public CmdAdd(UndoCommandData2019.Add data, Point subject) {
                super(subject);
                this.data = data;
            }

            @Override
            public void doUndo() {
                subject.setX(subject.getX() - data.value);
            }

            @Override
            public void doRedo() {
                subject.setX(subject.getX() + data.value);;
            }
        }

        class CmdSub extends BaseUndoCommand2019<Point> {


            private final UndoCommandData2019.Sub data;

            public CmdSub(UndoCommandData2019.Sub data, Point subject) {
                super(subject);
                this.data = data;
            }

            @Override
            public void doUndo() {
                subject.setX(subject.getX() + data.value);;
            }

            @Override
            public void doRedo() {
                subject.setX(subject.getX() - data.value);
            }
        }

    }


    static public class UndoCommandFactoryPoint2019 implements UndoCommandFactory2019<Point>{

        public BaseUndoCommand2019<Point> makeBy(Object data, Point subject) {

            if(data.getClass().equals(UndoCommandData2019.Add.class)) {
                return new UndoCommand2019.CmdAdd((UndoCommandData2019.Add) data, subject);
            }

            if(data.getClass().equals(UndoCommandData2019.Sub.class)) {
                return new UndoCommand2019.CmdSub((UndoCommandData2019.Sub) data, subject);
            }

            return null;

        }

    }


}
