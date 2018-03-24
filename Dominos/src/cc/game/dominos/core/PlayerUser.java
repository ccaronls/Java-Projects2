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

    public PlayerUser() {
    }

    public PlayerUser(int playerNum) {
        super(playerNum);
    }

    @Override
    public Move chooseMove(Dominos game, List<Move> moves) {

        clearMoves();
        this.moves.addAll(moves);
        for (Move m : moves) {
            usable.add(m.piece);
        }

        game.redraw();
        Utils.waitNoThrow(game, 2000);
        if (choosedMove != null)
            usable.clear();

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

    protected List<Move> getMoves() {
        return moves;
    }
}
