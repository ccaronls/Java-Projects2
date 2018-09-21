package cc.lib.board;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class CustomBoard extends Reflector<CustomBoard> {

    static Logger log = LoggerFactory.getLogger(CustomBoard.class);

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

    public void drawEdgesNumbered(AGraphics g) {
        g.begin();
        g.pushMatrix();
        g.translate(-5, 0);
        int index = 0;
        for (BEdge e : edges) {
            Vector2D mp = getMidpoint(e);
            g.drawString(String.valueOf(index++), mp.X(), mp.Y());
        }
        g.popMatrix();
    }

    public void drawCellsNumbered(AGraphics g) {
        g.begin();
        g.pushMatrix();
        g.translate(-5, 0);
        int index = 0;
        for (BCell c : cells) {
            g.drawString(String.valueOf(index++), c.cx, c.cy);
        }
        g.popMatrix();
    }

    public final Vector2D getMidpoint(BEdge e) {
        return Vector2D.add(verts.get(e.from), verts.get(e.to)).scaledBy(0.5f);
    }

    public void renderCell(BCell b, AGraphics g, float scale) {
        g.pushMatrix();
        g.translate(b);
        g.scale(scale);
        g.translate(-b.getX(), -b.getY());
        g.begin();
        for (int v : b.adjVerts) {
            g.vertex(verts.get(v));
        }
        g.popMatrix();
    }

    public void drawCells(AGraphics g, float scale) {
        for (BCell b : cells) {
            renderCell(b, g, scale);
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

    public void addEdge(int from, int to) {
        int index = edges.size();
        edges.add(newEdge(from, to));
    }

    public void addEdge(BEdge e) {
        edges.add(e);
    }

    public int pickEdge(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        for (BEdge e : edges) {
            g.setName(index++);
            renderEdge(e, g);
        }
        return g.pickLines(mx, my, 5);
    }

    public int pickCell(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        for (BCell c : cells) {
            g.setName(index++);
            renderCell(c, g, 1);
        }
        return g.pickClosest(mx, my);
    }

    public final void renderEdge(BEdge e, AGraphics g) {
        g.vertex(verts.get(e.from));
        g.vertex(verts.get(e.to));
    }

    protected BEdge newEdge(int from, int to) {
        return new BEdge(from, to);
    }

    public <V extends BVertex> V getVertex(int index) {
        return (V)verts.get(index);
    }

    public <E extends BEdge> E getEdge(int index) {
        return (E)edges.get(index);
    }

    public <E extends BEdge> E getEdge(int from, int to) {
        int index = Collections.binarySearch(edges, new BEdge(from, to));
        if (index < 0)
            return null;
        return (E)getEdge(index);
    }

    public <T extends BCell> T getCell(int index) {
        return (T)cells.get(index);
    }

    public void clear() {
        verts.clear();
        edges.clear();
        cells.clear();
    }

    public void compute() {

        log.debug("COMPUTE BEGIN\n   numV:%d\n   numE:%d\n   numC:%d", verts.size(), edges.size(), cells.size());

        for (BVertex v : verts) {
            v.reset();
        }

        Collections.sort(edges);
        Utils.unique(edges);

        for (BEdge e : edges) {
            e.numAdjCells=0;
            verts.get(e.from).addAdjacentVertex(e.to);
            verts.get(e.to).addAdjacentVertex(e.from);
        }

        cells.clear();
        // dfs search the edges

        int [][] lookup = new int[verts.size()][verts.size()];

        dfsCellSearch(0, "", 0, lookup, new LinkedList<Integer>());

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

        // now iterate over the edges and create the cell adjacencies
        for (BEdge e : edges) {
            if (e.numAdjCells == 2) {
                BCell c0 = cells.get(e.adjacentCells[0]);
                BCell c1 = cells.get(e.adjacentCells[1]);
                c0.adjCells.add(e.adjacentCells[1]);
                c1.adjCells.add(e.adjacentCells[0]);
            }
        }

        log.debug("COMPUTE END\n   numV:%d\n   numE:%d\n   numC:%d", verts.size(), edges.size(), cells.size());
    }

    protected BCell newCell(List<Integer> pts) {
        return new BCell(pts);
    }

    private void dfsCellSearch(int d, String indent, int v, int [][] visited, LinkedList<Integer> cell) {
        if (d > 256) {
            return;
        }
        log.debug("%sDFS %d %s", indent, v, cell);
        BVertex bv = verts.get(v);

        if (cell.size() > 2 && v == cell.getFirst()) {
            log.debug("%sADD CELL %s", indent, cell);
            cells.add(newCell(cell));
            cell.clear();
        }

        List<Integer> adjacent = bv.getAdjVerts();
        Iterator<Integer> it = adjacent.iterator();
        while (it.hasNext()) {
            int vv = it.next();
            if (visited[v][vv] != 0) {
                it.remove();
            }
        }
        log.debug("%sADJ=%s", indent, adjacent);
        if (adjacent.size() == 0)
            return;

        if (cell.size() > 0) {
            int prev = cell.getLast();
            boolean removed = adjacent.remove((Object)prev);
            if (adjacent.size() > 0) {
                Vector2D dv = Vector2D.sub(bv, verts.get(prev));
                log.debug("%sdv=%s", indent, dv);
                Float[] reference = new Float[adjacent.size()];
                Integer[] target = new Integer[adjacent.size()];
                for (int i = 0; i < adjacent.size(); i++) {
                    Vector2D v2 = Vector2D.sub(verts.get(adjacent.get(i)), bv);
                    reference[i] = dv.angleBetweenSigned(v2);
                    target[i] = adjacent.get(i);
                }
                Utils.bubbleSort(reference, target, true);
                log.debug("%sSorted edges: %s", indent, Arrays.toString(target));
                log.debug("%sReference vectors: %s", indent, Arrays.toString(reference));

                adjacent.clear();
                adjacent.addAll(Arrays.asList(target));
                if (Utils.isBetween(reference[0], 5, 175)) {
                    int first = adjacent.remove(0);
                    visited[v][first] = 1;
                    cell.add(v);
                    dfsCellSearch(d + 1, indent + "  ", first, visited, cell);
                }
            }
        }

        for (int vv : adjacent) {
            cell = new LinkedList<>();
            visited[v][vv] = 1;
            cell.add(v);
            dfsCellSearch(d+1, indent+"  ", vv, visited, cell);
        }

        log.debug("%sEND", indent);
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

    public final void removeVertex(int vIndex) {
        verts.remove(vIndex);
    }

    public final void removeEdge(int from, int to) {
        int eIndex = getEdgeIndex(from, to);
        if (eIndex >= 0) {
            edges.remove(eIndex);
        }
    }

    public final void removeEdge(int index) {
        edges.remove(index);
    }

    public final List<BCell> getAdjacentCells(BCell cell) {
        BCell [] cells = new BCell[cell.adjCells.size()];
        for (int i=0; i<cells.length; i++) {
            cells[i] = getCell(cell.adjCells.get(i));
        }
        return Arrays.asList(cells);
    }

    public final GRectangle getCellBoundingRect(int cellIndex) {
        GRectangle rect = new GRectangle();
        BCell cell = getCell(cellIndex);
        MutableVector2D min = new MutableVector2D(cell);
        MutableVector2D max = new MutableVector2D(cell);
        for (int vIndex : cell.adjVerts) {
            BVertex v = getVertex(vIndex);
            min.minEq(v);
            max.maxEq(v);
        }
        return new GRectangle(min, max);
    }
}
