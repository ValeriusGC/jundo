package com.gdetotut.jundo;

import java.util.*;

/**
 * Stack of entire {@link UndoCommand} chain for subject.
 * <b>Main characteristic of {@link UndoGraph} is that two different stacks should not share one subject.</b>
 * <p>Otherwise {@link UndoGroup} may not add them both, and there will be collision when undoing one subject
 * via different stacks.
 */
public class UndoGraph extends UndoStack {

    /**
     * Index of previous command.
     */
    int prevIdx;

    int currCmdIdx;

    Map<Integer, Set<Integer>> graph = new TreeMap<>();

    /**
     * Map history step to command index in commands
     */
    List<Integer> steps = new ArrayList<>();

    /**
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If group is not a null the stack is automatically added to the group.
     *
     * @param subj  for whom this stack was made. Can be null if no way to make it serializable. Required.
     * @param group possible group for this {@link UndoGraph}.
     */
    public UndoGraph(Object subj, UndoGroup group) {
        super(subj, group);
        // At this point we has no history
        prevIdx = 0;
        idx = 0;
        // No command at all, no current command, no index
        currCmdIdx = -1;
        graph.put(currCmdIdx, new HashSet<>());
        steps.add(-1);
    }

    /**
     * Constructs an empty undo stack (secondary CTR) without {@link #group}
     *
     * @param subj the object for whom this stack was made. Can be null if no way to prepare it serializable. Required.
     * @see #UndoGraph(Object)
     */
    public UndoGraph(Object subj) {
        this(subj, null);
    }

    /**
     * The same as base but not deletes command from stack.
     *
     * @param cmd new command to execute. Required.
     * @return
     * @throws Exception
     */
    @Override
    public UndoStack push(UndoCommand cmd) throws Exception {
        //return super.push(cmd);

        if (cmd == null) {
            throw new NullPointerException("cmd");
        } else if (!suspend) {

            cmd.setOwner(this);
            if (cmd.children != null) {
                for (UndoCommand child : cmd.children) {
                    child.setOwner(this);
                }
            }

            UndoCommand copy = clone(cmd);

            cmd.redo();

            boolean onMacro = null != macroCmd;

            if (commands == null) {
                commands = new ArrayList<>();
            }

            UndoCommand cur = currCmdIdx > 0 ? commands.get(currCmdIdx - 1) : null;

//
//            while (idx < commands.size()) {
//                commands.remove(commands.size() - 1);
//            }

            if (cleanIdx > idx) {
                cleanIdx = -1;
            }

            boolean canMerge = cur != null
                    && cur.id() != -1
                    && cur.id() == cmd.id()
                    && onMacro || idx != cleanIdx;

            if (canMerge && cur != null && cur.mergeWith(cmd)) {
                if (!onMacro && null != watcher) {
                    watcher.indexChanged(idx);
                    watcher.canUndoChanged(canUndo());
                    watcher.undoTextChanged(undoCaption());
                    watcher.canRedoChanged(canRedo());
                    watcher.redoTextChanged(redoCaption());
                }
            } else {
                if (onMacro) {
                    macroCmd.addChild(copy);

                    if (cur != null) // cur can not be null but do this to get rid of warning
                        cur.addChild(cmd);

                } else {

                    // And last actions
                    // 1. add cmd
                    commands.add(cmd);
                    // 2. to graph
                    int newCmdIdx = commands.size() - 1;
                    if(!graph.containsKey(newCmdIdx)) {
                        graph.put(newCmdIdx, new HashSet<>());
                    }
                    graph.get(currCmdIdx).add(newCmdIdx);
                    graph.get(newCmdIdx).add(currCmdIdx);
                    currCmdIdx = newCmdIdx;

                    checkUndoLimit();

                    addStep();
                    setIndex(commands.size(), false);
                }
            }
        }
        return this;
    }

    @Override
    public int count() {
        return steps.size() - 1;
    }

    /**
     * Adding step to history.
     */
    void addStep() {
        steps.add(currCmdIdx);
    }

    @Override
    public void undo() {
        setIndex(idx - 1);
    }

    @Override
    public void redo() {
        setIndex(idx + 1);
    }

    /**
     * If zero - all commands undo!
     * @param idx index to achieve.
     */
    @Override
    public void setIndex(int idx) {

        if (null != macroCmd) {
            System.err.println("UndoStack.setIndex(): cannot set index in the middle of a macro");
            return;
        }

        if (commands == null) {
            return;
        }

        // Index in history
        int historyIdx = idx;
        if (historyIdx < 0) {
            historyIdx = 0;
        } else if (idx > steps.size()) {
            historyIdx = steps.size() - 1;
        }

        int from = steps.get(this.idx);
        int to = steps.get(historyIdx);
        List<Integer> path = bfs_path(from, to, graph);

        if(path.size() < 2) {
            return;
        }

        try {
            suspend = true;
            int curr = from;
            for(int i=0; i<path.size() - 1; ++i) {
                int next = path.get(i + 1);
                if(next < curr) {
                    commands.get(curr).undo();
                }else if(next > curr) {
                    commands.get(next).redo();
                }
                curr = next;
            }
            currCmdIdx = curr;
            //addStep();
        } finally {
            suspend = false;
        }

        setIndex(idx, false);
    }

    /**
     *
     * @param idx
     * @param clean flag to set/unset clean state.
     */
    @Override
    public void setIndex(int idx, boolean clean) {
        prevIdx = this.idx;
        this.idx = idx;
//        prevIdx = this.idx;
//        this.idx = idx;
//        super.setIndex(idx, clean);
//        System.out.println("setIndex: " + prevIdx + ", " + this.idx + ", " + graph + ", " + commands.get(this.idx-1));
    }


    public int getPrevIdx() {
        return prevIdx;
    }

    public List<Integer> getSteps() {
        return steps;
    }

    private List<Integer> bfs_path(int from, int to, Map<Integer, Set<Integer>> graph) {

        if(!graph.containsKey(from) || !graph.containsKey(to)) {
            return new ArrayList<>();
        }

        List<Integer> path = new ArrayList<>();

        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.push(from);

        Map<Integer, Integer> d = new TreeMap<>();
        d.put(from, 0);

        Map<Integer, Boolean> m = new TreeMap<>();
        m.put(from, true);

        Map<Integer, Integer> prior = new TreeMap<>();

        while (!q.isEmpty()) {
            int v = q.pop();

            for (int i :
                    graph.get(v)) {
                if (m.get(i) == null || !m.get(i)){
                    prior.put(i, v);
                    d.put(i, d.get(v) + 1);
                    m.put(i, true);
                    q.push(i);
                }
            }

        }

        if(from != to) {
            path.add(to);
            while(prior.get(to) != from){
                to = prior.get(to);
                path.add(to);
            }
            path.add(from);
            Collections.reverse(path);
        }

        return path;
    }


}
