package cc.lib.game;

import cc.lib.utils.Reflector;

public class GDimension extends Reflector<GDimension> implements IDimension {

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

}
