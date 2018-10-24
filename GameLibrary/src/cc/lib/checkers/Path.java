package cc.lib.checkers;

import cc.lib.utils.Reflector;

public class Path extends Reflector<Path> {

    static {
        addAllFields(Path.class);
    }

    final int landRank, landCol;
    final int captureRank, captureCol;
    final Piece captured;

    public Path() {
        landRank = landCol = captureRank = captureCol = -1;
        captured = null;
    }

    Path(int landRank, int landCol, int captureRank, int captureCol, Piece captured) {
        this.landRank = landRank;
        this.landCol = landCol;
        this.captureRank = captureRank;
        this.captureCol = captureCol;
        this.captured = captured;
    }

    public int [] getLandPos() {
        return new int [] { landRank, landCol };
    }

    public int [] getCapturePos() {
        return new int [] { captureRank, captureCol };
    }

    public Piece getCaptured() {
        return captured;
    }
}
