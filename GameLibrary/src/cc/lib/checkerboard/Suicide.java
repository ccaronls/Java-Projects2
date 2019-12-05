package cc.lib.checkerboard;

/**
 * same as checkers only winner is the first to sacrafice all pieces or otherwise fail to be able to move
 */
public class Suicide extends Checkers {
    @Override
    int getWinner(Game game) {
        switch (super.getWinner(game)) {
            case Game.NEAR:
                return Game.FAR;
            case Game.FAR:
                return Game.NEAR;
        }
        return -1;
    }
}
