package cc.lib.geniussqaure;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class UIGeniusSquare extends GeniusSquare {

    int WIDTH = 0;
    int HEIGHT = 0;
    int DIM = 0;
    //int CELL_DIM = 0;
    //int BOARD_DIM = 0;

    Piece highlighted = null;
    boolean dragging = false;
    boolean clicked = false;

    protected abstract void repaint();

    public synchronized void paint(APGraphics g, final int mx, final int my) {
        //System.out.println("draw mx=" + mx + " my=" + my);
        WIDTH = g.getViewportWidth();
        HEIGHT = g.getViewportHeight();
        DIM = Math.min(WIDTH, HEIGHT);
        int CELL_DIM = DIM / (BOARD_DIM_CELLS+2);
        int BOARD_DIM = CELL_DIM * BOARD_DIM_CELLS;

        g.clearScreen(GColor.DARK_GRAY);

        if (dragging && highlighted != null) {
            //highlighted.center.set(mx,my);
            //g.transform(highlighted.center);
        }

        if (WIDTH > HEIGHT) {
            g.pushMatrix();
            g.translate(WIDTH-DIM+CELL_DIM, CELL_DIM);
            g.scale(CELL_DIM);
            final int [] pickedCell = drawBoard(g, mx, my);
            g.popMatrix();
            g.pushMatrix();
            g.scale(CELL_DIM);
            if (highlighted == null || !dragging)
                highlighted = drawPieces(g, mx, my);
            drawPiecesBeveled(g);

            if (highlighted != null) {
                //System.out.println("highlighted = " + highlighted);
                g.pushMatrix();
                g.setColor(GColor.WHITE);

                g.pushMatrix();
                g.translate(highlighted.getCenter());
                g.scale(1.05f);
                g.translate(-highlighted.getWidth()/2, -highlighted.getHeight()/2);
                renderPiece(g, highlighted);
                g.drawFilledRects();
                g.popMatrix();
                g.end();
                drawPieceBeveled(g, highlighted);
                //g.pushMatrix();
                //g.translate(highlighted.topLeft);
                //g.setColor(highlighted.pieceType.color);
                //renderPiece(g, highlighted);
                //g.drawFilledRects();
                //g.popMatrix();

                if (dragging) {
                    Vector2D pt = g.screenToViewport(mx, my);
                    highlighted.setCenter(pt);
                }


                g.popMatrix();
            }
            g.popMatrix();
            if (pickedCell != null) {
                g.setColor(GColor.WHITE);
                g.drawString("pickedCell: " + pickedCell[0] + "," + pickedCell[1], 10, 10);
            }

        } else {

        }
    }

    // return row/col for mx, my
    private int [] drawBoard(APGraphics g, final int mx, final int my) {
        int [] picked = null;
        g.setColor(GColor.BLACK);
        g.drawFilledRect(0, 0, BOARD_DIM_CELLS, BOARD_DIM_CELLS);
        g.setColor(GColor.WHITE);
        g.setLineWidth(3);
        for (int i=0; i<=BOARD_DIM_CELLS; i++) {
            g.drawLine(0, i, BOARD_DIM_CELLS, i);
            g.drawLine(i, 0, i, BOARD_DIM_CELLS);
        }
        for (int y=0; y<BOARD_DIM_CELLS; y++) {
            for (int x=0; x<BOARD_DIM_CELLS; x++) {
                g.pushMatrix();
                g.translate(x, y);
                if (picked == null) {
                    g.begin();
                    g.setName(1);
                    g.vertex(0, 0);
                    g.vertex(1, 1);
                    if (1 == g.pickRects(mx, my)) {
                        picked = new int[] { y, x };
                    }
                }
                PieceType pt = PieceType.values()[board[y][x]];
                switch (pt) {
                    case PIECE_0:
                        break;
                    case PIECE_CHIT: {
                        g.setColor(pt.color);
                        g.drawFilledCircle(0.5f, 0.5f, 0.4f);
                        break;
                    }

                    default: {
                        //g.setColor(pt.color);
                        //g.drawFilledRect(0, 0, 1, 1);
                    }
                }
                g.popMatrix();
            }
        }
        return picked;
    }

    private Piece drawPieces(APGraphics g, final int mx, final int my) {
        Piece picked = null;
        for (Piece p : pieces) {
            g.setColor(p.pieceType.color);
            g.pushMatrix();
            g.translate(p.topLeft);
            g.begin();
            g.setName(1);
            renderPiece(g, p);
            if (g.pickRects(mx, my) == 1) {
                picked = p;
            }
            //g.drawFilledRects();
            g.end();
            g.popMatrix();
            g.begin();
            g.vertex(p.getCenter());
            g.setColor(GColor.RED);
            g.drawPoints(5);
            g.end();
            //g.setColor(GColor.WHITE);
            //g.setLineWidth(1);
            //g.vertex(p.topLeft);
            //g.vertex(p.bottomRight);
            //g.drawRects();
            //g.end();
        }
        return picked;
    }

    private void renderPiece(AGraphics g, Piece p) {
        float w = p.getWidth()/2;
        float h = p.getHeight()/2;
        g.pushMatrix();
        //g.translate(-w, -h);
        int [][] cells = p.getShape();
        for (int cr=0; cr<cells.length; cr++) {
            for (int cc=0; cc<cells[0].length; cc++) {
                if (cells[cr][cc] == 0)
                    continue;
                g.vertex(cc, cr);
                g.vertex(cc+1, cr+1);
            }
        }
        g.popMatrix();
    }

    /**
     * Return -1 for off board or the cell assignment at row, col
     * @param row
     * @param col
     * @param rowMajorMatrix
     * @return
     */
    public static int getCellAt(int row, int col, int [][] rowMajorMatrix) {
        if (row < 0 || col < 0 || row >= rowMajorMatrix.length || col >= rowMajorMatrix[0].length)
            return -1;
        return rowMajorMatrix[row][col];
    }

    public void drawPieceBeveled(AGraphics g, Piece p) {
        final float BEVEL_INSET = 0.25f;
        final float PADDING = 0.05f;

        int [][] shape = p.getShape();
        for (int y=0; y<shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] == 0)
                    continue;
                g.pushMatrix();
                final int cell = shape[y][x];
                MutableVector2D topLeft = new MutableVector2D(p.getTopLeft()).addEq(x, y);
                MutableVector2D topRight = new MutableVector2D(topLeft).addEq(1, 0);
                MutableVector2D bottomLeft  = new MutableVector2D(topLeft).addEq(0, 1);
                MutableVector2D bottomRight = new MutableVector2D(topLeft).addEq(1, 1);
                MutableVector2D topLeftBevel = new MutableVector2D(p.getTopLeft()).add(x, y);
                MutableVector2D topRightBevel = new MutableVector2D(topLeftBevel).addEq(1, 0);
                MutableVector2D bottomLeftBevel = new MutableVector2D(topLeftBevel).addEq(0, 1);
                MutableVector2D bottomRightBevel = new MutableVector2D(topLeftBevel).addEq(1, 1);
                if (getCellAt(y - 1, x, shape) != cell) {
                    // draw 'top' for this cell with beveled
                    topLeft.addEq(0, PADDING);
                    topRight.addEq(0, PADDING);
                    topLeftBevel.addEq(0, BEVEL_INSET);
                    topRightBevel.addEq(0, BEVEL_INSET);
                }
                if (getCellAt(y + 1, x, shape) != cell) {
                    // draw 'bottom' for this cell with beveled
                    bottomLeft.subEq(0, PADDING);
                    bottomRight.subEq(0, PADDING);
                    bottomLeftBevel.subEq(0, BEVEL_INSET);
                    bottomRightBevel.subEq(0, BEVEL_INSET);
                }
                if (getCellAt(y, x-1, shape) != cell) {
                    // draw 'left' for this cell with beveled
                    topLeft.addEq(PADDING, 0);
                    bottomLeft.addEq(PADDING, 0);
                    topLeftBevel.addEq(BEVEL_INSET, 0);
                    bottomLeftBevel.addEq(BEVEL_INSET, 0);
                }
                if (getCellAt(y, x+1, shape) != cell) {
                    // draw 'right' for this cell with beveled
                    topRight.subEq(PADDING, 0);
                    bottomRight.subEq(PADDING, 0);
                    topRightBevel.subEq(BEVEL_INSET, 0);
                    bottomRightBevel.subEq(BEVEL_INSET, 0);
                }
                GColor cntrColor = p.pieceType.color;
                GColor leftColor = cntrColor.lightened(.2f);
                GColor rightColor = cntrColor.darkened(.2f);
                GColor topColor = cntrColor.lightened(.4f);
                GColor bottomColor = cntrColor.darkened(.4f);
                // top
                g.begin();
                g.setColor(topColor);
                g.vertex(topLeft);
                g.vertex(topRight);
                g.vertex(bottomLeftBevel);
                g.vertex(topRightBevel);
                g.drawQuadStrip();
                // right
                g.begin();
                g.setColor(rightColor);
                g.vertex(topRight);
                g.vertex(bottomRight);
                g.vertex(topRightBevel);
                g.vertex(bottomRightBevel);
                g.drawQuadStrip();
                // bottom
                g.begin();
                g.setColor(bottomColor);
                g.vertex(bottomRight);
                g.vertex(bottomLeft);
                g.vertex(bottomRightBevel);
                g.vertex(bottomLeftBevel);
                g.drawQuadStrip();
                // left
                g.begin();
                g.setColor(leftColor);
                g.vertex(bottomLeft);
                g.vertex(topLeft);
                g.vertex(bottomLeftBevel);
                g.vertex(topLeftBevel);
                g.drawQuadStrip();
                // center
                g.begin();
                g.setColor(cntrColor);
                g.vertex(topLeftBevel);
                g.vertex(topRightBevel);
                g.vertex(bottomLeftBevel);
                g.vertex(bottomRightBevel);
                g.drawQuadStrip();
                g.end();
                g.popMatrix();
            }
        }
    }

    public void drawPiecesBeveled(AGraphics g) {
        for (Piece p : pieces) {
            drawPieceBeveled(g, p);
        }
    }

    public synchronized void doClick() {
        if (highlighted != null) {
            highlighted.increment(1);
            repaint();
        }
    }

    public synchronized void startDrag() {
        if (highlighted != null) {
            dragging = true;
            repaint();
        }
    }

    public synchronized void stopDrag() {
        dragging = false;
        repaint();
    }

}
