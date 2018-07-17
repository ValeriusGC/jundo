package com.gdetotut.jundo;

import java.util.*;
import java.util.function.Consumer;

/**
 *
 * {@link UndoGraph} is a more powered and flexible version of {@link UndoStack}.
 *
 * While {@link UndoStack} has only one command vector at a time {@link UndoGraph} can has many ones.
 *
 * Practically it means that client can have several variants simultaneously and switch between them.
 *
 * It became possible thanks to new command layout as a graph against old plain 'stack' version.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
public class UndoGraph extends UndoStack {

    /**
     * Index of previous command.
     */
    private int prevIdx;

    private List<TreeSet<Integer>> graph = new ArrayList<>();

    private List<TreeSet<Integer>> branches = new ArrayList<>();

    /**
     * Constructs an empty undo stack. The stack will initially be in the clean state.
     * If group is not a null the stack is automatically added to the group.
     *
     * @param subj  for whom this stack was made. Can be null if no way to make it serializable. Required.
     * @param group possible group for this {@link UndoGraph}.
     */
    public UndoGraph(Object subj, UndoGroup group) {
        super(subj, group);
        // Adds 'zero' parent vertex.
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

        if (cmd == null) {
            throw new NullPointerException("null cmd at UndoGraph.push");
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

            UndoCommand cur = idx > 0 ? commands.get(idx - 1) : null;

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
                    addCmd(cmd, idx);
                    checkUndoLimit();
                    updateBranches();
                    setIndex(commands.size(), false);
                }
            }
        }
        return this;
    }

    /**
     * Updates terminal command vectors.
     */
    private void updateBranches() {
        //
//        System.out.println("currCmdIdx: " + currCmdIdx);
        List<Integer> path = bfs(0, idx, graph);
        path.removeIf(integer -> integer == 0);
        TreeSet<Integer> curBranch = new TreeSet<>();
        for (Set<Integer> b: branches) {
//            System.out.println("currCmdIdx: " + currCmdIdx + ", path.toArray: " + path + ", b.toArray: " + b);

            if(Arrays.equals(b.toArray(), path.toArray())){
                curBranch.addAll(b);
                branches.remove(b);
                break;
            }
        }
        if(curBranch.size() == 0) {
            curBranch.addAll(path);
        }
        curBranch.add(idx);
        branches.add(0, curBranch);
//        System.out.println("path: " + path + ", branches: " + branches);
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

                // Relink. First elem always is `parent`, next ones - `children`.
                // Link parent to children and vice versa.
                TreeSet<Integer> links = graph.get(1);
                Integer parLink = links.first();
                for (Integer l: links.tailSet(1)) {
                    graph.get(parLink).add(l);
                    graph.get(l).add(parLink);
                }

                // Remove
                graph.remove(1);

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
                if(--idx < 0) {
                    idx = 0;
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

        List<Integer> path = bfs(from, to, graph);

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
        } finally {
            suspend = false;
        }

        setIndex(to, false);
    }

    /**
     *
     * @param to
     * @param clean flag to set/unset clean state.
     */
    @Override
    protected void setIndex(int to, boolean clean) {
        final boolean wasClean = to == cleanIdx;

        if (idx != to) {
            prevIdx = idx;
            idx = to;
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
        List<Integer> path = bfs(0, idx, graph);
        path.removeIf(integer -> integer == 0);
        if(path.size() < 2) {
            return;
        }

        List<UndoCommand> tmpCmd = new ArrayList<>();
        for (int ix : path) {
            tmpCmd.add(commands.get(ix - 1));
        }
        commands.clear();

        graph = graph.subList(0, 1);
        graph.get(0).clear();
        final int[] tmpIdx = {0};

        tmpCmd.forEach(cmd -> addCmd(cmd, tmpIdx[0]++));

        setIndex(commands.size(), false);
    }

    private void addCmd(UndoCommand cmd, int parentIdx) {
        commands.add(cmd);

        // 2. to graph
        int newCmdIdx = commands.size();

        if(graph.size() <= newCmdIdx) {
            graph.add(new TreeSet<>());
        }

        graph.get(parentIdx).add(newCmdIdx);
        graph.get(newCmdIdx).add(parentIdx);
    }

    public int getPrevIdx() {
        return prevIdx;
    }

    private List<Integer> bfs(int from, int to, List<TreeSet<Integer>> graph) {

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
