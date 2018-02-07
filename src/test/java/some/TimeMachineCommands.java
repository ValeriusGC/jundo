package some;

import com.gdetotut.jundo.UndoCommand;
import com.gdetotut.jundo.UndoWatcher;

import java.io.Serializable;

/**
 * Don't forget implement {@link Serializable} for commands' superclass. It's mandatory.
 */
public class TimeMachineCommands implements Serializable {

    /**
     * As key for {@link com.gdetotut.jundo.UndoStack} context entry 'Document'.
     */
    public static final String DOC_CTX_KEY = "doc_context";

    /**
     * As ID in {@link com.gdetotut.jundo.UndoPacket}
     */
    public static final String SUBJ_ID = "some.TimeMachineCommands";

    public static abstract class TimeMachineBaseCmd extends UndoCommand {

        final Long time;

        TimeMachineBaseCmd(String caption, Long time) {
            super(caption);
            this.time = time;
        }

        public Long getTime() {
            return time;
        }
    }

    /**
     * Adds new line to document.
     */
    public static class AddNewLineCmd extends TimeMachineBaseCmd {

        public AddNewLineCmd(String caption, Long time) {
            super(caption, time);
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof TextSample) {
                TextSample doc = (TextSample)getOwner().getSubj();
                doc.addLine();
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof TextSample) {
                TextSample doc = (TextSample)getOwner().getSubj();
                doc.removeLine();
            }
        }
    }

    /**
     * Adds string of text to document.
     */
    public static class AddTextCmd extends TimeMachineBaseCmd {

        private String text;

        public AddTextCmd(String caption, Long time, String text) {
            super(caption, time);
            this.text = text;
        }

        @Override
        protected void doRedo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof TextSample) {
                TextSample doc = (TextSample)getOwner().getSubj();
                doc.add(text);
            }
        }

        @Override
        protected void doUndo() {
            Object o = getOwner().getSubj();
            if(o != null && o instanceof TextSample) {
                TextSample doc = (TextSample)getOwner().getSubj();
                doc.remove(text);
            }
        }
    }

}
