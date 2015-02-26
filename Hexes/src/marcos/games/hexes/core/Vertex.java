package marcos.games.hexes.core;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

public class Vertex extends Reflector<Vertex> implements IVector2D {

	static {
		addAllFields(Vertex.class);
	}
	
	public Vertex() {
		this(0,0);
	}
	
	public Vertex(int x, int y) {
		this.x = x;
		this.y = y;
	}

	final int x,y;
	final int [] p = new int[6];
	private int n = 0;
	
	void addPiece(int index) {
		p[n++] = index;
	}
	
	public final int getNum() {
		return n;
	}
	
	@Override
	public float getX() {
		return x;
	}
	@Override
	public float getY() {
		return y;
	}

	@Override
	public String toString() {
		return "[" + x + "," + y + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this== obj)
			return true;
		Vertex v = (Vertex)obj;
		return x==v.x && y==v.y;
	}

	
}
