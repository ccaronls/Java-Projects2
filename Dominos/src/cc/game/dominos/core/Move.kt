package cc.game.dominos.core;

import cc.lib.utils.Reflector;

public class Move extends Reflector<Move> {

    static {
        addAllFields(Move.class);
    }

    public Move() {
        this(null, -1, 1);
    }

	Move(Tile pc, int endPoint, int placement) {
	    this.piece = pc;
	    this.endpoint = endPoint;
        this.placment = placement;
    }

	public final Tile piece;
	public final int endpoint;
    public final int placment;
}
