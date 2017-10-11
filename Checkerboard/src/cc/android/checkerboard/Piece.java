package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
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

    public Piece() {
        playerNum = -1;
        type = PieceType.EMPTY;
    }

    Piece(int playerNum, PieceType type) {
        this.playerNum = playerNum;
        this.type = type;
    }

    public int getForward() {
        if (playerNum == ACheckboardGame.BLACK)
            return 1;
        if (playerNum == ACheckboardGame.RED)
            return -1;
        throw new AssertionError();
    }
}