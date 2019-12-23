package cc.lib.checkerboard;

/**
 * Variation on Russian checkers where captured pieces are not removed but placed under the jumping piece making for taller and taller stacks.
 * If a stacked piece is jumnped, only the top checker is removed causing pieces to be released.
 */
public class Columns extends Shashki {

    @Override
    public boolean isStackingCaptures() {
        return true;
    }

    @Override
    public boolean isFlyingKings() {
        return false;
    }
}
