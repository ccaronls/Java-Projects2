package cc.lib.checkerboard;

import cc.lib.game.Utils;

import static cc.lib.checkerboard.Game.FAR;
import static cc.lib.checkerboard.Game.NEAR;
import static cc.lib.checkerboard.PieceType.BISHOP;
import static cc.lib.checkerboard.PieceType.BLOCKED;
import static cc.lib.checkerboard.PieceType.DRAGON_IDLE;
import static cc.lib.checkerboard.PieceType.EMPTY;
import static cc.lib.checkerboard.PieceType.FLAG_DRAGON;
import static cc.lib.checkerboard.PieceType.KNIGHT;
import static cc.lib.checkerboard.PieceType.PAWN_IDLE;

public class DragonChess extends Chess {

    @Override
    void init(Game game) {
        whiteSide = Utils.flipCoin() ? FAR : NEAR;
        // this is to enforce the 'queen on her own color square' rule
        PieceType left = PieceType.QUEEN;
        PieceType right = PieceType.UNCHECKED_KING_IDLE;
        if (whiteSide == FAR) {
            right = PieceType.QUEEN;
            left = PieceType.UNCHECKED_KING_IDLE;
        }

        game.init(10, 8+6);
        game.initRank(0, FAR, BLOCKED, BLOCKED, BLOCKED, DRAGON_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, DRAGON_IDLE, BLOCKED, BLOCKED, BLOCKED);
        game.initRank(1, FAR, BLOCKED, BLOCKED, BLOCKED, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, BLOCKED, BLOCKED, BLOCKED);
        game.initRank(2, -1  , BLOCKED, BLOCKED, BLOCKED, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,EMPTY, EMPTY, EMPTY, BLOCKED, BLOCKED, BLOCKED);
        game.initRank(3, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(4, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(5, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(6, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(7, -1  , BLOCKED, BLOCKED, BLOCKED, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, BLOCKED, BLOCKED, BLOCKED);
        game.initRank(8, NEAR, BLOCKED, BLOCKED, BLOCKED, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, BLOCKED, BLOCKED, BLOCKED);
        game.initRank(9, NEAR, BLOCKED, BLOCKED, BLOCKED, DRAGON_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, DRAGON_IDLE, BLOCKED, BLOCKED, BLOCKED);

        game.setTurn(whiteSide);

    }

    @Override
    protected int[] getRookCastleCols(Game game) {
        return new int [] { 3, 3+7 };
    }

    @Override
    protected boolean isSquareAttacked(Game game, int rank, int col, int attacker) {
        if (super.isSquareAttacked(game, rank, col, attacker))
            return true;

        int [][] kd = pieceDeltas[DRAGON_IDLE.ordinal()][rank][col];
        int [] dr = kd[0];
        int [] dc = kd[1];
        for (int i = 0; i < dr.length; i++) {
            int rr = rank + dr[i];
            int cc = col + dc[i];
            if (testPiece(game, rr, cc, attacker, FLAG_DRAGON))
                return true;
        }

        return false;
    }
}
