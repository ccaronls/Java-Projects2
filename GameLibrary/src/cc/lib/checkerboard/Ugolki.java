package cc.lib.checkerboard;

import sun.jvm.hotspot.utilities.AssertionFailure;

import static cc.lib.checkerboard.Game.FAR;
import static cc.lib.checkerboard.Game.NEAR;
import static cc.lib.checkerboard.PieceType.DAMA_MAN;
import static cc.lib.checkerboard.PieceType.EMPTY;

public class Ugolki extends Checkers {
    @Override
    void init(Game game) {
        game.init(6, 6);
        game.initRank(0, FAR, EMPTY, EMPTY, EMPTY, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(1, FAR, EMPTY, EMPTY, EMPTY, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(2, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(4, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, EMPTY, EMPTY, EMPTY);
        game.initRank(5, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, EMPTY, EMPTY, EMPTY);
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
    public boolean isWinner(Game game, int side) {
        switch (side) {
            case FAR: {
                for (int i=0; i<3; i++) {
                    for (int ii=4; ii<6; ii++) {
                        Piece p = game.getPiece(ii, i);
                        if (p.getPlayerNum() != side)
                            return false;
                    }
                }
                break;
            }
            case NEAR: {
                for (int i=3; i<6; i++) {
                    for (int ii=0; ii<2; ii++) {
                        Piece p = game.getPiece(ii, i);
                        if (p.getPlayerNum() != side)
                            return false;
                    }
                }
                break;
            }
            default:
                throw new AssertionError("Unhandled case " + side);
        }
        return true;
    }
}
