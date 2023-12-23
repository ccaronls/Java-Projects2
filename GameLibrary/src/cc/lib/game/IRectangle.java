package cc.lib.game;

import org.jetbrains.annotations.NotNull;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

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
     * @param px
     * @param py
     * @return
     */
    default boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, X(), Y(), getWidth(), getHeight());
    }

    /**
     * @param other
     * @return
     */
    default boolean contains(IRectangle other) {
        return Utils.isPointInsideRect(other.getTopLeft().X(), other.getTopLeft().Y(), X(), Y(), getWidth(), getHeight())
                && Utils.isPointInsideRect(other.getBottomRight().X(), other.getBottomRight().Y(), X(), Y(), getWidth(), getHeight());
    }

    /**
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


    /**
     * @return
     */
    default Vector2D getRandomPointInside() {
        return new Vector2D(X() + Utils.randFloat(getWidth()), Y() + Utils.randFloat(getHeight()));
    }

    /**
     * @param s
     * @return
     */
    default GRectangle scaledBy(float s) {
        return scaledBy(s, s);
    }

    default GRectangle scaledBy(float s, Justify horz, Justify vert) {
        float newWidth = getWidth() * s;
        float newHeight = getHeight() * s;
        float newX = X();
        float newY = Y();
        switch (horz) {
            case LEFT:
                newX += (getWidth() - newWidth);
                break;
            case RIGHT:
                break;
            case CENTER:
                newX += (getWidth() - newWidth) / 2;
                break;
        }

        switch (vert) {
            case TOP:
                break;
            case BOTTOM:
                newY += (getHeight() - newHeight);
                break;
            case CENTER:
                newY += (getHeight() - newHeight) / 2;
                break;
        }
        return new GRectangle(newX, newY, newWidth, newHeight);
    }

    /**
     * @param sx
     * @param sy
     * @return
     */
    default GRectangle scaledBy(float sx, float sy) {
        float nw = getWidth() * sx;
        float nh = getHeight() * sy;
        float dw = nw - getWidth();
        float dh = nh - getHeight();
        return new GRectangle(X() - dw / 2, Y() - dh / 2, nw, nh);
    }

    /**
     * Return a rectangle of dimension not to exceed this dimension and
     * whose aspect ratio is that of rectToFit and is centered inside this.
     *
     * @param rectToFit
     * @return
     */
    default GRectangle fit(IDimension rectToFit) {
        return fit(rectToFit, Justify.CENTER, Justify.CENTER);
    }

    /**
     * Return a rectangle that fits inside this rect and with same aspect.
     * How to position inside this determined by horz/vert justifys.
     *
     * @param rectToFit
     * @return
     */
    default GRectangle fit(IDimension rectToFit, Justify horz, Justify vert) {
        float targetAspect = rectToFit.getAspect();
        float rectAspect = getAspect();
        float tx = 0, ty = 0, tw = 0, th = 0;
        if (targetAspect > rectAspect) {
            // target is wider than rect, so fit lengthwise
            tw = getWidth();
            th = getWidth() / targetAspect;
            tx = X();
            switch (vert) {
                case CENTER:
                    ty = Y() + getHeight() / 2 - th / 2;
                    break;
                case TOP:
                    ty = Y();
                    break;
                case BOTTOM:
                    ty = Y() + getHeight() - th;
                    break;
            }
        } else {
            th = getHeight();
            tw = getHeight() * targetAspect;
            ty = Y();
            switch (horz) {
                case CENTER:
                    tx = X() + getWidth() / 2 - tw / 2;
                    break;
                case LEFT:
                    tx = X();
                    break;
                case RIGHT:
                    tx = X() + getWidth() - tw;
                    break;
            }
        }
        return new GRectangle(tx, ty, tw, th);
    }

    default boolean canContain(IRectangle other) {
        return getWidth() >= other.getWidth() && getHeight() >= other.getHeight();
    }

    default Vector2D getDeltaToContain(IRectangle other) {
        if (getWidth() < other.getWidth() || getHeight() < other.getHeight())
            return Vector2D.ZERO;
        Vector2D delta = other.getCenter().sub(getCenter());
        float x = other.getTopLeft().X() < getTopLeft().X() ? getTopLeft().X()
                : other.getBottomRight().X() > getBottomRight().X() ? getBottomRight().X() - other.getWidth()
                : other.getTopLeft().X();

        float y = other.getTopLeft().Y() < getTopLeft().Y() ? getTopLeft().Y()
                : other.getBottomRight().Y() > getBottomRight().Y() ? getBottomRight().Y() - other.getHeight()
                : other.getTopLeft().Y();
        GRectangle contained = new GRectangle(x, y, other.getWidth(), other.getHeight());

        Vector2D delta2 = contained.getCenter().sub(getCenter());
        return delta.sub(delta2);
    }

}
