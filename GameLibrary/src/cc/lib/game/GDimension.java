package cc.lib.game;

import cc.lib.utils.Reflector;

public class GDimension extends Reflector<GDimension> {

    public final static GDimension EMPTY = new GDimension();

    static {
        addAllFields(GDimension.class);
    }

	public final float width, height;
	
	public GDimension() {
		this(0,0);
	}
	
	public GDimension(float w, float h) {
		this.width = w;
		this.height = h;
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
}
