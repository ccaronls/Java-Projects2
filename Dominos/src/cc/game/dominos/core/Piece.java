package cc.game.dominos.core;

import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public final class Piece extends Reflector<Piece> {

	static {
		addAllFields(Piece.class);
	}
	
	final int num1, num2;

	int openPips;

	public Piece() {
	    this(0,0);
    }

	public Piece(int num1, int num2) {
		this.num1 = num1;
		this.num2 = num2;
		openPips = num1;
	}

	public int getClosedPips() {
	    if (num1 == openPips)
	        return num2;
	    return num1;
    }

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		Piece p = (Piece)o;
		return num1 == p.num1 && num2 == p.num2;
	}

	public final boolean isDouble() {
		return num1 == num2;
	}
	
}
