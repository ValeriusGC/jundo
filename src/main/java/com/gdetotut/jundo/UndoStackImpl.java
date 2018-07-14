package com.gdetotut.jundo;

import java.io.*;
import java.util.*;

/**
 * Stack of entire {@link UndoCommand} chain for subject.
 * <b>Main characteristic of {@link UndoStackImpl} is that two different stacks should not share one subject.</b>
 * <p>Otherwise {@link UndoGroup} may not add them both, and there will be collision when undoing one subject
 * via different stacks.
 */
public class UndoStackImpl extends UndoStack {

    /**
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If group is not a null the stack is automatically added to the group.
     *
     * @param subj  for whom this stack was made. Can be null if no way to make it serializable. Required.
     * @param group possible group for this {@link UndoStackImpl}.
     */
    public UndoStackImpl(Object subj, UndoGroup group) {
        super(subj, group);
    }

    /**
     * Constructs an empty undo stack (secondary CTR) without {@link #group}
     *
     * @param subj the object for whom this stack was made. Can be null if no way to prepare it serializable. Required.
     * @see #UndoStackImpl(Object)
     */
    public UndoStackImpl(Object subj) {
        this(subj, null);
    }


}
