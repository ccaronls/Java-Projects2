package cc.lib.game;

import org.jetbrains.annotations.NotNull;

import cc.lib.math.MutableVector2D;

public interface IRectangle extends IDimension, IShape, Comparable<IRectangle> {

    float X();

    float Y();

    /**
     * @return
     */
    default MutableVector2D getCenter() {
        return new MutableVector2D(X() + getWidth() / 2, Y() + getHeight() / 2);
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
        g.begin();
        g.vertex(getTopLeft());
        g.vertex(getBottomLeft());
        g.vertex(getTopRight());
        g.vertex(getBottomRight());
        g.drawQuadStrip();
//        g.drawFilledRect(X(), Y(), getWidth(), getHeight());
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
        double w = getWidth();
        double h = getHeight();
        return (float) Math.sqrt(w * w + h * h) / 2;
    }

    @Override
    default float getArea() {
        return getWidth() * getHeight();
    }

    @Override
    default int compareTo(@NotNull IRectangle o) {
        int c = Float.compare(getArea(), o.getArea());
        if (c != 0)
            return c;
        if (X() != o.X())
            return Float.compare(X(), o.X());
        return Float.compare(Y(), o.Y());
    }
}
