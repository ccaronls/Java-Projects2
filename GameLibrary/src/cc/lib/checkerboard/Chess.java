package cc.lib.checkerboard;

import java.util.Iterator;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;

import static cc.lib.checkerboard.PieceType.*;
import static cc.lib.checkerboard.Game.*;

/**
 * Created by chriscaron on 10/10/17.
 */

public class Chess extends Rules {

    static {
        addAllFields(Chess.class);
    }

    private int whiteSide = -1;
    private long timerLength, timerFar, timerNear;

    @Omit
    private long startTimeMS = 0;

    public void setTimer(int seconds) {
        this.timerLength = this.timerFar = this.timerNear = seconds*1000;
        startTimeMS = 0;
    }

    public void timerTick(Game game, long uptimeMillis) {
        if (startTimeMS <= 0)
            startTimeMS = uptimeMillis;
        long dt = uptimeMillis-startTimeMS;
        switch (game.getTurn()) {
            case FAR: timerFar -= dt; break;
            case NEAR: timerNear-= dt; break;
        }
        startTimeMS = uptimeMillis;
    }

    public int getTimerLength() {
        return (int)(timerLength/1000);
    }

    public int getTimerFar() {
        return (int)(timerFar/1000);
    }

    public int getTimerNear() {
        return (int)(timerNear/1000);
    }

    public boolean isTimerExpired(Game game) {
        if (timerLength <= 0)
            return false;
        if (game.getTurn() == FAR && timerFar <= 0)
            return true;
        if (game.getTurn() == NEAR && timerNear <= 0)
            return true;
        return false;
    }

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

        game.init(8, 8);
        game.initRank(0, FAR, ROOK_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, ROOK_IDLE);
        game.initRank(1, FAR, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        game.initRank(2, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(3, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(4, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(5, -1  , EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(6, NEAR, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE, PAWN_IDLE);
        game.initRank(7, NEAR, ROOK_IDLE, KNIGHT, BISHOP, left, right, BISHOP, KNIGHT, ROOK_IDLE);

        game.setTurn(whiteSide);
    }

    @Override
    public void executeMove(Game game, Move move) {
        lock = null;
        Piece p = null;
        game.clearMoves();
        {
            switch (move.getMoveType()) {
                case END:
                    if (game.getCurrentPlayer().isForfeited()) {
                        game.onGameOver(getWinner(game));
                        return;
                    }
                    break;
                case JUMP:
                case SLIDE:
                    if (move.getCaptured() != null) {
                        game.clearPiece(move.getCaptured());
                    }
                    p = game.movePiece(move);
                    // check for pawn advancing
                    if (p.getType() == PAWN_TOSWAP) {
                        computeMovesForSquare(game, move.getEnd()[0], move.getEnd()[1], null);
                        lock = p;
                    }
                    // see if this move result
                    break;
                case SWAP:
                    game.getPiece(move.getStart()).setType(move.getEndType());
                    break;
                case CASTLE: {
                    game.movePiece(move);
                    p = game.getPiece(move.getCastleRookStart());
                    Utils.assertTrue(p.getType() == ROOK_IDLE);
                    p.setType(ROOK);
                    game.setBoard(move.getCastleRookEnd(), p);
                    game.clearPiece(move.getCastleRookStart());
                    break;
                }
                default:
                    throw new AssertionError();
            }
            updateOpponentKingCheckedState(game);
        }

        if (p != null && timerLength > 0) {
            lock = p;
            lock.clearMoves();
            lock.addMove(new Move(MoveType.END, p.getPlayerNum()));//, null, null, move.getEnd()));
        }

        if (lock == null) {
            game.nextTurn();
            if (computeMoves(game, false) == 0) {
                game.onGameOver(getWinner(game));
            }
        }

        // check for game over
    }

    private void updateOpponentKingCheckedState(Game game) {
        // see if we are checking the opponent
        int [] opponentKing = findPiecePosition(game, game.getOpponent(), CHECKED_KING, CHECKED_KING_IDLE, UNCHECKED_KING, UNCHECKED_KING_IDLE);
        Piece king = game.getPiece(opponentKing);
        boolean checked = isSquareAttacked(game, opponentKing[0], opponentKing[1], game.getTurn());
        switch (king.getType()) {
            case CHECKED_KING_IDLE:
                if (!checked)
                    king.setType(UNCHECKED_KING_IDLE);
                break;
            case CHECKED_KING:
                if (!checked)
                    king.setType(UNCHECKED_KING);
                break;
            case UNCHECKED_KING_IDLE:
                if (checked)
                    king.setType(CHECKED_KING_IDLE);
                break;
            case UNCHECKED_KING:
                if (checked)
                    king.setType(CHECKED_KING);
                break;
        }

    }

    @Override
    protected void reverseMove(Game game, Move m, boolean recompute) {
        super.reverseMove(game, m, recompute);
        updateOpponentKingCheckedState(game);
    }

    // Return true if p.getType() is on set of types and p.getPlayerNum() equals playerNum
    private boolean testPiece(Piece p, int playerNum, PieceType ... types) {
        for (PieceType t : types) {
            if (p.getPlayerNum() == playerNum && p.getType() == t)
                return true;
        }
        return false;
    }

    /**
     * Return true if playerNum is attacking the position
     * @param rank
     * @param col
     * @param playerNum
     * @return
     */
    final boolean isSquareAttacked(Game game, int rank, int col, int playerNum) {
        boolean b = isSquareAttackedP(game, rank, col, playerNum);
        return b;
    }

    final private boolean isSquareAttackedP(Game game, int rank, int col, int playerNum) {

        // search in the eight directions and knights whom can
        int [][] kd = getKnightDeltas(game, rank, col);
        int [] dr = kd[0];
        int [] dc = kd[1];

        for (int i=0; i<dr.length; i++) {
            if (testPiece(game.board[rank +dr[i]][col +dc[i]], playerNum, KNIGHT)) {
                return true;
            }
        }

        final int adv = game.getAdvanceDir(game.getOpponent(playerNum));
        // look for pawns
        if (testPiece(game.getPiece(rank +adv, col +1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;
        if (testPiece(game.getPiece(rank +adv, col -1), playerNum, PAWN, PAWN_ENPASSANT, PAWN_IDLE))
            return true;

        // fan out in all eight directions looking for a opponent king
        dr = NSEW_DIAG_DELTAS[0];
        dc = NSEW_DIAG_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(game.getPiece(rank +dr[i], col +dc[i]), playerNum, CHECKED_KING, UNCHECKED_KING, UNCHECKED_KING_IDLE))
                return true;
        }

        dr = NSEW_DELTAS[0];
        dc = NSEW_DELTAS[1];
        // search NSEW for rook, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = game.getPiece(rank +dr[i]*ii, col +dc[i]*ii);
                if (p.getType() == UNAVAILABLE)
                    break;
                if (testPiece(p, playerNum, ROOK, ROOK_IDLE, QUEEN))
                    return true;
                if (p.getType() != EMPTY)
                    break;
            }
        }

        dr = DIAGONAL_DELTAS[0];
        dc = DIAGONAL_DELTAS[1];
        // search DIAGonals for bishop, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                Piece p = game.getPiece(rank +dr[i]*ii, col +dc[i]*ii);
                if (p.getType() == UNAVAILABLE)
                    break;
                if (testPiece(p, playerNum, BISHOP, QUEEN))
                    return true;
                if (p.getType() != EMPTY)
                    break;
            }
        }

        return false;
    }

    private void checkForCastle(Game game, int rank, int kingCol, int rookCol) {
        Piece king = game.getPiece(rank, kingCol);
        Piece rook = game.getPiece(rank, rookCol);
        if (king.getType() != UNCHECKED_KING_IDLE)
            return;
        if (rook.getType() != ROOK_IDLE)
            return;
        // check that there are no places in between king and rook and also none of the square is attacked
        int kingEndCol;
        int rookEndCol;
        int opponent = game.getOpponent(game.getTurn());
        if (rookCol > kingCol) {
            for (int i=kingCol+1; i<rookCol; i++) {
                if (game.getPiece(rank, i).getType() != EMPTY)
                    return;
                if (isSquareAttacked(game, rank, i, opponent))
                    return;
            }
            kingEndCol = kingCol+2;
            rookEndCol = kingEndCol-1;
        } else {
            // long side castle
            for (int i=rookCol+1; i<kingCol; i++) {
                if (game.getPiece(rank, i).getType() != EMPTY)
                    return;
                if (isSquareAttacked(game, rank, i, opponent))
                    return;
            }
            kingEndCol=kingCol-2;
            rookEndCol=kingEndCol+1;
        }
        king.addMove(new Move(MoveType.CASTLE, king.getPlayerNum()).setStart(rank, kingCol, UNCHECKED_KING_IDLE).setEnd(rank, kingEndCol, UNCHECKED_KING).setCastle(rank, rookCol, rank, rookEndCol));
    }

    @Override
    protected void computeMovesForSquare(Game game, int rank, int col, Move parent) {
        final Piece p = game.getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = game.getOpponent(p.getPlayerNum());
        int [] dr=null;
        int [] dc=null;
        int d = Math.max(game.ranks, game.cols);
        MoveType mt = MoveType.SLIDE;
        PieceType nextType = p.getType();
        switch (p.getType()) {
            case PAWN_ENPASSANT:
                p.setType(PAWN);
            case PAWN_IDLE:
            case PAWN: {
                // check in front of us 1 space
                tr=rank + game.getAdvanceDir(p.getPlayerNum());
                tc=col;
                PieceType nextPawn = PAWN;
                if (tr == game.getStartRank(game.getOpponent(p.getPlayerNum())))
                    nextPawn = PAWN_TOSWAP;
                if (game.getPiece(tr, col).getType() == EMPTY) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextPawn));
                    if (p.getType() == PAWN_IDLE) {
                        int tr2 = rank + game.getAdvanceDir(p.getPlayerNum())*2;
                        // if we have not moved yet then we may be able move 2 squares
                        if (game.getPiece(tr2, col).getType() == EMPTY) {
                            p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr2, tc, PAWN_ENPASSANT));
                        }

                    }
                }
                // check if we can capture to upper right or upper left
                if ((tp=game.getPiece(tr, (tc=col+1))).getPlayerNum() == opponent) {
                    // if this opponent is the king, then we will be 'checking' him
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextPawn));
                }
                // check if we can capture to upper right or upper left
                if ((tp=game.getPiece(tr, (tc=col-1))).getPlayerNum() == opponent) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(tr, tc, tp.getType()).setEnd(tr, tc, nextPawn));
                }
                // check en passant
                tr = rank;
                if ((tp=game.getPiece(tr, (tc=col+1))).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr+game.getAdvanceDir(p.getPlayerNum()), tc, nextPawn));
                }
                if ((tp=game.getPiece(tr, (tc=col-1))).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                    p.addMove(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setCaptured(rank, tc, tp.getType()).setEnd(tr+game.getAdvanceDir(p.getPlayerNum()), tc, nextPawn));
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Utils.toArray(ROOK, KNIGHT, BISHOP, QUEEN)) {
                    p.addMove(new Move(MoveType.SWAP, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(rank, col, np));
                }
                break;
            case BISHOP:
                dr = DIAGONAL_DELTAS[0];
                dc = DIAGONAL_DELTAS[1];
                break;
            case KNIGHT: {
                int [][] kd = getKnightDeltas(game, rank, col);
                dr = kd[0];//KNIGHT_DELTAS[0];
                dc = kd[1];//KNIGHT_DELTAS[1];
                d = 1;
                mt = MoveType.JUMP;
                break;
            }
            case ROOK_IDLE:
                nextType = ROOK;
            case ROOK:
                dr = NSEW_DELTAS[0];
                dc = NSEW_DELTAS[1];
                break;
            case UNCHECKED_KING_IDLE:
                checkForCastle(game, rank, col, 0);
                checkForCastle(game, rank, col, game.cols-1);
            case CHECKED_KING_IDLE:
                nextType = UNCHECKED_KING;
            case UNCHECKED_KING:
            case CHECKED_KING:
                d=1;
            case QUEEN:
                dr = NSEW_DIAG_DELTAS[0];
                dc = NSEW_DIAG_DELTAS[1];
                break;
            default:
                throw new AssertionError("Unknown pieceType " + p.getType());
        }

        if (dr != null) {
            Utils.assertTrue(dr.length == dc.length);
            for (int i=0; i<dr.length; i++) {
                // search max d units in a specific direction
                for (int ii=1; ii<=d; ii++) {
                    tr=rank+dr[i]*ii;
                    tc=col +dc[i]*ii;
                    if (!game.isOnBoard(tr, tc))
                        continue;
                    tp = game.getPiece(tr, tc);
                    if (tp.getPlayerNum() == opponent) { // look for capture
                        p.addMove(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType).setCaptured(tr, tc, tp.getType()));
                        break; // can no longer search along this path
                    } else if (tp.getType() == EMPTY) { // look for open
                        p.addMove(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType));
                    } else {
                        break; // can no longer search along this path
                    }
                }
            }
        }

        // now search moves and remove any that cause our king to be checked
        List<Move> moves = p.getMovesList();
        Iterator<Move> it = moves.iterator();
        while (it.hasNext()) {
            Move m = it.next();
            switch (m.getMoveType()) {
                case CASTLE:
                case SWAP:
                    continue;
            }
            if (!m.hasEnd())
                continue;
            game.movePiece(m);
            int [] kingPos = findPiecePosition(game, game.getTurn(), UNCHECKED_KING_IDLE, UNCHECKED_KING, CHECKED_KING, CHECKED_KING_IDLE);
            if (isSquareAttacked(game, kingPos[0], kingPos[1], opponent))
                it.remove();
            reverseMove(game, m, false);
        }
        game.getPiece(rank, col).setMovesList(moves);
    }

    int[] findPiecePosition(Game game, int playerNum, PieceType ... types) {
        for (int rank=0; rank<game.ranks; rank++) {
            for (int col =0; col<game.cols; col++) {
                if (game.board[rank][col].getPlayerNum() == playerNum) {
                    if (Utils.linearSearch(types, game.board[rank][col].getType()) >= 0) {
                        return new int[]{rank, col};
                    }
                }
            }
        }
        return null;
    }

    public final Piece findPiece(Game game, int playerNum, PieceType ... types) {
        int[] pos = findPiecePosition(game, playerNum, types);
        if (pos != null) {
            return game.board[pos[0]][pos[1]];//getPiece(pos[0], pos[1]);
        }
        return null;
    }
    public final static int [][] DIAGONAL_DELTAS = {
            {-1, -1, 1, 1},
            {-1, 1, -1, 1}
    };
    //public final static int [][] KNIGHT_DELTAS = {
    //        {-2, -2, -1, 1, 2,  2,  1, -1},
    //        {-1,  1,  2, 2, 1, -1, -2, -2}
    //};

    // precompute knight deltas for each square

    @Omit
    private static int[][][][] knightDeltas = null;

    public final static int [][] ALL_KNIGHT_DELTAS = {
            {-2, -2, -1, 1, 2,  2,  1, -1},
            {-1,  1,  2, 2, 1, -1, -2, -2}
    };

    int [][] computeKnightDeltaFor(Game game, int rank, int col) {
        int [][] d = new int[2][8];
        int n = 0;
        for (int i=0; i<8; i++) {
            if (game.isOnBoard(rank+ALL_KNIGHT_DELTAS[0][i], col+ALL_KNIGHT_DELTAS[1][i])) {
                d[0][n] = ALL_KNIGHT_DELTAS[0][i];
                d[1][n] = ALL_KNIGHT_DELTAS[1][i];
                n++;
            }
        }
        if (n < 8) {
            int[] t = d[0];
            d[0] = new int[n];
            System.arraycopy(t, 0, d[0], 0, n);

            t = d[1];
            d[1] = new int[n];
            System.arraycopy(t, 0, d[1], 0, n);
        }

        return d;
    }

    private void buildKnightDeltas(Game game) {
        knightDeltas = new int[8][8][][];
        for (int i=0; i<8; i++) {
            for (int ii=0; ii<8; ii++) {
                knightDeltas[i][ii] = computeKnightDeltaFor(game, i, ii);
            }
        }
    }

    public int [][] getKnightDeltas(Game game, int rank, int col) {
        if (knightDeltas == null) {
            buildKnightDeltas(game);
        }
        return knightDeltas[rank][col];
    }

    public final static int [][] NSEW_DELTAS = {
            {1, 0, -1, 0},
            {0, 1, 0, -1}
    };

    public final static int [][] NSEW_DIAG_DELTAS = {
            {1, 0, -1, 0, -1, -1, 1, 1},
            {0, 1, 0, -1, -1, 1, -1, 1}
    };

    @Override
    public Color getPlayerColor(int side) {
        if (whiteSide == side)
            return Color.WHITE;
        return Color.BLACK;
    }
}
