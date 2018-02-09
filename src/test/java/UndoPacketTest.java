import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoPacket;
import com.gdetotut.jundo.UndoStack;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.junit.Test;
import some.SimpleUndoWatcher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * We must make it serializable because some inner classes are subject to serialize.
 */
public class UndoPacketTest implements Serializable {

    /**
     * Here {@link UndoStack} has non-serializable subject.
     *
     * @throws Exception
     */
    @Test
    public void testForNonSerializable() throws Exception {

        class Util {
            private String circleToString(Circle circle) {
                ArrayList<String> param = new ArrayList<>();
                param.add(String.valueOf(circle.getRadius()));
                param.add(String.valueOf(circle.getFill()));
                return String.join(",", param);
            }

            private Object stringToCircle(String str) {
                List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
                Circle c = new Circle(Double.valueOf(items.get(0)), Color.valueOf(items.get(1)));
                return c;
            }

        }

        class CircleRadiusUndoCmd extends UndoCommand {

            Double oldV;
            Double newV;

            public CircleRadiusUndoCmd(Circle circle, Double newV) {
                super("");
                oldV = circle.getRadius();
                this.newV = newV;
            }

            @Override
            protected void doRedo() {
                Circle c = (Circle) getOwner().getSubj();
                c.setRadius(newV);
            }

            @Override
            protected void doUndo() {
                Circle c = (Circle) getOwner().getSubj();
                c.setRadius(oldV);
            }
        }

        int count = 101;

        Circle circle = new Circle(20.0, Color.RED);
        // Circle is non-serializable subject.
        UndoStack stack = new UndoStack(circle);
        stack.setWatcher(new SimpleUndoWatcher());
        for (int i = 0; i < count; ++i) {
            stack.push(new CircleRadiusUndoCmd(circle, i * 2.0));
        }
        assertEquals(count, stack.count());
        assertEquals(0, Double.compare(200.0, circle.getRadius()));

        while (stack.canUndo())
            stack.undo();
        assertEquals(count, stack.count());
        assertEquals(0, Double.compare(20.0, circle.getRadius()));

        String store = UndoPacket
                // It's a good practice always specify id.
                .make(stack, "javafx.scene.shape.Circle", 1)
                // Circle is not serializable so we have to make it by hands.
                .onStore(subj -> new Util().circleToString((Circle) subj))
                .zipped(true) // why not?
                .store();
        UndoPacket packetBack = UndoPacket
                // When we have no handlers, we still need to specify it explicitly.
                .peek(store, subjInfo -> {
                    // If not, no further restore and no excessive work.
                    return "javafx.scene.shape.Circle".equals(subjInfo.id);
                })
                .restore((processedSubj, subjInfo) -> {
                    // Strictly speaking we check id above, so check here just version.
                    if (subjInfo.version != 1) {
                        throw new Exception("Unexpected version");
                    }
                    return new Util().stringToCircle((String) processedSubj);
                }, null);
        UndoStack stack2 = packetBack
                // When we have no handler, we still need to specify it explicitly.
                .prepare(null);

        Circle circle1 = (Circle) stack2.getSubj();
        assertEquals(count, stack2.count());
        assertEquals(0, Double.compare(20.0, circle1.getRadius()));

        while (stack2.canRedo())
            stack2.redo();
        assertEquals(0, Double.compare(200.0, circle1.getRadius()));
    }


    /**
     * Tests complex non-serializable class as subject for UndoStack
     *
     * @throws Exception
     */
    @Test
    public void testComplexSubj() throws Exception {

        // Non-serializable subject
        class Canvas {
            final static int CT_Circle = 1;
            final static int CT_Rect = 2;
            List<Shape> shapes = new ArrayList<>();

        }


        // Context #1
        class LocalContext {
            String getAdd() {
                return "cmd_add";
            }

            String getRemove() {
                return "cmd_remove";
            }

            String getResize() {
                return "cmd_resize";
            }
        }

        // Context #2
        class LocalContext1 extends LocalContext{

            @Override
            String getAdd() {
                return "cmd_add1";
            }

            @Override
            String getRemove() {
                return "cmd_remove1";
            }

            @Override
            String getResize() {
                return "cmd_resize1";
            }

        }

        // Makes Shapes for Canvas and converts String <--> Canvas
        class Factory {

            Shape make(int type) {
                switch (type) {
                    case Canvas.CT_Circle:
                        return new Circle(10.0, Color.RED);
                    case Canvas.CT_Rect:
                    default:
                        return new Rectangle(10.0, 10.0, Color.BLUE);
                }
            }

            private String toStr(Canvas canvas) {
                ArrayList<String> param = new ArrayList<>();
                for (Shape shape :
                        canvas.shapes) {
                    if (shape.getClass() == Circle.class) {
                        param.add(String.valueOf(Canvas.CT_Circle));
                        param.add(String.valueOf(((Circle) shape).getRadius()));
                    } else {
                        param.add(String.valueOf(Canvas.CT_Rect));
                        param.add(String.valueOf(((Rectangle) shape).getWidth()));
                    }
                }
                return String.join(",", param);
            }

            private Object toSubj(String str) {
                Canvas canvas = new Canvas();
                List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
                int i = 0;
                Shape shape = null;
                while (i < items.size() - 1) {
                    int type = Integer.parseInt(items.get(i++));
                    Double val = Double.parseDouble(items.get(i++));
                    if (type == Canvas.CT_Circle) {
                        shape = new Circle(val);
                    } else {
                        shape = new Rectangle(val, val);
                    }
                    canvas.shapes.add(shape);
                }
                return canvas;
            }

        }

        // Controller of Canvas undo commands.
        class CanvasCmdCtrl implements Serializable {

            // Adds Shape to Canvas
            class Add extends UndoCommand {
                int type;

                public Add(int type) {
                    super("");
                    this.type = type;
                }

                @Override
                protected void doRedo() {
                    LocalContext ctx = (LocalContext) getOwner().getLocalContexts().get("resources");
                    setCaption(ctx.getAdd());
                    ((Canvas) getOwner().getSubj()).shapes.add(new Factory().make(type));
                }

                @Override
                protected void doUndo() {
                    List<Shape> shapes = ((Canvas) getOwner().getSubj()).shapes;
                    shapes.remove(shapes.size() - 1);
                }
            }

            // Removes Shape from Canvas
            class Remove extends UndoCommand {
                int type;

                public Remove() {
                    super("");
                }

                @Override
                protected void doRedo() {

                    List<Shape> shapes = ((Canvas) getOwner().getSubj()).shapes;
                    this.type = shapes.get(shapes.size() - 1) instanceof Circle ? Canvas.CT_Circle : Canvas.CT_Rect;
                    LocalContext ctx = (LocalContext) getOwner().getLocalContexts().get("resources");
                    setCaption(ctx.getRemove());


                    shapes.remove(shapes.size() - 1);
                }

                @Override
                protected void doUndo() {
                    ((Canvas) getOwner().getSubj()).shapes.add(new Factory().make(type));
                }
            }

            //
            class Resize extends UndoCommand {
                int idx;
                double oldV;
                double newV;

                public Resize(int idx, double newValue) {
                    super("");
                    this.idx = idx;
                    this.newV = newValue;
                }

                @Override
                protected void doRedo() {
                    List<Shape> shapes = ((Canvas) getOwner().getSubj()).shapes;

                    LocalContext ctx = (LocalContext) getOwner().getLocalContexts().get("resources");
                    setCaption(ctx.getResize());

                    Shape shape = shapes.get(idx);
                    if (shape instanceof Circle) {
                        oldV = ((Circle) shape).getRadius();
                        ((Circle) shape).setRadius(newV);
                    } else {
                        oldV = ((Rectangle) shape).getWidth();
                        ((Rectangle) shape).setWidth(newV);
                        ((Rectangle) shape).setHeight(newV);
                    }
                }

                @Override
                protected void doUndo() {
                    List<Shape> shapes = ((Canvas) getOwner().getSubj()).shapes;
                    Shape shape = shapes.get(idx);
                    if (shape instanceof Circle) {
                        ((Circle) shape).setRadius(oldV);
                    } else {
                        ((Rectangle) shape).setWidth(oldV);
                        ((Rectangle) shape).setHeight(oldV);
                    }
                }
            }

        }

        /////////////////////////////

        // First only add/remove
        Canvas canvas = new Canvas();
        // Use non-serializable subject.
        UndoStack stack = new UndoStack(canvas);
        // Use local context.
        stack.getLocalContexts().put("resources", new LocalContext());
        stack.push(new CanvasCmdCtrl().new Add(Canvas.CT_Circle));
        stack.push(new CanvasCmdCtrl().new Add(Canvas.CT_Rect));
        stack.push(new CanvasCmdCtrl().new Add(Canvas.CT_Circle));
        stack.push(new CanvasCmdCtrl().new Add(Canvas.CT_Rect));
        assertEquals(4, canvas.shapes.size());
        assertEquals(Circle.class, canvas.shapes.get(0).getClass());
        assertEquals(Rectangle.class, canvas.shapes.get(1).getClass());
        stack.undo();
        stack.undo();
        stack.undo();
        stack.undo();
        assertEquals(0, canvas.shapes.size());
        stack.setIndex(100);
        assertEquals(4, stack.getIdx());
        assertEquals(4, canvas.shapes.size());

        // Remove last one and return back
        stack.push(new CanvasCmdCtrl().new Remove());
        assertEquals(3, canvas.shapes.size());

        stack.undo();
        assertEquals(4, canvas.shapes.size());
        assertEquals(5, stack.count());

        assertEquals(new LocalContext().getAdd(), stack.getCommand(0).getCaption());
        assertEquals(new LocalContext().getAdd(), stack.getCommand(1).getCaption());
        assertEquals(new LocalContext().getAdd(), stack.getCommand(2).getCaption());
        assertEquals(new LocalContext().getAdd(), stack.getCommand(3).getCaption());
        assertEquals(new LocalContext().getRemove(), stack.getCommand(4).getCaption());

        // undo two and add new one. Should start new branch
        stack.undo();
        stack.undo();
        assertEquals(2, canvas.shapes.size());
        stack.push(new CanvasCmdCtrl().new Add(Canvas.CT_Rect));
        assertEquals(3, stack.count());


        // Serialization after add/remove
        {
            String store = UndoPacket
                    // It's a good practice always specify id.
                    .make(stack, "local.Canvas", 2)
                    // We need convert non-serializable subject by hand.
                    .onStore(subj -> new Factory().toStr((Canvas) subj))
                    .zipped(true) //
                    .store();
            UndoStack stack1 = UndoPacket
                    // When we have no handlers, we still need to specify it explicitly.
                    .peek(store, subjInfo -> {
                        // This way we check our subject; if false no further unpacking and no excessive work.
                        return "local.Canvas".equals(subjInfo.id);
                    })
                    .restore((processedSubj, subjInfo) -> {
                        if (subjInfo.version != 2) {
                            throw new Exception("unexpected version");
                        }
                        return new Factory().toSubj((String) processedSubj);
                    }, null)
                    .prepare((stack2, subjInfo, result) -> {
                        // Good place to restore local contexts.
                        stack2.getLocalContexts().put("resources", new LocalContext1());
                    });
            Canvas canvas1 = (Canvas) stack1.getSubj();

            stack1.undo();
            stack1.undo();
            assertEquals(1, canvas1.shapes.size());
            stack1.setIndex(100);
            assertEquals(3, stack1.getIdx());
            assertEquals(3, canvas1.shapes.size());
            assertEquals(3, stack1.count());

            UndoCommand cmd = stack.getCommand(0);
            assertEquals(new LocalContext().getAdd(), cmd.getCaption());
        }

        // Now change
        Rectangle rect = (Rectangle) canvas.shapes.get(1);
        assertEquals(0, Double.compare(10.0, rect.getWidth()));
        stack.push(new CanvasCmdCtrl().new Resize(1, 50.0));
        assertEquals(0, Double.compare(50.0, rect.getWidth()));
        stack.undo();
        assertEquals(0, Double.compare(10.0, rect.getWidth()));
        stack.redo();
        assertEquals(3, canvas.shapes.size());
        {
            String store = UndoPacket
                    // It's a good practice always specify id.
                    .make(stack, "local.Canvas", 2)
                    // We need convert non-serializable subject by hand.
                    .onStore(subj -> new Factory().toStr((Canvas) subj))
                    .zipped(true) //
                    .store();
            UndoStack stack1 = UndoPacket
                    // When we have no handlers, we still need to specify it explicitly.
                    .peek(store, subjInfo -> {
                        // This way we check our subject; if false no further unpacking and no excessive work.
                        return "local.Canvas".equals(subjInfo.id);
                    })
                    .restore((processedSubj, subjInfo) -> {
                        if (subjInfo.version != 2) {
                            throw new Exception("unexpected version");
                        }
                        return new Factory().toSubj((String) processedSubj);
                    }, null)
                    .prepare((stack2, subjInfo, result) -> {
                        // Good place to restore local contexts.
                        stack2.getLocalContexts().put("resources", new LocalContext1());
                    });
            Canvas canvas1 = (Canvas) stack1.getSubj();
            assertEquals(3, canvas1.shapes.size());

            Rectangle rect1 = (Rectangle) canvas1.shapes.get(1);
            assertEquals(0, Double.compare(50.0, rect1.getWidth()));
        }

    }
}
