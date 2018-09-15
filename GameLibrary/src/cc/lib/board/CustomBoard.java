package cc.lib.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class CustomBoard extends Reflector<CustomBoard> {

    static {
        addAllFields(CustomBoard.class);
    }

    private final Vector<BVertex> verts = new Vector<>();
    private final Vector<BEdge> edges = new Vector<>();
    private final Vector<BCell> cells = new Vector<>();

    public void drawEdges(AGraphics g) {
        g.begin();
        for (BEdge e : edges) {
            g.vertex(verts.get(e.from));
            g.vertex(verts.get(e.to));
        }
        g.drawLines();
    }

    public void drawVerts(AGraphics g) {
        g.begin();
        for (BVertex v : verts) {
            g.vertex(v);
        }
        g.drawPoints();
    }

    public void drawCells(AGraphics g) {
        for (BCell b : cells) {
            g.pushMatrix();
            g.translate(b);
            g.scale(0.9f);
            g.translate(-b.getX(), -b.getY());
            g.begin();
            for (int v : b.adjVerts) {
                g.vertex(verts.get(v));
            }
            g.drawLineLoop();
            g.popMatrix();
            // draw center point
            g.begin();
            g.vertex(b);
            g.drawPoints();
        }
    }

    public int pickVertex(APGraphics g, int mx, int my) {
        g.begin();
        for (BVertex v : verts) {
            g.vertex(v);
        }
        return g.pickPoints(mx, my, 5);
    }

    public int addVertex(IVector2D v) {
        int index = verts.size();
        verts.add(new BVertex(v));
        return index;
    }

    public int addVertex(float x, float y) {
        return addVertex(new Vector2D(x, y));
    }

    public int addEdge(int from, int to) {
        int index = edges.size();
        edges.add(new BEdge(from, to));
        return index;
    }

    public BVertex getVertex(int index) {
        return verts.get(index);
    }

    public BEdge getEdge(int index) {
        return edges.get(index);
    }

    public BCell getCell(int index) {
        return cells.get(index);
    }

    public void clear() {
        verts.clear();
        edges.clear();
        cells.clear();
    }

    public void compute() {

        for (BVertex v : verts) {
            v.numAdjCells=0;
            v.numAdjVerts=0;
        }

        for (BEdge e : edges) {
            e.numAdjCells=0;
            verts.get(e.from).addAdjacentVertex(e.to);
            verts.get(e.to).addAdjacentVertex(e.from);
        }

        Collections.sort(edges);
        cells.clear();
        // dfs search the edges

        int [][] lookup = new int[verts.size()][verts.size()];
        for (BEdge e : edges) {
            lookup[e.from][e.to] = lookup[e.to][e.from] = 1;
            e.numAdjCells = 0;
        }

        Queue<int[]> Q = new LinkedList<int[]>();
        Q.add(new int[] { 0, -1 });
        while (!Q.isEmpty()) {
            int [] q = Q.remove();
            dfsGenCells(Q, lookup, q[0], q[1], new LinkedList<Integer>());
        }
        //bfsGenCells();

        // now compute the center of each cell and assign adjCells to edges.
        MutableVector2D mv = new MutableVector2D();
        for (int cIndex=0; cIndex<cells.size(); cIndex++) {
            BCell c = cells.get(cIndex);
            mv.zero();
            if (c.adjVerts.size()<3)
                throw new AssertionError("Invalid cell");
            int p = c.adjVerts.get(c.adjVerts.size()-1);
            for (int i : c.adjVerts) {
                mv.addEq(verts.get(i));
                int eIndex = getEdgeIndex(i, p);
                BEdge e = getEdge(eIndex);
                e.adjacentCells[e.numAdjCells++] = cIndex;
            }
            mv.scaleEq(1.0f / c.adjVerts.size());
            c.cx = mv.getX();
            c.cy = mv.getY();
        }
    }

    private void dfsGenCells(Queue<int []> strays, int [][] lookup, int v, int p, LinkedList<Integer> cell) {
        System.out.println("DFS " + v + " cell: " + cell);
        if (cell.size() > 2 && v == cell.getFirst()) {
            System.out.println("ADD CELL " + cell);
            cells.add(new BCell(cell));
            int l = cell.getLast();
            for (int ll : cell) {
                lookup[l][ll] = 0;
                l = ll;
            }
            cell.clear();
        } else {
            BVertex bv = verts.get(v);
            List<Integer> list = new ArrayList<>();
            for (int i=0; i<bv.numAdjVerts; i++) {
                int vv = bv.adjacentVerts[i];
                if (vv != p && lookup[v][vv] == 1) {
                    list.add(vv);
                }
            }
            Integer [] target = list.toArray(new Integer[list.size()]);
            if (p >= 0 && target.length > 1) {
                // sort the list in decending order of angle of incidence of v->l[i] and prev->v
                Vector2D V0 = Vector2D.sub(bv, verts.get(p));
                assert(!V0.isZero());
                Float [] reference = new Float[list.size()];
                for (int i=0; i<reference.length; i++) {
                    Vector2D V1 = Vector2D.sub(verts.get(list.get(i)), bv);
                    reference[i] = V0.angleBetweenSigned(V1);
                    assert(!reference[i].isNaN());
                }
                Utils.bubbleSort(reference, target, false);
                System.out.println("Sorted edges: " + Arrays.toString(target));
                System.out.println("Reference vectors: " + Arrays.toString(reference));
            }
            cell.add(v);
            for (int i=1; i<target.length; i++) {
                strays.add(new int[] { target[i], v });
            }
            if (target.length>0) {
                dfsGenCells(strays, lookup, target[0], v, cell);
            }
        }
    }

    public int getEdgeIndex(int from, int to) {
        if (from < 0 || from >= verts.size())
            throw new IndexOutOfBoundsException("From not in range 0-" + verts.size());
        if (to < 0 || to >= verts.size())
            throw new IndexOutOfBoundsException("To not in range 0-" + verts.size());
        return Collections.binarySearch(edges, new BEdge(from, to));
    }

    public final int getNumVerts() {
        return verts.size();
    }

    public final int getNumEdges() {
        return edges.size();
    }

    public final int getNumCells() {
        return cells.size();
    }

}
