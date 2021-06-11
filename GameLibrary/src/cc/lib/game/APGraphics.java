package cc.lib.game;

import java.util.List;

import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 2/13/18.
 *
 * Graphics interface that supports picking
 */
public abstract class APGraphics extends AGraphics {

    protected final Renderer r;

    protected APGraphics(int viewportWidth, int viewportHeight) {
        super(viewportWidth, viewportHeight);
        this.r = new Renderer(this);
    }

    @Override
    public final  void begin() {
        r.clearVerts();
    }

    @Override
    public final  void end() {
        r.clearVerts();
    }

    @Override
    public final  void ortho(float left, float right, float top, float bottom) {
        r.setOrtho(left, right, top, bottom);
    }

    @Override
    public final  void pushMatrix() {
        r.pushMatrix();
    }

    @Override
    public final  void popMatrix() {
        r.popMatrix();
    }

    @Override
    public final  void translate(float x, float y) {
        r.translate(x, y);
    }

    @Override
    public final  void rotate(float degrees) {
        r.rotate(degrees);
    }

    @Override
    public final  void scale(float x, float y) {
        r.scale(x, y);
    }

    @Override
    public final  void setIdentity() {
        r.makeIdentity();
    }

    @Override
    public void multMatrix(Matrix3x3 m) {
        r.multiply(m);
    }

    @Override
    public final  void transform(float x, float y, float[] result) {
        r.transformXY(x, y, result);
    }

    @Override
    public final MutableVector2D screenToViewport(int screenX, int screenY) {
        return r.untransform(screenX, screenY);
    }

    public final MutableVector2D screenToViewport(float screenX, float screenY) {
        return r.untransform(screenX, screenY);
    }

    public final MutableVector2D screenToViewport(IVector2D screen) {
        return r.untransform(screen.getX(), screen.getY());
    }

    float [] lastVertex = new float[2];

    @Override
    public final void vertex(float x, float y) {
        Utils.copyElems(lastVertex, x, y);
        r.addVertex(x, y);
    }

    @Override
    public void moveTo(float dx, float dy) {
        vertex(lastVertex[0] + dx, lastVertex[1] + dy);
    }

    /**
     *
     * @param x
     * @param y
     * @param size
     * @return
     */
    public final int pickPoints(int x, int y, int size) {
        int picked = -1;
        int bestD = Integer.MAX_VALUE;
        for (int i=0; i<r.getNumVerts(); i++) {
            int dx = x - Math.round(r.getX(i));
            int dy = y - Math.round(r.getY(i));
            int d = Utils.fastLen(dx, dy);
            if (d <= size) {
                if (picked < 0 || d < bestD) {
                    picked = r.getName(i);
                    bestD = d;
                }
            }
        }
        return picked;
    }

    /**
     *
     * @param thickness
     * @param x
     * @param y
     * @return
     */
    public final int pickLines(int x, int y, int thickness) {
        int picked = -1;
        for (int i=0; i<r.getNumVerts(); i+=2) {

            float x0 = r.getX(i);
            float y0 = r.getY(i);
            float x1 = r.getX(i+1);
            float y1 = r.getY(i+1);

            float d0 = Utils.distSqPointLine(x, y, x0, y0, x1, y1);
            if (d0 > thickness)
                continue;

            float dx = x1 - x0;
            float dy = y1 - y0;

            float dot_p_d1 = (x-x0)*dx + (y-y0)*dy;
            float dot_p_d2 = (x-x1)*-dx + (y-y1)*-dy;

            if (dot_p_d1 < 0 || dot_p_d2 < 0)
                continue;

            picked = r.getName(i);
        }
        return picked;
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    public final int pickRects(int x, int y) {
        int picked = -1;
        for (int i=0; i<=r.getNumVerts()-2; i+=2) {
            IVector2D v0 = r.getVertex(i);
            IVector2D v1 = r.getVertex(i+1);
            if (r.getName(i) < 0)
                continue;
            float X = Math.min(v0.getX(), v1.getX());
            float Y = Math.min(v0.getY(), v1.getY());
            float W = Math.abs(v0.getX()-v1.getX());
            float H = Math.abs(v0.getY()-v1.getY());

            //Utils.println("pick rect[%d] m[%d,%d] r[%3.1f,%3.1f,%3.1f,%3.1f]", getName(i),x, y, X, Y, W, H);

            if (Utils.isPointInsideRect(x,y,X,Y,W,H)) {
                picked = r.getName(i);
                break;
            }
        }
        return picked;
    }

    /**
     * Returns name of closest vertex to x,y
     * @param x
     * @param y
     * @return
     */
    public final int pickClosest(int x, int y) {
        int picked = -1;
        float closest = Float.MAX_VALUE;
        for (int i=0; i<r.getNumVerts(); i++) {
            if (r.getName(i) < 0)
                continue;
            Vector2D v = r.getVertex(i);
            Vector2D dv =  v.sub(x, y);
            float d = dv.magSquared();
            if (d < closest) {
                closest = d;
                picked = r.getName(i);
            }
        }
        return picked;
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    public final int pickQuads(int x, int y) {
        int picked = -1;
        for (int i=0; i<=r.getNumVerts()-4; i+=4) {
            if (Utils.isPointInsidePolygon(x, y, r.getVertex(i), r.getVertex(i+1), r.getVertex(i+2), r.getVertex(i+3))) {
                picked = r.getName(i);
            }
        }
        return picked;
    }

    /**
     *
     * @param index
     */
    public final void setName(int index) {
        r.setName(index);
    }

    /**
     *
     * @param name
     * @return
     */
    public final List<IVector2D> getVerticesForName(int name) {
        return r.getVerticesForName(name);
    }


    @Override
    public final void clearMinMax() {
        r.clearBoundingRect();
    }

    @Override
    public final Vector2D getMinBoundingRect() {
        return r.getMin();
    }

    @Override
    public final Vector2D getMaxBoundingRect() {
        return r.getMax();
    }

    @Override
    public void getTransform(Matrix3x3 result) {
        result.assign(r.getCurrentTransform());
    }

}
