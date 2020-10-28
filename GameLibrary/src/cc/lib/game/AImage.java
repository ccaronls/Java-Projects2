package cc.lib.game;

public abstract class AImage {

    public abstract int getWidth();
    
    public abstract int getHeight();

    public abstract int[] getPixels();

    public float getAspect() {
        return (float)getWidth() / (float)getHeight();
    }
    
}
