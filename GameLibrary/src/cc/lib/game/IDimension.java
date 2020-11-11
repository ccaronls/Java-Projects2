package cc.lib.game;

public interface IDimension {
    float getWidth();
    float getHeight();
    default float getAspect() {
        return getWidth()/getHeight();
    }
}
