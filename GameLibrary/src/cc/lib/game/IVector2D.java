package cc.lib.game;

import cc.lib.math.MutableVector2D;

public interface IVector2D {

	float getX();
	float getY();

	default MutableVector2D toMutable() {
	    return new MutableVector2D(this);
    }
}
