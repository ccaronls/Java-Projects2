package cc.lib.game;

import cc.lib.reflector.Reflector;

public final class GDimension extends Reflector<GDimension> implements IDimension {

    public final static GDimension EMPTY = new GDimension();

    static {
        addAllFields(GDimension.class);
    }

    private float width, height;

    public GDimension() {
        this(0, 0);
    }

    public GDimension(IDimension dim) {
        this(dim.getWidth(), dim.getHeight());
    }

    public GDimension(float w, float h) {
        this.width = w;
        this.height = h;
    }

    public GDimension set(float w, float h) {
        width = w;
        height = h;
        return this;
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
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        GDimension og = (GDimension) o;
        return og.width == width && og.height == height;
    }

    @Override
    protected boolean isImmutable() {
        return true;
    }


    public GDimension scaleBy(float sx, float sy) {
        return set(width * sx, height * sy);
    }

    public GDimension scaleBy(float s) {
        return scaleBy(s, s);
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }
}
