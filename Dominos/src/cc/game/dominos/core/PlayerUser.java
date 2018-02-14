package cc.game.dominos.core;

import java.util.List;

/**
 * Created by chriscaron on 2/14/18.
 */

public class PlayerUser extends Player {

    Tile tile = null;
    int endpoint = -1;
    int placement = 0;

    @Override
    public Move chooseMove(Dominos game, List<Move> moves) {

        tile = null;
        endpoint = -1;
        placement = 0;

        synchronized (game) {
            try {
                game.wait();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

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
