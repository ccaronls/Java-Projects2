package cc.lib.checkerboard;

import cc.lib.game.Utils;

import static cc.lib.checkerboard.PieceType.*;
import static cc.lib.checkerboard.Game.*;

public class Dama extends Checkers {

    @Override
    public void init(Game game) {
        game.init(8, 8);
        game.initRank(0, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(1, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(2, FAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(3, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(4, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.initRank(5, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(6, NEAR, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN, DAMA_MAN);
        game.initRank(7, -1, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
        game.setTurn(Utils.flipCoin() ? FAR : NEAR);
    }

    @Override
    public boolean isJumpsMandatory() {
        return true;
    }

    @Override
    public boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    public boolean canJumpSelf() {
        return false;
    }

    @Override
    public boolean isFlyingKings() {
        return true;
    }
}
