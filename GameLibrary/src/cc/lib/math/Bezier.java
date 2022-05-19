package cc.lib.math;

import cc.lib.game.IInterpolator;
import cc.lib.game.IVector2D;

/**
 * Generate a bezier curve
 * 
 * @author chriscaron
 *
 */
public final class Bezier implements IInterpolator<Vector2D> {
	
	private final IVector2D [] ctrl;
    private int numCtrl = 0;
	
	public Bezier() {
		ctrl = new IVector2D[4];
	}
	
	public Bezier (IVector2D [] v) {
		this.ctrl = v;
		numCtrl = v.length;
	}
	
	public Bezier addPoint(float x, float y) {
        ctrl[numCtrl++] = new Vector2D(x, y);
        return this;
	}

	public void addPoint(IVector2D v) {
        ctrl[numCtrl++] = new Vector2D(v);
    }

    public void reset() {
        numCtrl = 0;
    }

    @Override
	public Vector2D getAtPosition(float t) {
		if (numCtrl < 4)
			throw new cc.lib.utils.GException();
        float fW = 1 - t; 
        float fA = fW * fW * fW;
        float fB = 3 * t * fW * fW; 
        float fC = 3 * t * t * fW;
        float fD = t * t * t;
        float fX = fA * ctrl[0].getX() + fB * ctrl[1].getX() + fC * ctrl[2].getX() + fD * ctrl[3].getX(); 
        float fY = fA * ctrl[0].getY() + fB * ctrl[1].getY() + fC * ctrl[2].getY() + fD * ctrl[3].getY();
        return new Vector2D(fX, fY);
	}

	public static IInterpolator<Vector2D> build(Vector2D r0, Vector2D r1, float arc) {

	    if (Math.abs(arc) < 0.001)
	        return Vector2D.getLinearInterpolator(r0, r1);

	    Bezier curve = new Bezier();
        curve.addPoint(r0);
        Vector2D dv = r1.sub(r0);
        MutableVector2D N = dv.norm().scaledBy(arc);
        if (N.getY() > 0) {
            N.setY(-N.getY());
        }
        curve.addPoint(r0.add(dv.scaledBy(.33f)).add(N));
        curve.addPoint(r0.add(dv.scaledBy(.66f)).add(N));

        curve.addPoint(r1);
        return curve;
    }
}
