package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Iterator;
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
    AAnimation<AGraphics> animation = null;

    boolean hasTile(int n1, int n2) {
        for (Tile p : tiles) {
            if (p.pip1 == n1 && p.pip2 == n2)
                return true;
            if (p.pip2 == n1 && p.pip1 == n2)
                return true;
        }
        return false;
    }

    public Tile removeTile(int n1, int n2) {
        Iterator<Tile> it = tiles.iterator();
        while (it.hasNext()) {
            Tile p = it.next();
            if ((p.pip1 == n1 && p.pip2 == n2) ||
                (p.pip2 == n1 && p.pip1 == n2)) {
                it.remove();
                return p;
            }
        }
        return null;
    }

    public Move chooseMove(Dominos game, List<Move> moves) {
        return moves.get(Utils.rand() % moves.size());
    }

    public final List<Tile> getTiles() {
        return tiles;//new Collections.unmodifiableList(tiles);
    }

    public final int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isPiecesVisible() { return false; }
}
