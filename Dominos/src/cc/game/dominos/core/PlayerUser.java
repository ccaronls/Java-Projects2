package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by chriscaron on 2/14/18.
 */

public class PlayerUser extends Player {

    Move choosedMove = null;
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
        synchronized (game) {
            try {
                game.wait();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

        usable.clear();

        return choosedMove;
    }

    @Override
    public boolean isPiecesVisible() {
        return true;
    }
}
