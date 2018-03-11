package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 2/14/18.
 */

public class PlayerUser extends Player {

    private Move choosedMove = null;
    final HashSet<Tile> usable = new HashSet<>();
    final List<Move> moves = new ArrayList<>();

    @Override
    public Move chooseMove(Dominos game, List<Move> moves) {

        choosedMove = null;
        usable.clear();
        this.moves.clear();
        this.moves.addAll(moves);
        for (Move m : moves) {
            usable.add(m.piece);
        }

        game.redraw();
        if (game.isGameRunning()) {
            Utils.waitNoThrow(game, -1);
            usable.clear();
        }

        return choosedMove;
    }

    @Override
    public boolean isPiecesVisible() {
        return true;
    }

    public Move getChoosedMove() {
        return choosedMove;
    }

    public void setChoosedMove(Move choosedMove) {
        this.choosedMove = choosedMove;
    }

    protected void clearMoves() {
        moves.clear();
        usable.clear();
        choosedMove = null;
    }
}
