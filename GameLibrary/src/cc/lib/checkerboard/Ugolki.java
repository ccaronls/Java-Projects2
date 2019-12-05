package cc.lib.checkerboard;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

import static cc.lib.checkerboard.Game.FAR;
import static cc.lib.checkerboard.Game.NEAR;
import static cc.lib.checkerboard.PieceType.DAMA_MAN;
import static cc.lib.checkerboard.PieceType.EMPTY;

public class Ugolki extends Checkers {

    private final static int [][] FAR_POSITIONS = {
            { 0,3 }, { 0,4 }, { 0,5 },
            { 1,3 }, { 1,4 }, { 1,5 }
    };

    private final static int [][] NEAR_POSITIONS = {
            { 4,0 }, { 4,1 }, { 4,2 },
            { 5,0 }, { 5,1 }, { 5,2 }
    };

    @Override
    void init(Game game) {
        game.init(6, 6);
        game.clear();
        for (int [] pos : FAR_POSITIONS) {
            game.setPiece(pos, FAR, DAMA_MAN);
        }
        for (int [] pos : NEAR_POSITIONS) {
            game.setPiece(pos, NEAR, DAMA_MAN);
        }
        game.setTurn(Utils.flipCoin() ? NEAR : FAR);
    }

    @Override
    public Color getPlayerColor(int side) {
        return side == FAR ? Color.BLACK : Color.WHITE;
    }

    @Override
    public boolean isNoCaptures() {
        return true;
    }

    @Override
    public boolean isKingPieces() {
        return false;
    }

    @Override
    boolean isDraw(Game game) {
        return false; // draw game not possible in Ukogli
    }

    @Override
    int getWinner(Game game) {
        if (isWinner(game, NEAR)) {
            return NEAR;
        }
        if (isWinner(game, FAR)) {
            return FAR;
        }
        return -1;
    }

    public boolean isWinner(Game game, int side) {
        switch (side) {
            case FAR: {
                for (int [] pos : FAR_POSITIONS) {
                    Piece p = game.getPiece(pos);
                    if (p.getPlayerNum() != side)
                        return false;
                }
                break;
            }
            case NEAR: {
                for (int [] pos : NEAR_POSITIONS) {
                    Piece p = game.getPiece(pos);
                    if (p.getPlayerNum() != side)
                        return false;
                }
                break;
            }
            default:
                throw new AssertionError("Unhandled case " + side);
        }
        return true;
    }

    @Override
    public boolean canJumpSelf() {
        return true;
    }

    @Override
    public long evaluate(Game game, Move move) {
        if (isWinner(game, game.getTurn())) {
            // having no moves is good!
            return Long.MAX_VALUE;
        }
        if (isWinner(game, game.getOpponent())) {
            return Long.MIN_VALUE;
        }
        long value = game.getMoves().size(); // move options is good
        int numPiecesInPlace = 0;
        int [][] nearPositions = new int[NEAR_POSITIONS.length][];
        int [][] farPositions = new int[FAR_POSITIONS.length][];
        int numNearPositions = 0;
        int numFarPositions = 0;
        switch (game.getTurn()) {
            case FAR: {
                for (int [] pos : FAR_POSITIONS) {
                    Piece p = game.getPiece(pos);
                    if (p.getPlayerNum() == NEAR)
                        numPiecesInPlace ++;
                    else if (p.getType() == EMPTY) {
                        farPositions[numFarPositions++] = pos;
                    }
                }
                break;
            }
            case NEAR: {
                for (int [] pos : NEAR_POSITIONS) {
                    Piece p = game.getPiece(pos);
                    if (p.getPlayerNum() == FAR)
                        numPiecesInPlace ++;
                    else if (p.getType() == EMPTY) {
                        nearPositions[numNearPositions++] = pos;
                    }
                }
                break;
            }
            default:
                throw new AssertionError("Unhandled case " + game.getTurn());
        }

        value += 1000 * numPiecesInPlace;
        for (int r = 0; r<game.getRanks(); r++) {
            for (int c = 0; c < game.getColumns(); c++) {
                Piece p = game.getPiece(r, c);
                if (p.getType() == EMPTY)
                    continue;
                if (p.getType() != DAMA_MAN)
                    throw new AssertionError("Invalid piece");
                switch (p.getPlayerNum()) {
                    case FAR:
                        value -= distBetween(p.getPosition(), nearPositions, numNearPositions);
                        break;
                    case NEAR:
                        value -= distBetween(p.getPosition(), farPositions, numFarPositions);
                        break;
                }
            }
        }
        return value;
    }

    int distBetween(int [] p0, int [][] positions, int num) {
        if (num == 0)
            return 0;
        int minD = Integer.MAX_VALUE;
        for (int i=0; i<num; i++) {
            int [] p1 = positions[i];
            int d = Math.abs(p0[0] - p1[0]) + Math.abs(p1[0] - p1[1]);
            minD = Math.min(minD, d);
            if (minD == 0)
                return 0;
        }
        return minD;
    }
}
