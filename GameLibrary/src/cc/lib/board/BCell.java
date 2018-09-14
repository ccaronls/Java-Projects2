package cc.lib.board;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class BCell extends Reflector<BCell> implements IVector2D {

    static {
        addAllFields(BCell.class);
    }

    float cx, cy;

    int [] adjacentVerts = new int[16];
    int numAdjVerts;

    public BCell() {}

    @Override
    public float getX() {
        return cx;
    }

    @Override
    public float getY() {
        return cy;
    }
}
