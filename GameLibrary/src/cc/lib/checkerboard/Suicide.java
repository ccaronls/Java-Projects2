package cc.lib.checkerboard;

/**
 * same as checkers only winner is the first to sacrafice all pieces or otherwise fail to be able to move
 */
public class Suicide extends Checkers {
    @Override
    Player getWinner(Game game) {
        return game.getCurrentPlayer();
    }
}
