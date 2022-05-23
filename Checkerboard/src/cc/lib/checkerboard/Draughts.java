package cc.lib.checkerboard;

import cc.lib.game.Utils;

import static cc.lib.checkerboard.Game.*;
import static cc.lib.checkerboard.PieceType.*;

public class Draughts extends Checkers {

    @Override
    public void init(Game game) {
        game.init(10, 10);
        game.initRank(0, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(1, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.initRank(2, FAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(3, FAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(5, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(6, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(7, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.initRank(8, NEAR, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY);
        game.initRank(9, NEAR, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER, EMPTY, CHECKER);
        game.setTurn(Utils.flipCoin() ? FAR : NEAR);
    }

    @Override
    public boolean isFlyingKings() {
        return true;
    }

    @Override
    public boolean canJumpSelf() {
        return false;
    }

    @Override
    public boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    public boolean isJumpsMandatory() {
        return true;
    }

    @Override
    public boolean isCaptureAtEndEnabled() {
        return true;
    }
}
