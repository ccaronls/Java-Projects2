package cc.lib.probot;

public enum Direction {
    Right(1, 0),
    Down(0, 1),
    Left(-1, 0),
    Up(0, -1),
    ;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    final int dx, dy;
}