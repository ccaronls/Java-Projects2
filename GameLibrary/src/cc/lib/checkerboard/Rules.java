package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
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
     * Return true if any of the moves are a jump
     *
     * @param game
     * @param rank
     * @param col
     * @param parent
     * @param moves
     * @return
     */
    abstract boolean computeMovesForSquare(Game game, int rank, int col, Move parent, List<Move> moves);

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
                game.clearPiece(m.getEnd());
                if (m.hasCaptured()) {
                    for (int i=0; i<m.getNumCaptured(); i++) {
                        game.getPlayer(m.getPlayerNum()).removeCaptured(m.getCapturedType(i));
                        game.setPiece(m.getCaptured(i), game.getOpponent(m.getPlayerNum()), m.getCapturedType(i));
                    }
                }
                //fallthrough
            case SWAP:
            case STACK: {
                game.setPiece(m.getStart(), m.getPlayerNum(), m.getStartType());
                break;
            }
        }
        if (m.hasOpponentKing()) {
            game.getPiece(m.getOpponentKingPos()).setType(m.getOpponentKingTypeStart());
        }
        game.setTurn(m.getPlayerNum());
    }

    final List<Move> computeMoves(Game game) {
        List<Move> moves = new ArrayList<>();
        boolean hasJumps = false;
        for (int rank = 0; rank < game.getRanks(); rank++) {
            for (int col = 0; col < game.getColumns(); col++) {
                int num = moves.size();
                Piece p = game.getPiece(rank, col);
                if (p.getPlayerNum() == game.getTurn())
                    hasJumps |= computeMovesForSquare(game, rank, col, null, moves);
            }
        }
        if (hasJumps && isJumpsMandatory()) {
            // remove non-jumps
            Iterator<Move> it = moves.iterator();
            while (it.hasNext()) {
                Move m = it.next();
                switch (m.getMoveType()) {
                    case JUMP:
                    case FLYING_JUMP:
                        continue;
                }
                it.remove();
            }
        }
        return moves;
    }

    public boolean isJumpsMandatory() { return false; }
}
