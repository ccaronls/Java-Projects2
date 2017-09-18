package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Move extends Reflector<Move> {

    static {
        addAllFields(Move.class);
    }

    public final Checkers.MoveType type;
    public final int startRank, startCol;
    public final int endRank, endCol;
    public final int captureRank, captureCol;
    public final int playerNum;

    public Move() {
        this(null, 0, 0, 0, 0, 0, 0, 0);
    }

    public Move(Checkers.MoveType type, int startRack, int startCol, int endRank, int endCol, int captureRank, int captureCol, int playerNum) {
        this.type = type;
        this.startRank = startRack;
        this.startCol = startCol;
        this.endRank = endRank;
        this.endCol = endCol;
        this.captureCol = captureCol;
        this.captureRank = captureRank;
        this.playerNum = playerNum;
    }
}