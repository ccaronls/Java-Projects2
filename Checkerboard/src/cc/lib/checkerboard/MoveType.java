package cc.lib.checkerboard;

public enum MoveType {
    END(1, false),
    SLIDE(0, false),
    FLYING_JUMP(5, true),
    JUMP(2, true),
    STACK(10, false),
    SWAP(10, false),
    CASTLE(6, false);

    MoveType(int value, boolean isJump) {
        this.value = value;
        this.isJump = isJump;
    }

    final int value;
    final boolean isJump;
}