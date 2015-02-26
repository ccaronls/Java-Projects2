package cc.game.dominos.core;

public enum Direction {
	RIGHT_TO_LEFT(true, 180),
	LEFT_TO_RIGHT(true, 0),
	TOP_TO_BOTTOM(false, 90),
	BOTTOM_TO_TOP(false, 270);
	
	final boolean horizontal;
	final int angle;
	
	Direction(boolean horizontal, int angle) {
		this.horizontal = horizontal;
		this.angle = angle;
	}
}
