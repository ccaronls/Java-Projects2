package cc.lib.checkerboard;

import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;

import static cc.lib.checkerboard.Game.FAR;
import static cc.lib.checkerboard.Game.NEAR;

/**
 * http://www.geekyhobbies.com/kings-court-1986-board-game-review-and-rules/
 */
public class KingsCourt extends Checkers {

    @Override
    void init(Game game) {
        game.init(8, 8);
        game.clear();
        int pnum = Game.NEAR;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (r >= 2 && r <= 5 && c >= 2 && c <= 5)
                    continue; // no pieces on the court
                game.setPiece(r, c, pnum, PieceType.CHIP_4WAY);
                pnum = (pnum + 1) % 2;
            }
            pnum = (pnum + 1) % 2;
        }
        game.setTurn(Utils.flipCoin() ? Game.NEAR : Game.FAR);
        game.setInitialized(true);
    }

    @Override
    int getWinner(Game game) {
        if (game.getMoveHistoryCount() < 2)
            return -1;

        // if a player has no pieces in the court, they are a loser
        int numNear = 0;
        int numFar = 0;
        for (int r = 2; r < 6; r++) {
            for (int c = 2; c < 6; c++) {
                Piece p = game.getPiece(r, c);
                if (p.getType() != PieceType.EMPTY) {
                    if (p.getPlayerNum() == Game.NEAR) {
                        if (numFar > 0)
                            return -1;
                        numNear++;
                    } else {
                        if (numNear > 0)
                            return -1;
                        numFar++;
                    }
                }
            }
        }
        if (numNear == 0 && numFar == 0)
            return -1;
        if (numNear > 0)
            return Game.NEAR;
        return Game.FAR;
    }

    @Override
    List<Move> computeMoves(Game game) {
        List<Move> moves = super.computeMoves(game);
        if (game.getMoveHistoryCount() < 2) {
            // remove anything with a jump or capture.
            Iterator<Move> it = moves.iterator();
            while (it.hasNext()) {
                Move m = it.next();
                if (m.hasCaptured()) {
                    it.remove();
                }
            }
            if (moves.size() < 1)
                throw new AssertionError("Bad state");
        }
        return moves;
    }

    static int [][] distToCourtTable = {
            { 4, 3, 2, 2, 2, 2, 3, 4 },
            { 3, 2, 1, 1, 1, 1, 2, 3 },
            { 2, 1, 0, 0, 0, 0, 1, 2 },
            { 2, 1, 0, 0, 0, 0, 1, 2 },
            { 2, 1, 0, 0, 0, 0, 1, 2 },
            { 2, 1, 0, 0, 0, 0, 1, 2 },
            { 3, 2, 1, 1, 1, 1, 2, 3 },
            { 4, 3, 2, 2, 2, 2, 3, 4 },
    };

    @Override
    public long evaluate(Game game, Move move) {
        if (game.isDraw())
            return 0;
        int winner;
        switch (winner = game.getWinnerNum()) {
            case NEAR:
            case FAR:
                return (winner == move.getPlayerNum() ? Long.MAX_VALUE : Long.MIN_VALUE);
        }
        long value = 0; // no its not game.getMoves().size(); // move options is good
        //if (move.hasCaptured())
        //    value += 1000;
        for (int r = 0; r < game.getRanks(); r++) {
            for (int c = 0; c < game.getColumns(); c++) {
                Piece p = game.getPiece(r, c);
                final int scale = p.getPlayerNum() == move.getPlayerNum() ? 1 : -1;
                switch (p.getType()) {
                    case EMPTY:
                        break;
                    case CHIP_4WAY:
                        value += scale * (10 - distToCourtTable[r][c]);
                        break;
                    default:
                        throw new AssertionError("Unhandled case '" + p.getType() + "'");
                }

            }
        }
        return value;
    }

    @Override
    public boolean isKingPieces() {
        return false;
    }

}
