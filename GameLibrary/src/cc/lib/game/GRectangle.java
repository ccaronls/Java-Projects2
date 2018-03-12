package cc.lib.game;

import cc.lib.math.MutableVector2D;
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

    public GRectangle(float x, float y, GDimension dim) {
        this(x, y, dim.width, dim.height);
    }

    public GRectangle(IVector2D v0, IVector2D v1) {
        set(v0, v1);
    }

    public float x, y, w, h;

    public void set(IVector2D v0, IVector2D v1) {
        x = Math.min(v0.getX(), v1.getX());
        y = Math.min(v0.getY(), v1.getY());
        w = Math.abs(v0.getX()-v1.getX());
        h = Math.abs(v0.getY()-v1.getY());
    }

    public final Vector2D getCenter() {
        return new Vector2D(x+w/2, y+h/2);
    }

    public final boolean isIntersectingWidth(GRectangle other) {
        return Utils.isBoxesOverlapping(x, y, w, h, other.x, other.y, other.w, other.h);
    }

    public final boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, x, y, w, h);
    }

    public final boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }

    public final GRectangle getInterpolationTo(GRectangle r, float position) {
        if (position < 0.01f)
            return this;
        if (position > 0.99f)
            return r;
        MutableVector2D v0 = new MutableVector2D(x, y);
        MutableVector2D v1 = new MutableVector2D(x+w, y+h);
        MutableVector2D r0 = new MutableVector2D(r.x, r.y);
        MutableVector2D r1 = new MutableVector2D(r.x+r.w, r.y+r.h);
        v0.addEq(r0.subEq(v0).scaleEq(position));
        v1.addEq(r1.subEq(v1).scaleEq(position));
        return new GRectangle(v0, v1);
    }
}
