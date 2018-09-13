package cc.lib.board;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class BVertex extends Reflector<BVertex> implements IVector2D {

    static {
        addAllFields(BVertex.class);
    }

    float x, y;

    public BVertex() {}

    public BVertex(IVector2D v) {
        x = v.getX();
        y = v.getY();
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    public void set(IVector2D v) {
        x = v.getX();
        y = v.getY();
    }
}
