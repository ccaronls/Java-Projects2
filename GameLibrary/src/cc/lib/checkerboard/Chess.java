package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
    boolean isDraw(Game game) {
        // in chess, draw game if only 2 kings left or current player cannot move but is not in check
        // if down to only 2 kings, one of each color, then game is a draw. Also a king and bishop alone cannot checkmate
        int numBishops = 0;
        int numPieces = 0;
        final boolean noMoves = game.getMoves().size() == 0;
        boolean inCheck = false;
        for (int r = 0; r < game.getRanks(); r++) {
            for (int c = 0; c < game.getColumns(); c++) {
                Piece p;
                switch ((p=game.getPiece(r, c)).getType()) {
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
            for (int r = 0; r < game.getRanks(); r++) {
                for (int c = 0; c < game.getColumns(); c++) {
                    Piece p;
                    switch ((p=game.getPiece(r, c)).getType()) {
                        case CHECKED_KING:
                        case CHECKED_KING_IDLE:
                            return game.getOpponent(p.getPlayerNum());
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public void executeMove(Game game, Move move) {
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
                    Utils.assertTrue(p.getType() == ROOK_IDLE);
                    game.setPiece(move.getCastleRookEnd(), move.getPlayerNum(), ROOK);
                    game.clearPiece(move.getCastleRookStart());
                    break;
                }
                default:
                    throw new AssertionError();
            }
        } finally {
            game.getPiece(move.getOpponentKingPos()).setType(move.getOpponentKingTypeEnd());
        }

        if (p != null && timerLength > 0) {
            throw new AssertionError("I dont understand this logic");
            //game.getMovesInternal().add(new Move(MoveType.END, p.getPlayerNum()));
        }

        game.nextTurn();
    }

    // Return true if p.getType() is on set of types and p.getPlayerNum() equals playerNum
    private final boolean testPiece(Game game, int rank, int col, int playerNum, int flag) {
        if (!game.isOnBoard(rank, col))
            return false;
        Piece p = game.getPiece(rank, col);
        return p.getPlayerNum() == playerNum && (p.getType().flag & flag) != 0;
    }

    /**
     * Return true if playerNum is attacking the position
     * @param rank
     * @param col
     * @param playerNum
     * @return
     */
    final boolean isSquareAttacked(Game game, int rank, int col, int playerNum) {

        // search in the eight directions and knights whom can
        int [][] kd = knightDeltas[rank][col];
        int [] dr = kd[0];
        int [] dc = kd[1];

        for (int i=0; i<dr.length; i++) {
            if (testPiece(game, rank +dr[i], col +dc[i], playerNum, FLAG_KNIGHT)) {
                return true;
            }
        }

        final int adv = game.getAdvanceDir(game.getOpponent(playerNum));
        // look for pawns
        if (testPiece(game, rank +adv, col +1, playerNum, FLAG_PAWN))
            return true;
        if (testPiece(game, rank +adv, col -1, playerNum, FLAG_PAWN))
            return true;

        // fan out in all eight directions looking for a opponent king
        dr = NSEW_DIAG_DELTAS[0];
        dc = NSEW_DIAG_DELTAS[1];

        for (int i=0; i<8; i++) {
            if (testPiece(game, rank +dr[i], col +dc[i], playerNum, FLAG_KING))
                return true;
        }

        dr = NSEW_DELTAS[0];
        dc = NSEW_DELTAS[1];
        // search NSEW for rook, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                int rr = rank +dr[i]*ii;
                int cc = col +dc[i]*ii;
                if (testPiece(game, rr, cc, playerNum, FLAG_ROOK_OR_QUEEN))
                    return true;
                if (game.isOnBoard(rr, cc) && game.getPiece(rr, cc).getType() != EMPTY)
                    break;
            }
        }

        dr = DIAGONAL_DELTAS[0];
        dc = DIAGONAL_DELTAS[1];
        // search DIAGonals for bishop, queen
        for (int i=0; i<4; i++) {
            for (int ii=1; ii<8; ii++) {
                int rr = rank +dr[i]*ii;
                int cc = col +dc[i]*ii;
                if (game.isOnBoard(rr, cc)) {
                    if (testPiece(game, rr, cc, playerNum, FLAG_BISHOP_OR_QUEEN))
                        return true;
                    if (game.getPiece(rr, cc).getType() != EMPTY)
                        break;
                }
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
        game.getMovesInternal().add(new Move(MoveType.CASTLE, king.getPlayerNum()).setStart(rank, kingCol, UNCHECKED_KING_IDLE).setEnd(rank, kingEndCol, UNCHECKED_KING).setCastle(rank, rookCol, rank, rookEndCol));
    }

    final List<Move> computeMoves(Game game) {
        List<Move> moves = new ArrayList<>();
        for (int rank = 0; rank < game.getRanks(); rank++) {
            for (int col = 0; col < game.getColumns(); col++) {
                int num = moves.size();
                Piece p = game.getPiece(rank, col);
                if (p.getPlayerNum() == game.getTurn()) {
                    computeMovesForSquare(game, rank, col, null, moves);
                }
            }
        }
        return moves;
    }


    private void computeMovesForSquare(Game game, int rank, int col, Move parent, List<Move> moves) {
        int startNumMoves = moves.size();
        final Piece p = game.getPiece(rank, col);
        int tr, tc;
        Piece tp;
        final int opponent = game.getOpponent(p.getPlayerNum());
        int [] dr=null;
        int [] dc=null;
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
                // check if we can capture to upper right
                if (game.isOnBoard(tr, (tc=col+1))) {
                    if ((tp = game.getPiece(tr, tc)).getPlayerNum() == opponent) {
                        // if this opponent is the king, then we will be 'checking' him
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(tr, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    }
                }
                // check if we can capture to upper left
                if (game.isOnBoard(tr, tc=col-1)) {
                    if ((tp = game.getPiece(tr, tc)).getPlayerNum() == opponent) {
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(tr, tc, tp.getType()).setEnd(tr, tc, PAWN));
                    }
                }
                // check en passant
                tr = rank;
                if (game.isOnBoard(tr, tc=col+1)) {
                    if ((tp = game.getPiece(tr, tc)).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(rank, tc, tp.getType()).setEnd(tr + game.getAdvanceDir(p.getPlayerNum()), tc, PAWN));
                    }
                }
                if (game.isOnBoard(tr, tc=col-1)) {
                    if ((tp = game.getPiece(tr, tc)).getPlayerNum() == opponent && tp.getType() == PAWN_ENPASSANT) {
                        moves.add(new Move(MoveType.SLIDE, p.getPlayerNum()).setStart(rank, col, p.getType()).addCaptured(rank, tc, tp.getType()).setEnd(tr + game.getAdvanceDir(p.getPlayerNum()), tc, PAWN));
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
                dr = DIAGONAL_DELTAS[0];
                dc = DIAGONAL_DELTAS[1];
                break;
            case KNIGHT: {
                int [][] kd = knightDeltas[rank][col];
                dr = kd[0];
                dc = kd[1];
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
                checkForCastle(game, rank, col, game.getColumns()-1);
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case CHECKED_KING:
                nextType = UNCHECKED_KING;
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
                        moves.add(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType).addCaptured(tr, tc, tp.getType()));
                        break; // can no longer search along this path
                    } else if (tp.getType() == EMPTY) { // look for open
                        moves.add(new Move(mt, p.getPlayerNum()).setStart(rank, col, p.getType()).setEnd(tr, tc, nextType));
                    } else {
                        break; // can no longer search along this path
                    }
                }
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
                game.movePiece(m);
                do {
                    if (m.getMoveType() != MoveType.SWAP) {
                        Piece king = findKing(game, game.getTurn());
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
                            throw new AssertionError("Unhandled case:" + opponentKingStartType);
                    }
                    m.setOpponentKingType(opponentKing.getRank(), opponentKing.getCol(), opponentKingStartType, opponentKingEndType);

                } while (false);
                reverseMove(game, m);
            }
        }
    }

    private Piece findKing(Game game, int playerNum) {
        Piece p;
        for (int rank=0; rank<game.getRanks(); rank++) {
            for (int col =0; col<game.getColumns(); col++) {
                if (((p=game.getPiece(rank, col)).getPlayerNum() == playerNum) && (p.getType().flag & FLAG_KING) != 0) {
                    return p;
                }
            }
        }
        throw new AssertionError("Logic Error: Cannot find king for player " + playerNum);
    }

    private final static int [][] DIAGONAL_DELTAS = {
            {-1, -1, 1, 1},
            {-1, 1, -1, 1}
    };

    // precompute knight deltas for each square

    @Omit
    private static final int[][][][] knightDeltas;

    static {
        knightDeltas = new int[8][8][][];
        for (int i=0; i<8; i++) {
            for (int ii=0; ii<8; ii++) {
                knightDeltas[i][ii] = computeKnightDeltaFor(i, ii);
            }
        }
    }

    private static int [][] computeKnightDeltaFor(int rank, int col) {
        int [][] ALL_KNIGHT_DELTAS = {
                {-2, -2, -1, 1, 2,  2,  1, -1},
                {-1,  1,  2, 2, 1, -1, -2, -2}
        };

        int [][] d = new int[2][8];
        int n = 0;
        for (int i=0; i<8; i++) {
            final int r = rank+ALL_KNIGHT_DELTAS[0][i];
            final int c = col+ALL_KNIGHT_DELTAS[1][i];
            if (r < 0 || c < 0 || r >= 8 || c >= 8)
                continue;

            d[0][n] = ALL_KNIGHT_DELTAS[0][i];
            d[1][n] = ALL_KNIGHT_DELTAS[1][i];
            n++;
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

    private final static int [][] NSEW_DELTAS = {
            {1, 0, -1, 0},
            {0, 1, 0, -1}
    };

    private final static int [][] NSEW_DIAG_DELTAS = {
            {1, 0, -1, 0, -1, -1, 1, 1},
            {0, 1, 0, -1, -1, 1, -1, 1}
    };

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
//            if (game.getMoves().size() == 0) {
//                return value=Long.MIN_VALUE;
//            }
            value = 10 * game.getMoves().size(); // move options is good
            switch (move.getMoveType()) {
                //case SWAP:
                case CASTLE:
                    value += 100; // give preference to these move types
                    break;
                case SLIDE:
                    //if (move.getStartType()==PieceType.UNCHECKED_KING)
                    //    value += 1;
            }
            for (int r = 0; r < game.getRanks(); r++) {
                for (int c = 0; c < game.getColumns(); c++) {
                    Piece p = game.getPiece(r, c);
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
                        case ROOK:
                            value += 500 * scale;
                            break;
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
                                int dist = Math.max(Math.abs(r-move.getOpponentKingPos()[0]), Math.abs(c-move.getOpponentKingPos()[1]));
                                //System.out.println("dist:"+  dist);
                                value -= (dist-2); // 2 units away is best
                            }
                            break;
                        case UNCHECKED_KING_IDLE:
                            value += 1000 * scale; // we want avoid moving this piece
                            break;
                        default:
                            throw new AssertionError("Unhandled case '" + p.getType() + "'");
                    }
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
                Utils.assertTrue(p.getType() == PieceType.ROOK, "Expected ROOK was " + p.getType());
                game.setPiece(m.getCastleRookStart(), m.getPlayerNum(), PieceType.ROOK_IDLE);
                game.clearPiece(m.getCastleRookEnd());
                // fallthrough
            case SLIDE:
            case JUMP:
                game.clearPiece(m.getEnd());
                if (m.hasCaptured()) {
                    if (m.getNumCaptured() != 1)
                        throw new AssertionError("Logic Error: cannot have more than one captured piece in chess");
                    game.setPiece(m.getCaptured(0), game.getOpponent(m.getPlayerNum()), m.getCapturedType(0));
                }
                //fallthrough
            case SWAP:
                game.setPiece(m.getStart(), m.getPlayerNum(), m.getStartType());
                break;
            default:
                throw new AssertionError("Unhandled Case " + m.getMoveType());
        }
        game.setTurn(m.getPlayerNum());
    }
}