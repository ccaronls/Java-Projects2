package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.GException;

import static cc.lib.checkerboard.PieceType.*;
import static cc.lib.checkerboard.Game.*;

/**
 * Created by chriscaron on 10/10/17.
 */

public class Chess extends Rules {

    static {
        addAllFields(Chess.class);
    }

    protected int whiteSide = -1;
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
    boolean isDraw(Game game) {
        // in chess, draw game if only 2 kings left or current player cannot move but is not in check
        // if down to only 2 kings, one of each color, then game is a draw. Also a king and bishop alone cannot checkmate
        int numBishops = 0;
        int numPieces = 0;
        final boolean noMoves = game.getMoves().size() == 0;
        boolean inCheck = false;
        for (Piece p : game.getPieces(-1)) {
            switch (p.getType()) {
                case CHECKED_KING:
                case CHECKED_KING_IDLE:
                    if (p.getPlayerNum() == game.getTurn())
                        inCheck = true;
                    break;
                case BISHOP:
                    numBishops++;
                    break;
                case UNCHECKED_KING:
                case UNCHECKED_KING_IDLE:
                case EMPTY:
                    break;
                default:
                    numPieces++;
            }
        }
        if (noMoves && !inCheck)
            return true;
        if (noMoves)
            return false;
        if (numPieces > 0)
            return false;
        return numBishops < 2;
    }

    @Override
    int getWinner(Game game) {
        if (game.getMoves().size() == 0) {
            Piece p = findKing(game, game.getTurn());
            switch (p.getType()) {
                case CHECKED_KING:
                case CHECKED_KING_IDLE:
                    return game.getOpponent(p.getPlayerNum());
            }
        }
        return -1;
    }

    @Override
    public void executeMove(Game game, Move move) {
        Move previous = game.getPreviousMove(game.getTurn());
        executeMoveInternal(game, move);
        findKing(game, move.getPlayerNum()).setChecked(false);
        game.nextTurn();
        if (previous != null && previous.getEndType() == PAWN_ENPASSANT && game.getPiece(previous.getEnd()).getType() == PAWN_ENPASSANT) {
            game.getPiece(previous.getEnd()).setType(PAWN);
            move.setEnpassant(previous.getEnd());
        }
    }

    private void executeMoveInternal(Game game, Move move) {
        Piece p = null;
        try {
            switch (move.getMoveType()) {
                case END:
                    // does this ever get called?
                    System.out.println("!!!!!! I GOT CALLED !!!!!!!!!!!!!!!");
                    break;
                case JUMP:
                case SLIDE:
                    if (move.hasCaptured()) {
                        game.clearPiece(move.getLastCaptured());
                    }
                    p = game.movePiece(move);
                    // check for pawn advancing
                    if (p.getType() == PAWN_TOSWAP) {
                        computeMovesForSquare(game, move.getEnd()[0], move.getEnd()[1], null, game.getMovesInternal());
                        return;
                    }
                    // see if this move result
                    break;
                case SWAP:
                    game.getPiece(move.getStart()).setType(move.getEndType());
                    break;
                case CASTLE: {
                    game.movePiece(move);
                    p = game.getPiece(move.getCastleRookStart());
                    Utils.assertTrue(p.getType() == ROOK_IDLE || p.getType() == DRAGON_IDLE);
                    game.setPiece(move.getCastleRookEnd(), move.getPlayerNum(), p.getType() == DRAGON_IDLE ? DRAGON : ROOK);
                    game.clearPiece(move.getCastleRookStart());
                    break;
                }
                default:
                    throw new GException();
            }
        } finally {
            if ((move.getOpponentKingPos() != null))
                game.getPiece(move.getOpponentKingPos()).setType(move.getOpponentKingTypeEnd());
        }

        if (p != null && timerLength > 0) {
            throw new GException("I dont understand this logic");
            //game.getMovesInternal().add(new Move(MoveType.END, p.getPlayerNum()));
        }

    }

    // Return true if p.getType() is on set of types and p.getPlayerNum() equals playerNum
    final boolean testPiece(Game game, int rank, int col, int playerNum, int flag) {
        //if (!game.isOnBoard(rank, col))
        //    return false;
        Piece p = game.board[rank][col];//rank, col);
        return p.playerNum == playerNum && (p.type.flag & flag) != 0;
    }

    /**
     * Return true if playerNum is attacking the position
     * @param rank
     * @param col
     * @param attacker
     * @return
     */
    protected boolean isSquareAttacked(Game game, int rank, int col, int attacker) {

        if (pieceDeltas == null) {
            computePieceDeltas(game);
        }

        int [][][][] knightDeltas = pieceDeltas[DELTAS_KNIGHT];

        // search in the eight directions and knights whom can
        int [][] kd = knightDeltas[rank][col];
        int [] dr = kd[0];
        int [] dc = kd[1];

        for (int i=0; i<dr.length; i++) {
            int rr = rank +dr[i];
            int cc = col +dc[i];
            if (testPiece(game, rr, cc, attacker, FLAG_KNIGHT)) {
                return true;
            }
        }

        final int adv = game.getAdvanceDir(game.getOpponent(attacker));
        // look for pawns
        if (game.isOnBoard(rank+adv, col+1) && testPiece(game, rank +adv, col +1, attacker, FLAG_PAWN))
            return true;
        if (game.isOnBoard(rank+adv, col-1) && testPiece(game, rank +adv, col -1, attacker, FLAG_PAWN))
            return true;

//        int [][][] kdn = new int[8][][];
        int kn = 0;

        // fan out in all eight directions looking for a opponent king
        kd = pieceDeltas[DELTAS_KING][rank][col];
        dr = kd[0];//pieceDeltas[UNCHECKED_KING.ordinal()];///NSEW_DIAG_DELTAS[0];
        dc = kd[1];//NSEW_DIAG_DELTAS[1];

        for (int i=0; i<dr.length; i++) {
            int rr = rank +dr[i];
            int cc = col +dc[i];
            if (testPiece(game, rr, cc, attacker, FLAG_KING))
                return true;
        }

        kn=computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W);
        for (int k=0; k<kn; k++) {
            kd = kdn[k];
            dr = kd[0];
            dc = kd[1];
            for (int i = 0; i < dr.length; i++) {
                int rr = rank + dr[i];
                int cc = col + dc[i];
                if (testPiece(game, rr, cc, attacker, FLAG_ROOK_OR_QUEEN))
                    return true;
                if (game.getPiece(rr, cc).getType() != EMPTY)
                    break;
            }
        }



        kn=computeKDN(rank, col, DELTAS_NE, DELTAS_SE, DELTAS_NW, DELTAS_SW);
        for (int k=0; k<kn; k++) {
            kd = kdn[k];
            dr = kd[0];//DIAGONAL_DELTAS[0];
            dc = kd[1];//DIAGONAL_DELTAS[1];
            // search DIAGonals for bishop, queen
            for (int i = 0; i < dr.length; i++) {
                int rr = rank + dr[i];
                int cc = col + dc[i];
                if (testPiece(game, rr, cc, attacker, FLAG_BISHOP_OR_QUEEN))
                    return true;
                if (game.getPiece(rr, cc).getType() != EMPTY)
                    break;
            }
        }

        return false;
    }

    private void checkForCastle(Game game, int rank, int kingCol, int rookCol, List<Move> moves) {
        Piece king = game.getPiece(rank, kingCol);
        Piece rook = game.getPiece(rank, rookCol);
        if (king.getType() != UNCHECKED_KING_IDLE)
            return;
        if (!rook.getType().canCastleWith())
            return;
        // check that there are no places in between king and rook and also none of the square is attacked
        int kingEndCol;
        int rookEndCol;
        int opponent = game.getOpponent(game.getTurn());
        if (rookCol > kingCol) {
            for (int i=kingCol+1; i<rookCol; i++) {
                Piece p;
                if ((p=game.getPiece(rank, i)).getType() != EMPTY)
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
        moves.add(new Move(MoveType.CASTLE, king.getPlayerNum())
                .setStart(rank, kingCol, UNCHECKED_KING_IDLE)
                .setEnd(rank, kingEndCol, UNCHECKED_KING)
                .setCastle(rank, rookCol, rank, rookEndCol));
    }

    final List<Move> computeMoves(Game game) {
        if (pieceDeltas == null) {
            computePieceDeltas(game);
        }
        List<Move> moves = new ArrayList<>();
        for (Piece p : game.getPieces(game.getTurn())) {
            computeMovesForSquare(game, p.getRank(), p.getCol(), null, moves);
        }
        return moves;
    }

    @Omit
    int [] castleRookCols = null;

    protected int [] getRookCastleCols(Game game) {
        return new int [] { 0, game.getColumns()-1 };
    }

    @Omit
    final int [][][] kdn = new int[8][][];

    int computeKDN(int rank, int col, int ... which) {
        int n=0;
        for (int w : which) {
            if (pieceDeltas[w][rank][col].length > 0)
                kdn[n++] = pieceDeltas[w][rank][col];
        }
        return n;
    }

    private void computeMovesForSquare(Game game, int rank, int col, Move parent, List<Move> moves) {
        final int startNumMoves = moves.size();
        final Piece p = game.getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = game.getOpponent(p.getPlayerNum());
        int [] dr=null;
        int [] dc=null;
        int [][] kd = null;
        int kn = 0;
        int d = Math.max(game.getRanks(), game.getColumns());
        MoveType mt = MoveType.SLIDE;
        PieceType nextType = p.getType();
        switch (p.getType()) {
            case PAWN_ENPASSANT:
            case PAWN_IDLE:
            case PAWN: {
                // check in front of us 1 space
                tr=rank + game.getAdvanceDir(p.getPlayerNum());
                tc=col;
                if (game.isOnBoard(tr, col) && game.getPiece(tr, col).getType() == EMPTY) {
                    boolean atEnd = tr == game.getStartRank(game.getOpponent(p.getPlayerNum()));
                    moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, atEnd ? PAWN_TOSWAP : PAWN));
                    if (p.getType() == PAWN_IDLE) {
                        int tr2 = rank + game.getAdvanceDir(p.getPlayerNum())*2;
                        // if we have not moved yet then we may be able move 2 squares
                        if (game.getPiece(tr2, col).getType() == EMPTY) {
                            moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr2, tc, PAWN_ENPASSANT));
                        }

                    }
                }
                int enpassantRank = game.getStartRank(opponent) + 3*game.getAdvanceDir(opponent);
                // check if we can capture to upper right
                if (game.isOnBoard(tr, (tc=col+1))) {
                    tp = game.getPiece(tr, tc);
                    if (tp.getPlayerNum() == opponent) {
                        // if this opponent is the king, then we will be 'checking' him
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(tr, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    } else if (rank == enpassantRank && (tp = game.getPiece(rank, tc)).getType() == PAWN_ENPASSANT) {
                        // check en passant
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(rank, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    }
                }
                // check if we can capture to upper left
                if (game.isOnBoard(tr, tc=col-1)) {
                    tp = game.getPiece(tr, tc);
                    if (tp.getPlayerNum() == opponent) {
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(tr, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    } else if (rank == enpassantRank && (tp = game.getPiece(rank, tc)).getType() == PAWN_ENPASSANT) {
                        // check enpassant
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(rank, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    }
                }
                break;
            }

            case PAWN_TOSWAP:
                for (PieceType np : Arrays.asList(ROOK, KNIGHT, BISHOP, QUEEN)) { // TODO: Have option to only allow from pieces already captured
                    moves.add(new Move(MoveType.SWAP, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(rank, col, np));
                }
                break;
            case BISHOP:
                kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW);
                break;
            case KNIGHT: {
                kdn[kn++] = pieceDeltas[DELTAS_KNIGHT][rank][col];
                mt = MoveType.JUMP;
                break;
            }
            case ROOK_IDLE:
                nextType = ROOK;
            case ROOK:
                kn = computeKDN(rank, col, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W);
                break;
            case DRAGON_IDLE:
                nextType = DRAGON;
            case DRAGON:
                kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W);
                d = 3;
                break;
            case UNCHECKED_KING_IDLE:
                if (castleRookCols == null)
                    castleRookCols = getRookCastleCols(game);
                for (int n : castleRookCols)
                    checkForCastle(game, rank, col, n, moves);
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case CHECKED_KING:
                nextType = UNCHECKED_KING;
                kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W);
                d = 1;
                break;
            case QUEEN:
                kn = computeKDN(rank, col, DELTAS_NE, DELTAS_NW, DELTAS_SE, DELTAS_SW, DELTAS_N, DELTAS_S, DELTAS_E, DELTAS_W);
                break;
            default:
                throw new GException("Unknown pieceType " + p.getType());
        }

        for (int k=0; k<kn; k++) {
            kd = kdn[k];
            dr = kd[0];
            dc = kd[1];
            Utils.assertTrue(dr.length == dc.length);
            int n = Math.min(d, dr.length);
            for (int i=0; i<n; i++) {
                // search max d units in a specific direction
                tr=rank+dr[i];
                tc=col +dc[i];
                tp = game.getPiece(tr, tc);
                if (tp.getPlayerNum() == opponent) { // look for capture
                    moves.add(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType).addCaptured(tr, tc, tp.getType()));

                } else if (tp.getType() == EMPTY) { // look for open
                    moves.add(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType));
                    continue;
                }
                if (mt == MoveType.SLIDE)
                    break;
            }
        }

        // now search moves and remove any that cause our king to be checked
        if (moves.size() > startNumMoves) {
            Piece opponentKing = findKing(game, opponent);
            if (opponentKing == null)
                throw new NullPointerException();
            PieceType opponentKingStartType = opponentKing.getType();

            Iterator<Move> it = moves.iterator();
            int num = 0;
            while (it.hasNext()) {
                Move m = it.next();
                if (num++ < startNumMoves)
                    continue;
                if (!m.hasEnd())
                    continue;
                executeMoveInternal(game, m);
                do {
                    if (m.getMoveType() != MoveType.SWAP) {
                        final Piece king = findKing(game, game.getTurn());
                        if (isSquareAttacked(game, king.getRank(), king.getCol(), opponent)) {
                            it.remove();
                            break;
                        }
                    }
                    PieceType opponentKingEndType = opponentKingStartType;
                    boolean attacked = isSquareAttacked(game, opponentKing.getRank(), opponentKing.getCol(), m.getPlayerNum());

                    switch (opponentKingStartType) {
                        case CHECKED_KING:
                            if (!attacked)
                                opponentKingEndType = UNCHECKED_KING;
                            break;
                        case CHECKED_KING_IDLE:
                            if (!attacked)
                                opponentKingEndType = UNCHECKED_KING_IDLE;
                            break;
                        case UNCHECKED_KING:
                            if (attacked)
                                opponentKingEndType = CHECKED_KING;
                            break;
                        case UNCHECKED_KING_IDLE:
                            if (attacked)
                                opponentKingEndType = CHECKED_KING_IDLE;
                            break;
                        default:
                            throw new GException("Unhandled case:" + opponentKingStartType);
                    }
                    m.setOpponentKingType(opponentKing.getRank(), opponentKing.getCol(), opponentKingStartType, opponentKingEndType);

                } while (false);
                reverseMove(game, m);
            }
        }
    }

    @Omit
    private Piece [] kingCache = new Piece[2];

    private Piece findKing(Game game, int playerNum) {
        if (kingCache[playerNum] != null) {
            if (0 != (kingCache[playerNum].type.flag & FLAG_KING))
                return kingCache[playerNum];
        }

        for (Piece p : game.getPieces(playerNum)) {
            if ((p.getType().flag & FLAG_KING) != 0) {
                kingCache[playerNum] = p;
                return p;
            }
        }
        throw new GException("Logic Error: Cannot find king for player " + playerNum);
    }

    // precompute knight deltas for each square

    // precompute deltas for all piece types


    // pieceType.ordinal(), rank, col,
    @Omit
    int[][][][][] pieceDeltas = null;

    final static int DELTAS_N = 0;
    final static int DELTAS_S = 1;
    final static int DELTAS_E = 2;
    final static int DELTAS_W = 3;
    final static int DELTAS_NE = 4;
    final static int DELTAS_NW = 5;
    final static int DELTAS_SE = 6;
    final static int DELTAS_SW = 7;
    final static int DELTAS_KNIGHT = 8;
    final static int DELTAS_KING = 9;
    final static int NUM_DELTAS = 10;

    void computePieceDeltas(Game game) {
        pieceDeltas = new int[NUM_DELTAS][][][][];
        pieceDeltas[DELTAS_KNIGHT] = computeKnightDeltas(game);
        pieceDeltas[DELTAS_N] = computeDeltas(game, -1, 0);
        pieceDeltas[DELTAS_S] = computeDeltas(game, 1, 0);
        pieceDeltas[DELTAS_E] = computeDeltas(game, 0, 1);
        pieceDeltas[DELTAS_W] = computeDeltas(game, 0, -1);
        pieceDeltas[DELTAS_NE] = computeDeltas(game, -1, 1);
        pieceDeltas[DELTAS_NW] = computeDeltas(game, -1, -1);
        pieceDeltas[DELTAS_SE] = computeDeltas(game, 1, 1);
        pieceDeltas[DELTAS_SW] = computeDeltas(game, 1, -1);
        pieceDeltas[DELTAS_KING] = computeQueenDeltas(game, 1);
    }

    int [][][][] computeDeltas(Game game, int dr, int dc) {
        int ranks = game.getRanks();
        int cols = game.getColumns();
        int [][][][] deltas = new int[ranks][cols][][];
        for (int i = 0; i < ranks; i++) {
            for (int ii = 0; ii < cols; ii++) {
                deltas[i][ii] = computeDeltaFor(game, i, ii, dr, dc);
            }
        }
        return deltas;

    }

    private int [][] computeDeltaFor(Game game, int rank, int col, int dr, int dc) {
        int max = Math.max(game.getRanks(), game.getColumns());
        int [][] d = new int[2][max];
        int n = 0;
        for (int i=1; i<max; i++) {
            final int r = rank + dr*i;
            final int c = col + dc*i;
            if (!game.isOnBoard(r, c))
                break;

            d[0][n] = dr*i;
            d[1][n] = dc*i;
            n++;
        }

//        Utils.assertTrue(n > 0);
        int[] t = d[0];
        d[0] = new int[n];
        System.arraycopy(t, 0, d[0], 0, n);

        t = d[1];
        d[1] = new int[n];
        System.arraycopy(t, 0, d[1], 0, n);

        return d;
    }


    int [][][][] computeBishopDeltas(Game game) {

        final int [][] DIAGONAL_DELTAS = {
                {-1, -1, 1, 1},
                {-1, 1, -1, 1}
        };

        int ranks = game.getRanks();
        int cols = game.getColumns();
        int [][][][] deltas = new int[ranks][cols][][];
        for (int i = 0; i < ranks; i++) {
            for (int ii = 0; ii < cols; ii++) {
                deltas[i][ii] = computeDeltaFor(game, i, ii, DIAGONAL_DELTAS, Math.max(ranks, cols));
            }
        }
        return deltas;
    }

    int [][][][] computeRookDeltas(Game game) {
        int ranks = game.getRanks();
        int cols = game.getColumns();
        final int [][] NSEW_DELTAS = {
                {1, 0, -1, 0},
                {0, 1, 0, -1}
        };

        int [][][][] deltas = new int[ranks][cols][][];
        for (int i = 0; i < ranks; i++) {
            for (int ii = 0; ii < cols; ii++) {
                deltas[i][ii] = computeDeltaFor(game, i, ii, NSEW_DELTAS, Math.max(ranks, cols));
            }
        }
        return deltas;

    }

    int [][][][] computeQueenDeltas(Game game, int max) {
        int ranks = game.getRanks();
        int cols = game.getColumns();
        final int [][] NSEW_DIAG_DELTAS = {
                {1, 0, -1, 0, -1, -1, 1, 1},
                {0, 1, 0, -1, -1, 1, -1, 1}
        };

        int [][][][] deltas = new int[ranks][cols][][];
        for (int i = 0; i < ranks; i++) {
            for (int ii = 0; ii < cols; ii++) {
                if (game.getPiece(i, ii).getType() != BLOCKED)
                    deltas[i][ii] = computeDeltaFor(game, i, ii, NSEW_DIAG_DELTAS, max);
            }
        }
        return deltas;

    }

    int [][][][] computeKnightDeltas(Game game) {
        int [][] ALL_KNIGHT_DELTAS = {
                {-2, -2, -1, 1, 2,  2,  1, -1},
                {-1,  1,  2, 2, 1, -1, -2, -2}
        };

        int ranks = game.getRanks();
        int cols = game.getColumns();
        int[][][][] deltas = new int[ranks][cols][][];
        for (int i = 0; i < ranks; i++) {
            for (int ii = 0; ii < cols; ii++) {
                if (game.getPiece(i, ii).type != BLOCKED)
                    deltas[i][ii] = computeDeltaFor(game, i, ii, ALL_KNIGHT_DELTAS, 1);
            }
        }
        return deltas;
    }

    private int [][] computeDeltaFor(Game game, int rank, int col, int [][] ALL_DELTAS, int num) {
        int max = ALL_DELTAS[0].length * num;
        int [][] d = new int[2][max];
        int n = 0;
        for (int i=0; i<ALL_DELTAS[0].length; i++) {
            for (int ii=1; ii<=num; ii++) {
                final int r = rank + ALL_DELTAS[0][i]*ii;
                final int c = col + ALL_DELTAS[1][i]*ii;
                if (!game.isOnBoard(r, c))
                    break;

                d[0][n] = ALL_DELTAS[0][i]*ii;
                d[1][n] = ALL_DELTAS[1][i]*ii;
                n++;
            }
        }

        Utils.assertTrue(n > 0);
        int[] t = d[0];
        d[0] = new int[n];
        System.arraycopy(t, 0, d[0], 0, n);

        t = d[1];
        d[1] = new int[n];
        System.arraycopy(t, 0, d[1], 0, n);

        return d;
    }


    @Override
    public Color getPlayerColor(int side) {
        if (whiteSide == side)
            return Color.WHITE;
        return Color.BLACK;
    }

    @Override
    public long evaluate(Game game, Move move) {
        long value=0;
        try {
            if (game.isDraw())
                return value=0;
            int side;
            switch(side=game.getWinnerNum()) {
                case NEAR:
                case FAR:
                    return side == move.getPlayerNum() ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            for (Piece p : game.getPieces(-1)) {
                int scale = p.getPlayerNum() == move.getPlayerNum() ? 1 : -1;
                //if (p.getType() == EMPTY)
                //    continue;
                //System.out.println("scale=" + scale);
                switch (p.getType()) {
                    case EMPTY:
                        break;
                    case PAWN:
                        value += 110 * scale;
                        break;
                    case PAWN_IDLE:
                        value += 100 * scale;
                        break;
                    case PAWN_ENPASSANT:
                        value += 120 * scale;
                        break;
                    case PAWN_TOSWAP:
                        value += 5000 * scale;
                        break;
                    case BISHOP:
                        value += 300 * scale;
                        break;
                    case KNIGHT:
                        value += 310 * scale;
                        break;
                    case DRAGON:
                    case ROOK:
                        value += 500 * scale;
                        break;
                    case DRAGON_IDLE:
                    case ROOK_IDLE:
                        value += 550 * scale;
                        break;
                    case QUEEN:
                        value += 800 * scale;
                        break;
                    case CHECKED_KING:
                        value -= 500 * scale;
                        break;
                    case CHECKED_KING_IDLE:
                        value -= 300 * scale;
                        break;
                    case UNCHECKED_KING: // once lost idle state we want to chase the other king
                        value -= 100 * scale;
                        if (p.getPlayerNum() == move.getPlayerNum()) {
                            int dist = Math.max(Math.abs(p.getRank()-move.getOpponentKingPos()[0]), Math.abs(p.getCol()-move.getOpponentKingPos()[1]));
                            //System.out.println("dist:"+  dist);
                            value -= (dist-2); // 2 units away is best
                        }
                        break;
                    case UNCHECKED_KING_IDLE:
                        value += 1000 * scale; // we want avoid moving this piece
                        break;
                    default:
                        throw new GException("Unhandled case '" + p.getType() + "'");
                }
            }
            return value;
        } finally {
            //System.out.println("[" + value + "] " + move);
        }
    }

    @Override
    void reverseMove(Game game, Move m) {
        Piece p;
        switch (m.getMoveType()) {
            case END:
                break;
            case CASTLE:
                p = game.getPiece(m.getCastleRookEnd());
                Utils.assertTrue(p.getType() == ROOK || p.getType() == DRAGON, "Expected ROOK was " + p.getType());
                game.setPiece(m.getCastleRookStart(), m.getPlayerNum(), p.getType() == DRAGON ? DRAGON_IDLE : ROOK_IDLE);
                game.clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case JUMP:
                game.clearPiece(m.getEnd());
                if (m.hasCaptured()) {
                    if (m.getNumCaptured() != 1)
                        throw new GException("Logic Error: cannot have more than one captured piece in chess");
                    game.setPiece(m.getCaptured(0), game.getOpponent(m.getPlayerNum()), m.getCapturedType(0));
                }
                //fallthrough
            case SWAP:
                game.setPiece(m.getStart(), m.getPlayerNum(), m.getStartType());
                break;
            default:
                throw new GException("Unhandled Case " + m.getMoveType());
        }
        if (m.getOpponentKingPos() != null)
            game.getPiece(m.getOpponentKingPos()).setType(m.getOpponentKingTypeStart());
        game.setTurn(m.getPlayerNum());
    }
}
