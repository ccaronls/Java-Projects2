package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

import static cc.lib.checkerboard.Game.*;
import static cc.lib.checkerboard.PieceType.*;

public class Checkers extends Rules {

    @Override
    void init(Game game) {
        game.init(8, 8);
        game.initRank(0, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(1, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.initRank(2, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(5, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.initRank(6, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(7, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.setTurn(Utils.flipCoin() ? FAR : NEAR);
    }

    @Override
    boolean computeMovesForSquare(Game game, int rank, int col, Move parent, List<Move> moves) {
        Piece p = game.getPiece(rank, col);
        if (p.getPlayerNum() != game.getTurn())
            throw new AssertionError();

        int startSize = moves.size();
        try {
            if (p.getType() == FLYING_KING || p.getType() == DAMA_KING) {
                return computeFlyingKingMoves(game, p, rank, col, parent, moves);
            } else {
                return computeMenKingMoves(game, p, rank, col, parent, moves);
            }
        } finally {
            p.numMoves = moves.size() - startSize;
        }
    }

    @Override
    int getWinner(Game game) {
        if (game.getMoves().size() == 0)
            return game.getOpponent();
        return -1;
    }

    @Override
    boolean isDraw(Game game) {
        // if down to onlyt 2 kings, one of each color, then game is a draw
        int numNear=0;
        int numFar=0;
        for (int r = 0; r < game.getRanks(); r++) {
            for (int c = 0; c < game.getColumns(); c++) {
                Piece p;
                switch ((p = game.getPiece(r, c)).getType()) {

                    case EMPTY:
                        break;
                    case KING:
                    case FLYING_KING:
                    case DAMA_KING:
                        if (p.getPlayerNum() == NEAR && ++numNear > 1)
                            return false;
                        else if (p.getPlayerNum() == FAR && ++numFar > 1)
                            return false;
                        break;
                    case CHECKER:
                    case DAMA_MAN:
                        return false;
                    default:
                        throw new AssertionError("Unhandled case: " + p.getType());
                }
            }
        }
        return true;
    }

    boolean computeMenKingMoves(Game game, Piece p, int rank, int col, Move parent, List<Move> moves) {
        boolean hasJump = false;
        int [] jdr=null, jdc=null, dr=null, dc=null;
        switch (p.getType()) {
            case KING:
                jdr = dr = new int[] { 1, 1, -1, -1 };
                jdc = dc = new int[] { -1, 1, -1, 1 };
                break;
            case CHECKER:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = new int [] { -1, -1 };
                    dc = new int [] { -1, 1 };
                } else { // red
                    // positive
                    dr = new int [] { 1, 1 };
                    dc = new int [] { -1, 1 };
                }
                if (canMenJumpBackwards()) {
                    jdr = new int[]{1, 1, -1, -1};
                    jdc = new int[]{-1, 1, -1, 1};
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case DAMA_MAN:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = new int [] { -1, 0, 0 };
                    dc = new int [] {  0, 1, -1 };
                } else { // red
                    // positive
                    dr = new int [] { 1, 0, 0 };
                    dc = new int [] { 0, 1, -1 };
                }
                if (canMenJumpBackwards()) {
                    jdr = new int[] { 1, -1, 0, 0};
                    jdc = new int[] { 0,  0, -1, 1};
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case DAMA_KING:
                jdr = dr = new int[] { 1, -1, 0, 0};
                jdc = dc = new int[] { 0,  0, -1, 1};
                break;
            default:
                Utils.unhandledCase(p.getType());
        }

        // check for jumps
        for (int i=0; i<jdr.length; i++) {
            final int rdr  = rank+jdr[i];
            final int cdc  = col +jdc[i];
            final int rdr2 = rank+jdr[i]*2;
            final int cdc2 = col +jdc[i]*2;

            if (!game.isOnBoard(rdr, cdc))
                continue;

            if (!game.isOnBoard(rdr2, cdc2))
                continue;

            if (parent != null) {
                if (rdr2 == parent.getStart()[0] && cdc2 == parent.getStart()[1])
                    continue; // cannot make 180 degree turns
            }

            Piece cap = game.getPiece(rdr, cdc);
            Piece t = game.getPiece(rdr2, cdc2);
            if (t.getType() != EMPTY)
                continue;

            if (canJumpSelf() && cap.getPlayerNum() == game.getTurn()) {
                moves.add(new Move(MoveType.JUMP, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr2, cdc2, p.getType()));
                hasJump = true;
            } else if (!cap.isCaptured() && cap.getPlayerNum() == game.getOpponent()) {
                Move mv = new Move(MoveType.JUMP, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr2, cdc2, p.getType());
                if (!isNoCaptures())
                    mv.addCaptured(rdr, cdc, cap.getType());
                moves.add(mv);
                hasJump = true;
            }

        }

        // check for slides
        if (parent == null && !(isJumpsMandatory() && p.getNumMoves()>0)) {
            for (int i = 0; i < dr.length; i++) {
                final int rdr = rank + dr[i];
                final int cdc = col + dc[i];

                if (!game.isOnBoard(rdr, cdc))
                    continue;
                // t is piece one unit away in this direction
                Piece t = game.getPiece(rdr, cdc);
                if (t.getType() == EMPTY) {
                    moves.add(new Move(MoveType.SLIDE, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()));
                    //new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
                }
            }
        }
        return hasJump;
    }

    /*
    flying kings move any distance along unblocked diagonals, and may capture an opposing man any distance away
    by jumping to any of the unoccupied squares immediately beyond it.

    There are 2 cases to consider determined by: isCaptureAtEndEnabled()
    1> (Simple) Pieces are removed from the board as they are jumped or
    2> (Complex) Jumped pieces remain on the board until the turn is complete, in which case,
       it is possible to reach a position in a multi-jump move where the flying king is blocked
       from capturing further by a piece already jumped.
    */
    boolean computeFlyingKingMoves(Game game, Piece p, int rank, int col, Move parent, List<Move> moves) {
        boolean hasJump = false;
        final int d = Math.max(game.getRanks(), game.getColumns());

        int [] dr, dc;
        switch (p.getType()) {
            case FLYING_KING:
                dr = new int[] { 1, 1, -1, -1 };
                dc = new int[] { -1, 1, -1, 1 };
                break;
            case DAMA_KING:
                dr = new int[] { 1, -1, 0, 0 };
                dc = new int[] { 0,  0, -1, 1 };
                break;
            default:
                throw new AssertionError("Unhandled case");
        }

        for (int i=0; i<4; i++) {
            MoveType mt = MoveType.SLIDE;
            Piece captured = null;
            int capturedRank=0;
            int capturedCol =0;

            int ii=1;
            if (parent != null) {
                // we are in a multijump, so search forward for the piece to capture
                int ddr = CMath.signOf(parent.getStart()[0] - rank);
                int ddc = CMath.signOf(parent.getStart()[1] - col);

                if (ddr == dr[i] && ddc == dc[i])
                    continue; // cannot go backwards

                for ( ; ii<=d; ii++) {
                    // square we are moving too
                    final int rdr = rank+dr[i]*ii;
                    final int cdc = col+dc[i]*ii;

                    if (!game.isOnBoard(rdr, cdc))
                        break;

                    Piece t = game.getPiece(rdr, cdc);
                    if (t.getType() == EMPTY)
                        continue;

                    if (t.isCaptured())
                        break; // cannot jump a piece we already did

                    if (t.getPlayerNum() == game.getOpponent()) {
                        captured = t;
                        ii++;
                        capturedRank=rdr;
                        capturedCol=cdc;
                    }
                    break;
                }

                if (captured == null)
                    continue;

                mt = MoveType.FLYING_JUMP;
            }

            for ( ; ii<d; ii++) {

                // square we are moving too
                final int rdr = rank+dr[i]*ii;
                final int cdc = col+dc[i]*ii;

                if (!game.isOnBoard(rdr, cdc))
                    break;

                // t is piece one unit away in this direction
                Piece t = game.getPiece(rdr, cdc);

                if (t.getType() == EMPTY) {
                    if (captured == null)
                        moves.add(new Move(mt, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()));
                    else
                        moves.add(new Move(mt, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()).addCaptured(capturedRank, capturedCol, captured.getType()));
                    if (mt == MoveType.FLYING_JUMP)
                        hasJump = true;
                    continue;
                }

                if (mt != MoveType.SLIDE)
                    break;

                if (t.getPlayerNum() != game.getOpponent())
                    break;

                mt = MoveType.FLYING_JUMP;
                captured = t;
                capturedRank = rdr;
                capturedCol = cdc;

            }
        }
        return hasJump;
    }

    @Override
    void executeMove(Game game, Move move) {
        if (move.getPlayerNum() != game.getTurn())
            throw new AssertionError();
        boolean isKinged = false;
        boolean isDamaKing = false;
        Piece p = game.getPiece(move.getStart());
        // clear everyone all moves
        if (move.hasEnd()) {
            if (isKingPieces()) {
                int rank = move.getEnd()[0];
                isKinged = (p.getType() == CHECKER && game.getStartRank(game.getOpponent()) == rank);
                isDamaKing = (p.getType() == DAMA_MAN && game.getStartRank(game.getOpponent()) == rank);
            }
            game.movePiece(move);
            if (move.getEnd() != null) {
                p = game.getPiece(move.getEnd());
            }
        }

        switch (move.getMoveType()) {
            case SLIDE:
                if (isKinged) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                    break;
                }
                if (isDamaKing) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1],  PieceType.DAMA_KING));
                    break;
                }
            case END:
                endTurnPrivate(game);
                return;
            case FLYING_JUMP:
            case JUMP:
                if (move.hasCaptured()) {
                    if (isCaptureAtEndEnabled()) {
                        game.getPiece(move.getLastCaptured()).setCaptured(true);
                    } else {
                        game.getPlayer(move.getPlayerNum()).addCaptured(move.getLastCapturedType());
                        game.clearPiece(move.getLastCaptured());
                    }
                    if ((isKinged || isDamaKing) && isJumpsMandatory()) {
                        // we cannot king if we can still jump.
                        computeMovesForSquare(game, move.getEnd()[0], move.getEnd()[1], move, game.getMovesInternal());
                        Piece pp = game.getPiece(move.getEnd());
                        if (pp.getNumMoves() > 0) {
                            break;
                        }
                    }
                }
                if (isKinged) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                }
                if (isDamaKing) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()).setEnd(move.getEnd()[0], move.getEnd()[1], PieceType.DAMA_KING));
                }
                break;
            case STACK:
                game.setPiece(move.getStart(), move.getPlayerNum(), move.getEndType());
                break;
        }

        if (!isKinged && !isDamaKing) {
            // recursive compute next move if possible after a jump
            if (move.hasEnd())
                computeMovesForSquare(game, move.getEnd()[0], move.getEnd()[1], move, game.getMovesInternal());
            if (p.getNumMoves() == 0) {
                endTurnPrivate(game);
            } else if (!isJumpsMandatory()) {
                game.getMovesInternal().add(new Move(MoveType.END, move.getPlayerNum()).setStart(move.getEnd()[0], move.getEnd()[1], p.getType()));
            }
        }
    }

    void endTurnPrivate(Game game) {
        List<int[]> captured = new ArrayList<>();
        if (!isNoCaptures()) {
            for (int i = 0; i < game.getRanks(); i++) {
                for (int ii = 0; ii < game.getColumns(); ii++) {
                    Piece p = game.getPiece(i, ii);
                    if (p.isCaptured()) {
                        captured.add(new int[]{i, ii});
                    }
                }
            }
            if (isCaptureAtEndEnabled() && captured.size() > 0) {
                for (int[] pos : captured) {
                    game.getCurrentPlayer().addCaptured(game.getPiece(pos).getType());
                    game.clearPiece(pos);
                }
            }
        }
        game.nextTurn();
    }

    @Override
    public Color getPlayerColor(int side) {
        switch (side) {
            case FAR:
                return Color.RED;
            case NEAR:
                return Color.BLACK;
        }
        return Color.WHITE;
    }

    @Override
    public long evaluate(Game game, Move move) {
        if (game.isDraw())
            return 0;
        int winner;
        switch (winner=game.getWinnerNum()) {
            case NEAR:
            case FAR:
                return (winner == move.getPlayerNum() ? Long.MAX_VALUE : Long.MIN_VALUE);
        }
        long value = 0; // no its not game.getMoves().size(); // move options is good
        //if (move.hasCaptured())
        //    value += 1000;
        for (int r = 0; r<game.getRanks(); r++) {
            for (int c = 0; c<game.getColumns(); c++) {
                Piece p = game.getPiece(r, c);
                final int scale = p.getPlayerNum() == move.getPlayerNum() ? 1 : -1;
                switch (p.getType()) {

                    case EMPTY:
                        value += 1 * scale;
                        break;
                    case CHECKER:
                    case DAMA_MAN:
                        value += 2 * scale;
                        break;

                    case KING:
                        value += 10 * scale;
                        break;
                    case FLYING_KING:
                    case DAMA_KING:
                        value += 50 * scale;
                        break;

                    default:
                        throw new AssertionError("Unhandled case '" + p.getType() + "'");
                }
            }
        }
        return value;
    }

    public boolean canJumpSelf() {
        return true; // true for traditional checkers
    }

    public boolean canMenJumpBackwards() {
        return false; // true for international/russian draughts
    }

    /**
     * Men/King must jump when possible
     * @return
     */
    public boolean isJumpsMandatory() {
        return false;
    }

    /**
     * Men/King must take moves that lead to most jumps
     * @return
     */
    public boolean isMaxJumpsMandatory() {
        return false;
    }

    public boolean isCaptureAtEndEnabled() {
        return false;
    }

    public boolean isFlyingKings() {
        return false;
    }

    public boolean isNoCaptures() {
        return false;
    }

    public boolean isKingPieces() {
        return true;
    }
}
