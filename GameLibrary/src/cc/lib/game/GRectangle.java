package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.reflector.Reflector;

public class GRectangle extends Reflector<GRectangle> implements IRectangle {

    static {
        addAllFields(GRectangle.class);
    }

    public final static GRectangle EMPTY = new GRectangle() {
        @Override
        protected boolean isImmutable() {
            return true;
        }
    };

    public float x, y, w, h;

    public GRectangle() {}

    public GRectangle(IDimension dim) {
        this(0, 0, dim);
    }

    public GRectangle(IRectangle toCopy) {
        this(toCopy.getLeft(), toCopy.getTop(), toCopy.getWidth(), toCopy.getHeight());
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
    public float getLeft() {
        return x;
    }

    @Override
    public float getTop() {
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
        w = Math.abs(v0.getX() - v1.getX());
        h = Math.abs(v0.getY() - v1.getY());
    }

    public void set(IRectangle r) {
        x = r.getLeft();
        y = r.getTop();
        w = r.getWidth();
        h = r.getHeight();
    }

    /**
     * Useful for mouse dragging rectangular bounds
     *
     * @param v
     */
    public void setEnd(IVector2D v) {
        x = Math.min(x, v.getX());
        y = Math.min(y, v.getY());
        w = Math.abs(x - v.getX());
        h = Math.abs(y - v.getY());
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

    public static IInterpolator<GRectangle> getInterpolator(GRectangle start, GRectangle end) {
        return new IInterpolator<GRectangle>() {
            @Override
            public GRectangle getAtPosition(float position) {
                return start.getInterpolationTo(end, position);
            }
        };
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
    public GRectangle grownBy(float pixels) {
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

    public GRectangle scaleDimension(float s) {
        w *= s;
        h *= s;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        //if (!super.equals(o)) return false;
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
        y = cntr.getY() - h / 2;
        return this;
    }

    public GRectangle setPosition(IVector2D topLeft) {
        x = topLeft.getX();
        y = topLeft.getY();
        return this;
    }

    public GRectangle setTopRightPosition(IVector2D topRight) {
        x = topRight.getX() - getWidth();
        y = topRight.getY();
        return this;
    }

    public GRectangle setDimension(IDimension dim) {
        IVector2D cntr = getCenter();
        w = dim.getWidth();
        h = dim.getHeight();
        return setCenter(cntr);
    }

    public GRectangle setDimension(float width, float height) {
        w = width;
        h = height;
        return this;
    }

    public GRectangle setDimensionJustified(float width, float height, Justify horz, Justify vert) {
        w = width;
        h = height;
        switch (horz) {
            case LEFT:
                break;
            case RIGHT:
                moveBy(-width, 0);
                break;
            case CENTER:
                moveBy(-width / 2, 0);
                break;
        }
        switch (vert) {
            case TOP:
                break;
            case BOTTOM:
                moveBy(0, -height);
                break;
            case CENTER:
                moveBy(0, -height / 2);
                break;
        }
        return this;
    }

    public GRectangle moveBy(float dx, float dy) {
        x += dx;
        y += dy;
        return this;
    }

    public GRectangle moveBy(IVector2D dv) {
        return moveBy(dv.getX(), dv.getY());
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
     * @param dv
     * @param w
     * @param h
     * @return
     */
    public GRectangle addEq(IVector2D dv, float w, float h) {
        return addEq(dv.getX(), dv.getY(), w, h);
    }

    /**
     * @param g
     * @return
     */
    public GRectangle addEq(IRectangle g) {
        if (w == 0 || h == 0) {
            copyFrom(new GRectangle(g));
            return this;
        }
        Vector2D tl = getTopLeft().minEq(g.getTopLeft());
        Vector2D br = getBottomRight().maxEq(g.getBottomRight());
        set(tl, br);
        return this;
    }

    public final GRectangle setAspect(float aspect) {
        float a = getAspect();
        Vector2D cntr = getCenter();
        if (a > aspect) {
            // grow the height to meet the target aspect
            h = w / aspect;
        } else {
            // grow the width to meet the target aspect
            w = h * aspect;
        }
        setCenter(cntr);
        return this;
    }

    public final GRectangle setAspectReduce(float aspect) {
        float a = getAspect();
        Vector2D cntr = getCenter();
        if (a < aspect) {
            // grow the height to meet the target aspect
            h = w / aspect;
        } else {
            // grow the width to meet the target aspect
            w = h * aspect;
        }
        setCenter(cntr);
        return this;
    }
}
