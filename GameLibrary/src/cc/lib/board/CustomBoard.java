package cc.lib.board;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.reflector.Reflector;

public class CustomBoard<V extends BVertex, E extends BEdge, C extends BCell> extends Reflector<CustomBoard> {

    static Logger log = LoggerFactory.getLogger(CustomBoard.class);

    static {
        addAllFields(CustomBoard.class);
    }

    private final Vector<V> verts = new Vector<>();
    private final Vector<E> edges = new Vector<>();
    private final Vector<C> cells = new Vector<>();
    private GDimension dimension =  new GDimension();

    /**
     *
     * @param g
     */
    public void drawEdges(AGraphics g) {
        g.begin();
        for (BEdge e : edges) {
            g.vertex(verts.get(e.getFrom()));
            g.vertex(verts.get(e.getTo()));
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
        float radius = g.getTextHeight();
        g.translate(radius, radius);
        int index = 0;
        for (BVertex v : verts) {
            IVector2D vv = clampToScreen(g, v, radius);
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledCircle(vv, radius);
            g.setColor(GColor.CYAN);
            g.drawJustifiedString(vv.getX(), vv.getY(), Justify.CENTER, Justify.CENTER, String.valueOf(index++));
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
        float radius = g.getTextHeight();
        for (BEdge e : edges) {
            IVector2D mp = clampToScreen(g, getMidpoint(e), radius);
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledCircle(mp, radius);
            g.setColor(GColor.CYAN);
            g.drawJustifiedString(mp.getX(), mp.getY(), Justify.CENTER, Justify.CENTER, String.valueOf(index++));
        }
        g.popMatrix();
    }

    IVector2D clampToScreen(AGraphics g, IVector2D v, float radius) {
        if (v.getX() >= radius && v.getY() >= radius && v.getX() <= g.getViewportWidth()-radius && v.getY() <= g.getViewportHeight()-radius)
            return v;

        float newX = Utils.clamp(v.getX(), radius, g.getViewportWidth()-radius);
        float newY = Utils.clamp(v.getY(), radius, g.getViewportHeight()-radius);

        return new Vector2D(newX, newY);
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
        int p = b.getAdjVertex(b.getNumAdjVerts()-1);
        for (int v : b.getAdjVerts()) {
            IVector2D v0 = verts.get(p);
            IVector2D v3 = verts.get(v);
            Vector2D  e  = Vector2D.sub(v3, v0);
            Vector2D ue = e.normalized();//.scaledBy(0.2f);
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
        return Vector2D.add(verts.get(e.getFrom()), verts.get(e.getTo())).scaledBy(0.5f);
    }

    public void renderCell(BCell b, AGraphics g) {
        renderCell(b, g, 1);
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
        for (int v : b.getAdjVerts()) {
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
     * @param mouse
     * @return
     */
    public int pickVertex(APGraphics g, Vector2D mouse) {
        g.begin();
        int index = 0;
        for (BVertex v : verts) {
            if (v != null) {
                g.setName(index++);
                g.vertex(v);
            }
        }
        return g.pickPoints(mouse, 5);
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
    protected V newVertex(IVector2D v) {
        return (V)new BVertex(v);
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
    public int getOrAddEdge(int from, int to) {
        E edge = newEdge(from, to);
        int idx = Collections.binarySearch(edges, edge);
        if (idx < 0) {
            idx = (-idx)-1;
            edges.insertElementAt(edge, idx);
        }
        return idx;
    }

    /**
     *
     * @param e
     */
    public void addEdge(E e) {
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

    public int pickEdge(APGraphics g, IVector2D mouse) {
        g.begin();
        int index = 0;
        for (BEdge e : edges) {
            g.setName(index++);
            renderEdge(e, g);
        }
        return g.pickLines(mouse, 5);
    }

    /**
     * Add a cell and return its idex in the array
     *
     * @param verts
     * @return
     */
    public final int addCell(Integer... verts) {
        return addCell(Arrays.asList(verts));
    }

    public final int addCell(List<Integer> verts) {
        int index = cells.size();
        cells.add(newCell(verts));
        computeCell(index);
        return index;
    }

    /**
     * @param g
     * @param mouse
     * @return
     */
    public int pickCell(APGraphics g, IVector2D mouse) {
        g.begin();
        int index = 0;
        for (BCell c : cells) {
            if (isPointInsideCell(mouse, index))
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
        g.vertex(verts.get(e.getFrom()));
        g.vertex(verts.get(e.getTo()));
    }

    public final V getVertex(int idx) {
        return verts.get(idx);
    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    protected E newEdge(int from, int to) {
        return (E)new BEdge(from, to);
    }

    /**
     *
     * @param index
     * @return
     */
    public E getEdge(int index) {
        return edges.get(index);
    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    public E getEdge(int from, int to) {
        int index = Collections.binarySearch(edges, new BEdge(from, to));
        if (index < 0)
            return null;
        return getEdge(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public C getCell(int index) {
        return cells.get(index);
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
            e.reset();
            verts.get(e.getFrom()).addAdjacentVertex(e.getTo());
            verts.get(e.getTo()).addAdjacentVertex(e.getFrom());
        }

        // dfs search to compute the cells
        Set<Integer> unvisited = new LinkedHashSet<>();
        for (int i=0; i<verts.size(); i++) {
            unvisited.add(i);
        }
        cells.clear();
        while (unvisited.size() > 0) {
            int v = unvisited.iterator().next();
            int[][] lookup = new int[verts.size()][verts.size()];
            Queue<int[]> queue = new LinkedList<>();
            dfsCellSearch(queue, 0, "", v, lookup, new LinkedList(), unvisited);
            while (queue.size() > 0) {
                int[] e = queue.remove();
                int v0 = e[0];
                int v1 = e[1];
                LinkedList<Integer> cell = new LinkedList<>();
                lookup[v0][v1] = 1;
                cell.add(v0);
                dfsCellSearch(queue, 0, "", v1, lookup, cell, unvisited);
            }
        }
/*
        // now compute the center of each cell and assign adjCells to edges.
        MutableVector2D mv = new MutableVector2D();
        for (int cIndex=0; cIndex<cells.size(); cIndex++) {
            BCell c = cells.get(cIndex);
            mv.zero();
            if (c.getNumAdjVerts()<3)
                throw new cc.lib.utils.GException("Invalid cell: " + cIndex);
            int p = c.getAdjVertex(c.getNumAdjVerts()-1);
            for (int vIndex : c.getAdjVerts()) {
                mv.addEq(verts.get(vIndex));
                int eIndex = getEdgeIndex(vIndex, p);
                if (eIndex < 0)
                    throw new cc.lib.utils.GException("Cannot find edge " + vIndex + "->" + p);
                BEdge e = getEdge(eIndex);
                e.addAdjCell(cIndex);
                p = vIndex;
                BVertex v = verts.get(vIndex);
                v.addAdjacentCell(cIndex);
            }
            mv.scaleEq(1.0f / c.getNumAdjVerts());
            c.cx = mv.getX();
            c.cy = mv.getY();
            // now compute the radius
            float magSquared = 0;
            for (int vIndex : c.getAdjVerts()) {
                Vector2D dv = Vector2D.sub(verts.get(vIndex), c);
                float m = dv.magSquared();
                if (m > magSquared)
                    magSquared = m;
            }
            c.radius = (float)Math.sqrt(magSquared);
        }*/

        // now iterate over the edges and create the cell adjacency
        Iterator<E> it = edges.iterator();
        while (it.hasNext()) {
            BEdge e = it.next();
            if (e.getNumAdjCells() == 2) {
                BCell c0 = cells.get(e.getAdjCell(0));
                BCell c1 = cells.get(e.getAdjCell(1));
                c0.addAdjCell(e.getAdjCell(1));
                c1.addAdjCell(e.getAdjCell(0));
            } else if (e.getNumAdjCells() == 0) {
                //it.remove();
            }
        }

        log.debug("COMPUTE END\n   numV:%d\n   numE:%d\n   numC:%d", verts.size(), edges.size(), cells.size());
    }

    void computeCell(int cellIdx) {
        BCell cell = getCell(cellIdx);
        if (cell.getNumAdjVerts() == 0) {
            log.warn("Cell has no adjacent vertices: " + cellIdx);
            return;
        }
        MutableVector2D mv = new MutableVector2D();
        cell.radius = 0;
        for (int adjIdx : cell.getAdjVerts()) {
            mv.addEq(getVertex(adjIdx));
        }
        mv.scaleEq(1f / cell.getNumAdjVerts());
        cell.cx = mv.getX();
        cell.cy = mv.getY();
        for (int adjIdx : cell.getAdjVerts()) {
            cell.radius = Math.max(cell.radius, mv.sub(getVertex(adjIdx)).mag());
        }
    }

    /**
     *
     * @param pts
     * @return
     */
    protected C newCell(List<Integer> pts) {
        return (C)new BCell(pts);
    }

    private void dfsCellSearch(Queue<int[]> q, int d, String indent, int v, int [][] visited, LinkedList<Integer> cell, Set<Integer> unvisited) {
        log.debug("%sDFS %d %s", indent, v, cell);
        BVertex bv = verts.get(v);
        unvisited.remove((Object)v);

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
                    dfsCellSearch(q, d + 1, indent + " ", first, visited, cell, unvisited);
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
        // remove adjacency references to vIndex in other verts
        int lastV = verts.size()-1;
        for (int i=0; i<verts.size(); i++) {
            if (i == vIndex)
                continue;
            BVertex v = verts.get(i);
            v.removeAndRenameAdjacentVertex(vIndex, lastV);
        }


        // remove all edges associated with this vertex
        Iterator<E> it = edges.iterator();
        while (it.hasNext()) {
            BEdge e = it.next();
            if (e.getFrom() == vIndex || e.getTo() == vIndex) {
                it.remove();
            } else if (e.getFrom() == lastV) {
                e.setFrom(vIndex);
            } else if (e.getTo() == lastV) {
                e.setTo(vIndex);
            }
        }
        // remove this index from any cells we are adjacent too
        // remove the cell if it becomes invalid
        for (int i=0; i<cells.size(); i++) {
            BCell cell = cells.get(i);
            cell.removeAndRenameAdjVertex(vIndex,lastV);
            if (cell.getNumAdjVerts() < 3) {
                removeCell(i);
            }
        }

        // finally remove the vertex YAY!
        verts.set(vIndex, verts.get(lastV));
        verts.remove(lastV);
    }

    public void removeCell(int idx) {
        // remove occurrances of ourself from vertices, edges and other cells
        int lastC = cells.size()-1;
        for (BVertex v : verts) {
            v.removeAndRenameAdjacentCell(idx, lastC);
        }
        for (BEdge e : edges) {
            e.removeAndReplaceAdjacentCell(idx, lastC);
        }
        for (int adj : cells.get(idx).getAdjCells()) {
            cells.get(adj).removeAndRenameAdjCell(idx, lastC);
        }
        cells.set(idx, cells.get(cells.size()-1));
        cells.remove(cells.size()-1);
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
    public final List<C> getAdjacentCells(BCell cell) {
        return Utils.map(cell.getAdjCells(), idx -> getCell(idx));
    }

    /**
     *
     * @param cellIdx
     * @return
     */
    public final List<C> getAdjacentCell(int cellIdx) {
        return getAdjacentCells(getCell(cellIdx));
    }

    /**
     *
     * @param edge
     * @return
     */
    public final Iterable<C> getAdjacentCells(final BEdge edge) {
        return () -> new Iterator<C>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < edge.getNumAdjCells();
            }

            @Override
            public C next() {
                return cells.get(edge.getAdjCell(index++));
            }
        };
    }

    /**
     *
     * @param vertex
     * @return
     */
    public final Iterable<C> getAdjacentCells(final BVertex vertex) {
        return () -> new Iterator<C>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < vertex.getNumAdjCells();
            }

            @Override
            public C next() {
                return cells.get(vertex.getAdjCell(index++));
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
        for (int vIndex : cell.getAdjVerts()) {
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
            Vector2D side = Vector2D.sub(verts.get(c.getAdjVertex(i)), pv).normEq();
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
            if (c.getNumAdjVerts()<3)
                throw new cc.lib.utils.GException("Invalid cell: " + cIndex);
            int p = c.getAdjVertex(c.getNumAdjVerts()-1);
            for (int vIndex : c.getAdjVerts()) {
                mv.addEq(verts.get(vIndex));
            }
            mv.scaleEq(1.0f / c.getNumAdjVerts());
            c.cx = mv.getX();
            c.cy = mv.getY();
            // now compute the radius
            float magSquared = 0;
            for (int vIndex : c.getAdjVerts()) {
                Vector2D dv = Vector2D.sub(verts.get(vIndex), c);
                float m = dv.magSquared();
                if (m > magSquared)
                    magSquared = m;
            }
            c.radius = (float)Math.sqrt(magSquared);
        }

    }

    public void generateGrid(int rows, int cols, float width, float height) {
        float dx = (width-1)/cols;
        float dy = (height-1)/rows;
        for (int i=0; i<=rows; i++) {
            for (int ii=0; ii<=cols; ii++) {
                int index = addVertex(dx * ii, dy * i);
                if (ii > 0) {
                    getOrAddEdge(index-1, index);
                }
                if (i > 0) {
                    getOrAddEdge(index-cols-1, index);
                }
                if (i > 0 && ii > 0) {
                    addCell(index-cols-2, index-cols-1, index, index-1);
                }
            }
        }
    }

    public Iterable<C> getCells() {
        return cells;
    }

    public GDimension getDimension() {
        return dimension;
    }

    public void setDimension(GDimension dimension) {
        this.dimension = dimension;
    }

    public void setDimension(float width, float height) {
        this.dimension = new GDimension(width, height);
    }

    public void moveVertexBy(int idx, IVector2D dv) {
        BVertex bv = getVertex(idx);
        MutableVector2D v = new MutableVector2D(bv);
        v.addEq(dv);
        bv.set(v);
        for (int cellIdx : bv.getAdjCells()) {
            computeCell(cellIdx);
        }
    }

}
