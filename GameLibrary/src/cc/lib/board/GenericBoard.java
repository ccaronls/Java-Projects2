package cc.lib.board;

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
        if (from < 0)
            throw new AssertionError("From Index < 0");
        if (to < 0)
            throw new AssertionError("From Index < 0");
        if (from >= to)
            throw new AssertionError("From > to");

        int index = edges.size();
        edges.add(new BEdge(Math.min(from, to), Math.max(from, to)));
        return index;
    }

    public BVertex getVertex(int index) {
        return verts.get(index);
    }
}
