package cc.game.dominos.core;

import cc.lib.utils.Reflector;

public class Move extends Reflector<Move> {

	public Move() {
		
	}

	Move(Piece pc, int endPoint) {
	    this.piece = pc;
	    this.endpoint = endPoint;
    }

	Piece piece;
	int endpoint;
}
