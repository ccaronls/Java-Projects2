package cc.lib.game;

import cc.lib.math.Vector2D;

public interface IDimension {
    float getWidth();
    float getHeight();
    default float getAspect() {
        return getWidth()/getHeight();
    }
    default IVector2D getCenter() { return new Vector2D(getWidth()/2, getHeight()/2); }
}
