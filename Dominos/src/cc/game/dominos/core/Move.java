package cc.game.dominos.core;

public class Move {

	Move(Tile pc, int endPoint, int placement) {
	    this.piece = pc;
	    this.endpoint = endPoint;
        this.placment = placement;
    }

	public final Tile piece;
	public final int endpoint;
    public final int placment;
}
