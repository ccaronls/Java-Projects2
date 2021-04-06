package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.GException;

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

    private final static int [][] OTHER_POSITIONS;

    static {
        OTHER_POSITIONS = new int[6*6-FAR_POSITIONS.length-NEAR_POSITIONS.length][];
        int index=0;
        for (int i=0; i<6; i++) {
            for (int ii=0; ii<6; ii++) {
                if (!isInArray(i, ii, FAR_POSITIONS) && !isInArray(i, ii, NEAR_POSITIONS)) {
                    OTHER_POSITIONS[index++] = new int[] { i, ii };
                }
            }
        }
        if (index != OTHER_POSITIONS.length) {
            throw new GException();
        }
    }

    static boolean isInArray(int i0, int i1, int [][] arr) {
        for (int i=0; i<arr.length; i++) {
            if (arr[i][0] == i0 && arr[i][1] == i1)
                return true;
        }
        return false;
    }

    @Override
    void init(Game game) {
        game.init(6, 6);
        game.clear();
        for (int [] pos : FAR_POSITIONS) {
            int p = pos[0]<<8 | pos[1];
            game.setPiece(p, FAR, DAMA_MAN);
        }
        for (int [] pos : NEAR_POSITIONS) {
            int p = pos[0]<<8 | pos[1];
            game.setPiece(p, NEAR, DAMA_MAN);
        }
        game.setTurn(Utils.flipCoin() ? NEAR : FAR);
        game.setInitialized(true);
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
                for (int [] pos : NEAR_POSITIONS) {
                    Piece p = game.getPiece(pos[0]<<8 | pos[1]);
                    if (p.getPlayerNum() != side)
                        return false;
                }
                break;
            }
            case NEAR: {
                for (int [] pos : FAR_POSITIONS) {
                    Piece p = game.getPiece(pos[0]<<8 | pos[1]);
                    if (p.getPlayerNum() != side)
                        return false;
                }
                break;
            }
            default:
                throw new GException("Unhandled case " + side);
        }
        return true;
    }

    @Override
    public boolean canJumpSelf() {
        return true;
    }

    @Override
    public long evaluate(Game game, Move move) {

        int playerNum = move.getPlayerNum();
        List<int []> targetPositions = new ArrayList<>();
        if (playerNum == NEAR)
            targetPositions.addAll(Arrays.asList(FAR_POSITIONS));
        else
            targetPositions.addAll(Arrays.asList(NEAR_POSITIONS));

        int totalD = 0;
        for (Piece p : game.getPieces(playerNum)) {

            int clIdx = getClosest(p.getPosition(), targetPositions);
            int [] pos = targetPositions.remove(clIdx);

            totalD += dist(pos, p.getPosition());
        }

        Utils.assertTrue(totalD > 0);

        return Integer.MAX_VALUE - totalD;
    }

    private long evaluate_old(Game game, Move move) {

        int numNearPiecesInPlace = 0;
        int numFarPiecesInPlace = 0;
        int [][] nearPositions = new int[NEAR_POSITIONS.length][];
        int [][] farPositions = new int[FAR_POSITIONS.length][];
        List<Piece> farPiecesNotInPlace = new ArrayList<>();
        List<Piece> nearPiecesNotInPlace = new ArrayList<>();
        int numNearPositions = 0;
        int numFarPositions = 0;

        for (int r=0; r<game.getRanks(); r++) {
            for (int c = 0; c < game.getColumns(); c++) {
                Piece p = game.getPiece(r, c);
                if (p.getType() == EMPTY)
                    continue;
                Utils.assertTrue(p.getType() == DAMA_MAN, "Invalid piece");
                switch (p.getPlayerNum()) {
                    case NEAR:
                    case FAR:
                }
            }
        }

        for (int [] pos : NEAR_POSITIONS) {
            Piece p = game.getPiece(pos[0], pos[1]);
            if (p.getType() == EMPTY)
                continue;
            if (p.getType() != DAMA_MAN)
                throw new GException("Invalid piece");
            switch (p.getPlayerNum()) {
                case FAR:
                    numFarPiecesInPlace++;
                    break;
                case NEAR:
                    nearPiecesNotInPlace.add(p);
                    nearPositions[numNearPositions++] = pos;
            }
        }

        for (int [] pos : FAR_POSITIONS) {
            Piece p = game.getPiece(pos[0], pos[1]);
            if (p.getType() == EMPTY)
                continue;
            if (p.getType() != DAMA_MAN)
                throw new GException("Invalid piece");
            switch (p.getPlayerNum()) {
                case NEAR:
                    numNearPiecesInPlace++;
                    break;
                case FAR:
                    farPiecesNotInPlace.add(p);
                    farPositions[numFarPositions++] = pos;
            }
        }

        for (int [] pos : OTHER_POSITIONS) {
            Piece p = game.getPiece(pos[0], pos[1]);
            if (p.getType() == EMPTY)
                continue;
            if (p.getType() != DAMA_MAN)
                throw new GException("Invalid piece");
            switch (p.getPlayerNum()) {
                case NEAR:
                    nearPiecesNotInPlace.add(p);
                    break;
                case FAR:
                    farPiecesNotInPlace.add(p);
                    break;
            }
        }

        long nearValue=1000*numNearPiecesInPlace, farValue=1000*numFarPiecesInPlace;
        for (Piece p :nearPiecesNotInPlace) {
            nearValue -= distBetween(p.getPosition(), farPositions, numFarPositions);
        }
        for (Piece p : farPiecesNotInPlace) {
            farValue -= distBetween(p.getPosition(), nearPositions, numNearPositions);
        }

        if (move.getPlayerNum() == NEAR) {
            return nearValue - farValue;
        } else {
            return farValue - nearValue;
        }
    }

    int distBetween(int pos, int [][] positions, int num) {
        if (num == 0)
            return 0;
        int minD = Integer.MAX_VALUE;
        for (int i=0; i<num; i++) {
            int [] p1 = positions[i];
            int d = dist(p1, pos);
            minD = Math.min(minD, d);
        }
        return minD;
    }

    int getClosest(int pos, List<int []> positions) {
        Utils.assertTrue(positions.size() > 0);
        int [] cl = positions.get(0);
        int p0r = pos>>8;
        int p0c = pos&0xff;
        int minD = Math.abs(p0r - cl[0]) + Math.abs(p0c - cl[1]);
        for (int i=0; i<positions.size(); i++) {
            int [] p1 = positions.get(i);
            int d = dist(p1, pos);
            minD = Math.min(minD, d);
        }
        return minD;
    }

    int dist(int [] p0, int pos) {
        final int posrank = pos>>8;
        final int poscol  = pos & 0xff;
        return Math.abs(p0[0] - posrank) + Math.abs(p0[1] - poscol);
    }
}
