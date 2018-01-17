package some;


import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoStack;

import java.io.Serializable;

/**
 * Implements {@link Serializable} because of UndoCommands requirement.
 */
public class TextSampleCommands implements Serializable {

    public static final String TEXT_CTX_KEY = "text_context";
    public static final String SUBJ_ID = "some.TextSample";

    public static class AddString extends UndoCommand {

        private String str;

        public AddString(UndoStack owner, String caption, String str, UndoCommand parent) {
            super(owner, caption, parent);
            this.str = str;
        }

        @Override
        protected void doRedo() {
            TextSample textSample = (TextSample) owner.getLocalContexts().get(TEXT_CTX_KEY);
            textSample.add(str);
            owner.setSubj(textSample.text);
        }

        @Override
        protected void doUndo() {
            TextSample textSample = (TextSample) owner.getLocalContexts().get(TEXT_CTX_KEY);
            textSample.remove(str);
            owner.setSubj(textSample.text);
        }
    }

    public static class AddLine extends UndoCommand {

        public AddLine(UndoStack owner, String caption, UndoCommand parent) {
            super(owner, caption, parent);
        }

        @Override
        protected void doRedo() {
            TextSample textSample = (TextSample) owner.getLocalContexts().get(TEXT_CTX_KEY);
            textSample.addLine();
            owner.setSubj(textSample.text);
        }

        @Override
        protected void doUndo() {
            TextSample textSample = (TextSample) owner.getLocalContexts().get(TEXT_CTX_KEY);
            textSample.removeLine();
            owner.setSubj(textSample.text);
        }
    }


}
