package cc.lib.game;

import java.util.Objects;

import cc.lib.math.MutableVector2D;
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

    public GRectangle(IVector2D v, GDimension d) {
        this(v.getX(), v.getY(), d);
    }

    public GRectangle(IVector2D v0, IVector2D v1) {
        set(v0, v1);
    }

    public float x, y, w, h;

    /**
     * Create a rect that bounds 2 vectors
     *
     * @param v0
     * @param v1
     */
    public void set(IVector2D v0, IVector2D v1) {
        x = Math.min(v0.getX(), v1.getX());
        y = Math.min(v0.getY(), v1.getY());
        w = Math.abs(v0.getX()-v1.getX());
        h = Math.abs(v0.getY()-v1.getY());
    }

    public void setEnd(IVector2D v) {
        x = Math.min(x, v.getX());
        y = Math.min(y, v.getY());
        w = Math.abs(x-v.getX());
        h = Math.abs(y-v.getY());
    }

    public void set(float left, float top, float right, float bottom) {
        x = left;
        y = top;
        w = right-left;
        h = bottom-top;
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getCenter() {
        return new MutableVector2D(x+w/2, y+h/2);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getTopLeft() {
        return new MutableVector2D(x, y);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getTopRight() {
        return new MutableVector2D(x+w, y);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getBottomLeft() {
        return new MutableVector2D(x, y+h);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getBottomRight() {
        return new MutableVector2D(x+w, y+h);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getCenterLeft() {
        return new MutableVector2D(x, y+h/2);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getCenterRight() {
        return new MutableVector2D(x+w, y+h/2);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getCenterTop() {
        return new MutableVector2D(x+w/2, y);
    }

    /**
     *
     * @return
     */
    public final MutableVector2D getCenterBottom() {
        return new MutableVector2D(x+w/2, y+h);
    }


    /**
     *
     * @param other
     * @return
     */
    public final boolean isIntersectingWidth(GRectangle other) {
        return Utils.isBoxesOverlapping(x, y, w, h, other.x, other.y, other.w, other.h);
    }

    /**
     *
     * @param px
     * @param py
     * @return
     */
    public final boolean contains(float px, float py) {
        return Utils.isPointInsideRect(px, py, x, y, w, h);
    }

    /**
     *
     * @param v
     * @return
     */
    public final boolean contains(IVector2D v) {
        return contains(v.getX(), v.getY());
    }

    /**
     *
     * @param r
     * @param position
     * @return
     */
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

    /**
     *
     * @param pixels
     */
    public void grow(float pixels) {
        x-=pixels;
        y-=pixels;
        w+=pixels*2;
        h+=pixels*2;
    }

    /**
     *
     * @param pixels
     * @return
     */
    public GRectangle grownBy(int pixels) {
        w += pixels;
        h += pixels;
        x -= pixels/2;
        y -= pixels/2;
        return this;
    }

    /**
     *
     * @param g
     */
    public void drawFilled(AGraphics g) {
        g.drawFilledRect(x, y, w, h);
    }

    /**
     *
     * @param g
     * @param radius
     */
    public void drawRounded(AGraphics g, float radius) {
        g.drawFilledRoundedRect(x, y, w, h, radius);
    }

    /**
     *
     * @param g
     * @param thickness
     */
    public void drawOutlined(AGraphics g, int thickness) {
        g.drawRect(x, y, w, h, thickness);
    }

    /**
     * Scale the dimension of this rect by some amout. s < 1 reduces size. s > 1 increases size.
     * @param sx
     * @param sy
     */
    public void scale(float sx, float sy) {
        float nw = w * sx;
        float nh = h * sy;
        float dw = nw-w;
        float dh = nh-h;
        x -= dw/2;
        y -= dh/2;
        w = nw;
        h = nh;
    }

    /**
     *
     * @param s
     * @return
     */
    public GRectangle scaledBy(float s) {
        return scaledBy(s, s);
    }

    /**
     *
     * @param sx
     * @param sy
     * @return
     */
    public GRectangle scaledBy(float sx, float sy) {
        float nw = w * sx;
        float nh = h * sy;
        float dw = nw-w;
        float dh = nh-h;
        return new GRectangle(x-dw/2, y-dh/2, nw, nh);
    }

    public GDimension getDimension() {
        return new GDimension(w, h);
    }

    public final float getAspect() {
        return w/h;
    }

    /**
     * Return a rectangle of dimension not to exceed this dimension and
     * whose aspect ratio is that of rectToFit and is centered inside this.
     *
     * @param rectToFit
     * @return
     */
    public final GRectangle fit(IDimension rectToFit) {
        return fit (rectToFit, Justify.CENTER, Justify.CENTER);
    }

    /**
     * Return a rectangle of dimension not to exceed this dimension and
     * whose aspect ratio is that of rectToFit and is centered inside this.
     *
     * @param rectToFit
     * @return
     */
    public final GRectangle fit(IDimension rectToFit, Justify horz, Justify vert) {
        float targetAspect = rectToFit.getAspect();
        float rectAspect = getAspect();
        float tx=0, ty=0, tw=0, th=0;
        if (targetAspect > rectAspect) {
            // target is wider than rect, so fit lengthwise
            tw = w;
            th = w / targetAspect;
            tx = x;
            switch (vert) {
                case CENTER:
                    ty = y + h/2 - th/2;
                    break;
                case TOP:
                    ty = y;
                    break;
                case BOTTOM:
                    ty = y + h-th;
                    break;
            }
        } else {
            th = h;
            tw = h * targetAspect;
            ty = y;
            switch (horz) {
                case CENTER:
                    tx = x + w/2 - tw/2;
                    break;
                case LEFT:
                    tx = x;
                    break;
                case RIGHT:
                    tx = w + w - tw;
                    break;
            }
        }
        return new GRectangle(tx, ty, tw, th);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GRectangle that = (GRectangle) o;
        return Float.compare(that.x, x) == 0 &&
                Float.compare(that.y, y) == 0 &&
                Float.compare(that.w, w) == 0 &&
                Float.compare(that.h, h) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, w, h);
    }
}
