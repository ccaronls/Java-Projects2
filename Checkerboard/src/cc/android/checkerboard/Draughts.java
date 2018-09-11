package cc.android.checkerboard;

import cc.lib.game.Utils;

import static cc.android.checkerboard.PieceType.CHECKER;
import static cc.android.checkerboard.PieceType.EMPTY;

public class Draughts extends Checkers {

    public Draughts() {
        super(10, 10);
    }

    public final void initBoard() {
        initRank(0, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(1, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(2, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(3, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(5, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        initRank(6, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(7, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        initRank(8, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        initRank(9, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        setTurn(Utils.flipCoin() ? FAR : NEAR);
    }

    @Override
    protected boolean isFlyingKings() {
        return true;
    }

    @Override
    protected boolean canJumpSelf() {
        return false;
    }

    @Override
    protected boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    protected boolean isJumpsMandatory() {
        return true;
    }
}
