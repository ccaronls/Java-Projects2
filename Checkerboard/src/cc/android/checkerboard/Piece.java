package cc.android.checkerboard;

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

    boolean hasJumpMove() {
        for (Move m : moves) {
            switch (m.getMoveType()) {
                case JUMP:
                case FLYING_JUMP:
                    return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return type.name() + " pNum=" + playerNum;
    }
}