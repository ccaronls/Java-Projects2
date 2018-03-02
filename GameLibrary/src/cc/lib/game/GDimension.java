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
	
}
