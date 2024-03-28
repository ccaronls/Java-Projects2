package cc.lib.game;

public abstract class AImage implements IDimension {

    public abstract int[] getPixels();

    public abstract void draw(AGraphics g, float x, float y);
}
