package cc.lib.game;

import cc.lib.math.MutableVector2D;

public interface IRectangle {

    float X();
    float Y();
    float W();
    float H();

    /**
     *
     * @return
     */
    default MutableVector2D getCenter() {
        return new MutableVector2D(X()+W()/2, Y()+H()/2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getTopLeft() {
        return new MutableVector2D(X(), Y());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getTopRight() {
        return new MutableVector2D(X()+W(), Y());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomLeft() {
        return new MutableVector2D(X(), Y()+H());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomRight() {
        return new MutableVector2D(X()+W(), Y()+H());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterLeft() {
        return new MutableVector2D(X(), Y()+H()/2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterRight() {
        return new MutableVector2D(X()+W(), Y()+H()/2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterTop() {
        return new MutableVector2D(X()+W()/2, Y());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterBottom() {
        return new MutableVector2D(X()+W()/2, Y()+H());
    }


    /**
     *
     * @param other
     * @return
     */
    default boolean isIntersectingWidth(IRectangle other) {
        return Utils.isBoxesOverlapping(X(), Y(), W(), H(), other.X(), other.Y(), other.W(), other.H());
    }

    /**
     *
     * @param px
     * @param py
     * @return
     */
    default boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, X(), Y(), W(), H());
    }

    /**
     *
     * @param v
     * @return
     */
    default boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }

    /**
     *
     * @param g
     */
    default void drawFilled(AGraphics g) {
        g.drawFilledRect(X(), Y(), W(), H());
    }

    /**
     *
     * @param g
     * @param radius
     */
    default void drawRounded(AGraphics g, float radius) {
        g.drawFilledRoundedRect(X(), Y(), W(), H(), radius);
    }

    /**
     *
     * @param g
     * @param thickness
     */
    default void drawOutlined(AGraphics g, int thickness) {
        g.drawRect(X(), Y(), W(), H(), thickness);
    }

    default GDimension getDimension() {
        return new GDimension(W(), H());
    }

    default float getAspect() {
        return W()/H();
    }

    /**
     * Return half of min(W,H)
     * @return
     */
    default float getRadius() {
        return Math.min(W(), H()) / 2;
    }

}
