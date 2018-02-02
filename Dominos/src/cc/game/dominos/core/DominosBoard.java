package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 2/1/18.
 */

public class DominosBoard extends Reflector<DominosBoard> {

    public final static int EP_LEFT     = 0;
    public final static int EP_RIGHT    = 1;
    public final static int EP_UP       = 2;
    public final static int EP_DOWN     = 3;

    Piece root;
    final LinkedList<Piece> [] endpoints = new LinkedList[4];

    void clear() {
        root = null;
        for (List<?> l : endpoints) {
            l.clear();
        }
    }

    public DominosBoard() {
        for (int i=0; i<4; i++)
            endpoints[i] = new LinkedList<>();
    }

    public List<Piece> collectPieces() {
        List<Piece> pieces = new ArrayList<>();
        if (root != null) {
            pieces.add(root);
            root = null;
        }
        for (int i=0; i<4; i++) {
            pieces.addAll(endpoints[i]);
            endpoints[i].clear();
        }
        return pieces;
    }

    public void placeRootPiece(Piece pc) {
        root = pc;
    }

    public List<Move> findMovesForPiece(Piece p) {
        List<Move> moves = new ArrayList<>();
        for (int i=0; i<4; i++) {
            if (endpoints[i].size() == 0) {
                if (canPieceTouch(p, root.num1)) {
                    moves.add(new Move(p, i));
                }
            } else {
                if (canPieceTouch(p, endpoints[i].getLast().openPips)) {
                    moves.add(new Move(p, i));
                }
            }
        }
        return moves;
    }

    private boolean canPieceTouch(Piece p, int pips) {
        return p.num1 == pips || p.num2 == pips;
    }

    public void doMove(Move mv) {
        int open = 0;
        if (endpoints[mv.endpoint].size() == 0) {
            open = root.openPips;
        } else {
            open = endpoints[mv.endpoint].getLast().openPips;
        }
        if (mv.piece.num1 == open) {
            mv.piece.openPips = mv.piece.num2;
        } else if (mv.piece.num2 == open) {
            mv.piece.openPips = mv.piece.num1;
        }
        endpoints[mv.endpoint].addLast(mv.piece);
    }

    public void draw(AGraphics g) {
        float DIM = 20;

        float left   = -(DIM + endpoints[EP_LEFT].size()*2*DIM + 2*DIM);
        float right  =  (DIM + endpoints[EP_RIGHT].size()*2*DIM + 2*DIM);
        float top    = -(DIM/2 + endpoints[EP_UP].size()*2*DIM + 2*DIM);
        float bottom =  (DIM/2 + endpoints[EP_DOWN].size()*2*DIM + 2*DIM);

        g.ortho(left, right, top, bottom);

        drawTile(g, -DIM, -DIM/2, DIM, root.num1, root.num2, EP_RIGHT);

        float x = -DIM;
        float y = -DIM/2;
        for (Piece p : endpoints[EP_LEFT]) {
            drawTile(g, x, y, DIM, p.getClosedPips(), p.openPips, EP_LEFT);
            x -= DIM*2;
        }
        x = DIM;
        y = -DIM/2;
        for (Piece p : endpoints[EP_RIGHT]) {
            drawTile(g, x, y, DIM, p.getClosedPips(), p.openPips, EP_RIGHT);
            x += DIM*2;
        }
        x = -DIM/2;
        y = -DIM/2;
        for (Piece p : endpoints[EP_UP]) {
            drawTile(g, x, y, DIM, p.getClosedPips(), p.openPips, EP_UP);
            y -= DIM*2;
        }
        x = -DIM/2;
        y = DIM/2;
        for (Piece p : endpoints[EP_DOWN]) {
            drawTile(g, x, y, DIM, p.getClosedPips(), p.openPips, EP_DOWN);
            y += DIM*2;
        }
    }

    protected void drawTile(AGraphics g, float x, float y, float dim, int pips1, int pips2, int direction) {
        g.setLineWidth(3);
        int pipDim = Math.round(dim/8);
        switch (direction) {
            case EP_DOWN:
                g.setColor(g.BLACK);
                g.drawFilledRectf(x, y, dim, dim*2);
                g.setColor(g.WHITE);
                g.drawRect(x, y, dim, dim*2);
                g.drawLine(x, y+dim, x+dim, y+dim);
                drawDie(g, x, y, dim, pipDim, pips1);
                drawDie(g, x, y+dim, dim, pipDim, pips2);
                break;
            case EP_UP:
                g.setColor(g.BLACK);
                g.drawFilledRectf(x, y-dim*2, dim, dim*2);
                g.setColor(g.WHITE);
                g.drawRect(x, y-dim*2, dim, dim*2);
                g.drawLine(x, y-dim, x+dim, y-dim);
                drawDie(g, x, y-dim, dim, pipDim, pips1);
                drawDie(g, x, y-dim*2, dim, pipDim, pips2);
                break;
            case EP_LEFT:
                g.setColor(g.BLACK);
                g.drawFilledRectf(x-dim*2, y, dim*2, dim);
                g.setColor(g.WHITE);
                g.drawRect(x, y, dim*2, dim);
                g.drawLine(x-dim, y, x-dim, y+dim);
                drawDie(g, x-dim, y, dim, pipDim, pips1);
                drawDie(g, x-dim*2, y, dim, pipDim, pips2);
                break;
            case EP_RIGHT:
                g.setColor(g.BLACK);
                g.drawFilledRectf(x, y, dim*2, dim);
                g.setColor(g.WHITE);
                g.drawRect(x, y, dim*2, dim);
                g.drawLine(x+dim, y, x+dim, y+dim);
                drawDie(g, x, y, dim, pipDim, pips1);
                drawDie(g, x+dim, y, dim, pipDim, pips2);
                break;
        }
    }

    public static void drawDie(AGraphics g, float x, float y, float dim, int dotSize, int numDots) {
        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        switch (numDots) {
            case 1:
                drawDot(g, x+dd2, y+dd2, dotSize);
                break;
            case 2:
                drawDot(g, x+dd4, y+dd4, dotSize);
                drawDot(g, x+dd34, y+dd34, dotSize);
                break;
            case 3:
                drawDot(g, x+dd4, y+dd4, dotSize);
                drawDot(g, x+dd2, y+dd2, dotSize);
                drawDot(g, x+dd34, y+dd34, dotSize);
                break;
            case 4:
                drawDot(g, x+dd4, y+dd4, dotSize);
                drawDot(g, x+dd34, y+dd34, dotSize);
                drawDot(g, x+dd4, y+dd34, dotSize);
                drawDot(g, x+dd34, y+dd4, dotSize);
                break;
            case 5:
                drawDot(g, x+dd4, y+dd4, dotSize);
                drawDot(g, x+dd34, y+dd34, dotSize);
                drawDot(g, x+dd4, y+dd34, dotSize);
                drawDot(g, x+dd34, y+dd4, dotSize);
                drawDot(g, x+dd2, y+dd2, dotSize);
                break;
            case 6:
                drawDot(g, x+dd4, y+dd4, dotSize);
                drawDot(g, x+dd34, y+dd34, dotSize);
                drawDot(g, x+dd4, y+dd34, dotSize);
                drawDot(g, x+dd34, y+dd4, dotSize);
                drawDot(g, x+dd4, y+dd2, dotSize);
                drawDot(g, x+dd34, y+dd2, dotSize);
                break;
        }
    }

    private static void drawDot(AGraphics g, float x, float y, int dotSize) {
        g.drawFilledOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
    }

    public int computeEndpointsTotal() {
        int score = 0;
        for (int i=0; i<4; i++) {
            if (endpoints[i].size() > 0) {
                score += endpoints[i].getLast().openPips;
            }
        }
        return score;
    }

}
