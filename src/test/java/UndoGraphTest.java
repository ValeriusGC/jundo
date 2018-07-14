
/**
 *                   #0
 *                   |
 *                -- #1---
 *              /         \
 *           - #2 ---      #8
 *         /         \      \
 *        #3         #5     #9
 *       /     \       \
 *    #4       #7      #6
 *
 */

import com.gdetotut.jundo.UndoGraph;
import com.gdetotut.jundo.UndoStack;
import com.gdetotut.jundo.UndoStackImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import some.TextSample;
import some.TextSampleCommands;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * What tests we need
 * <p>
 *     <ul>
 *         <li>we can find path from any point to any another one</li>
 *         <li>we can flatten graph to simple path</li>
 *     </ul>
 *
 *
 */
public class UndoGraphTest {


    @Test
    public void testBfs() {

        /*
         *  MARCH
         *  MARINE
         *  MASK
         *
         *
         *            NULL(-1) --
         *              |        \
         *             M(00)     G(10) --
         *              |                \.....
         *           --A(01)--
         *          /         \
         *        S(02)       R(03)
         *       /          /    \
         *     K(09)      I(04)   C(06)
         *               /         \
         *             N(05)        H(07)
         *            /
         *          E(08)
         *
         *  full path: ->M->A->S->R->I->N->C->H->E->K
         *
         *  history:
         *
         *  hist_idx    txt             cmd_idx
         *  ---         ---             ---
         *  00:         NULL            NA
         *  01:         M               00
         *  02:         MA              01
         *  03:         MAS             02
         *  04:         MA              01*
         *  05:         MAR             03
         *  06:         MARI            04
         *  07:         MARIN           05
         *  08:         MAR             03*
         *  09:         MARC            06
         *  10:         MARCH           07
         *  11:         MARIN           05*
         *  12:         MARINE          08
         *  13:         MAS             02*
         *  14:         MASK            09
         *
         *  branch #0: 0, 1, 2, 3, 10
         *  branch #1: 0, 1, 2, 4, 5, 6, 9
         *  branch #2: 0, 1, 2, 4, 7, 8
         *
         */

        Map<Integer, List<Integer>> graph = new TreeMap<>();

        // init...
        // This is zero-point
        graph.put(0, new ArrayList<>());
        //  step 01
        //  push: 1->'M'
        //  output: M
        //  prev_idx = 0, idx = 1, cnt = 1
        //  edges: 1<->0
        graph.put(1, new ArrayList<>());
        graph.get(0).add(1);
        graph.get(1).add(0);
        //  step 02
        //  push: 2->'A'
        //  output: MA
        //  prev_idx = 1, idx = 2, cnt = 2
        //  edges: 2<->1
        graph.put(2, new ArrayList<>());
        graph.get(2).add(1);
        graph.get(1).add(2);
        //  step 03
        //  push: 3->'S'
        //  output: MAS
        //  prev_idx = 2, idx = 3, cnt = 3
        //  edges: 3<->2
        graph.put(3, new ArrayList<>());
        graph.get(3).add(2);
        graph.get(2).add(3);
        //  step 04
        //  undo:
        //  output: MA
        //  prev_idx = 3, idx = 2, cnt = 3
        //------------
        //  step 05
        //  push: 4->'R'
        //  output: MAR
        //  prev_idx = 2, idx = 4, cnt = 4
        //  edges: 4<->2
        graph.put(4, new ArrayList<>());
        graph.get(4).add(2);
        graph.get(2).add(4);
        //  step 06
        //  push: 5->'I'
        //  output: MARI
        //  prev_idx = 4, idx = 5, cnt = 5
        //  edges: 4<->5
        graph.put(5, new ArrayList<>());
        graph.get(5).add(4);
        graph.get(4).add(5);
        //  step 07
        //  push: 6->'N'
        //  output: MARIN
        //  prev_idx = 5, idx = 6, cnt = 6
        //  edges: 5<->6
        graph.put(6, new ArrayList<>());
        graph.get(6).add(5);
        graph.get(5).add(6);
        //  step 08
        //  'super' undo: idx=6->4
        //  output: MAR
        //  prev_idx = 6, idx = 4, cnt = 6
        //----------------
        //  step 09
        //  push: 7->'C'
        //  output: MARC
        //  prev_idx = 4, idx = 7, cnt = 7
        //  edges: 4<->7
        graph.put(7, new ArrayList<>());
        graph.get(7).add(4);
        graph.get(4).add(7);
        //  step 10
        //  push: 8->'H'
        //  output: MARCH
        //  prev_idx = 7, idx = 8, cnt = 8
        //  edges: 7<->8
        graph.put(8, new ArrayList<>());
        graph.get(8).add(7);
        graph.get(7).add(8);
        //  step 11
        //  'super' undo: idx=8->6
        //  output: MARIN
        //  prev_idx = 8, idx = 6, cnt = 8
        //----------------
        //  step 12
        //  push: 9->'E'
        //  output: MARINE
        //  prev_idx = 6, idx = 9, cnt = 9
        //  edges: 6<->9
        graph.put(9, new ArrayList<>());
        graph.get(9).add(6);
        graph.get(6).add(9);
        //  step 13
        //  'super' undo: idx=9->3
        //  output: MAS
        //  prev_idx = 9, idx = 3, cnt = 9
        //----------------
        //  step 14
        //  push: 10->'K'
        //  output: MASK
        //  prev_idx = 3, idx = 10, cnt = 10
        //  edges: 3<->10
        graph.put(10, new ArrayList<>());
        graph.get(10).add(3);
        graph.get(3).add(10);

        // Restore as
        System.out.println(bfs_path(0, 10, graph));
        Assert.assertEquals(Arrays.asList(0,1,2,3,10), bfs_path(0, 10, graph));

        System.out.println(bfs_path(10, 0, graph));
        Assert.assertEquals(Arrays.asList(10, 3, 2, 1, 0), bfs_path(10, 0, graph));

        System.out.println(bfs_path(3, 4, graph));
        Assert.assertEquals(Arrays.asList(3, 2, 4), bfs_path(3, 4, graph));

        System.out.println(bfs_path(9, 10, graph));
        Assert.assertEquals(Arrays.asList(9, 6, 5, 4, 2, 3, 10), bfs_path(9, 10, graph));

        // Bad indices
        Assert.assertEquals(Collections.emptyList(), bfs_path(100, 100, graph));
        Assert.assertEquals(Collections.emptyList(), bfs_path(-100, 100, graph));
        Assert.assertEquals(Collections.emptyList(), bfs_path(3, 11, graph));
        Assert.assertEquals(Collections.emptyList(), bfs_path(13, 0, graph));



    }

    /**
     * We chould get correct texts.
     */
    @Test
    public void testUndoGraph() throws Exception {

        final String[] texts = {"MARCH", "MARINE", "MASK"};

        // TextSample is not serializable so we use context technique.
        final TextSample subj = new TextSample();
        final UndoGraph graph = new UndoGraph(new ArrayList<String>());
        graph.getLocalContexts().put(TextSampleCommands.TEXT_CTX_KEY, subj);
        //~

        TextSample testText = new TextSample();

        assertEquals(0, graph.count());
        assertEquals(0, graph.getPrevIdx());
        assertEquals(0, graph.getIdx());

        testText.set("M");
        graph.push(new TextSampleCommands.AddString("M", "M"));
        System.out.println("graph.getSteps(): " + graph.getSteps());
        assertEquals(1, graph.count());
        assertEquals(0, graph.getPrevIdx());
        assertEquals(1, graph.getIdx());
        //
//        testText.set("");
//        graph.setIndex(0);
//        assertEquals(testText.text, subj.text);

        testText.set("MAS");
        graph.push(new TextSampleCommands.AddString("A", "A"));
        System.out.println("graph.getSteps(): " + graph.getSteps());
        graph.push(new TextSampleCommands.AddString("S", "S"));
        System.out.println("graph.getSteps(): " + graph.getSteps());
        assertEquals(testText.text, subj.text);
        System.out.println(subj.print());
        assertEquals(3, graph.count());
        assertEquals(2, graph.getPrevIdx());
        assertEquals(3, graph.getIdx());
        //
        testText.set("MA");
        graph.undo();
        assertEquals(testText.text, subj.text);
        System.out.println("subj.text: " + subj.text);
        assertEquals(3, graph.count());
        assertEquals(3, graph.getPrevIdx());
        assertEquals(2, graph.getIdx());
        //
        testText.set("MARIN");
        graph.push(new TextSampleCommands.AddString("R", "R"));
        assertEquals(2, graph.getPrevIdx());
        graph.push(new TextSampleCommands.AddString("I", "I"));
        assertEquals(4, graph.getPrevIdx());
        graph.push(new TextSampleCommands.AddString("N", "N"));
        assertEquals(subj, testText);
        System.out.println(subj.print());
        assertEquals(6, graph.count());
        assertEquals(5, graph.getPrevIdx());
        assertEquals(6, graph.getIdx());
        System.out.println("graph.getSteps(): " + graph.getSteps() + " -> " + subj.text);
        System.out.println("idx: " + graph.getSteps() + " -> " + subj.text);
        //
        testText.set("MAR");
        graph.setIndex(4);
        System.out.println("graph.getSteps(): " + graph.getSteps() + " -> " + subj.text);
        assertEquals(testText.text, subj.text);
        assertEquals(6, graph.count());
        assertEquals(6, graph.getPrevIdx());
        assertEquals(4, graph.getIdx());
        //
        testText.set("MARCH");
        graph.push(new TextSampleCommands.AddString("C", "C"));
        assertEquals(4, graph.getPrevIdx());
        graph.push(new TextSampleCommands.AddString("H", "H"));
        assertEquals(7, graph.getPrevIdx());
        assertEquals(testText.text, subj.text);
        assertEquals(8, graph.count());
        assertEquals(8, graph.getIdx());
        System.out.println("graph.getSteps(): " + graph.getSteps() + " -> " + subj.text);

        //
        testText.set("MARIN");
        graph.setIndex(6);
        assertEquals(testText.text, subj.text);
        assertEquals(8, graph.count());
        assertEquals(8, graph.getPrevIdx());
        assertEquals(6, graph.getIdx());
        //
        testText.set("MARINE");
        graph.push(new TextSampleCommands.AddString("E", "E"));
        assertEquals(6, graph.getPrevIdx());
        assertEquals(subj, testText);
        assertEquals(9, graph.count());
        assertEquals(9, graph.getIdx());
        //
        testText.set("MAS");
        graph.setIndex(3);
        assertEquals(testText, subj);
        assertEquals(9, graph.count());
        assertEquals(9, graph.getPrevIdx());
        assertEquals(3, graph.getIdx());
        //
        testText.set("MASK");
        graph.push(new TextSampleCommands.AddString("K", "K"));
        assertEquals(3, graph.getPrevIdx());
        assertEquals(subj, testText);
        assertEquals(10, graph.count());
        assertEquals(10, graph.getIdx());
        //
        graph.setIndex(0);
        testText.set("");
        assertEquals(testText.text, subj.text);

        System.out.println("graph.getSteps(): " + graph.getSteps() + " -> " + subj.text);
//        graph.redo();
//        System.out.println(subj.text);
//        graph.redo();
//        System.out.println(subj.text);
//        graph.redo();
//        System.out.println(subj.text);

        while (graph.getIdx() < graph.count()){
            graph.redo();
            System.out.println(subj.text);
        }



    }

    private List<Integer> bfs_path(int from, int to, Map<Integer, List<Integer>> graph) {

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
