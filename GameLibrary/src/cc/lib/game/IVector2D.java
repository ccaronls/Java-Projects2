package cc.lib.game;

import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public interface IVector2D {

    float getX();

    float getY();

    default MutableVector2D toMutable() {
        return new MutableVector2D(this);
    }

    default Vector2D toImmutable() {
        return new Vector2D(this);
    }

    default float radians() {
        return (float) Math.atan2(getY(), getX());
    }

    default MutableVector2D min(IVector2D v) {
        return new MutableVector2D(Math.min(getX(), v.getX()), Math.min(getY(), v.getY()));
    }

    default MutableVector2D max(IVector2D v) {
        return new MutableVector2D(Math.max(getX(), v.getX()), Math.max(getY(), v.getY()));
    }

    default MutableVector2D add(IVector2D v) {
        return new MutableVector2D(getX() + v.getX(), getY() + v.getY());
    }

    default MutableVector2D add(float x, float y) {
        return new MutableVector2D(getX() + x, getY() + y);
    }

    default MutableVector2D sub(IVector2D v) {
        return new MutableVector2D(getX() - v.getX(), getY() - v.getY());
    }

    default MutableVector2D sub(float x, float y) {
        return new MutableVector2D(this.getX() - x, this.getY() - y);
    }

    /**
     * Return a positive number if v is 270 - 90 degrees of this,
     * or a negative number if v is 90 - 270 degrees of this,
     * or zero if v is at right angle to this
     *
     * @param v
     * @return
     */
    default float dot(IVector2D v) {
        return getX() * v.getX() + getY() * v.getY();
    }

    /**
     * Return a positive number if v is 0-180 degrees of this,
     * or a negative number if v is 180-360 degrees of this
     *
     * @param v
     * @return
     */
    default float cross(IVector2D v) {
        return getX() * v.getY() - v.getX() * getY();
    }

    /**
     * @param s
     * @return
     */
    default MutableVector2D cross(float s) {
        final float tempy = -s * getX();
        return new MutableVector2D(s * getY(), tempy);
    }

    /**
     * @return
     */
    default float magSquared() {
        return getX() * getX() + getY() * getY();
    }

    /**
     * @return
     */
    default float mag() {
        return (float) Math.sqrt(magSquared());
    }

    /**
     * @param v
     * @return
     */
    default MutableVector2D midPoint(IVector2D v) {
        return new MutableVector2D((getX() + v.getX()) / 2, (getY() + v.getY()) / 2);
    }

    /**
     * Rotate out by 90 degrees
     *
     * @return
     */
    default MutableVector2D norm() {
        return new MutableVector2D(-getY(), getX());
    }

    /**
     * @return
     */
    default MutableVector2D unitLength() {
        float m = mag();
        if (m > CMath.EPSILON) {
            return new MutableVector2D(getX() / m, getY() / m);
        }
        return new MutableVector2D(getX(), getY());
    }

    /**
     * Return unit length vector
     *
     * @return
     */
    default MutableVector2D normalized() {
        float m = mag();
        if (m > CMath.EPSILON) {
            return new MutableVector2D(getX() / m, getY() / m);
        }
        return new MutableVector2D(this);
    }

    /**
     * @param s
     * @return
     */
    default MutableVector2D scaledBy(float s) {
        return new MutableVector2D(getX() * s, getY() * s);
    }

    /**
     * Return new vector scaled by amounts getX(),getY()
     *
     * @param sx
     * @param sy
     * @return
     */
    default MutableVector2D scaledBy(float sx, float sy) {
        return new MutableVector2D(getX() * sx, getY() * sy);
    }

    /**
     * @param degrees
     * @return
     */
    default MutableVector2D rotated(float degrees) {
        degrees *= CMath.DEG_TO_RAD;
        float cosd = (float) Math.cos(degrees);
        float sind = (float) Math.sin(degrees);
        return new MutableVector2D(getX() * cosd - getY() * sind, getX() * sind + getY() * cosd);
    }

    /**
     * Returns always positive angle between 2 vectors 0-180
     *
     * @param v
     * @return
     */
    default float angleBetween(IVector2D v) {
        float dv = dot(v);
        return (float) Math.acos(dv / (mag() * v.mag())) * CMath.RAD_TO_DEG;
    }

    /**
     * Returns value between 180-180
     *
     * @param v
     * @return
     */
    default float angleBetweenSigned(IVector2D v) {
        float ang = angleBetween(v);
        Utils.assertTrue(ang >= 0);
        if (cross(v) < 0) {
            return -ang;
        } else {
            return ang;
        }
    }

    /**
     * @return value between 0-360 where
     * 0 represents getX() position and getY() 0
     * 90 represents getX() zero and getY() position
     * 180 represents getX() negative and getY() 0
     * 270 represents getX() zero and getY() negative
     */
    default float angleOf() {
        if (Math.abs(getX()) < CMath.EPSILON)
            return (getY() > 0 ? 90 : 270);
        int r = (int) Math.round(Math.atan(getY() / getX()) * CMath.RAD_TO_DEG);
        return (getX() < 0 ? 180 + r : r < 0 ? 360 + r : r);
    }

    /**
     * Return this reflected off the wall defined by normal
     * <p>
     * <p>
     * |W  /
     * |  /
     * | /
     * |v
     * |------> N
     * | \
     * |  \
     * |   \
     * v
     * <p>
     * W = Wall
     * N = normalToWall
     * V = this
     * R = result
     *
     * @param normalToWall
     * @return
     */
    default Vector2D reflect(IVector2D normalToWall) {
        if (normalToWall.isZero())
            return Vector2D.ZERO;
        if (normalToWall.isNaN())
            return Vector2D.ZERO;
        if (normalToWall.isInfinite())
            return Vector2D.ZERO;
        float ndotv = normalToWall.dot(this);
        if (ndotv > 0)
            normalToWall = normalToWall.scaledBy(-1);
        ndotv = Math.abs(ndotv);
        float ndotn = normalToWall.dot(normalToWall);
        // projection vector
        Vector2D p = normalToWall.scaledBy(2 * ndotv / ndotn);
        return add(p);
    }

    default boolean equalsWithinRange(IVector2D v, float epsilon) {
        float dx = getX() - v.getX();
        float dy = getY() - v.getY();
        return (Math.abs(dx) < epsilon && Math.abs(dy) < epsilon);
    }

    default boolean isZero() {
        return equalsWithinRange(Vector2D.ZERO, CMath.EPSILON);
    }

    default boolean isNaN() {
        return Float.isNaN(getX()) || Float.isNaN(getY());
    }

    default boolean isInfinite() {
        return Float.isInfinite(getX()) || Float.isInfinite(getY());
    }

    default MutableVector2D wrapped(IVector2D min, IVector2D max) {
        return new MutableVector2D(this).wrap(min, max);
    }

    default IInterpolator<Vector2D> linearInterpolateTo(IVector2D other) {
        return Vector2D.getLinearInterpolator(this, other.toMutable());
    }

    default MutableVector2D toViewport(AGraphics g) {
        MutableVector2D v = new MutableVector2D(this);
        g.transform(v);
        return v;
    }
}
