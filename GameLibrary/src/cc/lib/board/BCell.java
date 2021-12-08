package cc.lib.board;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class BCell extends Reflector<BCell> implements IVector2D {

    static {
        addAllFields(BCell.class);
    }

    float cx, cy, radius;

    private final List<Integer> adjVerts = new ArrayList<>();
    private final List<Integer> adjCells = new ArrayList<>();

    public BCell() {}

    protected BCell(List<Integer> verts) {
        adjVerts.addAll(verts);
    }

    @Override
    public final float getX() {
        return cx;
    }

    @Override
    public final float getY() {
        return cy;
    }

    public final float getRadius() {
        return radius;
    }

    public final int getNumAdjVerts() {
        return adjVerts.size();
    }

    public final int getNumAdjCells() { return adjCells.size(); }

    public final int getAdjVertex(int index) {
        return adjVerts.get(index);
    }

    public final Iterable<Integer> getAdjCells() {
        return adjCells;
    }

    public final Iterable<Integer> getAdjVerts() {
        return adjVerts;
    }

    public final void addAdjCell(int cellIdx) {
        if (!adjCells.contains(cellIdx))
            adjCells.add(cellIdx);
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o instanceof BCell) {
            return Vector2D.dot(this, (BCell)o) < CMath.EPSILON;
        }
        return super.equals(o);
    }

    void removeAndRenameAdjVertex(int vtxToRemove, int vtxToRename) {
        adjVerts.remove((Object)vtxToRemove);
        int idx = adjVerts.indexOf((Object)vtxToRename);
        if (idx >= 0) {
            adjVerts.set(idx, vtxToRemove);
        }
    }

    void removeAndRenameAdjCell(int cellToRemove, int cellToRename) {
        adjCells.remove((Object)cellToRemove);
        int idx = adjCells.indexOf((Object)cellToRename);
        if (idx >= 0) {
            adjCells.set(idx, cellToRemove);
        }
    }

}
