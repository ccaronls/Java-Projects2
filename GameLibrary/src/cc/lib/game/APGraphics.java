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

    protected final Renderer R;

    protected APGraphics(int viewportWidth, int viewportHeight) {
        super(viewportWidth, viewportHeight);
        this.R = new Renderer(this);
    }

    @Override
    public final  void begin() {
        R.clearVerts();
    }

    @Override
    public final  void end() {
        R.clearVerts();
    }

    @Override
    public final void ortho(float left, float right, float top, float bottom) {
        R.setOrtho(left, right, top, bottom);
    }

    @Override
    public final void pushMatrix() {
        R.pushMatrix();
    }

    @Override
    public int getPushDepth() {
        return R.getStackSize();
    }

    public final void pushAndRun(Runnable runner) {
        R.pushMatrix();
        runner.run();
        R.popMatrix();
    }

    @Override
    public final void popMatrix() {
        R.popMatrix();
    }

    @Override
    public final  void translate(float x, float y) {
        R.translate(x, y);
    }

    @Override
    public final  void rotate(float degrees) {
        R.rotate(degrees);
    }

    @Override
    public final  void scale(float x, float y) {
        R.scale(x, y);
    }

    @Override
    public final  void setIdentity() {
        R.makeIdentity();
    }

    @Override
    public void multMatrix(Matrix3x3 m) {
        R.multiply(m);
    }

    @Override
    public final  void transform(float x, float y, float[] result) {
        R.transformXY(x, y, result);
    }

    @Override
    protected MutableVector2D untransform(float x, float y) {
        return R.untransform(x, y);
    }

    float [] lastVertex = new float[2];

    @Override
    public final void vertex(float x, float y) {
        Utils.copyElems(lastVertex, x, y);
        R.addVertex(x, y);
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
        for (int i = 0; i< R.getNumVerts(); i++) {
            int dx = x - Math.round(R.getX(i));
            int dy = y - Math.round(R.getY(i));
            int d = Utils.fastLen(dx, dy);
            if (d <= size) {
                if (picked < 0 || d < bestD) {
                    picked = R.getName(i);
                    bestD = d;
                }
            }
        }
        return picked;
    }

    public final int pickPoints(IVector2D m, int size) {
        int picked = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i< R.getNumVerts(); i++) {
            float dx = m.getX() - R.getX(i);
            float dy = m.getY() - R.getY(i);
            float d = Utils.fastLen(dx, dy);
            if (d <= size) {
                if (picked < 0 || d < bestD) {
                    picked = R.getName(i);
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
        for (int i = 0; i< R.getNumVerts(); i+=2) {

            float x0 = R.getX(i);
            float y0 = R.getY(i);
            float x1 = R.getX(i+1);
            float y1 = R.getY(i+1);

            float d0 = Utils.distSqPointLine(x, y, x0, y0, x1, y1);
            if (d0 > thickness)
                continue;

            float dx = x1 - x0;
            float dy = y1 - y0;

            float dot_p_d1 = (x-x0)*dx + (y-y0)*dy;
            float dot_p_d2 = (x-x1)*-dx + (y-y1)*-dy;

            if (dot_p_d1 < 0 || dot_p_d2 < 0)
                continue;

            picked = R.getName(i);
        }
        return picked;
    }

    public final int pickLines(IVector2D m, int thickness) {
        int picked = -1;
        for (int i = 0; i< R.getNumVerts(); i+=2) {

            float mx = m.getX();
            float my = m.getY();
            float x0 = R.getX(i);
            float y0 = R.getY(i);
            float x1 = R.getX(i+1);
            float y1 = R.getY(i+1);

            float d0 = Utils.distSqPointLine(mx, my, x0, y0, x1, y1);
            if (d0 > thickness)
                continue;

            float dx = x1 - x0;
            float dy = y1 - y0;

            float dot_p_d1 = (mx-x0)*dx + (my-y0)*dy;
            float dot_p_d2 = (mx-x1)*-dx + (my-y1)*-dy;

            if (dot_p_d1 < 0 || dot_p_d2 < 0)
                continue;

            picked = R.getName(i);
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
        for (int i = 0; i<= R.getNumVerts()-2; i+=2) {
            IVector2D v0 = R.getVertex(i);
            IVector2D v1 = R.getVertex(i+1);
            if (R.getName(i) < 0)
                continue;
            float X = Math.min(v0.getX(), v1.getX());
            float Y = Math.min(v0.getY(), v1.getY());
            float W = Math.abs(v0.getX()-v1.getX());
            float H = Math.abs(v0.getY()-v1.getY());

            //Utils.println("pick rect[%d] m[%d,%d] r[%3.1f,%3.1f,%3.1f,%3.1f]", getName(i),x, y, X, Y, W, H);

            if (Utils.isPointInsideRect(x,y,X,Y,W,H)) {
                picked = R.getName(i);
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
        for (int i = 0; i< R.getNumVerts(); i++) {
            if (R.getName(i) < 0)
                continue;
            Vector2D v = R.getVertex(i);
            Vector2D dv =  v.sub(x, y);
            float d = dv.magSquared();
            if (d < closest) {
                closest = d;
                picked = R.getName(i);
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
        for (int i = 0; i<= R.getNumVerts()-4; i+=4) {
            if (Utils.isPointInsidePolygon(x, y, R.getVertex(i), R.getVertex(i+1), R.getVertex(i+2), R.getVertex(i+3))) {
                picked = R.getName(i);
            }
        }
        return picked;
    }

    /**
     *
     * @param index
     */
    public final void setName(int index) {
        R.setName(index);
    }

    /**
     *
     * @param name
     * @return
     */
    public final List<IVector2D> getVerticesForName(int name) {
        return R.getVerticesForName(name);
    }


    @Override
    public final void clearMinMax() {
        R.clearBoundingRect();
    }

    @Override
    public final Vector2D getMinBoundingRect() {
        return R.getMin();
    }

    @Override
    public final Vector2D getMaxBoundingRect() {
        return R.getMax();
    }

    @Override
    public void getTransform(Matrix3x3 result) {
        result.assign(R.getCurrentTransform());
    }

}
