package cc.lib.math;

import cc.lib.game.IVector2D;


public class MutableVector2D extends Vector2D {

    public MutableVector2D() {
        super();
    }

    public MutableVector2D(float x, float y) {
        super(x, y);
    }

    public MutableVector2D(IVector2D v) {
        super(v);
    }

    public final MutableVector2D setX(float x) {
        this.x = x;
        return this;
    }

    public final MutableVector2D setY(float y) {
        this.y = y;
        return this;
    }

    public final MutableVector2D set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    public final MutableVector2D zero() {
    	return set(0,0);
    }
    
    public final MutableVector2D set(IVector2D v) {
        return set(v.getX(), v.getY());
    }

    public final MutableVector2D addEq(IVector2D v) {
        return set(x+v.getX(), y+v.getY());
    }

    public final MutableVector2D addEq(float dx, float dy) {
        return set(x+dx, y+dy);
    }

    public final MutableVector2D subEq(IVector2D v) {
        return set(x-v.getX(), y-v.getY());
    }

    public final MutableVector2D subEq(float dx, float dy) {
        return set(x-dx, y-dy);
    }

    public final MutableVector2D scaleEq(float scalar) {
        return set(x*scalar, y*scalar);
    }

    public final MutableVector2D scaleEq(float xscale, float yscale) {
        return set(x*xscale, y*yscale);
    }

    /**
     * Rotate this vector 90 degrees
     * @return
     */
    public final MutableVector2D normEq() {
        return norm(this);
    }

    public final MutableVector2D unitLengthEq() {
        return unitLength(this);
    }

    public final MutableVector2D rotateEq(float degrees) {
        return rotate(degrees, this);
    }

    public final MutableVector2D minEq(IVector2D v) {
        return min(v, this);
    }

    public final MutableVector2D maxEq(IVector2D v) {
        return max(v, this);
    }

    public final MutableVector2D reflectEq(Vector2D normalToWall) {
        return set(reflect(normalToWall));
    }

    public final MutableVector2D normalizedEq() {
        return normalized(this);
    }

    public final MutableVector2D wrap(IVector2D min, IVector2D max) {
        Vector2D delta = sub(max, min);
        while (getX() < min.getX()) {
            setX(getX() + delta.getX());
        }
        while (getX() > max.getX()) {
            setX(getX() - delta.getX());
        }
        while (getY() < min.getY()) {
            setY(getY() + delta.getY());
        }
        while (getY() > max.getY()) {
            setY(getY() - delta.getY());
        }
        return this;
    }
}

