package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public interface IRectangle extends IDimension, IShape {

    float getLeft();

    float getTop();

    default float getRight() {
        return getLeft() + getWidth();
    }

    default float getBottom() {
        return getTop() + getHeight();
    }

    default boolean isNan() {
        return getWidth() == Float.NaN || getHeight() == Float.NaN || getTop() == Float.NaN || getLeft() == Float.NaN;
    }

    /**
     * @return
     */
    default IVector2D getCenter() {
        return new MutableVector2D(getLeft() + getWidth() / 2, getTop() + getHeight() / 2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getTopLeft() {
        return new MutableVector2D(getLeft(), getTop());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getTopRight() {
        return new MutableVector2D(getLeft() + getWidth(), getTop());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomLeft() {
        return new MutableVector2D(getLeft(), getTop() + getHeight());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getBottomRight() {
        return new MutableVector2D(getLeft() + getWidth(), getTop() + getHeight());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterLeft() {
        return new MutableVector2D(getLeft(), getTop() + getHeight() / 2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterRight() {
        return new MutableVector2D(getLeft() + getWidth(), getTop() + getHeight() / 2);
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterTop() {
        return new MutableVector2D(getLeft() + getWidth() / 2, getTop());
    }

    /**
     *
     * @return
     */
    default MutableVector2D getCenterBottom() {
        return new MutableVector2D(getLeft() + getWidth() / 2, getTop() + getHeight());
    }


    /**
     *
     * @param other
     * @return
     */
    default boolean isIntersectingWidth(IRectangle other) {
        return Utils.isBoxesOverlapping(getLeft(), getTop(), getWidth(), getHeight(), other.getLeft(), other.getTop(), other.getWidth(), other.getHeight());
    }

    /**
     * @param px
     * @param py
     * @return
     */
    default boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, getLeft(), getTop(), getWidth(), getHeight());
    }

    /**
     * @param other
     * @return
     */
    default boolean contains(IRectangle other) {
        return Utils.isPointInsideRect(other.getTopLeft().X(), other.getTopLeft().Y(), getLeft(), getTop(), getWidth(), getHeight())
                && Utils.isPointInsideRect(other.getBottomRight().X(), other.getBottomRight().Y(), getLeft(), getTop(), getWidth(), getHeight());
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
        g.drawRoundedRect(getLeft(), getTop(), getWidth(), getHeight(), radius);
    }

    /**
     *
     * @param g
     * @param thickness
     */
    default void drawOutlined(AGraphics g, int thickness) {
        g.drawRect(getLeft(), getTop(), getWidth(), getHeight(), thickness);
    }

    default void drawOutlined(AGraphics g) {
        g.drawRect(getLeft(), getTop(), getWidth(), getHeight());
    }

    default GDimension getDimension() {
        return new GDimension(getWidth(), getHeight());
    }

    /**
     * Return half of min(W,H)
     * @return
     */
    @Override
    default float getRadius() {
        double w = getWidth();
        double h = getHeight();
        return (float) Math.sqrt(w * w + h * h) / 2;
    }

    /**
     * @return
     */
    default Vector2D getRandomPointInside() {
        return new Vector2D(getLeft() + Utils.randFloat(getWidth()), getTop() + Utils.randFloat(getHeight()));
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
        float newX = getLeft();
        float newY = getTop();
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
        return new GRectangle(getLeft() - dw / 2, getTop() - dh / 2, nw, nh);
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
            tx = getLeft();
            switch (vert) {
                case CENTER:
                    ty = getTop() + getHeight() / 2 - th / 2;
                    break;
                case TOP:
                    ty = getTop();
                    break;
                case BOTTOM:
                    ty = getTop() + getHeight() - th;
                    break;
            }
        } else {
            th = getHeight();
            tw = getHeight() * targetAspect;
            ty = getTop();
            switch (horz) {
                case CENTER:
                    tx = getLeft() + getWidth() / 2 - tw / 2;
                    break;
                case LEFT:
                    tx = getLeft();
                    break;
                case RIGHT:
                    tx = getLeft() + getWidth() - tw;
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

    default GRectangle withCenter(IVector2D cntr) {
        return new GRectangle(cntr.getX() - getWidth() / 2, cntr.getY() - getHeight() / 2, getWidth(), getHeight());
    }

    default GRectangle withPosition(IVector2D topLeft) {
        return new GRectangle(topLeft, this);
    }

    default GRectangle withDimension(IDimension dim) {
        return new GRectangle(getLeft(), getTop(), dim.getWidth(), dim.getHeight());
    }

    default GRectangle withDimension(float w, float h) {
        return new GRectangle(getLeft(), getTop(), w, h);
    }

    default GRectangle movedBy(float dx, float dy) {
        return new GRectangle(getLeft() + dx, getTop() + dy, getWidth(), getHeight());
    }

    default GRectangle movedBy(IVector2D dv) {
        return movedBy(dv.getX(), dv.getY());
    }

    default GRectangle add(IRectangle other) {
        return new GRectangle(
                Math.min(getLeft(), other.getLeft()),
                Math.min(getTop(), other.getTop()),
                Math.max(getWidth(), other.getWidth()),
                Math.max(getHeight(), other.getHeight())
        );
    }

    /**
     * @return
     */
    default IInterpolator<Vector2D> getRandomInterpolator() {
        return new IInterpolator<Vector2D>() {
            @Override
            public Vector2D getAtPosition(float position) {
                return getRandomPointInside();
            }
        };
    }

    default GRectangle shaked(float factor) {
        return shaked(factor, factor);
    }

    default GRectangle shaked(float xfactor, float yfactor) {
        float nx = getLeft() + getWidth() * Utils.randFloatPlusOrMinus(xfactor);
        float ny = getTop() + getHeight() * Utils.randFloatPlusOrMinus(yfactor);
        return new GRectangle(nx, ny, getWidth(), getHeight());
    }

    default GRectangle[] subDivide(int rows, int cols) {
        GRectangle[] divisions = new GRectangle[rows * cols];
        float wid = getWidth() / cols;
        float hgt = getHeight() / rows;
        int idx = 0;
        for (int i = 0; i < cols; i++) {
            MutableVector2D tl = getTopLeft().addEq(wid * i, 0);
            for (int ii = 0; ii < rows; ii++) {
                divisions[idx++] = new GRectangle(tl, wid, hgt);
                tl.addEq(0, hgt);
            }
        }
        return divisions;
    }

    default boolean isEmpty() {
        return this == GRectangle.EMPTY || (getWidth() <= 0 && getHeight() <= 0);
    }

    default boolean isInside(IRectangle other) {
        return (getTopLeft().getX() >= other.getTopLeft().X()
                && getBottomRight().X() <= other.getBottomRight().getX()
                && getTopLeft().getY() >= other.getTopLeft().getY()
                && getBottomRight().getY() <= other.getBottomRight().getY());
    }

    @Override
    default IRectangle enclosingRect() {
        return this;
    }

    @Override
    default float getArea() {
        return IDimension.super.getArea();
    }
}
