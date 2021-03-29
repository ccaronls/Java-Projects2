package cc.lib.checkerboard;

import cc.lib.game.Utils;

public class AIStats {

    final long startTime;
    long prunes=0;
    long evalCount = 0;
    long evalTimeTotalMSecs = 0;
    long [] pieceTypeCount = new long[PieceType.values().length];
    double [] pieceTypeValue = new double[PieceType.values().length];

    AIStats() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {

        String s = "Run Tiime: " + Utils.formatTime(System.currentTimeMillis()-startTime)
                + "\nprunes=" + prunes
                + "\nPruned %" + (100f * prunes / evalCount)
                + "\nevalCount=" + evalCount
                + "\nevalTimeTotalMSecs=" + evalTimeTotalMSecs;

        for (int i=0; i<pieceTypeValue.length; i++) {
            if (pieceTypeCount[i] > 0)
                s += String.format("\n%-20s AVG: %5.3f", PieceType.values()[i], (pieceTypeValue[i] / pieceTypeCount[i]));
        }


        return s;
    }
}
