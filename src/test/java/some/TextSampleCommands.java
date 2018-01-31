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

        public AddString(String caption, String str) {
            super(caption);
            this.str = str;
        }

        @Override
        protected void doRedo() {
            TextSample textSample = (TextSample) getOwner().getLocalContexts().get(TEXT_CTX_KEY);
            textSample.add(str);
            getOwner().setSubj(textSample.text);
        }

        @Override
        protected void doUndo() {
            TextSample textSample = (TextSample) getOwner().getLocalContexts().get(TEXT_CTX_KEY);
            textSample.remove(str);
            getOwner().setSubj(textSample.text);
        }
    }

    public static class AddLine extends UndoCommand {

        public AddLine(String caption) {
            super(caption);
        }

        @Override
        protected void doRedo() {
            TextSample textSample = (TextSample) getOwner().getLocalContexts().get(TEXT_CTX_KEY);
            textSample.addLine();
            getOwner().setSubj(textSample.text);
        }

        @Override
        protected void doUndo() {
            TextSample textSample = (TextSample) getOwner().getLocalContexts().get(TEXT_CTX_KEY);
            textSample.removeLine();
            getOwner().setSubj(textSample.text);
        }
    }


}
