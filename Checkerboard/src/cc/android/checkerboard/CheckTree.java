package cc.android.checkerboard;

/**
 * Created by chriscaron on 10/5/17.
 */

public class CheckTree extends DescisionTree<Checkers, Move> {

    public CheckTree(Checkers game) {
        super(game);
    }
    public CheckTree(Checkers game, Move move) {
        super(game,move);
    }

}