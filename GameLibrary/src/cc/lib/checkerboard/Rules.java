package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Base class for logical rules to govern a checkerboard style game.
 */
public abstract class Rules extends Reflector<Rules> {

    static {
        addAllFields(Rules.class);
    }

    /**
     * setup pieces and choose side.
     * @param game
     */
    abstract void init(Game game);

    /**
     * Return a color for the side
     * @param side
     * @return
     */
    abstract Color getPlayerColor(int side);

    /**
     * Perform the logical move
     * @param game
     * @param move
     */
    abstract void executeMove(Game game, Move move);

    /**
     * return the playerNum >= 0 is there is a winner, < 0 otherwise.
     * @param game
     * @return
     */
    abstract int getWinner(Game game);

    /**
     * Return true if the current state of the game is a tie
     * @param game
     * @return
     */
    abstract boolean isDraw(Game game);

    /**
     *
     * @param game
     * @param move
     * @return
     */
    public abstract long evaluate(Game game, Move move);

    /**
     *
     * @param game
     * @return
     */
    abstract List<Move> computeMoves(Game game);

    /**
     *
     * @param game
     * @param m
     */
    final void reverseMove(Game game, Move m) {
        Piece p;
        switch (m.getMoveType()) {
            case END:
                break;
            case CASTLE:
                p = game.getPiece(m.getCastleRookEnd());
                Utils.assertTrue(p.getType() == PieceType.ROOK, "Expected ROOK was " + p.getType());
                game.setPiece(m.getCastleRookStart(), m.getPlayerNum(), PieceType.ROOK_IDLE);
                game.clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case FLYING_JUMP:
            case JUMP:
                p = game.getPiece(m.getEnd());
                if (p.isStacked()) {
                    Piece captured = game.getPiece(m.getCaptured(0));
                    if (captured.getType() != PieceType.EMPTY) {
                        captured.addStackFirst(captured.getPlayerNum());
                    } else {
                        captured.setType(m.getCapturedType(0));
                    }
                    captured.setPlayerNum(p.removeStackLast());
                } else {
                    if (m.hasCaptured()) {
                        for (int i = 0; i < m.getNumCaptured(); i++) {
                            game.getPlayer(m.getPlayerNum()).removeCaptured(m.getCapturedType(i));
                            game.setPiece(m.getCaptured(i), game.getOpponent(m.getPlayerNum()), m.getCapturedType(i));
                        }
                    }
                }
                //fallthrough
            case SWAP:
            case STACK: {
                game.copyPiece(m.getEnd(), m.getStart());//setPiece(m.getStart(), m.getPlayerNum(), m.getStartType());
                game.clearPiece(m.getEnd());
                break;
            }
        }
        if (m.hasOpponentKing()) {
            game.getPiece(m.getOpponentKingPos()).setType(m.getOpponentKingTypeStart());
        }
        game.setTurn(m.getPlayerNum());
    }


}
