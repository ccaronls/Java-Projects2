package cc.lib.game;

import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public final class GDimension extends Reflector<GDimension> implements IDimension, IRectangle {

    public final static GDimension EMPTY = new GDimension();

    static {
        addAllFields(GDimension.class);
    }

	public final float width, height;

	public GDimension() {
		this(0,0);
	}

	public GDimension(IDimension dim) {
	    this(dim.getWidth(), dim.getHeight());
    }
	
	public GDimension(float w, float h) {
		this.width = w;
		this.height = h;
	}

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public final boolean equals(Object o) {
	    if (o == this)
	        return true;
	    if (o == null)
	        return false;
        GDimension og = (GDimension)o;
        return og.width == width && og.height == height;
    }

    @Override
    protected boolean isImmutable() {
        return true;
    }

    public float getAspect() {
	    return width/height;
    }

    /**
     * Return the rectangular region that encompasses this rectangle if it were to be rotated.
     * For instance, if a 4x2 rect were rotated 45 degrees, then the resulting rectangle would be approx 4.2x4.2
     * @param degrees
     * @return
     */
    public GDimension rotated(float degrees) {
        Vector2D tl = new Vector2D(-width/2, -height/2).rotate(degrees);
        Vector2D tr = new Vector2D(width/2, -height/2).rotate(degrees);

        float newWidth = Math.max(Math.abs(tl.getX()), Math.abs(tr.getX()))*2;
        float newHeight = Math.max(Math.abs(tl.getY()), Math.abs(tr.getY()))*2;

        return new GDimension(newWidth, newHeight);
    }

    public GDimension adjustedBy(float dw, float dh) {
        return new GDimension(width + dw, height + dh);
    }

    public GDimension interpolateTo(GDimension other, float factor) {
        float w = width + (other.width - width) * factor;
        float h = height + (other.height - height) * factor;
        return new GDimension(w, h);
    }

    public GDimension addVert(GDimension d) {
        return new GDimension(Math.max(width, d.width), height+d.height);
    }

    public GDimension addHorz(GDimension d) {
        return new GDimension(width+d.width, Math.max(height, d.height));
    }

    public MutableVector2D getCenter() {
        return new MutableVector2D(width/2, height/2);
    }

    public Float minLength() {
        return Math.min(width, height);
    }

    @Override
    public float X() {
        return 0;
    }

    @Override
    public float Y() {
        return 0;
    }

    @Override
    public String toString() {
        return "GDimension{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
