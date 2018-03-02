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
}
