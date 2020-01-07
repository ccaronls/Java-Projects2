package cc.lib.geniussqaure;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;
import cc.lib.utils.StopWatch;

public class GeniusSquares extends Reflector<GeniusSquares> { // GeniusSquare. 6x6 board

    public final static Logger log = LoggerFactory.getLogger(GeniusSquares.class);

    static {
        addAllFields(GeniusSquares.class);
        addAllFields(Piece.class);
    }

    public enum PieceType {
        PIECE_0(null, 0, 0, null), // this way the matrix value aligns with ordinal()
        PIECE_1x1(GColor.BLUE, 0, 0, new int [][][] {
                {{ 1 }}
        }), // matrices are row major
        PIECE_2x2(GColor.GREEN, 1, 0, new int [][][] {
                {{ 2, 2 }, { 2, 2 }}
        }),
        PIECE_1x2(GColor.BROWN, 0, 2, new int [][][] {
                {{ 3, 3 }}, {{ 3 }, { 3 }}
        }),
        PIECE_1x3(GColor.ORANGE, 0, 3, new int [][][] {
                {{ 4, 4, 4 }}, {{ 4 }, { 4 }, { 4 }}
        }),
        PIECE_1x4(GColor.GRAY, 0, 7, new int [][][] {
                {{ 5, 5, 5, 5 }},
                {{ 5 }, { 5 }, { 5 }, { 5 }}
        }),
        PIECE_EL(GColor.CYAN, 2, 0, new int [][][] {
                {{ 0, 6 }, { 0, 6 }, { 6, 6 }},
                {{ 6, 6 }, { 0, 6 }, { 0, 6 }},
                {{ 6, 6 }, { 6, 0 }, { 6, 0 }},
                {{ 6, 0 }, { 6, 0 }, { 6, 6 }},
                {{ 6, 0, 0 }, { 6, 6, 6 }},
                {{ 0, 0, 6 }, { 6, 6, 6 }},
                {{ 6, 6, 6 }, { 6, 0, 0 }},
                {{ 6, 6, 6 }, { 0, 0, 6 }}
        }),
        PIECE_BEND(GColor.MAGENTA, 2, 3, new int [][][] {
                {{ 0, 7 }, { 7, 7 }},
                {{ 7, 0 }, { 7, 7 }},
                {{ 7, 7 }, { 0, 7 }},
                {{ 7, 7 }, { 7, 0 }}}),
        PIECE_TEE(GColor.YELLOW, 0, 4, new int [][][] {
                {{ 8, 0 }, { 8, 8 }, { 8, 0 }},
                {{ 0, 8 }, { 8, 8 }, { 0, 8 }},
                {{ 8, 8, 8 }, { 0, 8, 0 }},
                {{ 0, 8, 0 }, { 8, 8, 8 }},
        }),

        PIECE_STEP(GColor.RED, 1, 5, new int [][][]  {{{ 0, 9, 9 }, { 9, 9, 0 }},
                {{ 9, 9, 0 }, { 0, 9, 9 }},
                {{ 9, 0 }, { 9, 9 }, { 0, 9 }},
                {{ 0, 9 }, { 9, 9 }, { 9, 0 }}}),
        PIECE_CHIT(new GColor(0xFFDBB584), 0, 0, new int [][][] {{{ 10 }}}),
        ;

        PieceType(GColor color, int startX, int startY, int [][][] orientations) {
            this.color = color;
            this.orientations = orientations;
            this.startX = startX;
            this.startY = startY;
        }

        final GColor color;
        final int [][][] orientations;
        final float startX, startY;
    }

    public static class Piece extends Reflector<Piece> {
        final PieceType pieceType;
        private int index = 0;
        final MutableVector2D topLeft = new MutableVector2D();
        final MutableVector2D bottomRight = new MutableVector2D();
        boolean dropped = false;

        public Piece() {
            this(null);
        }

        public Piece(PieceType pt) {
            this.pieceType = pt;
            if (pt != null) {
                reset();
            }
        }

        void reset() {
            index = 0;
            this.topLeft.set(pieceType.startX, pieceType.startY);
            this.bottomRight.set(topLeft).addEq(getWidth(), getHeight());
            dropped = false;
        }

        int [][] getShape() {
            return pieceType.orientations[index];
        }

        void increment(int amt) {
            setIndex((index + amt + pieceType.orientations.length) % pieceType.orientations.length);
        }

        void setIndex(int idx) {
            index = idx;
            Vector2D center = topLeft.add(bottomRight).scaledBy(0.5f);
            topLeft.set(center).subEq(getWidth()/2, getHeight()/2);
            bottomRight.set(topLeft).addEq(getWidth(), getHeight());
        }

        public float getWidth() {
            return getShape()[0].length;
        }

        public float getHeight() {
            return getShape().length;
        }

        public Vector2D getCenter() {
            return topLeft.add(bottomRight).scaledBy(0.5f);
        }

        public void setCenter(IVector2D cntr) {
            topLeft.set(cntr).subEq(getWidth()/2, getHeight()/2);
            bottomRight.set(cntr).addEq(getWidth()/2, getHeight()/2);
        }

        public Vector2D getTopLeft() {
            return topLeft;
        }

    }

    final List<Piece> pieces = new ArrayList<>();
    int [][] board = new int[BOARD_DIM_CELLS][BOARD_DIM_CELLS]; // row major
    final StopWatch timer = new StopWatch();
    long bestTime = 0;

    static int BOARD_DIM_CELLS = 6;
    static int NUM_BLOCKERS = 7;

    public GeniusSquares() {
    }

    public void newGame() {
        board = new int[BOARD_DIM_CELLS][BOARD_DIM_CELLS];
        for (int i = 0; i < NUM_BLOCKERS; ) {
            int r = Utils.randRange(0, BOARD_DIM_CELLS - 1);
            int c = Utils.randRange(0, BOARD_DIM_CELLS - 1);
            if (board[r][c] != 0)
                continue;
            board[r][c] = PieceType.PIECE_CHIT.ordinal();
            i++;
        }
        resetPieces();
        timer.start();
    }

    public synchronized void resetPieces() {
        pieces.clear();
        for (int i=1; i<=9; i++) {
            Piece p = new Piece(PieceType.values()[i]);
            pieces.add(p);
            liftPiece(p);
        }
    }

    public final boolean canDropPiece(Piece p, int cellX, int cellY) {
        if (cellX < 0 || cellY < 0)
            return false;
        int [][] shape = p.getShape();
        if (cellY + shape.length > BOARD_DIM_CELLS)
            return false;
        if (cellX + shape[0].length > BOARD_DIM_CELLS)
            return false;
        for (int y=0; y<shape.length; y++) {
            for (int x=0; x<shape[y].length; x++) {
                if (shape[y][x] != 0 && board[cellY+y][cellX+x] != 0)
                    return false;
            }
        }
        return true;
    }

    /**
     * search through all the possible orientations to fit the piece at cx, cy
     * @param p
     * @param cellX
     * @param cellY
     * @return 3 ints: orientation, y and x or null if not possible to fit
     *   if not result != null then can call dropPiece(p, result[0], result[1], result[2])
     */
    public final int [] findDropForPiece(Piece p, int cellX, int cellY) {
        for (int s=0; s<p.pieceType.orientations.length; s++) {
            final int shapeIndex = (p.index+s) % p.pieceType.orientations.length;
            final int [][] shape = p.pieceType.orientations[shapeIndex];
            for (int y=0; y<shape.length; y++) {
                for (int x=0; x<shape[y].length; x++) {
                    int [][] tested = new int[shape.length][shape[0].length];
                    if (searchDropPieceR(shape, x, y, cellX, cellY, tested)) {
                        return new int [] { shapeIndex, cellX-x, cellY-y };
                    }
                }
            }
        }
        return null;
    }

    /**
     * This version will fit the piece using its current orientation only
     *
     * @param p
     * @param cellX
     * @param cellY
     * @return
     */
    public final int [] findDropForPiece2(Piece p, int cellX, int cellY) {
        final int [][] shape = p.getShape();
        for (int y=0; y<shape.length; y++) {
            for (int x=0; x<shape[y].length; x++) {
                int [][] tested = new int[shape.length][shape[0].length];
                if (searchDropPieceR(shape, x, y, cellX, cellY, tested)) {
                    return new int [] { p.index, cellX-x, cellY-y };
                }
            }
        }
        return null;
    }

    private boolean searchDropPieceR(final int [][] shape, int px, int py, int cellX, int cellY, final int [][] tested) {
        if (px < 0 || py < 0 || py >= shape.length || px >= shape[0].length)
            return true;
        if (tested[py][px] != 0)
            return true;
        tested[py][px] = 1;
        if (cellX < 0 || cellY < 0 || cellX >= BOARD_DIM_CELLS || cellY >= BOARD_DIM_CELLS)
            return false;
        if (shape[py][px] != 0 && board[cellY][cellX] != 0)
            return false;
        return searchDropPieceR(shape, px-1, py, cellX-1, cellY, tested) &&
                searchDropPieceR(shape, px+1, py, cellX+1, cellY, tested) &&
                searchDropPieceR(shape, px, py-1, cellX, cellY-1, tested) &&
                searchDropPieceR(shape, px, py+1, cellX, cellY+1, tested);
    }

    synchronized void dropPiece(Piece p, int cellX, int cellY) {
        log.info("Dropping Piece");
        if (canDropPiece(p, cellX, cellY)) {
            //throw new AssertionError("Logic Error: Cannot drop piece");
            final int[][] shape = p.getShape();
            for (int y=0; y<shape.length; y++) {
                for (int x=0; x<shape[y].length; x++) {
                    if (board[cellY+y][cellX+x] != 0)
                        System.err.println("Logic Error: should not be able to drop piece");
                    if (shape[y][x] != 0)
                        board[cellY+y][cellX+x] = shape[y][x];
                }
            }
            p.dropped = true;
        } else {
            log.error("Cannot drop piece at: " + cellX + ", " + cellY);
        }
    }

    synchronized void liftPiece(Piece p) {
        log.info("Lifting Piece");
        for (int y=0; y<BOARD_DIM_CELLS; y++) {
            for (int x=0; x<BOARD_DIM_CELLS; x++) {
                if (board[y][x] == p.pieceType.ordinal())
                    board[y][x] = 0;
            }
        }
        p.dropped = false;
    }

    public boolean isCompleted() {
        for (int y=0; y<BOARD_DIM_CELLS; y++) {
            for (int x=0; x<BOARD_DIM_CELLS; x++) {
                if (board[y][x] == 0)
                    return false;
            }
        }
        timer.pause();
        if (bestTime == 0 || timer.getTime() < bestTime) {
            bestTime = timer.getTime();
        }
        return true;
    }

    public void pauseTimer() {
        synchronized (timer) {
            timer.pause();
        }
    }

    public void resumeTimer() {
        synchronized (timer) {
            timer.unpause();
        }
    }

}
