package cc.android.checkerboard;

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

    public Piece() {
        playerNum = stacks = 0;
    }

    public Piece(int playerNum, int stacks) {
        this.playerNum = playerNum;
        this.stacks = stacks;
    }
}