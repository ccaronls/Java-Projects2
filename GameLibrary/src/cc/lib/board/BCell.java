package cc.lib.board;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class BCell extends Reflector<BCell> implements IVector2D {

    static {
        addAllFields(BCell.class);
    }

    float cx, cy;

    final List<Integer> adjVerts = new ArrayList<>();
    public BCell() {}

    BCell(List<Integer> verts) {
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
}
