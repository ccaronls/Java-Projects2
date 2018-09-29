package cc.lib.board;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class BVertex extends Reflector<BVertex> implements IVector2D {

    static {
        addAllFields(BVertex.class);
    }

    float x, y;
    final int [] adjacentVerts = new int[8];
    int numAdjVerts;

    final int [] adjacentCells = new int[8];
    int numAdjCells;

    public BVertex() {}

    BVertex(IVector2D v) {
        x = v.getX();
        y = v.getY();
    }

    @Override
    public final float getX() {
        return x;
    }

    @Override
    public final float getY() {
        return y;
    }

    public final void set(IVector2D v) {
        x = v.getX();
        y = v.getY();
    }

    void addAdjacentVertex(int v) {
        adjacentVerts[numAdjVerts++] = v;
    }

    void addAdjacentCell(int c) { adjacentCells[numAdjCells++] = c; }

    public final List<Integer> getAdjVerts() {
        List<Integer> adj = new ArrayList<>();
        for (int i=0; i<numAdjVerts; i++) {
            adj.add(adjacentVerts[i]);
        }
        return adj;
    }

    public final List<Integer> getAdjCells() {
        List<Integer> adj = new ArrayList<>();
        for (int i=0; i<numAdjCells; i++) {
            adj.add(adjacentCells[i]);
        }
        return adj;
    }

    final void reset() {
        numAdjVerts=numAdjCells=0;
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o instanceof BVertex) {
            return Vector2D.dot(this, (BVertex)o) < CMath.EPSILON;
        }
        return false;
    }
}
