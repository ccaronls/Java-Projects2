package cc.lib.game;

import cc.lib.utils.Reflector;

public class GRectangle extends Reflector<GRectangle> {

    static {
        addAllFields(GRectangle.class);
    }

    public GRectangle() {}

    public GRectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width, height;
}
