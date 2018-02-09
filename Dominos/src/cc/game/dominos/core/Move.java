package cc.game.dominos.core;

public class Move {

	Move(Tile pc, int endPoint) {
	    this.piece = pc;
	    this.endpoint = endPoint;
    }

	public final Tile piece;
	public final int endpoint;
}
