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

    public final MutableVector2D  set(float x, float y) {
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
        return (MutableVector2D)set(x*scalar, y*scalar);
    }

    /**
     * Rotate this vector 90 degrees
     * @return
     */
    public final MutableVector2D normEq() {
        return (MutableVector2D)norm(this);
    }

    public final MutableVector2D unitLengthEq() {
        return (MutableVector2D)unitLength(this);
    }

    public final MutableVector2D rotateEq(float degrees) {
        return (MutableVector2D)rotate(degrees, this);
    }

    public final MutableVector2D minEq(IVector2D v) {
        return (MutableVector2D)min(v, this);
    }

    public final MutableVector2D maxEq(IVector2D v) {
        return (MutableVector2D)max(v, this);
    }

}

