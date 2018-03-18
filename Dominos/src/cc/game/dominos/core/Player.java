package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;
import cc.lib.utils.SyncList;

public class Player extends Reflector<Player> {

    static {
        addAllFields(Player.class);
    }

    List<Tile> tiles = new SyncList<>(new ArrayList<Tile>());
    int score;
    public final int playerNum;
    boolean smart = false;

    public Player() {
        this(-1);
    }

    public Player(int playerNum) {
        this.playerNum = playerNum;
    }

    public String getName() {
        return "P" + (playerNum+1);
    }

    @Omit
    final GRectangle outlineRect = new GRectangle();

    final void reset() {
        tiles.clear();
        score = 0;
    }

    final synchronized Tile findTile(int n1, int n2) {
        for (Tile p : tiles) {
            if (p.pip1 == n1 && p.pip2 == n2)
                return p;
            if (p.pip2 == n1 && p.pip1 == n2)
                return p;
        }
        return null;
    }

    /**
     * Override to change behavior. Base method does random pick of availabel choices
     *
     * @param game
     * @param moves
     * @return
     */
    public Move chooseMove(Dominos game, List<Move> moves) {
        if (smart) {
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
            if (best != null)
                return best;
        }
        return moves.get(Utils.rand() % moves.size());
    }

    public final List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public final int getScore() {
        return score;
    }

    public boolean isPiecesVisible() { return false; }
}
