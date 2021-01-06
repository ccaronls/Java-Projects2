package cc.lib.game;

import cc.lib.math.MutableVector2D;

public interface IRectangle extends IDimension, IShape {

    float X();
    float Y();

    /**
     *
     * @return
     */
    default MutableVector2D getCenter() {
        return new MutableVector2D(X()+ getWidth()/2, Y()+ getHeight()/2);
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
        return new MutableVector2D(X()+ getWidth(), Y());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomLeft() {
        return new MutableVector2D(X(), Y()+ getHeight());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomRight() {
        return new MutableVector2D(X()+ getWidth(), Y()+ getHeight());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterLeft() {
        return new MutableVector2D(X(), Y()+ getHeight()/2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterRight() {
        return new MutableVector2D(X()+ getWidth(), Y()+ getHeight()/2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterTop() {
        return new MutableVector2D(X()+ getWidth()/2, Y());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterBottom() {
        return new MutableVector2D(X()+ getWidth()/2, Y()+ getHeight());
    }


    /**
     *
     * @param other
     * @return
     */
    default boolean isIntersectingWidth(IRectangle other) {
        return Utils.isBoxesOverlapping(X(), Y(), getWidth(), getHeight(), other.X(), other.Y(), other.getWidth(), other.getHeight());
    }

    /**
     *
     * @param px
     * @param py
     * @return
     */
    default boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, X(), Y(), getWidth(), getHeight());
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
        g.drawFilledRect(X(), Y(), getWidth(), getHeight());
    }

    /**
     *
     * @param g
     * @param radius
     */
    default void drawRounded(AGraphics g, float radius) {
        g.drawRoundedRect(X(), Y(), getWidth(), getHeight(), radius);
    }

    /**
     *
     * @param g
     * @param thickness
     */
    default void drawOutlined(AGraphics g, int thickness) {
        g.drawRect(X(), Y(), getWidth(), getHeight(), thickness);
    }

    default void drawOutlined(AGraphics g) {
        g.drawRect(X(), Y(), getWidth(), getHeight());
    }

    default GDimension getDimension() {
        return new GDimension(getWidth(), getHeight());
    }

    /**
     * Return half of min(W,H)
     * @return
     */
    default float getRadius() {
        return Math.min(getWidth(), getHeight()) / 2;
    }

}
