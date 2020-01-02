package cc.lib.geniussqaure;

import cc.lib.game.GColor;
import cc.lib.utils.Reflector;

public class GeniusSquare extends Reflector<GeniusSquare> { // GeniusSquare. 6x6 board

    static {
        addAllFields(GeniusSquare.class);
        addAllFields(Piece.class);
    }

    public static class Piece extends Reflector<Piece> {
        @Omit
        final GColor color;
        @Omit
        final int [][][] shape; // first index is the shape. next 2 are the matrix

        int index;
        float x, y;

        public Piece(GColor color, int [][][] shape) {
            this.color = color;
            this.shape = shape;
        }

        void rotate(int degrees) {}

        void flipVertical() {}

        void flipHorizontal() {}
    }

    Piece p1x1 = new Piece(GColor.BLUE, new int [][][] {{{ 1 }}});
    Piece p2x2 = new Piece(GColor.GREEN, new int [][][] {{{ 2, 2 }, { 2, 2 }}});
    Piece p1x2 = new Piece(GColor.BROWN, new int [][][] {{{ 3, 3 }}, {{ 3 },{ 3 }}});
    Piece p1x3 = new Piece(GColor.ORANGE, new int [][][] {{{ 4, 4, 4 }}, {{ 4 }, { 4 }, { 4 }}});
    Piece p1x4 = new Piece(GColor.GRAY, new int [][][] {{{ 5, 5, 5, 5 }}, {{ 5 }, { 5 }, { 5 }, { 5 }}});
    Piece pEl  = new Piece(GColor.CYAN, new int [][][] {{{ 6, 0, 0 }, { 6, 6, 6 }},
                                                        {{ 0, 0, 6 }, { 6, 6, 6 }},
                                                        {{ 6, 6, 6 }, { 6, 0, 0 }},
                                                        {{ 6, 6, 6 }, { 0, 0, 6 }},
                                                        {{ 6, 6 }, { 0, 6 }, { 0, 6 }},
                                                        {{ 6, 6 }, { 6, 0 }, { 6, 0 }},
                                                        {{ 0, 6 }, { 0, 6 }, { 6, 6 }},
                                                        {{ 1, 0 }, { 6, 0 }, { 6, 6 }}});
    Piece pBend = new Piece(GColor.MAGENTA, new int [][][] {{{ 7, 0 }, { 7, 7 }},
                                                            {{ 0, 7 }, { 7, 7 }},
                                                            {{ 7, 7 }, { 0, 7 }},
                                                            {{ 7, 7 }, { 7, 0 }}});
    Piece pTee = new Piece(GColor.YELLOW, new int [][][] {{{ 8, 8, 8 }, { 0, 8, 0 }},
                                                          {{ 0, 8, 0 }, { 8, 8, 8 }},
                                                          {{ 8, 0 }, { 8, 8 }, { 8, 0 }},
                                                          {{ 0, 8 }, { 8, 8 }, { 0, 8 }}});

    Piece pStep= new Piece(GColor.RED, new int [][][]  {{{ 0, 9, 9 }, { 9, 9, 0 }},
                                                        {{ 9, 9, 0 }, { 0, 9, 9 }},
                                                        {{ 9, 0 }, { 9, 9 }, { 0, 9 }},
                                                        {{ 0, 9 }, { 9, 9 }, { 9, 0 }}});

    int [][] board = new int[6][6];

    static int NUM_BLOCKERS = 7;
    static int BLOCKER_ID   = 10;
}
