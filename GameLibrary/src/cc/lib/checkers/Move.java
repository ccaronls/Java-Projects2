package cc.lib.checkers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.IMove;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 9/14/17.
 */

public class Move extends Reflector<Move> implements IMove {

    static {
        addAllFields(Move.class);
    }

    private final MoveType type;
    private final int playerNum;
    private int startRank=-1, startCol=-1;
    private PieceType startType=null;
    private PieceType endType=null;
    private List<Path> steps = null;

    public Move() {
        type = null;
        playerNum = -1;
    }

    public Move(MoveType mt, int playerNum) {
        this.type = mt;
        this.playerNum = playerNum;
    }

    public Move setStart(int rank, int col, PieceType startType, PieceType endType) {
        this.startRank = rank;
        this.startCol = col;
        this.startType = startType;
        this.endType = endType != null ? endType : startType;
        return this;
    }

    public Move addPath(int landRank, int landCol) {
        return addPath(landRank, landCol, -1, -1, null);
    }

    public Move addPath(int landRank, int landCol, int captureRank, int captureCol, Piece captured) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(new Path(landRank, landCol, captureRank, captureCol, captured));
        return this;
    }

    @Override
    public final int getPlayerNum() {
        return playerNum;
    }

    public final MoveType getMoveType() {
        return type;
    }

    public final int [] getStart() {
        return new int[] { startRank, startCol };
    }

    public final int [] getEnd() {
        Path last = steps.get(steps.size()-1);
        return new int [] { last.landRank, last.landCol };
    }

    public final PieceType getNextType() {
        return endType;
    }

    public final PieceType getStartType() {
        return this.startType;
    }

    public final List<Piece> getCaptured() {
        ArrayList<Piece> captured = new ArrayList<>();
        if (steps != null) {
            for (Path i : steps) {
                if (i.captured != null)
                    captured.add(i.captured);
            }
        }
        return captured;
    }

    public int [] getCastleRookStart() {
        Path first = steps.get(0);
        return new int [] { first.captureRank, first.captureCol };
    }

    public int [] getCastleRookEnd() {
        Path first = steps.get(0);
        return new int [] { first.landRank, first.landCol };
    }

    public int [] getCapturedPosition(int index) {
        Path i = steps.get(index);
        return new int [] { i.captureRank, i.captureCol };
    }

    public Iterable<Path> getPath() {
        return steps;
    }

    public Iterable<int[]> getCapturedPositions() {
        return new Iterable<int[]>() {
            @Override
            public Iterator<int[]> iterator() {
                return new Iterator<int[]>() {
                    int index = 0;
                    @Override
                    public boolean hasNext() {
                        return steps != null && index < steps.size();
                    }

                    @Override
                    public int[] next() {
                        int [] pos = { steps.get(index).captureRank, steps.get(index).captureCol };
                        index++;
                        return pos;
                    }
                };
            }
        };
    }

    public boolean hasEnd() {
        return steps != null;
    }

    /*
    public final MoveType type;
    public final int playerNum;

    private final int [][] squares;

    public final Piece captured;
    public PieceType nextType;

    public Move(MoveType t, int playerNum, Piece captured, PieceType nextType, int ... positions) {
        this.type = t;
        this.captured = captured;
        this.nextType = nextType;
        this.playerNum = playerNum;
        squares = new int[positions.length/2][];
        for (int i=0; i<positions.length; i+=2) {
            int sr = positions[i];
            int er = positions[i+1];
            squares[i/2] = new int[] { sr, er };
        }
    }

    public Move() {
        this(null, -1, null, null);
    }

    public final int [] getStart() {
        return squares[0];
    }

    public final int [] getEnd() {
        return squares[1];
    }

    public final boolean hasEnd() {
        return squares.length >= 2;
    }

    public final int [] getCaptured() {
        return squares[2];
    }

    public final int [] getCastleRookStart() {
        return squares[2];
    }

    public final int [] getCastleRookEnd() {
        return squares[3];
    }

    @Override
    public final String toString() {
        return type.name() + Utils.toString(squares) + " pn:" + playerNum + " nt: " + nextType + " cap:"
                + (captured == null ? "null" : captured.type.name() + "[" + captured.playerNum + "]")
                ;
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }*/
}