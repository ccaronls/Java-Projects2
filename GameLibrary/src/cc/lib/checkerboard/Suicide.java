package cc.lib.checkerboard;

/**
 * same as checkers only winner is the first to sacrifice all pieces or otherwise fail to be able to move
 */
public class Suicide extends Checkers {
    @Override
    int getWinner(Game game) {
        int winner;
        switch (winner=super.getWinner(game)) {
            case Game.NEAR:
                return Game.FAR;
            case Game.FAR:
                return Game.NEAR;
        }
        return winner;
    }

    @Override
    public long evaluate(Game game, Move move) {
        return -1L * super.evaluate(game, move);
    }

    @Override
    public boolean isJumpsMandatory() {
        return true;
    }

    @Override
    String getDescription() {
        return "Objective: to LOSE all your pieces";
    }
}
