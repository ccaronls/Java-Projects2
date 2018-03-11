package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Player extends Reflector<Player> {

    static {
        addAllFields(Player.class);
    }

    List<Tile> tiles = new ArrayList<>();
    int score;

    @Omit
    AAnimation<AGraphics> textAnimation = null;

    final void reset() {
        tiles.clear();
        score = 0;
        textAnimation = null;
    }

    final Tile findTile(int n1, int n2) {
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
