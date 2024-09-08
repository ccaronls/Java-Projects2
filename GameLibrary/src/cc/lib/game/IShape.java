package cc.lib.game;

import cc.lib.math.MutableVector2D;

public interface IShape {

    boolean contains(float x, float y);

    default boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }

    void drawOutlined(AGraphics g);

    void drawFilled(AGraphics g);

    MutableVector2D getCenter();

    float getArea();

    IRectangle enclosingRect();

    float getRadius();
}
