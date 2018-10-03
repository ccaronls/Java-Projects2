package cc.lib.board;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.CMath;
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

    /**
     *
     * @param g
     */
    public void drawEdges(AGraphics g) {
        g.begin();
        for (BEdge e : edges) {
            g.vertex(verts.get(e.from));
            g.vertex(verts.get(e.to));
        }
        g.drawLines();
    }

    /**
     *
     * @param g
     */
    public void drawVerts(AGraphics g) {
        g.begin();
        for (BVertex v : verts) {
            if (v != null)
                g.vertex(v);
        }
        g.drawPoints();
    }

    /**
     *
     * @param g
     */
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

    /**
     *
     * @param g
     */
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

    /**
     *
     * @param g
     */
    public void drawCellsNumbered(AGraphics g) {
        g.begin();
        g.pushMatrix();
        g.translate(-5, 0);
        int index = 0;
        for (BCell c : cells) {
            g.drawString(String.format("%d", index++), c.cx, c.cy);
        }
        g.popMatrix();
    }

    /**
     *
     * @param b
     * @param g
     */
    public void drawCellArrowed(BCell b, AGraphics g) {
        g.pushMatrix();
        g.translate(b);
        g.scale(0.9f);
        g.translate(-b.getX(), -b.getY());
        int p = b.adjVerts.get(b.adjVerts.size()-1);
        for (int v : b.adjVerts) {
            IVector2D v0 = verts.get(p);
            IVector2D v3 = verts.get(v);
            Vector2D  e  = Vector2D.sub(v3, v0);
            Vector2D ue = e.scaledBy(0.2f);
            IVector2D v1 = Vector2D.sub(v3, ue);
            ue = ue.norm();
            IVector2D v2 = Vector2D.add(v1, ue);
            IVector2D v4 = Vector2D.sub(v1, ue);
            g.begin();
            g.vertexArray(v0, v1);
            g.drawLineStrip();
            g.begin();
            g.vertexArray(v2, v3, v4);
            g.drawTriangles();
            p = v;
        }
        g.popMatrix();
    }

    /**
     *
     * @param e
     * @return
     */
    public final Vector2D getMidpoint(BEdge e) {
        return Vector2D.add(verts.get(e.from), verts.get(e.to)).scaledBy(0.5f);
    }

    /**
     *
     * @param b
     * @param g
     * @param scale
     */
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

    /**
     *
     * @param g
     * @param scale
     */
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

    /**
     *
     * @param g
     * @param mx
     * @param my
     * @return
     */
    public int pickVertex(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        for (BVertex v : verts) {
            if (v != null) {
                g.setName(index++);
                g.vertex(v);
            }
        }
        return g.pickPoints(mx, my, 5);
    }

    /**
     *
     * @param v
     * @return
     */
    public int addVertex(IVector2D v) {
        for (int index=0; index<verts.size(); index++) {
            if (verts.get(index)==null) {
                verts.set(index, newVertex(v));
                return index;
            }
        }
        int index = verts.size();
        verts.add(newVertex(v));
        return index;
    }

    /**
     *
     * @param v
     * @return
     */
    protected BVertex newVertex(IVector2D v) {
        return new BVertex(v);
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    public int addVertex(float x, float y) {
        return addVertex(new Vector2D(x, y));
    }

    /**
     *
     * @param from
     * @param to
     */
    public void addEdge(int from, int to) {
        int index = edges.size();
        edges.add(newEdge(from, to));
    }

    /**
     *
     * @param e
     */
    public void addEdge(BEdge e) {
        edges.add(e);
    }

    /**
     *
     * @param g
     * @param mx
     * @param my
     * @return
     */
    public int pickEdge(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        for (BEdge e : edges) {
            g.setName(index++);
            renderEdge(e, g);
        }
        return g.pickLines(mx, my, 5);
    }

    /**
     *
     * @param g
     * @param mx
     * @param my
     * @return
     */
    public int pickCell(APGraphics g, int mx, int my) {
        g.begin();
        int index = 0;
        Vector2D mv = new Vector2D(mx, my);
        for (BCell c : cells) {
            if (isPointInsideCell(mv, index))
                return index;
            index++;
        }
        return -1;
    }

    /**
     *
     * @param e
     * @param g
     */
    public final void renderEdge(BEdge e, AGraphics g) {
        g.vertex(verts.get(e.from));
        g.vertex(verts.get(e.to));
    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    protected BEdge newEdge(int from, int to) {
        return new BEdge(from, to);
    }

    /**
     *
     * @param index
     * @param <V>
     * @return
     */
    public <V extends BVertex> V getVertex(int index) {
        return (V)verts.get(index);
    }

    /**
     *
     * @param index
     * @param <E>
     * @return
     */
    public <E extends BEdge> E getEdge(int index) {
        return (E)edges.get(index);
    }

    /**
     *
     * @param from
     * @param to
     * @param <E>
     * @return
     */
    public <E extends BEdge> E getEdge(int from, int to) {
        int index = Collections.binarySearch(edges, new BEdge(from, to));
        if (index < 0)
            return null;
        return (E)getEdge(index);
    }

    /**
     *
     * @param index
     * @param <T>
     * @return
     */
    public <T extends BCell> T getCell(int index) {
        return (T)cells.get(index);
    }

    /**
     *
     */
    public void clear() {
        verts.clear();
        edges.clear();
        cells.clear();
    }

    /**
     *
     */
    public void compute() {

        log.debug("COMPUTE BEGIN\n   numV:%d\n   numE:%d\n   numC:%d", verts.size(), edges.size(), cells.size());

        for (BVertex v : verts) {
            v.reset();
        }

        Collections.sort(edges);
        Utils.unique(edges);

        // compute verts adjacent to each other
        for (BEdge e : edges) {
            e.numAdjCells=0;
            verts.get(e.from).addAdjacentVertex(e.to);
            verts.get(e.to).addAdjacentVertex(e.from);
        }

        // dfs search to compute the cells
        cells.clear();
        int [][] lookup = new int[verts.size()][verts.size()];
        Queue<int[]> queue = new LinkedList<>();
        dfsCellSearch(queue, 0, "", 0, lookup, new LinkedList<Integer>());
        while (queue.size() > 0) {
            int [] e = queue.remove();
            int v = e[0];
            int vv = e[1];
            LinkedList<Integer> cell = new LinkedList<>();
            lookup[v][vv] = 1;
            cell.add(v);
            dfsCellSearch(queue, 0, "", vv, lookup, cell);
        }

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
                BVertex v = verts.get(vIndex);
                v.addAdjacentCell(cIndex);
            }
            mv.scaleEq(1.0f / c.adjVerts.size());
            c.cx = mv.getX();
            c.cy = mv.getY();
            // now compute the radius
            float magSquared = 0;
            for (int vIndex : c.adjVerts) {
                Vector2D dv = Vector2D.sub(verts.get(vIndex), c);
                float m = dv.magSquared();
                if (m > magSquared)
                    magSquared = m;
            }
            c.radius = (float)Math.sqrt(magSquared);
        }

        // now iterate over the edges and create the cell adjacencies
        Iterator<BEdge> it = edges.iterator();
        while (it.hasNext()) {
            BEdge e = it.next();
            if (e.numAdjCells == 2) {
                BCell c0 = cells.get(e.adjacentCells[0]);
                BCell c1 = cells.get(e.adjacentCells[1]);
                c0.adjCells.add(e.adjacentCells[1]);
                c1.adjCells.add(e.adjacentCells[0]);
            } else if (e.numAdjCells == 0) {
                //it.remove();
            }
        }

        log.debug("COMPUTE END\n   numV:%d\n   numE:%d\n   numC:%d", verts.size(), edges.size(), cells.size());
    }

    /**
     *
     * @param pts
     * @return
     */
    protected BCell newCell(List<Integer> pts) {
        return new BCell(pts);
    }

    private void dfsCellSearch(Queue<int[]> q, int d, String indent, int v, int [][] visited, LinkedList<Integer> cell) {
        log.debug("%sDFS %d %s", indent, v, cell);
        BVertex bv = verts.get(v);

        if (cell.size() > 2 && cell.contains(v)) {
            while (cell.getFirst() != v) {
                cell.removeFirst();
            }
            if (cell.size()>2) {
                log.debug("%sADD CELL %s", indent, cell);
                cells.add(newCell(cell));
                cell.clear();
                return;
            }
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
                if (Utils.isBetween(reference[0], 0.1f, 179.9f)) {
                    int first = adjacent.remove(0);
                    visited[v][first] = 1;
                    cell.add(v);
                    dfsCellSearch(q, d + 1, indent + " ", first, visited, cell);
                }
            }
        }

        for (int vv : adjacent) {
            //cell = new LinkedList<>();
            //visited[v][vv] = 1;
            //cell.add(v);
            //dfsCellSearch(d+1, indent+" ", vv, visited, cell);
            q.add(new int[] { v, vv });
        }

        log.debug("%sEND", indent);
    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    public final int getEdgeIndex(int from, int to) {
        if (from < 0 || from >= verts.size())
            throw new IndexOutOfBoundsException("From not in range 0-" + verts.size());
        if (to < 0 || to >= verts.size())
            throw new IndexOutOfBoundsException("To not in range 0-" + verts.size());
        return Collections.binarySearch(edges, new BEdge(from, to)); // TODO: Do we really want to use factory?
    }

    /**
     *
     * @return
     */
    public final int getNumVerts() {
        return verts.size();
    }

    /**
     *
     * @return
     */
    public final int getNumEdges() {
        return edges.size();
    }

    /**
     *
     * @return
     */
    public final int getNumCells() {
        return cells.size();
    }

    /**
     *
     * @param vIndex
     */
    public final void removeVertex(int vIndex) {
        // remove all edges associated with this vertex
        Iterator<BEdge> it = edges.iterator();
        while (it.hasNext()) {
            BEdge e = it.next();
            if (e.from == vIndex || e.to == vIndex) {
                it.remove();
            }
        }
        // remove this index from any cells we are adjacent too
        for (BCell c : getAdjacentCells(verts.get(vIndex))) {
            c.adjVerts.remove((Object)vIndex);
        }
        verts.set(vIndex, null);
    }

    /**
     *
     * @param from
     * @param to
     */
    public final void removeEdge(int from, int to) {
        int eIndex = getEdgeIndex(from, to);
        if (eIndex >= 0) {
            edges.remove(eIndex);
        }
    }

    /**
     *
     * @param index
     */
    public final void removeEdge(int index) {
        edges.remove(index);
    }

    /**
     *
     * @param cell
     * @return
     */
    public final List<BCell> getAdjacentCells(BCell cell) {
        BCell [] cells = new BCell[cell.adjCells.size()];
        for (int i=0; i<cells.length; i++) {
            cells[i] = getCell(cell.adjCells.get(i));
        }
        return Arrays.asList(cells);
    }

    /**
     *
     * @param edge
     * @return
     */
    public final Iterable<BCell> getAdjacentCells(final BEdge edge) {
        return new Iterable<BCell>() {
            @Override
            public Iterator<BCell> iterator() {
                return new Iterator<BCell>() {
                    int index = 0;
                    @Override
                    public boolean hasNext() {
                        return index < edge.numAdjCells;
                    }

                    @Override
                    public BCell next() {
                        return cells.get(edge.adjacentCells[index++]);
                    }
                };
            }
        };
    }

    /**
     *
     * @param vertex
     * @return
     */
    public final Iterable<BCell> getAdjacentCells(final BVertex vertex) {
        return new Iterable<BCell>() {
            @Override
            public Iterator<BCell> iterator() {
                return new Iterator<BCell>() {
                    int index = 0;
                    @Override
                    public boolean hasNext() {
                        return index < vertex.numAdjCells;
                    }

                    @Override
                    public BCell next() {
                        return cells.get(vertex.adjacentCells[index++]);
                    }
                };
            }
        };
    }

    /**
     *
     * @param cellIndex
     * @return
     */
    public final GRectangle getCellBoundingRect(int cellIndex) {
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

    /**
     * Return true if vertex resides within the convex bounding polygon formed by the points
     * @param vertex
     * @param cIndex
     * @return
     */
    public final boolean isPointInsideCell(IVector2D vertex, int cIndex) {
        BCell c = cells.get(cIndex);
        if (c == null)
            return false;

        // early out check against the radius
        if (Vector2D.sub(vertex, c).magSquared() > c.radius*c.radius)
            return false;

        int p = c.getNumAdjVerts()-1;
        int sign = 0;
        for (int i=0; i<c.getNumAdjVerts(); i++) {
            IVector2D pv = verts.get(c.getAdjVertex(p));
            Vector2D side = Vector2D.sub(verts.get(c.adjVerts.get(i)), pv).normEq();
            Vector2D dv = Vector2D.sub(vertex, pv);
            int s = CMath.signOf(side.dot(dv));
            if (sign == 0) {
                sign = s;
            } else if (sign != s) {
                return false;
            }
            p = i;
        }
        return true;
    }

    public final GRectangle getBounds() {
        MutableVector2D min = new MutableVector2D(Vector2D.MAX);
        MutableVector2D max = new MutableVector2D(Vector2D.MIN);

        for (BVertex v : verts) {
            if (v != null) {
                min.minEq(v);
                max.maxEq(max);
            }
        }

        return new GRectangle(min, max);
    }

    /**
     * Normailze the vertices so that board fits in rect (0,0)-(1,1) while keeping aspect ratio
     */
    public void normalize() {
        MutableVector2D min = new MutableVector2D(Vector2D.MAX);
        MutableVector2D max = new MutableVector2D(Vector2D.MIN);

        for (BVertex v : verts) {
            if (v == null)
                continue;
            min.minEq(v);
            max.maxEq(v);
        }

        MutableVector2D dim = max.sub(min);
        if (dim.isNaN() || dim.getX() == 0 || dim.getY() == 0 || dim.isInfinite())
            return;

        float scale = 1.0f / Math.max(dim.getX(), dim.getY());
        for (BVertex v : verts) {
            if (v == null)
                continue;

            MutableVector2D mv = new MutableVector2D(v);
            mv.subEq(min);
            mv.scaleEq(scale);

            v.set(mv);
        }

        // now recompuet all the cell centers
        MutableVector2D mv = new MutableVector2D();
        for (int cIndex=0; cIndex<cells.size(); cIndex++) {
            BCell c = cells.get(cIndex);
            mv.zero();
            if (c.adjVerts.size()<3)
                throw new AssertionError("Invalid cell: " + c.adjVerts);
            int p = c.adjVerts.get(c.adjVerts.size()-1);
            for (int vIndex : c.adjVerts) {
                mv.addEq(verts.get(vIndex));
            }
            mv.scaleEq(1.0f / c.adjVerts.size());
            c.cx = mv.getX();
            c.cy = mv.getY();
            // now compute the radius
            float magSquared = 0;
            for (int vIndex : c.adjVerts) {
                Vector2D dv = Vector2D.sub(verts.get(vIndex), c);
                float m = dv.magSquared();
                if (m > magSquared)
                    magSquared = m;
            }
            c.radius = (float)Math.sqrt(magSquared);
        }

    }
}
