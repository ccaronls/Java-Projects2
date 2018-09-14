package cc.lib.board;

import java.util.Collections;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class GenericBoard extends Reflector<GenericBoard> {

    static {
        addAllFields(GenericBoard.class);
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

    public int addEdge(int from, int to) {
        int index = edges.size();
        edges.add(new BEdge(from, to));
        return index;
    }

    public BVertex getVertex(int index) {
        return verts.get(index);
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
        int [] flag = new int[edges.size()];

    }

    private void dfsFindCells(int [] edgeflags, int v) {
        BVertex bv = verts.get(v);
        for (int i=0; i<bv.numAdjVerts; i++) {
            int eIndex = getEdgeIndex(v, bv.adjacentVerts[i]);
            if (edgeflags[eIndex] != 0)
                continue;
        }
    }

    public int getEdgeIndex(int from, int to) {
        if (from < 0 || from >= verts.size())
            throw new IndexOutOfBoundsException("From not in range 0-" + verts.size());
        if (to < 0 || to >= verts.size())
            throw new IndexOutOfBoundsException("To not in range 0-" + verts.size());
        return Collections.binarySearch(edges, new BEdge(from, to));
    }
}
