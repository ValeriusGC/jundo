
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

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
         *
         *  MARCH
         *  MARINE
         *  MASK
         *
         *  00: M
         *  01: MA
         *  02: MAS
         *  01: MA
         *  03: MAR
         *  04: MARI
         *  05: MARIN
         *  03: MAR
         *  06: MARC
         *  07: MARCH
         *  05: MARIN
         *  08: MARINE
         *  02: MAS
         *  09: MASK
         *
         *
         *                                M(0)
         *                                 |
         *                              --A(1)--
         *                             /        \
         *                           S(2)      R(3)
         *                          /         /   \
         *                        K(9)      I(4)  C(6)
         *                                 /        \
         *                               N(5)      H(7)
         *                              /
         *                            E(8)
         *
         *  full path: M.A.S.R.I.N.C.H.E.K
         *
         *  history:
         *  01: M           00
         *  02: MA          01
         *  03: MAS         02
         *  04: MA          01*
         *  05: MAR         03
         *  06: MARI        04
         *  07: MARIN       05
         *  08: MAR         03*
         *  09: MARC        06
         *  10: MARCH       07
         *  11: MARIN       05*
         *  12: MARINE      08
         *  13: MAS         02*
         *  14: MASK        09
         *
         *  branch #0: 0, 1, 2, 9
         *  branch #1: 0, 1, 3, 4, 5, 8
         *  branch #2: 0, 1, 3, 6, 7
         *
         *
         */

        Map<Integer, List<Integer>> graph = new TreeMap<>();

        //  step 01
        //  push: 0->'M'
        //  output: M
        //  parent_idx = -1, idx = 0, cnt = 1
        graph.put(0, new ArrayList<>());
        graph.get(0).add(1);
        //  step 02
        //  push: 1->'A'
        //  output: MA
        //  parent_idx = 0, idx = 1, cnt = 2
        //  edges: 1-0
        graph.put(1, new ArrayList<>());
        graph.get(1).add(0);
        //  step 03
        //  push: 2->'S'
        //  output: MAS
        //  parent_idx = 1, idx = 2, cnt = 3
        //  edges: 2-1
        graph.put(2, new ArrayList<>());
        graph.get(2).add(1);
        //  step 04
        //  undo:
        //  output: MA
        //  parent_idx = 2, idx = 1, cnt = 3
        //------------
        //  step 05
        //  push: 3->'R'
        //  output: MAR
        //  parent_idx = 1, idx = 3, cnt = 4
        //  edges: 3-1
        graph.put(3, new ArrayList<>());
        graph.get(3).add(1);
        //  step 06
        //  push: 4->'R'
        //  output: MARI
        //  parent_idx = 3, idx = 4, cnt = 5
        //  edges: 4-3
        graph.put(4, new ArrayList<>());
        graph.get(4).add(3);
        //  step 07
        //  push: 5->'N'
        //  output: MARIN
        //  parent_idx = 4, idx = 5, cnt = 6
        //  edges: 5-4
        graph.put(5, new ArrayList<>());
        graph.get(5).add(4);

        graph.get(1).add(2);
        graph.get(1).add(3);

        graph.get(2).add(9);

        graph.get(3).add(4);
        graph.get(3).add(6);

        graph.get(4).add(5);

        graph.get(5).add(8);

        graph.put(6, new ArrayList<>());
        graph.get(6).add(3);
        graph.get(6).add(7);
        graph.put(7, new ArrayList<>());
        graph.get(7).add(6);
        graph.put(8, new ArrayList<>());
        graph.get(8).add(5);
        graph.put(9, new ArrayList<>());
        graph.get(9).add(2);

        // Restore as
        System.out.println(bfs_path(0, 9, graph));
        Assert.assertEquals(Arrays.asList(0,1,2,9), bfs_path(0, 9, graph));

        System.out.println(bfs_path(9, 0, graph));
        Assert.assertEquals(Arrays.asList(9, 2, 1, 0), bfs_path(9, 0, graph));

        System.out.println(bfs_path(2, 3, graph));
        Assert.assertEquals(Arrays.asList(2, 1, 3), bfs_path(2, 3, graph));

        System.out.println(bfs_path(8, 9, graph));
        Assert.assertEquals(Arrays.asList(8, 5, 4, 3, 1, 2, 9), bfs_path(8, 9, graph));


    }

    private List<Integer> bfs_path(int from, int to, Map<Integer, List<Integer>> graph) {
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
