package cc.lib.game;

import cc.lib.math.MutableVector2D;

public interface IShape {

    boolean contains(float x, float y);

    void drawOutlined(AGraphics g);

    void drawFilled(AGraphics g);

    MutableVector2D getCenter();

    float getArea();
}
