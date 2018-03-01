package cc.lib.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

/**
 * Immutable.  Use MutableVector2D for mutator operations.
 * @author chriscaron
 *
 */
public class Vector2D extends Reflector<Vector2D> implements IVector2D, Serializable {

	public static boolean USE_TEMPS = false;
	
    static {
        addAllFields(Vector2D.class);
    }
    
    public final static Vector2D MIN = new Vector2D(-Float.MAX_VALUE, -Float.MAX_VALUE);
    public final static Vector2D MAX = new Vector2D(Float.MAX_VALUE, Float.MAX_VALUE);
    public final static Vector2D ZERO = new Vector2D(0,0);
    
    protected float x, y;
    
    public Vector2D() {}
    public Vector2D(float x, float y) {
        this.x = x; this.y = y;
    }

    public Vector2D(IVector2D v) {
        this.x = v.getX(); this.y = v.getY();
    }

    public final float X() {
        return x;
    }
    
    public final float Y() {
        return y;
    }

    public final int Xi() {
        return Math.round(x);
    }
    
    public final int Yi() {
        return Math.round(y);
    }

    public final float getX() {
        return x;
    }
    
    public final float getY() {
        return y;
    }

    public final float radians() {
        return (float)Math.atan2(y, x);
    }

    public final MutableVector2D min(IVector2D v, MutableVector2D out) {
        return out.set(Math.min(x, v.getX()), Math.min(y, v.getY()));
    }

    public final MutableVector2D min(IVector2D v) {
        return min(v, newTemp());
    }

    public final MutableVector2D max(IVector2D v, MutableVector2D out) {
        return out.set(Math.max(x, v.getX()), Math.max(y, v.getY()));
    }

    public final MutableVector2D max(IVector2D v) {
        return max(v, newTemp());
    }

    public final MutableVector2D add(IVector2D v, MutableVector2D out) {
        return out.set(x+v.getX(), y+v.getY());
    }

    public final MutableVector2D add(IVector2D v) {
        return add(v, newTemp());
    }

    public final MutableVector2D add(float x, float y) {
        return new MutableVector2D(this.x + x, this.y + y);
    }

    public final MutableVector2D sub(IVector2D v, MutableVector2D out) {
        return out.set(x-v.getX(), y-v.getY());
    }

    public final MutableVector2D sub(IVector2D v) {
        return sub(v, newTemp());
    }

    public final MutableVector2D sub(float x, float y) {
        return new MutableVector2D(this.x - x, this.y - y);
    }

    public final float dot(IVector2D v) {
        return x*v.getX() + y*v.getY();
    }
    
    public final float cross(IVector2D v) {
        return x*v.getY() - v.getX()*y;
    }
    
    public final MutableVector2D cross(float s, MutableVector2D out) {
        final float tempy = -s * x;
        return out.set(s*y, tempy);
    }

    public final MutableVector2D cross(float s) {
        return cross(s, newTemp());
    }

    public final float magSquared() {
        return x*x + y*y;
    }
    
    public static float mag(IVector2D v) {
    	final float x = v.getX();
    	final float y = v.getY();
    	return (float)Math.sqrt(x*x+y*y);
    }
    
    public final float mag() {
        return (float)Math.sqrt(magSquared());
    }
    
    public final MutableVector2D midPoint(IVector2D v, MutableVector2D out) {
        return out.set((x+v.getX())/2, (y+v.getY())/2);
    }

    public final MutableVector2D midPoint(IVector2D v) {
        return midPoint(v, newTemp());
    }

    /**
     * Rotate out by 90 degrees
     * @param out
     * @return
     */
    public final MutableVector2D norm(MutableVector2D out) {
        return out.set(-y, x);
    }

    /**
     * Return a temp vector that is 90 rotated from this
     * @return
     */
    public final MutableVector2D norm() {
        return norm(newTemp());
    }

    public final MutableVector2D unitLength(MutableVector2D out) {
        float m = mag();
        if (m > CMath.EPSILON) {
            return out.set(x/m, y/m);
        }
        return out.set(x,y);
    }

    public final MutableVector2D unitLength() {
        return unitLength(newTemp());
    }
    
    /**
     * Return unit length vector
     * @param out
     * @return
     */
    public final MutableVector2D normalized(MutableVector2D out) {
        float m = mag();
        if (m > CMath.EPSILON) {
            return out.set(x/m, y/m);
        }
        return out.set(this);
    }

    /**
     * Return unit length vector
     * @return
     */
    public final MutableVector2D normalized() {
        return normalized(newTemp());
    }
    
    public final MutableVector2D scale(float s, MutableVector2D out) {
        return out.set(x*s, y*s);
    }

    /**
     * Return new vector scaled by amount s
     *
     * @param s
     * @return
     */
    public final MutableVector2D scaledBy(float s) {
        return scale(s, newTemp());
    }
    
    public final MutableVector2D rotate(float degrees, MutableVector2D out) {
        degrees *= CMath.DEG_TO_RAD;
        float cosd = (float)Math.cos(degrees);
        float sind = (float)Math.sin(degrees);
        return out.set(x*cosd - y*sind, x*sind + y*cosd);
    }

    public final MutableVector2D rotate(float degrees) {
        return rotate(degrees, newTemp());
    }

    public final float angleBetween(IVector2D v) {
        float dv = dot(v);
        return (float)Math.acos(dv / (mag() * mag(v))) * CMath.RAD_TO_DEG;
    }
    
    public final float angleOf() {
        if (Math.abs(x) < CMath.EPSILON)
            return (y > 0 ? 90 : 270);
        int r = (int)Math.round(Math.atan(y/x) * CMath.RAD_TO_DEG);
        return (x < 0 ? 180 + r : r < 0 ? 360 + r : r);
    }
    
    public final String toString() {
        return String.format("<%6f, %6f>", x, y);
    }    
    
    private static MutableVector2D [] pool = new MutableVector2D[64];
    private static int poolIndex = 0;
    static {
        for (int i=0; i<pool.length; i++) {
            pool[i] = new MutableVector2D();
        }
    }
    
    private static MutableVector2D getFromPool() {
    	if (USE_TEMPS) {
            MutableVector2D next = pool[poolIndex];
            poolIndex = (poolIndex+1) % pool.length;
            return next;
    	}
    	return new MutableVector2D();
    }
    
    public static MutableVector2D newTemp() {
        return getFromPool().set(0,0);
    }

    public static MutableVector2D newTemp(float x, float y) {
        return getFromPool().set(x,y);
    }

    public static MutableVector2D newTemp(IVector2D v) {
        return getFromPool().set(v);
    }

    @Override
    public final boolean equals(Object o) {
    	if (this == o)
    		return true;
    	
        if (!(o instanceof Vector2D))
            return false;
        
        Vector2D v = (Vector2D)o;
        float dx = x-v.x;
        float dy = y-v.y;
        return (Math.abs(dx) < CMath.EPSILON && Math.abs(dy) < CMath.EPSILON);
    }
    
    /**
     * Get a hashCode for the 2D vector.
     * <p>
     * All NaN values have the same hash code.</p>
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (isNaN()) {
            return 542;
        }
        return 122 * (76 * Float.valueOf(x).hashCode() +  Float.valueOf(y).hashCode());
    }
    
    public boolean isZero() {
    	return equals(ZERO);
    }
    
    public boolean isNaN() {
    	return Float.isNaN(x) || Float.isNaN(y);
    }

    protected void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);;
    }
        
    protected void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	x = in.readFloat();
    	y = in.readFloat();
    }
        
    @SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
        x=y=0;
    }

}
