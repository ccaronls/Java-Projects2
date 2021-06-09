package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public final class GRectangle extends Reflector<GRectangle> implements IRectangle {

    static {
        addAllFields(GRectangle.class);
    }

    public float x, y, w, h;

    public GRectangle() {}

    public GRectangle(IDimension dim) {
        this(0, 0, dim);
    }

    public GRectangle(IRectangle toCopy) {
        this(toCopy.X(), toCopy.Y(), toCopy.getWidth(), toCopy.getHeight());
    }

    public GRectangle(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public GRectangle(float x, float y, IDimension dim) {
        this(x, y, dim.getWidth(), dim.getHeight());
    }

    public GRectangle(IVector2D topLeft, float w, float h) {
        this(topLeft.getX(), topLeft.getY(), w, h);
    }

    public GRectangle(IVector2D v, IDimension d) {
        this(v.getX(), v.getY(), d);
    }

    public GRectangle(IVector2D v0, IVector2D v1) {
        set(v0, v1);
    }

    @Override
    public float X() {
        return x;
    }

    @Override
    public float Y() {
        return y;
    }

    @Override
    public float getWidth() {
        return w;
    }

    @Override
    public float getHeight() {
        return h;
    }

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

    /**
     * Useful for mouse dragging rectangular bounds
     * @param v
     */
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
     * Adjust bounds by some number of pixels
     *
     * @param pixels
     */
    public GRectangle grow(float pixels) {
        x-=pixels;
        y-=pixels;
        w+=pixels*2;
        h+=pixels*2;
        return this;
    }

    /**
     *
     * @param pixels
     * @return
     */
    public GRectangle grownBy(int pixels) {
        return new GRectangle(x -pixels/2,y - pixels/2,w + pixels,h + pixels);
    }

    /**
     * Scale the dimension of this rect by some amout. s < 1 reduces size. s > 1 increases size.
     * @param sx
     * @param sy
     */
    public GRectangle scale(float sx, float sy) {
        float nw = w * sx;
        float nh = h * sy;
        float dw = nw-w;
        float dh = nh-h;
        x -= dw/2;
        y -= dh/2;
        w = nw;
        h = nh;
        return this;
    }

    public GRectangle scale(float s) {
        return scale(s,s);
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
     * Return a rectangle that fits inside this rect and with same aspect.
     * How to position inside this determined by horz/vert justifys.
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
                    tx = x + w - tw;
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
        return Utils.hashCode(x, y, w, h);
    }

    public GRectangle setCenter(IVector2D cntr) {
        x=cntr.getX()-w/2;
        y=cntr.getY()-h/2;
        return this;
    }

    public GRectangle withCenter(IVector2D cntr) {
        return new GRectangle(cntr.getX()-w/2, cntr.getY()-h/2, w, h);
    }

    public GRectangle withDimension(GDimension dim) {
        return new GRectangle(x, y, dim.width, dim.height);
    }

    public GRectangle withDimension(float w, float h) {
        return new GRectangle(x, y, w, h);
    }

    public GRectangle movedBy(float dx, float dy) {
        x+=dx;
        y+=dy;
        return this;
    }

    public GRectangle movedBy(IVector2D dv) {
        x+=dv.getX();
        y+=dv.getY();
        return this;
    }

    /**
     * Add a rectangle to this such that the bound of this grow to contain all of input.
     * This rect will never be reduces in size.
     *
     * @param x
     * @param y
     * @param w
     * @param h
     * @return
     */
    public GRectangle addEq(float x, float y, float w, float h) {
        return addEq(new GRectangle(x, y, w, h));
    }

    /**
     *
     * @param g
     * @return
     */
    public GRectangle addEq(GRectangle g) {
        if (w == 0 || h == 0) {
            copyFrom(g);
            return this;
        }
        Vector2D tl = getTopLeft().minEq(g.getTopLeft());
        Vector2D br = getBottomRight().maxEq(g.getBottomRight());
        set(tl, br);
        return this;
    }
}
