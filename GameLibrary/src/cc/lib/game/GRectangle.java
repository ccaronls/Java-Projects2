package cc.lib.game;

import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public final class GRectangle extends Reflector<GRectangle> {

    static {
        addAllFields(GRectangle.class);
    }

    public GRectangle() {}

    public GRectangle(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float x, y, w, h;

    public Vector2D getCenter() {
        return new Vector2D(x+w/2, y+h/2);
    }

    public boolean isIntersectingWidth(GRectangle other) {
        return Utils.isBoxesOverlapping(x, y, w, h, other.x, other.y, other.w, other.h);
    }

    public boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, x, y, w, h);
    }

    public boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }
}
