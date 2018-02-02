package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;

public class Player {

    List<Piece> tiles = new ArrayList<>();
    int score;

    boolean hasTile(int n1, int n2) {
        for (Piece p : tiles) {
            if (p.num1 == n1 && p.num2 == n2)
                return true;
            if (p.num2 == n1 && p.num1 == n2)
                return true;
        }
        return false;
    }

    public Piece removeTile(int n1, int n2) {
        Iterator<Piece> it = tiles.iterator();
        while (it.hasNext()) {
            Piece p = it.next();
            if ((p.num1 == n1 && p.num2 == n2) ||
                (p.num2 == n1 && p.num1 == n2)) {
                it.remove();
                return p;
            }
        }
        return null;
    }

    public Move chooseMove(List<Move> moves) {
        return moves.get(Utils.rand() % moves.size());
    }
}
