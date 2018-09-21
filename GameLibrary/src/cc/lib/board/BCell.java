package cc.lib.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class BCell extends Reflector<BCell> implements IVector2D {

    static {
        addAllFields(BCell.class);
    }

    float cx, cy;

    final List<Integer> adjVerts = new ArrayList<>();
    final List<Integer> adjCells = new ArrayList<>();

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

    public final int getNumAdjVerts() {
        return adjVerts.size();
    }

    public final int getAdjVertex(int index) {
        return adjVerts.get(index);
    }

    public final List<Integer> getAdjCells() {
        return Collections.unmodifiableList(adjCells);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o != null && o instanceof BCell) {
            return Vector2D.dot(this, (BCell)o) < CMath.EPSILON;
        }
        return super.equals(o);
    }
}
