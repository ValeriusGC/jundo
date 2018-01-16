import com.gdetotut.jundo.*;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.junit.Test;
import some.SimpleUndoWatcher;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * We must make it serializable because some inner classes are subject to serialize.
 */
public class UndoSerializer2Test implements Serializable{

    @Test
    public void serialize() throws Exception {

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

            public CircleRadiusUndoCmd(UndoStack owner, Circle circle, Double newV, UndoCommand parent) {
                super(owner, "", parent);
                oldV = circle.getRadius();
                this.newV = newV;
                if(!owner.getLocalContexts().containsKey("circle")) {
                    owner.getLocalContexts().put("circle", circle);
                }
            }

            @Override
            protected void doRedo() {
//                Circle c = (Circle)owner.localContexts.get("circle");
                Circle c = (Circle)owner.getSubj();
                c.setRadius(newV);
            }

            @Override
            protected void doUndo() {
//                Circle c = (Circle)owner.localContexts.get("circle");
                Circle c = (Circle)owner.getSubj();
                c.setRadius(oldV);
            }
        }

        int count = 101;

        Circle circle = new Circle(20.0, Color.RED);
        UndoStack stack = new UndoStack(circle, null);
        stack.getLocalContexts().put("circle", circle);
        stack.getLocalContexts().put("color_picker", new Color(1.0,1.0, 1.0, 1.0));
        stack.setWatcher(new SimpleUndoWatcher());
        for(int i = 0; i < count; ++i){
            stack.push(new CircleRadiusUndoCmd(stack, circle, i*2.0, null));
        }
        assertEquals(count, stack.count());
        assertEquals(0, Double.compare(200.0, circle.getRadius()));

        while (stack.canUndo())
            stack.undo();
        assertEquals(count, stack.count());
        assertEquals(0, Double.compare(20.0, circle.getRadius()));

        UndoSerializer managerBack = null;
        // Make unzipped serialization
        UndoSerializer manager = new UndoSerializer(null,2, stack);
        String data = UndoSerializer.serialize(manager, false, subj -> new Util().circleToString((Circle)subj));
        managerBack = UndoSerializer.deserialize(data, (subjAsString, subjInfo) -> new Util().stringToCircle(subjAsString));
        assertEquals(manager.subjInfo, managerBack.subjInfo);
        assertEquals(Circle.class, managerBack.getStack().getSubj().getClass());
        Circle circle1 = (Circle) managerBack.getStack().getSubj();
        assertEquals(0, Double.compare(circle1.getRadius(), circle.getRadius()));
        assertEquals(circle.getFill(), circle1.getFill());

        UndoStack stack2 = managerBack.getStack();
        stack2.getLocalContexts().put("circle", circle1);
        assertEquals(count, stack2.count());
        assertEquals(0, Double.compare(20.0, circle1.getRadius()));

        while (stack2.canRedo())
            stack2.redo();
        assertEquals(0, Double.compare(200.0, circle1.getRadius()));
    }


    /**
     * Tests complex non-serializable class as subject for UndoStack2
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
            final String ADD = "cmd_add";
            final String REMOVE = "cmd_remove";
            final String RESIZE = "cmd_resize";
        }

        // Context #2
        class LocalContext1 {
            final String ADD = "cmd_add1";
            final String REMOVE = "cmd_remove1";
            final String RESIZE = "cmd_resize1";
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
                for (Shape shape:
                     canvas.shapes) {
                    if(shape.getClass() == Circle.class) {
                        param.add(String.valueOf(Canvas.CT_Circle));
                        param.add(String.valueOf(((Circle)shape).getRadius()));
                    }else {
                        param.add(String.valueOf(Canvas.CT_Rect));
                        param.add(String.valueOf(((Rectangle)shape).getWidth()));
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
                    if(type == Canvas.CT_Circle) {
                        shape = new Circle(val);
                    }else {
                        shape = new Rectangle(val, val);
                    }
                    canvas.shapes.add(shape);
                }
                return canvas;
            }

        }

        // Controller of Canvas undo commands.
        class CanvasCmdCtrl implements Serializable{

            // Adds Shape to Canvas
            class Add extends UndoCommand {
                int type;
                public Add(UndoStack owner, int type, UndoCommand parent) {
                    super(owner, "", parent);
                    this.type = type;
                    LocalContext ctx = (LocalContext)owner.getLocalContexts().get("resources");
                    setCaption(ctx.ADD);
                }

                @Override
                protected void doRedo() {
                    ((Canvas)owner.getSubj()).shapes.add(new Factory().make(type));
                }

                @Override
                protected void doUndo() {
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    shapes.remove(shapes.size()-1);
                }
            }

            // Removes Shape from Canvas
            class Remove extends UndoCommand {
                int type;
                public Remove(UndoStack owner, UndoCommand parent) {
                    super(owner, "", parent);
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    this.type = shapes.get(shapes.size()-1) instanceof Circle ? Canvas.CT_Circle : Canvas.CT_Rect;

                    LocalContext ctx = (LocalContext)owner.getLocalContexts().get("resources");
                    setCaption(ctx.REMOVE);
                }

                @Override
                protected void doRedo() {
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    shapes.remove(shapes.size()-1);
                }

                @Override
                protected void doUndo() {
                    ((Canvas)owner.getSubj()).shapes.add(new Factory().make(type));
                }
            }

            //
            class Resize extends UndoCommand {
                int idx;
                double oldV;
                double newV;
                public Resize(UndoStack owner, UndoCommand parent, int idx, double newValue) {
                    super(owner, "", parent);
                    this.idx = idx;
                    this.newV = newValue;

                    LocalContext ctx = (LocalContext)owner.getLocalContexts().get("resources");
                    setCaption(ctx.RESIZE);

                }

                @Override
                protected void doRedo() {
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    Shape shape = shapes.get(idx);
                    if(shape instanceof Circle) {
                        oldV = ((Circle)shape).getRadius();
                        ((Circle)shape).setRadius(newV);
                    }else{
                        oldV = ((Rectangle)shape).getWidth();
                        ((Rectangle)shape).setWidth(newV);
                        ((Rectangle)shape).setHeight(newV);
                    }
                }

                @Override
                protected void doUndo() {
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    Shape shape = shapes.get(idx);
                    if(shape instanceof Circle) {
                        ((Circle)shape).setRadius(oldV);
                    }else{
                        ((Rectangle)shape).setWidth(oldV);
                        ((Rectangle)shape).setHeight(oldV);
                    }
                }
            }

        }

        /////////////////////////////

        // First only add/remove
        Canvas canvas = new Canvas();
        UndoStack stack = new UndoStack(canvas, null);
        stack.getLocalContexts().put("resources", new LocalContext());
        stack.push(new CanvasCmdCtrl().new Add(stack, Canvas.CT_Circle, null));
        stack.push(new CanvasCmdCtrl().new Add(stack, Canvas.CT_Rect, null));
        stack.push(new CanvasCmdCtrl().new Add(stack, Canvas.CT_Circle, null));
        stack.push(new CanvasCmdCtrl().new Add(stack, Canvas.CT_Rect, null));
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
        stack.push(new CanvasCmdCtrl().new Remove(stack, null));
        assertEquals(3, canvas.shapes.size());

        stack.undo();
        assertEquals(4, canvas.shapes.size());
        assertEquals(5, stack.count());

        assertEquals(new LocalContext().ADD, stack.getCommand(0).getCaption());
        assertEquals(new LocalContext().ADD, stack.getCommand(1).getCaption());
        assertEquals(new LocalContext().ADD, stack.getCommand(2).getCaption());
        assertEquals(new LocalContext().ADD, stack.getCommand(3).getCaption());
        assertEquals(new LocalContext().REMOVE, stack.getCommand(4).getCaption());

        // undo two and add new one. Should start new branch
        stack.undo();
        stack.undo();
        assertEquals(2, canvas.shapes.size());
        stack.push(new CanvasCmdCtrl().new Add(stack, Canvas.CT_Rect, null));
        assertEquals(3, stack.count());



        // Serialization after add/remove
        {
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String data = UndoSerializer.serialize(manager, false, subj -> new Factory().toStr((Canvas)subj));
            UndoSerializer manager1 = UndoSerializer.deserialize(data, (subjAsString, subjInfo) -> new Factory().toSubj(subjAsString));
            assertEquals(manager.subjInfo, manager1.subjInfo);

            assertEquals(Canvas.class, manager1.getStack().getSubj().getClass());
            Canvas canvas1 = (Canvas) manager1.getStack().getSubj();
            assertEquals(3, canvas1.shapes.size());

            UndoStack stack1 = manager1.getStack();
            // Here we adds new resources
            stack1.getLocalContexts().put("resources", new LocalContext1());
            stack1.undo();
            stack1.undo();
            assertEquals(1, canvas1.shapes.size());
            stack1.setIndex(100);
            assertEquals(3, stack1.getIdx());
            assertEquals(3, canvas1.shapes.size());
            assertEquals(3, stack1.count());

            UndoCommand cmd = stack.getCommand(0);
            assertEquals(new LocalContext().ADD, cmd.getCaption());
        }

        // Now change
        Rectangle rect = (Rectangle) canvas.shapes.get(1);
        assertEquals(0, Double.compare(10.0, rect.getWidth()));
        stack.push(new CanvasCmdCtrl().new Resize(stack, null, 1, 50.0));
        assertEquals(0, Double.compare(50.0, rect.getWidth()));
        stack.undo();
        assertEquals(0, Double.compare(10.0, rect.getWidth()));
        stack.redo();
        assertEquals(3, canvas.shapes.size());
        {
            UndoSerializer manager = new UndoSerializer(null,2, stack);
            String data = UndoSerializer.serialize(manager, false, subj -> new Factory().toStr((Canvas)subj));
            UndoSerializer manager1 = UndoSerializer.deserialize(data, (subjAsString, subjInfo) -> new Factory().toSubj(subjAsString));
            assertEquals(manager.subjInfo, manager1.subjInfo);
            assertEquals(Canvas.class, manager1.getStack().getSubj().getClass());
            Canvas canvas1 = (Canvas) manager1.getStack().getSubj();
            assertEquals(3, canvas1.shapes.size());

            Rectangle rect1 = (Rectangle) canvas1.shapes.get(1);
            assertEquals(0, Double.compare(50.0, rect1.getWidth()));
        }

    }
}
