package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Piece extends Reflector<Piece> {

    static {
        addAllFields(Piece.class);
    }

    public int playerNum;
    public int stacks;
    public final List<Move> moves = new ArrayList<>();

    public Piece() {
        playerNum = -1;
        stacks = 0;
    }

    Piece(int playerNum, int stacks) {
        this.playerNum = playerNum;
        this.stacks = stacks;
    }
}