package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 2/1/18.
 */

public class Board extends Reflector<Board> {

    static {
        addAllFields(Board.class);
    }

    public final static int EP_LEFT     = 0;
    public final static int EP_RIGHT    = 1;
    public final static int EP_UP       = 2;
    public final static int EP_DOWN     = 3;

    Tile root;
    final LinkedList<Tile> [] endpoints = new LinkedList[4];

    @Omit
    private Set<Integer> highlightedEndpoints = new HashSet<>();
    @Omit
    private int selectedEndpoint = -1;
    @Omit
    private Tile highlightedTile = null;

    private class PlaceTileAnim extends AAnimation<AGraphics> {

        final IVector2D start, end;
        final Tile tile;

        PlaceTileAnim(Tile tile, int startPlayerPosition, int endPoint) {
            super(2000);
            this.tile = tile;
            switch (startPlayerPosition) {
                case EP_DOWN:
                    start = new Vector2D(0.5f, 1); break;
                case EP_LEFT:
                    start = new Vector2D(0f, 0.5f); break;
                case EP_RIGHT:
                    start = new Vector2D(1f, 0.5f); break;
                case EP_UP:
                    start = new Vector2D(0.5f, 0); break;
                default:
                    start = Vector2D.ZERO;
            }

            switch (endPoint) {
                case EP_DOWN:
                    end = new Vector2D(0, 0.5f + 2*endpoints[EP_DOWN].size()); break;
                case EP_LEFT:
                    end = new Vector2D(1f + 2*endpoints[EP_DOWN].size(), 0); break;
                case EP_RIGHT:
                    end = new Vector2D(1f + 2*endpoints[EP_DOWN].size(), 0); break;
                case EP_UP:
                    end = new Vector2D(0, -0.5f - 2*endpoints[EP_DOWN].size()); break;
                default:
                    end = Vector2D.ZERO;

            }
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {

        }
    }



    public final void clear() {
        root = null;
        for (List<?> l : endpoints) {
            l.clear();
        }
        clearSelection();
    }

    public final void clearSelection() {
        highlightedEndpoints.clear();
        selectedEndpoint = -1;
        highlightedTile = null;
    }

    public Board() {
        for (int i=0; i<4; i++)
            endpoints[i] = new LinkedList<>();
    }

    public final List<Tile> collectPieces() {
        List<Tile> pieces = new ArrayList<>();
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

    public final void placeRootPiece(Tile pc) {
        root = pc;
    }

    public final List<Move> findMovesForPiece(Tile p) {
        List<Move> moves = new ArrayList<>();
        for (int i=0; i<4; i++) {
            if (endpoints[i].size() == 0) {
                if (canPieceTouch(p, root.pip1)) {
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

    private final boolean canPieceTouch(Tile p, int pips) {
        return p.pip1 == pips || p.pip2 == pips;
    }

    public final void doMove(Move mv) {
        int open = 0;
        if (endpoints[mv.endpoint].size() == 0) {
            open = root.openPips;
        } else {
            open = endpoints[mv.endpoint].getLast().openPips;
        }
        if (mv.piece.pip1 == open) {
            mv.piece.openPips = mv.piece.pip2;
        } else if (mv.piece.pip2 == open) {
            mv.piece.openPips = mv.piece.pip1;
        }
        endpoints[mv.endpoint].addLast(mv.piece);
    }

    private boolean drawHighlighted(AGraphics g, int endpoint, int mouseX, int mouseY, float DIM) {
        if (highlightedEndpoints.contains(endpoint)) {
            Vector2D mv = g.screenToViewport(mouseX, mouseY);
            boolean inside;
            if ((inside=Utils.isPointInsideRect(mv.getX(), mv.getY(), 0, 0, DIM*2, DIM))) {
                g.setColor(g.RED);
            } else {
                g.setColor(g.CYAN);
            }
            //Utils.println("inside(%s): mouse(%d,%d) trans(%3.2f,%3.2f) dim(%3.2f)", inside, mouseX, mouseY, mv.getX(), mv.getY(), DIM);
            g.drawRect(0, 0, DIM*2, DIM, 3);
            if (inside)
                selectedEndpoint = endpoint;
            return inside;
        }
        return false;
    }

    public synchronized final void draw(AGraphics g, float vpWidth, float vpHeight, int mouseX, int mouseY) {
        // choose an ortho that keeps the root in the middle and an edge around
        // that allows for a piece to be placed

        if (root == null)
            return;

        float maxPcW = Math.max(endpoints[EP_LEFT].size(), endpoints[EP_RIGHT].size());
        float maxPcH = Math.max(endpoints[EP_UP].size(), endpoints[EP_DOWN].size());

        float dimW = vpWidth/(2*(3+2*maxPcW));
        float dimH = vpHeight/(2*(2+2*maxPcH));

        float DIM = Math.min(dimW, dimH);

        selectedEndpoint = -1;
        g.pushMatrix();
            g.translate(vpWidth/2, vpHeight/2);

            Vector2D mv = g.screenToViewport(mouseX, mouseY);
            g.setColor(g.YELLOW);
            g.drawCircle(mv.getX(), mv.getY(), 10);

            g.pushMatrix();
                g.translate(-DIM, -DIM/2);
                drawTile(g, DIM, root.pip1, root.pip2);
            g.popMatrix();

            g.pushMatrix();
                g.translate(-DIM, -DIM/2);
                g.scale(-1, 1);
                for (Tile p : endpoints[EP_LEFT]) {
                    drawTile(g, DIM, p.getClosedPips(), p.openPips);
                    g.translate(DIM*2, 0);
                }
                drawHighlighted(g, EP_LEFT, mouseX, mouseY, DIM);
            g.popMatrix();
            g.pushMatrix();
                g.translate(DIM, -DIM/2);
                for (Tile p : endpoints[EP_RIGHT]) {
                    drawTile(g, DIM, p.getClosedPips(), p.openPips);
                    g.translate(DIM*2, 0);
                }
                drawHighlighted(g, EP_RIGHT, mouseX, mouseY, DIM);
            g.popMatrix();
            g.pushMatrix();
                g.translate(0, -DIM);
                g.rotate(-90);
                g.translate(-DIM/2, -DIM/2);
                for (Tile p : endpoints[EP_UP]) {
                    drawTile(g, DIM, p.getClosedPips(), p.openPips);
                    g.translate(DIM*2, 0);
                }
                drawHighlighted(g, EP_UP, mouseX, mouseY, DIM);
            g.popMatrix();
            g.pushMatrix();
                g.translate(DIM, DIM);
                g.rotate(90);
                g.translate(-DIM/2, DIM/2);
                for (Tile p : endpoints[EP_DOWN]) {
                    drawTile(g,DIM, p.getClosedPips(), p.openPips);
                    g.translate(DIM*2, 0);
                }
                drawHighlighted(g, EP_DOWN, mouseX, mouseY, DIM);
            g.popMatrix();
        g.popMatrix();
    }

    public static void drawTile(AGraphics g, float dim, int pips1, int pips2) {
        g.pushMatrix();
        int pipDim = Math.round(dim/8);
        g.setColor(g.BLACK);
        g.drawFilledRoundedRect(0, 0, dim*2, dim, dim/4);
        g.setColor(g.WHITE);
        g.drawRoundedRect(0, 0, dim*2, dim, 1, dim/4);
        g.setColor(g.WHITE);
        g.drawLine(dim, 0, dim, dim, 2);
        g.setColor(g.WHITE);
        drawDie(g, 0, 0, dim, pipDim, pips1);
        drawDie(g, dim, 0, dim, pipDim, pips2);
        g.popMatrix();
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

    public final int computeEndpointsTotal() {
        int score = 0;
        for (int i=0; i<4; i++) {
            if (endpoints[i].size() > 0) {
                score += endpoints[i].getLast().openPips;
            }
        }
        if (root != null) {
            if (endpoints[EP_LEFT].size() == 0) {
                score += root.pip1;
            }
            if (endpoints[EP_RIGHT].size() == 0) {
                score += root.pip2;
            }
        }
        return score;
    }

    public final void highlightMovesForPiece(Tile piece) {
        highlightedEndpoints.clear();
        highlightedTile = piece;
        if (piece != null) {
            for (Move m : findMovesForPiece(piece)) {
                highlightedEndpoints.add(m.endpoint);
            }
        }
    }

    public final int getSelectedEndpoint() {
        return this.selectedEndpoint;
    }

    public final Tile getHighlightedTile() {
        return this.highlightedTile;
    }
}
