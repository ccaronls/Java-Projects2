package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public abstract class Rules extends Reflector<Rules> {

    static {
        addAllFields(Rules.class);
    }

    abstract void init(Game game);

    abstract Color getPlayerColor(int side);

    abstract void executeMove(Game game, Move move);

    Player getWinner(Game game) {
        return game.getWinner();
    }

    void reverseMove(Game game, Move m) {
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
        game.setTurn(m.getPlayerNum());
    }

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

    final List<Move> computeMoves(Game game) {
        List<Move> moves = new ArrayList<>();
        boolean hasJumps = false;
        for (int rank = 0; rank < game.getRanks(); rank++) {
            for (int col = 0; col < game.getColumns(); col++) {
                int num = moves.size();
                Piece p = game.getPiece(rank, col);
                if (p.getPlayerNum() == game.getTurn())
                    hasJumps |= computeMovesForSquare(game, rank, col, null, moves);
                game.getPiece(rank, col).numMoves = moves.size() - num;
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
                int num = --game.getPiece(m.getStart()).numMoves;
                if (num < 0)
                    throw new AssertionError();
                it.remove();
            }
        }
        return moves;
    }

    public boolean isJumpsMandatory() { return false; }

    public enum BoardType {
        CHECKERBOARD,
        DAMABOARD,
        OTHER
    }

}
