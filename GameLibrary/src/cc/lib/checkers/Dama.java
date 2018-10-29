package cc.lib.checkers;

import cc.lib.game.Utils;

import static cc.lib.checkers.PieceType.DAMA_MAN;
import static cc.lib.checkers.PieceType.EMPTY;

public class Dama extends Checkers {

    public final void initBoard() {
        initRank(0, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(1, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(2, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(6, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        initRank(7, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        setTurn(Utils.flipCoin() ? FAR : NEAR);
    }

    @Override
    protected boolean isJumpsMandatory() {
        return true;
    }

    @Override
    protected boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    protected boolean canJumpSelf() {
        return false;
    }

    @Override
    public BoardType getBoardType() {
        return BoardType.DAMA;
    }

}
