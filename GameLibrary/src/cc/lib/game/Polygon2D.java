package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public class Polygon2D {

	/**
	 * Initialize the polygon with data
	 * @param pts
	 * @param color
	 * @param radius
	 */
	public Polygon2D(Vector2D [] pts, GColor color, float radius)	{
		this.color = color;
		this.pts = new MutableVector2D[pts.length];
		for (int i=0; i<pts.length; i++)
			this.pts[i] = new MutableVector2D(pts[i]);
		float r = center();
		if (r > 0)
			scale(radius/r);
	}
	
	public void draw(AGraphics g) {
        g.setColor(color);
	    g.begin();
		for (int i=0; i<getNumPts(); i++)
		{
			g.vertex(pts[i]);
		}
		g.drawLineLoop();
	}
	
	public void fill(AGraphics g) {
		for (int i=0; i<getNumPts(); i++)
		{
			g.vertex(pts[i]);
		}
		g.setColor(color);
		g.drawTriangleFan();
	}

	/**
	 * center the polygon points
	 * @return the length of the longest point from center
	 */
	public float center() {
		if (getNumPts() == 0)
			return 0;
		MutableVector2D c = Vector2D.newTemp();
		for (int i=0; i<getNumPts(); i++)
		{
			c.addEq(pts[i]);
		}
		c.scaleEq(1.0f / getNumPts());
		float max_d2 = 0;
		for (int i=0; i<getNumPts(); i++)
		{
			pts[i].subEq(c);
			float d2 = pts[i].dot(pts[i]);
			if (d2 > max_d2)
				max_d2 = d2;
		}
		return (float)Math.sqrt(max_d2);
	}

	/**
	 * scale all points by a scalar
	 * @param s
	 */
	public void scale(float s) {
		for (int i=0; i<getNumPts(); i++) {
			pts[i].scaleEq(s);
		}
	}

	public void translate(float dx, float dy) {
		translate(Vector2D.newTemp(dx, dx));
	}
	
	/**
	 * 
	 * @param dx
	 * @param dy
	 */
	public void translate(Vector2D dv) {
		for (int i=0; i<getNumPts(); i++) {
			pts[i].addEq(dv);
		}
	}
	
	/**
	 *
	 * @return number of points of this polygon
	 */
	public int getNumPts() {
		return pts.length;
	}

	////////////////////////////////////////////////////
	// PRIVATE STUFF ///////////////////////////////////
	////////////////////////////////////////////////////
	
	private GColor color;
	private MutableVector2D [] pts;
}
