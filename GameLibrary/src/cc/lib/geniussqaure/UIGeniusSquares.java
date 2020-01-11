package cc.lib.geniussqaure;

import java.util.Arrays;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class UIGeniusSquares extends GeniusSquares {

    int WIDTH = 0;
    int HEIGHT = 0;
    int DIM = 0;
    int CELL_DIM = 0;
    int BOARD_DIM = 0;

    Piece highlighted = null;
    int [] pickedCell = null;
    boolean dragging = false;
    boolean autoFitPieces = true;

    public abstract void repaint();

    public synchronized void paint(APGraphics g, final int mx, final int my) {
        final String PREFIX = "paint(" + mx + "," + my + ")";
        //log.info("paint x=" + mx + " y=" + my);
        WIDTH = g.getViewportWidth();
        HEIGHT = g.getViewportHeight();
        DIM = Math.min(WIDTH, HEIGHT);
        CELL_DIM = DIM / (BOARD_DIM_CELLS+2);
        BOARD_DIM = CELL_DIM * BOARD_DIM_CELLS;

        if (WIDTH > HEIGHT) {
            g.pushMatrix();
            g.translate(WIDTH-DIM+CELL_DIM, CELL_DIM);
            g.scale(CELL_DIM);

            if (highlighted != null && pickedCell != null && !dragging) {
                liftPiece(highlighted);
                int [] drop = findDropForPiece2(highlighted, pickedCell[0], pickedCell[1]);
                if (drop != null) {
                    dropPiece(highlighted, drop[1], drop[2]);
                }
            }

            pickedCell = drawBoard(g, mx, my);
            //log.info(PREFIX + "pickedCell: " + Arrays.toString(pickedCell));
            g.popMatrix();
            g.pushMatrix();
            g.scale(CELL_DIM);

            if (highlighted == null || !dragging) {
                highlighted = pickPieces(g, mx, my);
            }
            drawPiecesBeveled(g);

            if (highlighted == null && pickedCell != null) {
                int color = board[pickedCell[1]][pickedCell[0]];
                //System.out.println("color = " + color);

                if (color > PieceType.PIECE_0.ordinal() && color < PieceType.PIECE_CHIT.ordinal()) {
                    highlighted = pieces.get(color-1);
                }
            }

            if (highlighted != null) {
                log.info(PREFIX + "highlighted = " + highlighted);
                g.pushMatrix();

                g.setColor(GColor.WHITE);
                boolean useFind = true;

                if (dragging) {
                    liftPiece(highlighted);
                    Vector2D pt = g.screenToViewport(mx, my);
                    highlighted.setCenter(pt);
                    if (pickedCell != null) {
                        int[] result;
                        if (useFind && (result = findDropForPiece2(highlighted, pickedCell[0], pickedCell[1])) != null) {
                            log.info(PREFIX + "Droppable at: " + result[1] + "," + result[2] + " index:" + result[0]);
                            highlighted.setIndex(result[0]);
                            dropPiece(highlighted, result[1], result[2]);
                        } else if (!useFind && canDropPiece(highlighted, pickedCell[0], pickedCell[1])) {
                            dropPiece(highlighted, pickedCell[0], pickedCell[1]);
                        } else {
                            g.setColor(GColor.RED);
                        }
                    }
                }

                if (!highlighted.dropped) {
                    g.pushMatrix();
                    g.translate(highlighted.getCenter());
                    g.scale(1.03f);
                    g.translate(-highlighted.getWidth() / 2, -highlighted.getHeight() / 2);
                    renderPiece(g, highlighted);
                    g.drawFilledRects();
                    g.popMatrix();
                    g.end();
                    drawPieceBeveled(g, highlighted);
                }

                g.popMatrix();
            }
            g.popMatrix();
            g.setTextHeight(CELL_DIM/2);
            timer.capture();
            int timeSecs = (int)(timer.getTime()/1000);
            int timeMins = timeSecs / 60;
            timeSecs -= timeMins*60;
            String bestTimeStr = "";
            if (this.bestTime > 0) {
                int bestTimeSecs = (int)(bestTime/1000);
                int bestTimeMins = bestTimeSecs/60;
                bestTimeSecs -= bestTimeMins*60;
                bestTimeStr = String.format("   %sBEST %02d:%02d", GColor.WHITE.toString(), bestTimeMins, bestTimeSecs);
            }
            GColor timeColor = GColor.GREEN;
            if (bestTime > 0 && timer.getTime() > bestTime)
                timeColor = GColor.RED;
            String curTimeStr = String.format("%sTIME %02d:%02d", timeColor.toString(), timeMins, timeSecs);
            g.drawAnnotatedString(curTimeStr + bestTimeStr, WIDTH-BOARD_DIM-CELL_DIM, CELL_DIM/5);

            if (false && pickedCell != null) {
                g.setColor(GColor.WHITE);
                String hl = "";
                if (highlighted != null) {
                    hl = highlighted.pieceType.name()+" " + (highlighted.dropped ? "v" : "^") + " ";
                }
                g.drawJustifiedString(WIDTH-10, 10, Justify.RIGHT, Justify.TOP, hl+"pickedCell: " + pickedCell[0] + "," + pickedCell[1]);
            }

        } else {
            g.drawJustifiedString(WIDTH/2, HEIGHT/2, Justify.CENTER, Justify.CENTER, "Portrait not supported");
        }

        if (isCompleted()) {
            if (!endgameAnim.isStarted())
                endgameAnim.start();
        } else {
            endgameAnim.kill();
        }

        if (endgameAnim.isStarted()) {
            endgameAnim.update(g);
            repaint();
        }
    }

    AAnimation<AGraphics> endgameAnim = new AAnimation<AGraphics>(2000, -1, true) {
        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.setTextHeight(20f+position*20f);
            g.setColor(GColor.MAGENTA);
            g.drawJustifiedString((WIDTH-BOARD_DIM-CELL_DIM)/2, HEIGHT/2, Justify.CENTER, Justify.CENTER, "COMPLETED");
        }
    };

    @Override
    public synchronized void newGame() {
        super.newGame();
        endgameAnim.kill();
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
                if (picked == null) {
                    g.begin();
                    g.setName(1);
                    g.vertex(x, y);
                    g.vertex(x+1, y+1);
                    if (1 == g.pickRects(mx, my)) {
                        picked = new int[] { x, y };
                    }
                }
                PieceType pt = PieceType.values()[board[y][x]];
                switch (pt) {
                    case PIECE_0:
                        break;
                    case PIECE_CHIT: {
                        g.setColor(pt.color);
                        g.drawFilledCircle(x+0.5f, y+0.5f, 0.4f);
                        break;
                    }

                    default: {
                        drawCellBeveled(g, board, new Vector2D(0, 0), x, y);
                        //g.setColor(pt.color);
                        //g.drawFilledRect(0, 0, 1, 1);
                    }
                }
                g.popMatrix();
            }
        }
        return picked;
    }

    private Piece pickPieces(APGraphics g, final int mx, final int my) {
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
            g.end();
            g.popMatrix();
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

    final float BEVEL_INSET = 0.25f;
    final float BEVEL_PADDING = 0.05f;

    public void drawPieceBeveled(AGraphics g, Piece p) { //int [][] shape, Vector2D _topLeft, GColor color) {

        int [][] shape = p.getShape();

        for (int y=0; y<shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] == 0)
                    continue;
                g.pushMatrix();
                drawCellBeveled(g, shape, p.getTopLeft(), x, y);
                g.popMatrix();
            }
        }
    }

    private static final int BEVEL_TOP = 1;
    private static final int BEVEL_RIGHT = 1<<1;
    private static final int BEVEL_BOTTOM = 1<<2;
    private static final int BEVEL_LEFT = 1<<3;
    private static final int BEVEL_LEFT_TOP = BEVEL_LEFT|BEVEL_TOP;
    private static final int BEVEL_RIGHT_TOP = BEVEL_RIGHT|BEVEL_TOP;
    private static final int BEVEL_RIGHT_BOTTOM = BEVEL_RIGHT|BEVEL_BOTTOM;
    private static final int BEVEL_LEFT_BOTTOM = BEVEL_BOTTOM|BEVEL_LEFT;

    // wow programmatic beveling harder than I expected
    public void drawCellBeveled(AGraphics g, int [][] matrix, Vector2D _topLeft, int x, int y) {
        int cell = matrix[y][x];
        MutableVector2D topLeft = new MutableVector2D(_topLeft).addEq(x, y);
        MutableVector2D topRight = new MutableVector2D(topLeft).addEq(1, 0);
        MutableVector2D bottomLeft  = new MutableVector2D(topLeft).addEq(0, 1);
        MutableVector2D bottomRight = new MutableVector2D(topLeft).addEq(1, 1);
        MutableVector2D topLeftBevel = new MutableVector2D(_topLeft).add(x, y);
        MutableVector2D topRightBevel = new MutableVector2D(topLeftBevel).addEq(1, 0);
        MutableVector2D bottomLeftBevel = new MutableVector2D(topLeftBevel).addEq(0, 1);
        MutableVector2D bottomRightBevel = new MutableVector2D(topLeftBevel).addEq(1, 1);
        int bevel = 0;
        if (getCellAt(y - 1, x, matrix) != cell) {
            // draw 'top' for this cell with beveled
            topLeft.addEq(0, BEVEL_PADDING);
            topRight.addEq(0, BEVEL_PADDING);
            topLeftBevel.addEq(0, BEVEL_INSET);
            topRightBevel.addEq(0, BEVEL_INSET);
        } else {
            bevel |= BEVEL_TOP;
        }
        if (getCellAt(y + 1, x, matrix) != cell) {
            // draw 'bottom' for this cell with beveled
            bottomLeft.subEq(0, BEVEL_PADDING);
            bottomRight.subEq(0, BEVEL_PADDING);
            bottomLeftBevel.subEq(0, BEVEL_INSET);
            bottomRightBevel.subEq(0, BEVEL_INSET);
        } else {
            bevel |= BEVEL_BOTTOM;
        }
        if (getCellAt(y, x-1, matrix) != cell) {
            // draw 'left' for this cell with beveled
            topLeft.addEq(BEVEL_PADDING, 0);
            bottomLeft.addEq(BEVEL_PADDING, 0);
            topLeftBevel.addEq(BEVEL_INSET, 0);
            bottomLeftBevel.addEq(BEVEL_INSET, 0);
        } else {
            bevel |= BEVEL_LEFT;
        }
        if (getCellAt(y, x+1, matrix) != cell) {
            // draw 'right' for this cell with beveled
            topRight.subEq(BEVEL_PADDING, 0);
            bottomRight.subEq(BEVEL_PADDING, 0);
            topRightBevel.subEq(BEVEL_INSET, 0);
            bottomRightBevel.subEq(BEVEL_INSET, 0);
        } else {
            bevel |= BEVEL_RIGHT;
        }

        GColor topColor = PieceType.values()[cell].color.lightened(.3f);
        GColor cntrColor = topColor.darkened(.1f);
        GColor leftColor = topColor.darkened(.2f);
        GColor rightColor = topColor.darkened(.3f);
        GColor bottomColor = topColor.darkened(.4f);
        // top
        g.begin();
        g.setColor(topColor);
        g.vertex(topLeft);
        g.vertex(topRight);
        g.vertex(topLeftBevel);
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

        final boolean bevelTopLeft     = (bevel & BEVEL_LEFT_TOP) == BEVEL_LEFT_TOP && getCellAt(y-1, x-1, matrix) != cell;
        final boolean bevelTopRight    = (bevel & BEVEL_RIGHT_TOP) == BEVEL_RIGHT_TOP && getCellAt(y-1, x+1, matrix) != cell;
        final boolean bevelBottomLeft  = (bevel & BEVEL_LEFT_BOTTOM) == BEVEL_LEFT_BOTTOM && getCellAt(y+1, x-1, matrix) != cell;
        final boolean bevelBottomRight = (bevel & BEVEL_RIGHT_BOTTOM) == BEVEL_RIGHT_BOTTOM && getCellAt(y+1, x+1, matrix) != cell;

        g.begin();
        if (bevelTopLeft) {
            topLeftBevel.addEq(BEVEL_INSET, BEVEL_INSET);
        }
        if (bevelTopRight) {
            topRightBevel.addEq(-BEVEL_INSET, BEVEL_INSET);
        }
        if (bevelBottomRight) {
            bottomRightBevel.addEq(-BEVEL_INSET, -BEVEL_INSET);
        }
        if (bevelBottomLeft) {
            bottomLeftBevel.addEq(BEVEL_INSET, -BEVEL_INSET);
        }
        g.begin();
        g.setColor(cntrColor);
        if (bevelTopLeft || bevelTopRight) {
            // top
            g.vertex(topLeftBevel.min(topRightBevel).setY(topLeft.getY()));
            g.vertex(topLeftBevel.max(topRightBevel));
        }
        if (bevelTopLeft || bevelBottomLeft) {
            // left
            g.vertex(topLeftBevel.min(bottomLeftBevel).setX(topLeft.getX()));
            g.vertex(topLeftBevel.max(bottomLeftBevel));
        }
        if (bevelTopRight || bevelBottomRight) {
            // right
            g.vertex(topRightBevel.min(bottomRightBevel));
            g.vertex(topRightBevel.max(bottomRightBevel).setX(bottomRight.getX()));
        }
        if (bevelBottomLeft || bevelBottomRight) {
            // bottom
            g.vertex(bottomLeftBevel.min(bottomRightBevel));
            g.vertex(bottomLeftBevel.max(bottomRightBevel).setY(bottomRight.getY()));
        }

        g.drawFilledRects();
        g.begin();
        // center
        g.vertex(topLeftBevel);
        g.vertex(topRightBevel);
        g.vertex(bottomLeftBevel);
        g.vertex(bottomRightBevel);
        g.drawQuadStrip();

        // check the corners. This draws the little 'L' in the corner(s)
        // top/left corner
        if (bevelTopLeft) {
            g.begin();
            g.setColor(topColor);
            g.vertex(topLeft.getX(), topLeft.getY()+BEVEL_PADDING);
            g.vertex(topLeft.getX(), topLeft.getY()+BEVEL_INSET);
            g.vertex(topLeft.getX()+BEVEL_PADDING, topLeft.getY()+BEVEL_PADDING);
            g.vertex(topLeft.getX()+BEVEL_INSET, topLeft.getY()+BEVEL_INSET);
            g.drawQuadStrip();
            g.begin();
            g.setColor(leftColor);
            g.vertex(topLeft.getX()+BEVEL_PADDING, topLeft.getY());
            g.vertex(topLeft.getX()+BEVEL_INSET, topLeft.getY());
            g.vertex(topLeft.getX()+BEVEL_PADDING, topLeft.getY()+BEVEL_PADDING);
            g.vertex(topLeft.getX()+BEVEL_INSET, topLeft.getY()+BEVEL_INSET);
            g.drawQuadStrip();
        }

        // top/right corner
        if (bevelTopRight) {
            g.begin();
            g.setColor(topColor);
            g.vertex(topRight.getX(), topRight.getY()+BEVEL_PADDING);
            g.vertex(topRight.getX(), topRight.getY()+BEVEL_INSET);
            g.vertex(topRight.getX()-BEVEL_PADDING, topRight.getY()+BEVEL_PADDING);
            g.vertex(topRight.getX()-BEVEL_INSET, topRight.getY()+BEVEL_INSET);
            g.drawQuadStrip();
            g.begin();
            g.setColor(rightColor);
            g.vertex(topRight.getX()-BEVEL_PADDING, topRight.getY());
            g.vertex(topRight.getX()-BEVEL_INSET, topRight.getY());
            g.vertex(topRight.getX()-BEVEL_PADDING, topRight.getY()+BEVEL_PADDING);
            g.vertex(topRight.getX()-BEVEL_INSET, topRight.getY()+BEVEL_INSET);
            g.drawQuadStrip();
        }
        // bottom/right corner
        if (bevelBottomRight) {
            g.begin();
            g.setColor(bottomColor);
            g.vertex(bottomRight.getX(), bottomRight.getY()-BEVEL_PADDING);
            g.vertex(bottomRight.getX(), bottomRight.getY()-BEVEL_INSET);
            g.vertex(bottomRight.getX()-BEVEL_PADDING, bottomRight.getY()-BEVEL_PADDING);
            g.vertex(bottomRight.getX()-BEVEL_INSET, bottomRight.getY()-BEVEL_INSET);
            g.drawQuadStrip();
            g.begin();
            g.setColor(rightColor);
            g.vertex(bottomRight.getX()-BEVEL_PADDING, bottomRight.getY());
            g.vertex(bottomRight.getX()-BEVEL_INSET, bottomRight.getY());
            g.vertex(bottomRight.getX()-BEVEL_PADDING, bottomRight.getY()-BEVEL_PADDING);
            g.vertex(bottomRight.getX()-BEVEL_INSET, bottomRight.getY()-BEVEL_INSET);
            g.drawQuadStrip();
        }
        // bottom/left corner
        if (bevelBottomLeft) {
            g.begin();
            g.setColor(bottomColor);
            g.vertex(bottomLeft.getX(), bottomLeft.getY()-BEVEL_PADDING);
            g.vertex(bottomLeft.getX(), bottomLeft.getY()-BEVEL_INSET);
            g.vertex(bottomLeft.getX()+BEVEL_PADDING, bottomLeft.getY()-BEVEL_PADDING);
            g.vertex(bottomLeft.getX()+BEVEL_INSET, bottomLeft.getY()-BEVEL_INSET);
            g.drawQuadStrip();
            g.begin();
            g.setColor(leftColor);
            g.vertex(bottomLeft.getX()+BEVEL_PADDING, bottomLeft.getY());
            g.vertex(bottomLeft.getX()+BEVEL_INSET, bottomLeft.getY());
            g.vertex(bottomLeft.getX()+BEVEL_PADDING, bottomLeft.getY()-BEVEL_PADDING);
            g.vertex(bottomLeft.getX()+BEVEL_INSET, bottomLeft.getY()-BEVEL_INSET);
            g.drawQuadStrip();
        }

        g.end();
    }

    public void drawPiecesBeveled(AGraphics g) {
        for (Piece p : pieces) {
            if (p.dropped || p == highlighted)
                continue;
            drawPieceBeveled(g, p);
        }
    }

    public synchronized void doClick() {
        log.info("doClick highlighted=" + highlighted);
        if (highlighted != null) {
            highlighted.increment(1);
            log.info("doClick: incremented:" + highlighted);
            repaint();
        }
    }

    public synchronized void startDrag() {
        log.info("Stop Drag");
        if (highlighted != null) {
            dragging = true;
            repaint();
        }
    }

    public synchronized void stopDrag() {
        log.info("Stop Drag");
        dragging = false;
        repaint();
    }

    public boolean isAutoFitPieces() {
        return autoFitPieces;
    }

    public void setAutoFitPieces(boolean enabled) {
        this.autoFitPieces = enabled;
    }

}
