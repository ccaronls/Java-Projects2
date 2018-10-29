package cc.lib.checkers;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    public int playerNum;
    public PieceType type;
    public final List<Move> moves = new ArrayList<>();

    boolean captured = false;

    public Piece() {
        playerNum = -1;
        type = PieceType.EMPTY;
    }

    Piece(int playerNum, PieceType type) {
        this.playerNum = playerNum;
        this.type = type;
    }
}