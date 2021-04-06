package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.math.CMath;
import cc.lib.utils.GException;
import cc.lib.utils.Table;

import static cc.lib.checkerboard.Game.*;
import static cc.lib.checkerboard.PieceType.*;

public class Checkers extends Rules {

    private final static int [] PIECE_DELTAS_DIAGONALS_R = new int[] { 1, 1, -1, -1 };
    private final static int [] PIECE_DELTAS_DIAGONALS_C = new int[] { -1, 1, -1, 1 };

    private final static int [] PIECE_DELTAS_DIAGONALS_NEAR_R = new int[] { -1, -1 };
    private final static int [] PIECE_DELTAS_DIAGONALS_NEAR_C = new int[] { -1,  1 };

    private final static int [] PIECE_DELTAS_DIAGONALS_FAR_R = new int[] {  1, 1 };
    private final static int [] PIECE_DELTAS_DIAGONALS_FAR_C = new int[] { -1,  1 };

    private final static int [] PIECE_DELTAS_4WAY_R = new int[] { 1, -1,  0, 0 };
    private final static int [] PIECE_DELTAS_4WAY_C = new int[] { 0,  0, -1, 1 };

    private final static int [] PIECE_DELTAS_3WAY_NEAR_R = new int[] { -1,  0, 0 };
    private final static int [] PIECE_DELTAS_3WAY_NEAR_C = new int[] {  0,  -1, 1 };

    private final static int [] PIECE_DELTAS_3WAY_FAR_R = new int[] { 1,  0, 0 };
    private final static int [] PIECE_DELTAS_3WAY_FAR_C = new int[] { 0, -1, 1 };

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

    List<Move> computeMoves(Game game) {
        List<Move> moves = new ArrayList<>();
        int numJumps = 0;
        for (int rank = 0; rank < game.getRanks(); rank++) {
            for (int col = 0; col < game.getColumns(); col++) {
                Piece p = game.getPiece(rank, col);
                if (p == null)
                    throw new GException("Null piece at [" + rank + "," + col + "]");
                if (p.getPlayerNum() == game.getTurn())
                    numJumps += computeMovesForSquare(game, rank, col, null, moves);
            }
        }
        if (numJumps > 0 && (isJumpsMandatory() || isMaxJumpsMandatory())) {
            // remove non-jumps
            Iterator<Move> it = moves.iterator();
            int maxDepth = 0;
            while (it.hasNext()) {
                Move m = it.next();
                switch (m.getMoveType()) {
                    case JUMP:
                    case FLYING_JUMP:
                        if (isMaxJumpsMandatory()) {
                            m.jumpDepth = findMaxDepth(game.getTurn(), game, m);
                            maxDepth = Math.max(maxDepth, m.jumpDepth);
                        } else if (m.getJumped() >= 0) {
                            Piece p = game.getPiece(m.getJumped());
                            if (p.getPlayerNum() == m.getPlayerNum())
                                break; // remove jumps of our own pieces
                        }
                        continue;
                }
                it.remove();
            }
            if (isMaxJumpsMandatory()) {
                it = moves.iterator();
                while (it.hasNext()) {
                    Move m = it.next();
                    if (m.jumpDepth < maxDepth) {
                        it.remove();
                    }
                }
            }
        }
        return moves;
    }

    private int findMaxDepth(int playerNum, Game game, Move m) {
        if (m.getPlayerNum() != playerNum)
            return 0;
        game.movePiece(m);
        game.clearPiece(m.getLastCaptured().getPosition());
        //executeMove(game, m);
        int max = 0;
        List<Move> moves = new ArrayList<>();
        final int pos = m.getEnd();
        final int rnk = pos >> 8;
        final int col = pos & 0xff;
        final int numJumps = computeMovesForSquare(game, rnk, col, m, moves);
        if (numJumps > 0) {
            for (Move m2 : moves) {
                switch (m2.getMoveType()) {
                    case JUMP:
                    case FLYING_JUMP:
                        max = Math.max(max, 1 + findMaxDepth(playerNum, game, m2));
                }
            }
        }
        reverseMove(game, m);
        return max;
    }

    private final int computeMovesForSquare(Game game, int rank, int col, Move parent, List<Move> moves) {
        Piece p = game.getPiece(rank, col);
        if (p.getPlayerNum() != game.getTurn())
            throw new GException("Logic Error: Should not be able to move opponent piece");

        int startSize = moves.size();
        int numJumps = 0;
        if (p.getType().isFlying()) {
            numJumps = computeFlyingKingMoves(game, p, rank, col, parent, moves);
        } else {
            numJumps = computeMenKingMoves(game, p, rank, col, parent, moves);
        }
        p.numMoves = moves.size() - startSize;
        return numJumps;
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
                    case CHIP_4WAY:
                        return false;
                    default:
                        throw new GException("Unhandled case: " + p.getType());
                }
            }
        }
        return true;
    }

    int computeMenKingMoves(Game game, Piece p, int rank, int col, Move parent, List<Move> moves) {
        int numJumps = 0;
        int [] jdr=null, jdc=null, dr=null, dc=null;
        switch (p.getType()) {
            case KING:
                jdr = dr = PIECE_DELTAS_DIAGONALS_R;
                jdc = dc = PIECE_DELTAS_DIAGONALS_C;
                break;
            case CHECKER:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = PIECE_DELTAS_DIAGONALS_NEAR_R;
                    dc = PIECE_DELTAS_DIAGONALS_NEAR_C;
                } else { // red
                    // positive
                    dr = PIECE_DELTAS_DIAGONALS_FAR_R;
                    dc = PIECE_DELTAS_DIAGONALS_FAR_C;
                }
                if (canMenJumpBackwards()) {
                    jdr = PIECE_DELTAS_DIAGONALS_R;
                    jdc = PIECE_DELTAS_DIAGONALS_C;
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case DAMA_MAN:
                if (p.getPlayerNum() == NEAR) {
                    // negative
                    dr = PIECE_DELTAS_3WAY_NEAR_R;
                    dc = PIECE_DELTAS_3WAY_NEAR_C;
                } else { // red
                    // positive
                    dr = PIECE_DELTAS_3WAY_FAR_R;
                    dc = PIECE_DELTAS_3WAY_FAR_C;
                }
                if (canMenJumpBackwards()) {
                    jdr = PIECE_DELTAS_4WAY_R;
                    jdc = PIECE_DELTAS_4WAY_C;
                } else {
                    jdr = dr;
                    jdc = dc;
                }
                break;
            case CHIP_4WAY:
            case DAMA_KING:
                jdr = dr = PIECE_DELTAS_4WAY_R;
                jdc = dc = PIECE_DELTAS_4WAY_C;
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
                int pos = parent.getStart();
                int srnk = pos >> 8;
                int scol = pos & 0xff;
                if (rdr2 == srnk && cdc2 == scol)
                    continue; // cannot make 180 degree turns
            }

            Piece cap = game.getPiece(rdr, cdc);
            Piece t = game.getPiece(rdr2, cdc2);
            if (t.getType() != EMPTY)
                continue;

            if (canJumpSelf() && cap.getPlayerNum() == game.getTurn()) {
                moves.add(
                        new Move(MoveType.JUMP, game.getTurn())
                                .setStart(rank, col, p.getType())
                                .setEnd(rdr2, cdc2, p.getType())
                                .setJumped(rdr, cdc)
                );
                //numJumps++;
            } else if (!cap.isCaptured() && cap.getPlayerNum() == game.getOpponent()) {
                Move mv = new Move(MoveType.JUMP, game.getTurn())
                        .setStart(rank, col, p.getType())
                        .setEnd(rdr2, cdc2, p.getType())
                        .setJumped(rdr, cdc);
                if (!isNoCaptures())
                    mv.addCaptured(rdr, cdc, cap.getType());
                moves.add(mv);
                numJumps++;
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
                if (t == null)
                    throw new GException("Null piece at [" + rdr + "," + cdc + "]");
                if (t.getType() == EMPTY) {
                    moves.add(new Move(MoveType.SLIDE, game.getTurn()).setStart(rank, col, p.getType()).setEnd(rdr, cdc, p.getType()));
                    //new Move(MoveType.SLIDE, rank, col, rdr, cdc, getTurn()));
                }
            }
        }
        return numJumps;
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
    int computeFlyingKingMoves(Game game, Piece p, int rank, int col, Move parent, List<Move> moves) {
        int numJumps = 0;
        final int d = Math.max(game.getRanks(), game.getColumns());

        int [] dr, dc;
        switch (p.getType()) {
            case FLYING_KING:
                dr = PIECE_DELTAS_DIAGONALS_R;
                dc = PIECE_DELTAS_DIAGONALS_C;
                break;
            case DAMA_KING:
                dr = PIECE_DELTAS_4WAY_R;
                dc = PIECE_DELTAS_4WAY_C;
                break;
            default:
                throw new GException("Unhandled case");
        }

        for (int i=0; i<4; i++) {
            MoveType mt = MoveType.SLIDE;
            Piece captured = null;
            int capturedRank=0;
            int capturedCol =0;

            int ii=1;
            if (parent != null) {
                // we are in a multijump, so search forward for the piece to capture
                final int pos = parent.getStart();
                final int srnk = pos >> 8;
                final int scol = pos & 0xff;
                final int ddr = CMath.signOf(srnk - rank);
                final int ddc = CMath.signOf(scol - col);

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
                        numJumps++;
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
        return numJumps;
    }

    @Override
    void executeMove(Game game, Move move) {
        if (move.getPlayerNum() != game.getTurn())
            throw new GException();
        boolean isKinged = false;
        boolean isDamaKing = false;
        Piece p = game.getPiece(move.getStart());
        // clear everyone all moves
        final int epos = move.getEnd();
        final int ernk = epos >> 8;
        final int ecol = epos & 0xff;
        if (move.hasEnd()) {
            if (isKingPieces()) {
                isKinged = (p.getType() == CHECKER && game.getStartRank(game.getOpponent()) == ernk);
                isDamaKing = (p.getType() == DAMA_MAN && game.getStartRank(game.getOpponent()) == ernk);
            }
            p = game.movePiece(move);
        }

        switch (move.getMoveType()) {
            case SLIDE:
                if (isKinged) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                    break;
                }
                if (isDamaKing) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol,  PieceType.DAMA_KING));
                    break;
                }
            case END:
                endTurnPrivate(game);
                return;
            case FLYING_JUMP:
            case JUMP:
                if (move.hasCaptured()) {
                    if (isStackingCaptures()) {
                        Piece captured = game.getPiece(move.getLastCaptured().getPosition());
                        // capturing end stack becomes start stack
                        //capturing.addStackLast(captured.getPlayerNum());
                        if (!captured.isStacked()) {
                            game.clearPiece(captured.getPosition());
                        }
                    } else if (isCaptureAtEndEnabled()) {
                        game.getPiece(move.getLastCaptured().getPosition()).setCaptured(true);
                    } else {
                        game.clearPiece(move.getLastCaptured().getPosition());
                    }
                    if ((isKinged || isDamaKing) && isJumpsMandatory()) {
                        // we cannot king if we can still jump.
                        computeMovesForSquare(game, ernk, ecol, move, game.getMovesInternal());
                        Piece pp = game.getPiece(move.getEnd());
                        if (pp.getNumMoves() > 0) {
                            break;
                        }
                    }
                }
                if (isKinged) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, isFlyingKings() ? PieceType.FLYING_KING : PieceType.KING));
                }
                if (isDamaKing) {
                    game.getMovesInternal().add(new Move(MoveType.STACK, move.getPlayerNum()).setStart(ernk, ecol, p.getType()).setEnd(ernk, ecol, PieceType.DAMA_KING));
                }
                break;
            case STACK:
                game.getPiece(move.getStart()).setType(move.getEndType());
                break;
        }

        if (!isKinged && !isDamaKing) {
            // recursive compute next move if possible after a jump
            if (move.hasEnd())
                computeMovesForSquare(game, ernk, ecol, move, game.getMovesInternal());
            if (p.getNumMoves() == 0) {
                endTurnPrivate(game);
            } else if (!isJumpsMandatory()) {
                game.getMovesInternal().add(new Move(MoveType.END, move.getPlayerNum()).setStart(ernk, ecol, p.getType()));
            }
        }
    }

    void endTurnPrivate(Game game) {
        List<Integer> captured = new ArrayList<>();
        if (!isNoCaptures()) {
            for (int i = 0; i < game.getRanks(); i++) {
                for (int ii = 0; ii < game.getColumns(); ii++) {
                    Piece p = game.getPiece(i, ii);
                    if (p.isCaptured()) {
                        captured.add(p.getPosition());
                    }
                }
            }
            if (isCaptureAtEndEnabled() && captured.size() > 0) {
                for (int pos  : captured) {
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
                        throw new GException("Unhandled case '" + p.getType() + "'");
                }
            }
        }
        return value;
    }

    @Override
    final void reverseMove(Game game, Move m) {
        Piece p;
        switch (m.getMoveType()) {
            case END:
                break;
            case SLIDE:
            case FLYING_JUMP:
            case JUMP:
                p = game.getPiece(m.getEnd());
                if (m.hasCaptured()) {
                    if (isStackingCaptures()) {
                        Piece captured = game.getPiece(m.getLastCaptured().pos);
                        if (!p.isStacked())
                            throw new GException("Logic Error: Capture must result in stacked piece");
                        if (captured.getType() != EMPTY) {
                            //captured.addStackFirst(captured.getPlayerNum());
                        } else {
                            captured.setType(m.getLastCaptured().type);
                        }
//                        captured.setPlayerNum(p.removeStackLast());
                    } else {
                        int oppNum = game.getOpponent(m.getPlayerNum());
                        for (Move.CapturedPiece cur = m.getLastCaptured(); cur != null; cur = cur.next) {
                            game.setPiece(cur.getPosition(), oppNum, cur.getType());
                            Piece cp = game.getPiece(cur.getPosition());
                            //cp.setPlayerNum(game.getOpponent(m.getPlayerNum()));
                            //cp.setType(m.getCapturedType(i));
                            cp.setCaptured(isCaptureAtEndEnabled());
//                            game.setPiece(m.getCaptured(i), game.getOpponent(m.getPlayerNum()), m.getCapturedType(i));
                        }
                        if (isCaptureAtEndEnabled()) {
                            game.getPiece(m.getLastCaptured().getPosition()).setCaptured(false);
                        }
                    }
                }
                game.copyPiece(m.getEnd(), m.getStart());
                game.clearPiece(m.getEnd());
                break;
            case STACK:
                game.getPiece(m.getStart()).setType(m.getStartType());
                break;
            default:
                throw new GException("Unhandled case '" + m.getMoveType() + "'");
        }
        game.setTurn(m.getPlayerNum());
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

    public boolean isStackingCaptures() {
        return false;
    }

    @Override
    String getInstructions() {
        Table tab = new Table()
                .addRow("Can Jump Self", canJumpSelf())
                .addRow("Men Jump Backwards", canMenJumpBackwards())
                .addRow("Must Jump When Possible", isJumpsMandatory())
                .addRow("Must make maximum Jumps", isMaxJumpsMandatory())
                .addRow("Flying Kings", isFlyingKings())
                .addRow("Captures at the end", isCaptureAtEndEnabled())
                .addRow("Can Capture", !isNoCaptures());


        return String.format("Classic game of %s\n%s", getClass().getSimpleName(), tab.toString());
    }
}
