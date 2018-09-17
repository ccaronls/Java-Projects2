package cc.lib.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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

    public void drawVertsNumbered(AGraphics g) {
        g.begin();
        g.pushMatrix();
        g.translate(-5, 0);
        int index = 0;
        for (BVertex v : verts) {
            g.drawString(String.valueOf(index++), v.x, v.y);
        }
        g.popMatrix();
    }

    public void renderCell(AGraphics g, BCell b) {
        g.pushMatrix();
        g.translate(b);
        g.scale(0.9f);
        g.translate(-b.getX(), -b.getY());
        g.begin();
        for (int v : b.adjVerts) {
            g.vertex(verts.get(v));
        }
        g.popMatrix();
    }

    public void drawCells(AGraphics g) {
        for (BCell b : cells) {
            renderCell(g, b);
            g.drawLineLoop();
            // draw center point
            g.begin();
            g.vertex(b);
            g.drawPoints();
        }
    }

    public int pickVertex(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        for (BVertex v : verts) {
            g.setName(index++);
            g.vertex(v);
        }
        return g.pickPoints(mx, my, 5);
    }

    public int addVertex(IVector2D v) {
        int index = verts.size();
        verts.add(newVertex(v));
        return index;
    }

    protected BVertex newVertex(IVector2D v) {
        return new BVertex(v);
    }

    public int addVertex(float x, float y) {
        return addVertex(new Vector2D(x, y));
    }

    public int addEdge(int from, int to) {
        int index = edges.size();
        edges.add(newEdge(from, to));
        return index;
    }

    protected BEdge newEdge(int from, int to) {
        return new BEdge(from, to);
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

        dfsGenCells(0, "", lookup, 0, new LinkedList<Integer>());
        //bfsGenCells();

        // now compute the center of each cell and assign adjCells to edges.
        MutableVector2D mv = new MutableVector2D();
        for (int cIndex=0; cIndex<cells.size(); cIndex++) {
            BCell c = cells.get(cIndex);
            mv.zero();
            if (c.adjVerts.size()<3)
                throw new AssertionError("Invalid cell: " + c.adjVerts);
            int p = c.adjVerts.get(c.adjVerts.size()-1);
            for (int vIndex : c.adjVerts) {
                mv.addEq(verts.get(vIndex));
                int eIndex = getEdgeIndex(vIndex, p);
                if (eIndex < 0)
                    throw new AssertionError("Cannot find edge " + vIndex + "->" + p);
                BEdge e = getEdge(eIndex);
                e.adjacentCells[e.numAdjCells++] = cIndex;
                p = vIndex;
            }
            mv.scaleEq(1.0f / c.adjVerts.size());
            c.cx = mv.getX();
            c.cy = mv.getY();
        }
    }

    static Comparator<Vector2D> angComp = new Comparator<Vector2D>() {
        @Override
        public int compare(Vector2D t0, Vector2D t1) {
            return Math.round(t0.angleBetweenSigned(t1));
        }
    };

    protected BCell newCell(List<Integer> pts) {
        return new BCell(pts);
    }

    private void dfsGenCells(int d, String indent, int [][] lookup, int v,LinkedList<Integer> cell) {
        if (d > 10)
        {
            return;
        }
        System.out.println(indent + "DFS " + v + " cell: " + cell);
        int p = -1;
        if (cell.size() > 0)
            p = cell.getLast();
        if (cell.size() > 2 && v == cell.getFirst()) {
            System.out.println(indent + "ADD CELL " + cell);
            cells.add(newCell(cell));
            int l = cell.getLast();
            for (int ll : cell) {
                lookup[l][ll] = 0;
                l = ll;
            }
            cell.clear();
        } else {
            cell.addLast(v);
            BVertex bv = verts.get(v);
            List<Integer> list = new ArrayList<>();
            for (int i=0; i<bv.numAdjVerts; i++) {
                int vv = bv.adjacentVerts[i];
                if (vv != p && lookup[v][vv] == 1) {
                    list.add(vv);
                }
            }
            if (list.size() == 0)
                return;
            final Integer [] target = list.toArray(new Integer[list.size()]);
            int targetIndex = 0;
            if (p < 0 && target.length>1) {
                System.out.println(indent + "Case 1:");
                Vector2D [] reference = new Vector2D[target.length];
                for (int i=0; i<target.length; i++) {
                    reference[i] = Vector2D.sub(verts.get(target[i]), bv);
                }
                Utils.bubbleSort(reference, target, angComp);
                System.out.println(indent + "Sorted edges: " + Arrays.toString(target));
                System.out.println(indent + "Reference vectors: " + Arrays.toString(reference));
            } else if (p >=0 ){
                System.out.println(indent + "Case 2:");
                // sort the list in decending order of angle of incidence of v->l[i] and prev->v
                Vector2D V0 = Vector2D.sub(bv, verts.get(p));
                assert(!V0.isZero());
                Float [] reference = new Float[list.size()];
                for (int i=0; i<list.size(); i++) {
                    int index = list.get(i);
                    Vector2D V1 = Vector2D.sub(verts.get(index), bv);
                    float ang = V0.angleBetweenSigned(V1);
                    reference[i] = ang;
                }
                Utils.bubbleSort(reference, target, true);
                System.out.println(indent + "Sorted edges: " + Arrays.toString(target));
                System.out.println(indent + "Reference vectors: " + Arrays.toString(reference));

                if (reference.length == 1 || Utils.isBetween(reference[0], 5, 175)) {
                    int index = target[targetIndex++];
                    lookup[v][index] = 0;
                    dfsGenCells(d+1, indent+"  ", lookup, index, cell);
                }
            }
            for ( ; targetIndex < target.length; targetIndex++) {
                LinkedList<Integer> ll = new LinkedList<>();
                ll.addAll(cell);
                if (cell.size() > 1)
                    ll.removeFirst();
                dfsGenCells(d+1, indent+"  ", lookup, target[targetIndex], ll);
            }
        }
        System.out.println(indent + "END");
    }

    public int getEdgeIndex(int from, int to) {
        if (from < 0 || from >= verts.size())
            throw new IndexOutOfBoundsException("From not in range 0-" + verts.size());
        if (to < 0 || to >= verts.size())
            throw new IndexOutOfBoundsException("To not in range 0-" + verts.size());
        return Collections.binarySearch(edges, newEdge(from, to)); // TODO: Do we really want to use factory?
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
