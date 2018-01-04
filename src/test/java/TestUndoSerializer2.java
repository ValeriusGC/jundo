import com.gdetotut.jundo.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import some.SimpleUndoWatcher;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestUndoSerializer2 implements Serializable{

    @Test
    public void serialize() throws Exception {

        class Util {
            private String circleToString(@NotNull Circle circle) {
                ArrayList<String> param = new ArrayList<>();
                param.add(String.valueOf(circle.getRadius()));
                param.add(String.valueOf(circle.getFill()));
                return String.join(",", param);
            }

            private Object stringToCircle(@NotNull String str) {
                List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
                Circle c = new Circle(Double.valueOf(items.get(0)), Color.valueOf(items.get(1)));
                return c;
            }

        }

        class CircleRadiusUndoCmd extends UndoCommand2 {

            Double oldV;
            Double newV;

            public CircleRadiusUndoCmd(@NotNull UndoStack2 owner, @NotNull Circle circle, Double newV, UndoCommand2 parent) {
                super(owner, "", parent);
                oldV = circle.getRadius();
                this.newV = newV;
                if(!owner.contexts.containsKey("circle")) {
                    owner.contexts.put("circle", circle);
                }
            }

            @Override
            protected void doRedo() {
//                Circle c = (Circle)owner.contexts.get("circle");
                Circle c = (Circle)owner.getSubj();
                c.setRadius(newV);
            }

            @Override
            protected void doUndo() {
//                Circle c = (Circle)owner.contexts.get("circle");
                Circle c = (Circle)owner.getSubj();
                c.setRadius(oldV);
            }
        }

        Circle circle = new Circle(20.0, Color.RED);
        UndoStack2 stack = new UndoStack2(circle, null);
        stack.contexts.put("circle", circle);
        stack.setWatcher(new SimpleUndoWatcher());
        for(int i = 0; i < 1000; ++i){
            stack.push(new CircleRadiusUndoCmd(stack, circle, i*2.0, null));
        }
        assertEquals(1000, stack.count());
        assertEquals(0, Double.compare(1998.0, circle.getRadius()));

        while (stack.canUndo())
            stack.undo();
        assertEquals(1000, stack.count());
        assertEquals(0, Double.compare(20.0, circle.getRadius()));

        UndoSerializer2 managerBack = null;
        // Make unzipped serialization
        UndoSerializer2 manager = new UndoSerializer2(null,2, stack);
        String data = UndoSerializer2.serialize(manager, false, subj -> new Util().circleToString((Circle)subj));
        managerBack = UndoSerializer2.deserialize(data, subjAsString -> new Util().stringToCircle(subjAsString));
        assertEquals(manager.info, managerBack.info);
        assertEquals(Circle.class, managerBack.getStack().getSubj().getClass());
        Circle circle1 = (Circle) managerBack.getStack().getSubj();
        assertEquals(0, Double.compare(circle1.getRadius(), circle.getRadius()));
        assertEquals(circle.getFill(), circle1.getFill());

        UndoStack2 stack2 = managerBack.getStack();
        stack2.contexts.put("circle", circle1);
        assertEquals(1000, stack2.count());
        assertEquals(0, Double.compare(20.0, circle1.getRadius()));

        while (stack2.canRedo())
            stack2.redo();
        assertEquals(0, Double.compare(1998.0, circle1.getRadius()));
    }


    @Test
    public void testComplexSubj() throws Exception {

        // Non serializable
        class Canvas {
            final static int CT_Circle = 1;
            final static int CT_Rect = 2;
            List<Shape> shapes = new ArrayList<>();
        }

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

            private String toStr(@NotNull Canvas canvas) {
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

            private Object toSubj(@NotNull String str) {
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

        class CanvasCmd implements Serializable{
            class Add extends UndoCommand2 {
                int type;
                public Add(@NotNull UndoStack2 owner, int type, UndoCommand2 parent) {
                    super(owner, "add", parent);
                    this.type = type;
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

            class Remove extends UndoCommand2 {
                int type;
                public Remove(@NotNull UndoStack2 owner, UndoCommand2 parent) {
                    super(owner, "remove", parent);
                    List<Shape> shapes = ((Canvas)owner.getSubj()).shapes;
                    this.type = shapes.get(shapes.size()-1) instanceof Circle ? Canvas.CT_Circle : Canvas.CT_Rect;
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

        }

        /////////////////////////////

        Canvas canvas = new Canvas();
        UndoStack2 stack = new UndoStack2(canvas, null);
        stack.push(new CanvasCmd().new Add(stack, Canvas.CT_Circle, null));
        stack.push(new CanvasCmd().new Add(stack, Canvas.CT_Rect, null));
        assertEquals(2, canvas.shapes.size());
        assertEquals(Circle.class, canvas.shapes.get(0).getClass());
        assertEquals(Rectangle.class, canvas.shapes.get(1).getClass());
        stack.undo();
        stack.undo();
        assertEquals(0, canvas.shapes.size());
        stack.setIndex(100);
        assertEquals(2, stack.getIdx());
        assertEquals(2, canvas.shapes.size());

        // Serialization
        UndoSerializer2 manager = new UndoSerializer2(null,2, stack);
        String data = UndoSerializer2.serialize(manager, false, subj -> new Factory().toStr((Canvas)subj));
        UndoSerializer2 manager1 = UndoSerializer2.deserialize(data, subjAsString -> new Factory().toSubj(subjAsString));
        assertEquals(manager.info, manager1.info);

        assertEquals(Canvas.class, manager1.getStack().getSubj().getClass());
        Canvas canvas1 = (Canvas) manager1.getStack().getSubj();
        assertEquals(2, canvas1.shapes.size());

        UndoStack2 stack1 = manager1.getStack();
        stack1.undo();
        stack1.undo();
        assertEquals(0, canvas1.shapes.size());
        stack1.setIndex(100);
        assertEquals(2, stack1.getIdx());
        assertEquals(2, canvas1.shapes.size());

    }

}
