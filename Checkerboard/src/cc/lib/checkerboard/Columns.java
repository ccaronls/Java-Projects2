package cc.lib.checkerboard;

/**
 * Variation on Russian checkers where captured pieces are not removed but placed under the jumping piece making for taller and taller stacks.
 * If a stacked piece is jumnped, only the top checker is removed causing pieces to be released.
 */
public class Columns extends Checkers {

    @Override
    public boolean isStackingCaptures() {
        return true;
    }

    @Override
    public boolean canJumpSelf() {
        return false;
    }

    @Override
    public boolean canMenJumpBackwards() {
        return true;
    }

    @Override
    public boolean isJumpsMandatory() {
        return true;
    }

    @Override
    public boolean isFlyingKings() {
        return true;
    }

    @Override
    String getDescription() {
        return "A single piece may not move backward unless it has reached the opponent's king-row as a single man (whether or not covered and uncovered on the way), after which it is inverted or marked and may at any future time in the game move either backward or forward.\n"
            + "A stack composed of two or more checkers of the same or different colors may move either backward or forward. A single piece confronted with a double jump may, after completion of the first jump, when it becomes a double, continue jumping in any direction.\n"
            + "The same stack may not be jumped twice in the same series of jumps.\n"
            + "If a jump is possible it must be made. There is no huffing or alternative penalty.\n"
            + "The game is drawn if a player remains in possession of a man and cannot legally move.";
    }

    @Override
    boolean isDraw(Game game) {
        if (game.getMoves().size() == 0)
            return true;
        return super.isDraw(game);
    }

    @Override
    public long evaluate(Game game, Move move) {
        long value = 0;
        for (Piece p : game.getPieces(move.getPlayerNum())) {
            for (int i=0; i<p.getStackSize(); i++) {
                if (p.getStackAt(i) == move.getPlayerNum())
                    value++;
                else
                    break;
            }
        }

        for (Piece p : game.getPieces(Game.getOpponent(move.getPlayerNum()))) {
            // how many stacks of the top of the opponent player
            int cnt = 0;
            for (int i=0; i<p.getStackSize(); i++) {
                if (p.getStackAt(i) == move.getPlayerNum()) {
                    cnt++;
                }
                else {
                    value--;
                    if (cnt > 0) {
                        break;
                    }
                }
            }
        }
        return value;
    }
}
