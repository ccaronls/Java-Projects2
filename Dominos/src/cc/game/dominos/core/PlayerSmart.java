package cc.game.dominos.core;

import java.util.List;

/**
 * Created by chriscaron on 2/24/18.
 */

public class PlayerSmart extends Player {

    @Override
    public Move chooseMove(Dominos game, List<Move> moves) {
        Move best = null;
        int bestPts = 0;
        for (Move m : moves) {
            Board copy = game.getBoard().deepCopy();
            copy.doMove(m.piece, m.endpoint, m.placment);
            int pts = copy.computeEndpointsTotal();
            if (pts % 5 == 0) {
                if (bestPts < pts) {
                    bestPts = pts;
                    best = m;
                }
            }
        }
        return best;
    }
}
