package cc.lib.game;

public interface IShape {

    boolean contains(float x, float y);

    default boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }

    void drawOutlined(AGraphics g);

    void drawFilled(AGraphics g);

    IVector2D getCenter();

    float getArea();

    IRectangle enclosingRect();

    float getRadius();
}
