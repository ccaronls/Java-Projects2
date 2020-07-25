package cc.lib.checkerboard;

public enum MoveType {
    END(1), SLIDE(0), FLYING_JUMP(5), JUMP(2), STACK(10), SWAP(10), CASTLE(6);

    MoveType(int value) {
        this.value = value;
    }

    final int value;
}