package cc.lib.checkerboard;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public abstract class Rules extends Reflector<Rules> {

    static {
        addAllFields(Rules.class);
    }

    Piece lock = null;
    int computedMoves = 0;

    abstract void init(Game game);

    Player getWinner(Game game) {
        return game.getOpponentPlayer();
    }

    abstract Color getPlayerColor(int side);

    abstract void executeMove(Game game, Move move);

    void reverseMove(Game game, Move m, boolean recompute) {
        Piece p;
        switch (m.getMoveType()) {
            case END:
                break;
            case CASTLE:
                p = game.getPiece(m.getCastleRookEnd());
                Utils.assertTrue(p.getType() == PieceType.ROOK, "Expected ROOK was " + p.getType());
                p.setType(PieceType.ROOK_IDLE);
                game.setBoard(m.getCastleRookStart(), p);
                game.clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case FLYING_JUMP:
            case JUMP:
                game.clearPiece(m.getEnd());
                if (m.getCaptured() != null) {
                    Piece piece = game.getPiece(m.getCaptured());
                    game.onPieceCaptured(m.getCaptured(), piece.getType());
                    game.getPlayer(m.getPlayerNum()).captured.add(piece);
                    piece.setRankCol(new int [] { -1, -1 });
                    piece = new Piece(m.getCaptured(), game.getOpponent(m.getPlayerNum()), m.getCapturedType());
                    game.setBoard(m.getCaptured(), piece);
                }
                //fallthrough
            case SWAP:
            case STACK: {
                game.setBoard(m.getStart(), new Piece(m.getStart(), m.getPlayerNum(), m.getStartType()));
                break;
            }
        }

        if (!recompute)
            return;

        Move parent = null;
        game.clearMoves();
        if (game.getTurn() == m.getPlayerNum() && game.undoStack.size() > 0) {
            parent = game.undoStack.peek();
            if (parent.getPlayerNum() != game.getTurn())
                parent = null;
        }
        game.setTurn(m.getPlayerNum());

        if (parent == null) {
            lock = null;
            computedMoves = recomputeMoves(game);
        } else {
            lock = game.getPiece(parent.getStart());
            computedMoves = -1;
            computeMovesForSquare(game, lock.getRank(), lock.getCol(), parent);
            if (!isJumpsMandatory())
                lock.addMove(new Move(MoveType.END, m.getPlayerNum()).setStart(m.getStart()[0], m.getStart()[1], m.getStartType()));
        }
    }

    abstract void computeMovesForSquare(Game game, int rank, int col, Move parent);

    int recomputeMoves(Game game) {
        int num = 0;
        for (int rank = 0; rank < game.ranks; rank++) {
            for (int col = 0; col < game.cols; col++) {
                Piece p = game.getPiece(rank, col);
                p.clearMoves();
                if (p.getPlayerNum() == game.getTurn())
                    computeMovesForSquare(game, rank, col, null);
                num += game.getPiece(rank, col).getNumMoves();
            }
        }
        return num;
    }

    final int computeMoves(Game game, boolean refresh) {
        if (game.getCurrentPlayer().forfeited)
            return 0;
        if (lock == null) {
            if (computedMoves >= 0 && !refresh)
                return computedMoves;
            int num = recomputeMoves(game);
            return computedMoves = num;
        } else {
            return lock.getNumMoves();
        }
    }

    public boolean isJumpsMandatory() { return false; }

    public enum BoardType {
        CHECKERBOARD,
        DAMABOARD,
        OTHER
    }

    public boolean isWinner(Game game, int side) {
        return false;
    }

}
