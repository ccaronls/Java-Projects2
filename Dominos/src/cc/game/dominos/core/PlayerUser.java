package cc.game.dominos.core;

import java.util.HashSet;
import java.util.List;

/**
 * Created by chriscaron on 2/14/18.
 */

public class PlayerUser extends Player {

    Tile tile = null;
    int endpoint = -1;
    int placement = 0;
    final HashSet<Tile> usable = new HashSet<>();

    @Override
    public Move chooseMove(Dominos game, List<Move> moves) {

        tile = null;
        endpoint = -1;
        placement = 0;

        usable.clear();
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

        if (tile != null) {
            for (Move m : moves) {
                if (m.piece.equals(tile) && m.endpoint == endpoint && m.placment == placement) {
                    tile = null;
                    return m;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isPiecesVisible() {
        return true;
    }
}
