package cc.lib.checkerboard;

/**
 * Also known as Russian Checkers
 */
public class Shashki extends Checkers {
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
    public boolean isMaxJumpsMandatory() {
        return true;
    }

    @Override
    public boolean isCaptureAtEndEnabled() {
        return false;
    }

    @Override
    public boolean isFlyingKings() {
        return true;
    }

    @Override
    public boolean isKingPieces() {
        return true;
    }
}
