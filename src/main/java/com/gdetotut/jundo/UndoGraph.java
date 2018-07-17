package com.gdetotut.jundo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    //Map<Integer, Set<Integer>> graph = new TreeMap<>();
    List<TreeSet<Integer>> graph = new ArrayList<>();

    List<TreeSet<Integer>> branches = new ArrayList<>();

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
        currCmdIdx = 0;
        graph.add(new TreeSet<>());
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
                    watcher.indexChanged(idx, idx);
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
                    int newCmdIdx = commands.size();
                    if(graph.size() <= newCmdIdx) {
                        graph.add(new TreeSet<>());
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

    /**
     * Adding step to history.
     */
    void addStep() {
        //
        System.out.println("currCmdIdx: " + currCmdIdx);
        List<Integer> path = bfs_path(0, idx, graph);
        path.removeIf(integer -> integer == 0);
        TreeSet<Integer> curBranch = new TreeSet<>();
        for (Set<Integer> b: branches) {
            System.out.println("currCmdIdx: " + currCmdIdx + ", path.toArray: " + path + ", b.toArray: " + b);

            if(Arrays.equals(b.toArray(), path.toArray())){
                curBranch.addAll(b);
                branches.remove(b);
                break;
            }
        }
        if(curBranch.size() == 0) {
            curBranch.addAll(path);
        }
        curBranch.add(currCmdIdx);
        branches.add(0, curBranch);
        System.out.println("path: " + path + ", branches: " + branches);
    }

    @Override
    protected void checkUndoLimit() {

        if (undoLimit <= 0
                || (commands == null)
                || undoLimit >= commands.size()
                || (null != macroCmd)) {
            return;
        }

        int delCnt = commands.size() - undoLimit;

        for (int i = 0; i < delCnt; ++i) {
            if(graph.size() > 1) {

                System.out.println("graph_00: " + graph);

                // Relink. First elem always is `parent`, next ones - `children`.
                // Link parent to children and vice versa.
                TreeSet<Integer> links = graph.get(1);
                Integer parLink = links.first();
                for (Integer l: links.tailSet(1)) {
                    graph.get(parLink).add(l);
                    graph.get(l).add(parLink);
                }

                System.out.println("graph_01: " + graph);

                // Remove
                graph.remove(1);

                System.out.println("graph_02: " + graph);

                // decrem
                for(int j = 0; j < graph.size(); ++j) {
                    TreeSet<Integer> ls = graph.get(j);
                    TreeSet<Integer> tmp = new TreeSet<>(ls);
                    ls.clear();
                    tmp.forEach(integer -> ls.add(integer > 0 ? --integer : integer));
                    final int ix = j;
                    ls.removeIf(integer -> integer < 0 || integer == ix);
                }
                // Remove autolink from
                graph.get(0).removeIf(integer -> integer == 0);
                graph.removeIf(TreeSet::isEmpty);

                System.out.println("graph_03: " + graph);

                // remove command
                commands.remove(0);

                // re-branch
                List<TreeSet<Integer>> tmpb = new ArrayList<>();
                for (TreeSet<Integer> b : branches) {
                    Set<Integer> tmp = new TreeSet<>(b.tailSet(2));
                    b.clear();
                    tmp.forEach(integer -> b.add(--integer));
                    b.removeIf(integer -> integer < 1);
                    if(b.size() > 0) {
                        tmpb.add(b);
                    }
                }
                branches.clear();
                branches.addAll(tmpb);

                if(--prevIdx < 0) {
                    prevIdx = 0;
                }
                if(--currCmdIdx < 0) {
                    currCmdIdx = 0;
                }
            }

        }

        idx -= delCnt;
        if (cleanIdx != -1) {
            if (cleanIdx < delCnt) {
                cleanIdx = -1;
            } else {
                cleanIdx -= delCnt;
            }
        }

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
     * @param to index to achieve.
     */
    @Override
    public void setIndex(int to) {

        if (null != macroCmd) {
            System.err.println("UndoStack.setIndex(): cannot set index in the middle of a macro");
            return;
        }

        if (commands == null) {
            return;
        }

        // Index in history
        if (to < 0) {
            to = 0;
        } else if (idx > commands.size()) {
            to = commands.size();
        }

        int from = this.idx;

//        System.out.println(String.format(
//                "this.idx=%d, historyIdx=%d, from=%d, to=%d, steps=%s",
//                this.idx, to, from, to, steps));

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
                    commands.get(curr - 1).undo();
                }else if(next > curr) {
                    commands.get(next - 1).redo();
                }
                curr = next;
            }
            currCmdIdx = curr;
            //addStep();
        } finally {
            suspend = false;
        }

        setIndex(to, false);
    }

    @Override
    public boolean canUndo() {
        return super.canUndo();
    }

    /**
     *
     * @param to
     * @param clean flag to set/unset clean state.
     */
    @Override
    public void setIndex(int to, boolean clean) {
        final boolean wasClean = to == cleanIdx;

        if (this.idx != to) {
            prevIdx = this.idx;
            this.idx = to;
            if (null != watcher) {
                watcher.indexChanged(prevIdx, to);
                watcher.canUndoChanged(canUndo());
                watcher.undoTextChanged(undoCaption());
                watcher.canRedoChanged(canRedo());
                watcher.redoTextChanged(redoCaption());
            }
        }

        if (clean) {
            cleanIdx = to;
        }

        final boolean isClean = to == cleanIdx;
        if (isClean != wasClean && null != watcher) {
            watcher.cleanChanged(isClean);
        }

    }

    public void flatten() {
        List<Integer> path = bfs_path(0, currCmdIdx, graph);
        path.removeIf(integer -> integer == 0);
        if(path.size() < 2) {
            return;
        }

        List<UndoCommand> tmpCmd = new ArrayList<>();
        for (int ix : path) {
            tmpCmd.add(commands.get(ix - 1));
        }
        commands.clear();
        commands.addAll(tmpCmd);

        List<TreeSet<Integer>> tmpGraph = new ArrayList<>();
        tmpGraph.add(new TreeSet<>());
        for(int ix = 0; ix < path.size(); ++ix) {
            tmpGraph.add(new TreeSet<>());
        }
        for(int ix = 0; ix < tmpGraph.size() - 1; ++ix) {
            tmpGraph.get(ix).add(ix + 1);
            tmpGraph.get(ix + 1).add(ix);
        }
        graph.clear();
        graph.addAll(tmpGraph);
        this.idx = commands.size();
        currCmdIdx = idx;
        prevIdx = idx;
    }

    public int getPrevIdx() {
        return prevIdx;
    }

    private List<Integer> bfs_path(int from, int to, List<TreeSet<Integer>> graph) {

        if(from < 0 || from >= graph.size() || to < 0 || to >= graph.size()) {
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
