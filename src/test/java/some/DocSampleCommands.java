package some;


import com.gdetotut.jundo.UndoCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Команды для {@link DocSample}
 */
public class DocSampleCommands implements Serializable{

    public static final String DOC_CTX_KEY = "doc_context";
    public static final String FUNC_SQ_CTX_KEY = "func_sq_context";

    public static class FuncUndoCommand extends UndoCommand {

        public static final String PATTERN = "%s of %d = %d";

        String funcName;
        String text;
        int param;

        public FuncUndoCommand(String caption, String funcName, int param) {
            super(caption);
            this.funcName = funcName;
            this.param = param;
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                Function<Integer, Integer> fun = (Function<Integer, Integer>)getOwner().getLocalContexts().get(FUNC_SQ_CTX_KEY);
                int res = fun.apply(param);
                text = String.format(PATTERN, funcName, param, res);
                doc.add(text);
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                doc.remove(text);
            }
        }
    }

    public static class BiFuncUndoCommand extends UndoCommand {

        public static final String PATTERN = "%s of %d + %d = %d";

        String funcName;
        String text;
        int param1;
        int param2;

        public BiFuncUndoCommand(String caption, String funcName, int param1, int param2) {
            super(caption);
            this.funcName = funcName;
            this.param1 = param1;
            this.param2 = param2;
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                BiFunction<Integer, Integer, Integer> fun
                        = (BiFunction<Integer, Integer, Integer>)getOwner()
                        .getLocalContexts().get(FUNC_SQ_CTX_KEY);
                int res = fun.apply(param1, param2);
                text = String.format(PATTERN, funcName, param1, param2, res);
                doc.add(text);
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                doc.remove(text);
            }
        }
    }


    public static class AddLine extends UndoCommand {

        public AddLine(String caption) {
            super(caption);
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                doc.addLine();
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof DocSample) {
                DocSample doc = (DocSample)getOwner().getSubj();
                doc.removeLine();
            }
        }
    }


}
