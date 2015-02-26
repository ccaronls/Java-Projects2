package cc.game.dominos.core;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class Piece extends Reflector<Piece> implements Comparable<Piece>, IVector2D {

	final int num1, num2;
	int x, y; // center of the piece
	Direction dir = Direction.LEFT_TO_RIGHT; 
	
	// Pieces fit together to forma graph
	
	Piece top, bottom, right, left;

	public Piece(int num1, int num2) {
		this.num1 = num1;
		this.num2 = num2;
	}

	@Override
	public int compareTo(Piece o) {
		if (x == o.x)
			return y=o.y;
		return x-o.x;	
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		Piece p = (Piece)o;
		return num1 == p.num1 && num2 == p.num2;
	}

	@Override
	public float getX() {
		return x;
	}

	@Override
	public float getY() {
		return y;
	}
	
}
