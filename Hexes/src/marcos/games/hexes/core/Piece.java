package marcos.games.hexes.core;

import cc.lib.reflector.Reflector;

public class Piece extends Reflector<Piece> {

	static {
		addAllFields(Piece.class);
	}
	
	int player = 0; // 0 means not assigned to a player
	final int [] v = new int[3];//v0, v1, v2; // v0, v1 is always the base.  v1, v2 is always right side.  v2,v0 is always left side
	Shape type=Shape.NONE; // type of piece.  Never NONE when assigned to a player
	int groupId = 0; // group id
	Shape groupShape=Shape.NONE; // group shape type
	
	public Piece() {
	}
	
	Piece(int v0, int v1, int v2) {
		this.v[0] = v0;
		this.v[1] = v1;
		this.v[2] = v2;
	}

	public final int getPlayer() {
		return player;
	}

	public final Shape getType() {
		return type;
	}

	public final void setType(Shape type) {
		this.type = type;
	}
	
	final int getGroupId() {
		return groupId;
	}

	final Shape getGroupShape() {
		return groupShape;
	}

	@Override
	public String toString() {
		return "[" + v[0] + "," + v[1] + "," + v[2] + "] type=" + type + " player=" + player + " id=" + groupId + " shape=" + groupShape;
	}
	
	
}
