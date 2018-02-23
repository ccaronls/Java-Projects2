package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 2/1/18.
 *
 * Representation of a Dominos board.
 *
 */
public class Board extends Reflector<Board> {

    static {
        addAllFields(Board.class);
    }

    public final static int EP_LEFT     = 0;
    public final static int EP_RIGHT    = 1;
    public final static int EP_UP       = 2;
    public final static int EP_DOWN     = 3;

    public final static int PLACMENT_FWD        = 0;
    public final static int PLACMENT_FWD_RIGHT  = 1;
    public final static int PLACMENT_FWD_LEFT   = 2;
    public final static int PLACMENT_RIGHT      = 3;
    public final static int PLACMENT_LEFT       = 4;
    public final static int PLACEMENT_COUNT     = 5;

    private Tile root = null;
    private final LinkedList<Tile> [] endpoints = new LinkedList[4];
    private final Matrix3x3 [] endpointTransforms = new Matrix3x3[4];

    @Omit
    private List<Move> [] highlightedMoves = new List[4];
    @Omit
    Move selectedMove = null;

    private final List<Vector2D[]> rects = new ArrayList<>();
    private final MutableVector2D saveMinV = new MutableVector2D();
    private final MutableVector2D saveMaxV = new MutableVector2D();

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

    @Omit
    final List<AAnimation<AGraphics>> animations = new ArrayList<>();

    final void clear() {
        root = null;
        for (List<?> l : endpoints) {
            l.clear();
        }
        clearSelection();
        rects.clear();
        for (int i=0; i<4; i++) {
            endpointTransforms[i].identityEq();
            transformEndpoint(endpointTransforms[i], i);
        }
        saveMaxV.zero();
        saveMinV.zero();
    }

    final void clearSelection() {
        for (Collection<?> s: highlightedMoves)
            s.clear();
        selectedMove = null;
    }

    public Board() {
        for (int i=0; i<4; i++) {
            endpoints[i] = new LinkedList<>();
            endpointTransforms[i] = new Matrix3x3().identityEq();
            highlightedMoves[i] = new ArrayList<>();
        }
    }

    final List<Tile> collectPieces() {
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

    final void placeRootPiece(Tile pc) {
        root = pc;
        rects.add(new Vector2D[] {
                new Vector2D(-1, -0.5f),
                new Vector2D(1, 0.5f)
        });
    }

    final List<Move> findMovesForPiece(Tile p) {
        List<Move> moves = new ArrayList<>();
        for (int i=0; i<4; i++) {
            if (endpoints[i].size() == 0) {
                if (canPieceTouch(p, root.pip1)) {
                    moves.add(new Move(p, i, PLACMENT_FWD));
                }
            } else {
                if (canPieceTouch(p, endpoints[i].getLast().openPips)) {
                    if (endpoints[i].size() < 2)
                        moves.add(new Move(p, i, PLACMENT_FWD));
                    else {
                        Matrix3x3 m = new Matrix3x3();
                        for (int ii = 0; ii < PLACEMENT_COUNT; ii++) {
                            m.assign(endpointTransforms[i]);
                            transformPlacement(m, ii);
                            Vector2D v = new Vector2D(0.6f, 0.3f);
                            v = m.multiply(v);
                            if (isInsideRects(v))
                                continue;
                            v = new Vector2D(1.6f, 0.6f);
                            v = m.multiply(v);
                            if (isInsideRects(v))
                                continue;

                            moves.add(new Move(p, i, ii));
                        }
                        // TODO: Make sure there is at least one option?
                    }
                }
            }
        }
        return moves;
    }

    private final boolean canPieceTouch(Tile p, int pips) {
        return p.pip1 == pips || p.pip2 == pips;
    }

    final void doMove(Move mv) {
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
        mv.piece.placement = mv.placment;
        transformPlacement(endpointTransforms[mv.endpoint], mv.placment);
        Vector2D v0 = Vector2D.ZERO;
        Vector2D v1 = new Vector2D(2, 1);
        rects.add(new Vector2D[] {
            endpointTransforms[mv.endpoint].multiply(v0),
            endpointTransforms[mv.endpoint].multiply(v1)
        });
        endpointTransforms[mv.endpoint].multiplyEq(new Matrix3x3().setTranslate(2, 0));
    }

    private void transformPlacement(AGraphics g, int placement) {
        Matrix3x3 t = new Matrix3x3();
        g.getTransform(t);
        transformPlacement(t, placement);
        g.setIdentity();
        g.multMatrix(t);
    }

    private void transformPlacement(Matrix3x3 m, int placement) {
        Matrix3x3 t = new Matrix3x3();
        switch (placement) {
            case PLACMENT_FWD: break;
            case PLACMENT_FWD_LEFT:
                t.setTranslate(1, 0);
                m.multiplyEq(t);
                t.setRotation(90);
                m.multiplyEq(t);
                break;
            case PLACMENT_LEFT:
                t.setTranslate(0, 1);
                m.multiplyEq(t);
                t.setRotation(90);
                m.multiplyEq(t);
                break;
            case PLACMENT_FWD_RIGHT:
                t.setTranslate(0, 1);
                m.multiplyEq(t);
                t.setRotation(-90);
                m.multiplyEq(t);
                break;
            case PLACMENT_RIGHT:
                t.setTranslate(-1, 0);
                m.multiplyEq(t);
                t.setRotation(-90);
                m.multiplyEq(t);
                break;
        }
    }

    private String epIndexToString(int index) {
        switch (index) {
            case EP_LEFT: return "EP_LEFT";
            case EP_RIGHT: return "EP_RIGHT";
            case EP_UP: return "EP_UP";
            case EP_DOWN: return "EP_DOWN";
        }
        throw new AssertionError();
    }

    private String placementIndexToString(int index) {
        switch (index) {
            case PLACMENT_FWD: return "FWD";
            case PLACMENT_FWD_LEFT: return "FWD_LEFT";
            case PLACMENT_FWD_RIGHT: return "FWD_RIGHT";
            case PLACMENT_LEFT: return "LEFT";
            case PLACMENT_RIGHT: return "RIGHT";
        }
        throw new AssertionError();
    }

    private int drawHighlighted(APGraphics g, int endpoint, int mouseX, int mouseY, Tile dragged) {
        MutableVector2D mv = new MutableVector2D();
        g.setColor(GColor.CYAN);
        g.begin();
        int moveIndex = 0;
        for (Move move : highlightedMoves[endpoint]) {
            g.pushMatrix();
            transformPlacement(g, move.placment);
            g.setName(moveIndex++);
            mv.set(1, 0.5f);
            g.transform(mv);
            g.vertex(0, 0);
            g.vertex(2, 1);
            g.popMatrix();
            g.drawRects(3);
        }
        int picked = g.pickRects(mouseX, mouseY);
        g.end();
        if (picked >= 0) {
            selectedMove = highlightedMoves[endpoint].get(picked);
            int selectedEndpoint = selectedMove.endpoint;
            int newTotal = computeEndpointsTotal();
            int openPips = root.pip1;
            if (endpoints[selectedEndpoint].size() > 0) {
                newTotal -= openPips = endpoints[selectedEndpoint].getLast().openPips;
            } else if (selectedEndpoint == EP_LEFT || selectedEndpoint == EP_RIGHT) {
                newTotal -= root.pip1;
            }

            if (selectedMove.piece.pip1 == openPips) {
                newTotal += selectedMove.piece.pip2;
            } else {
                newTotal += selectedMove.piece.pip1;
            }
            Utils.println("Endpoint total:" + newTotal);

            g.begin();
            g.pushMatrix();
            g.setColor(GColor.RED);
            transformPlacement(g, selectedMove.placment);
            if (dragged != null) {
                int pip1 = dragged.pip1;
                int pip2 = dragged.pip2;
                if (pip2 == openPips) {
                    int t = pip1;
                    pip1 = pip2;
                    pip2 = t;
                }
                g.begin();
                drawTile(g, pip1, pip2, 1);
                g.end();
            }
            g.vertex(0, 0);
            g.vertex(2, 1);
            g.drawRects(3);
            g.popMatrix();
            g.end();
            Utils.println("selected endpoint = " + epIndexToString(selectedEndpoint) + " placement = " + placementIndexToString(selectedMove.placment));
        }
        return picked;
    }

    void transformEndpoint(AGraphics g, int endpoint) {
        Matrix3x3 t = new Matrix3x3();
        g.getTransform(t);
        transformEndpoint(t, endpoint);
        g.setIdentity();
        g.multMatrix(t);
    }

    private void transformEndpoint(Matrix3x3 m, int endpoint) {
        Matrix3x3 t = new Matrix3x3();
        switch (endpoint) {
            case EP_LEFT:
                t.setScale(-1, -1);
                m.multiplyEq(t);
            case EP_RIGHT:
                t.setTranslate(1, -0.5f);
                m.multiplyEq(t);
                break;
            case EP_DOWN:
                t.setScale(-1, -1);
                m.multiplyEq(t);
            case EP_UP:
                t.setTranslate(0.5f, 0.5f);
                m.multiplyEq(t);
                t.setRotation(90);
                m.multiplyEq(t);
                break;
        }
    }

    private void genMinMaxPts(APGraphics g) {
        g.vertex(-1, -0.5f);
        g.vertex(1, 0.5f);
        for (int i = 0; i < 4; i++) {
            g.pushMatrix();
            {
                transformEndpoint(g, i);
                for (Tile t : endpoints[i]) {
                    transformPlacement(g, t.placement);
                    g.vertex(0, 0);
                    g.vertex(0, 1);
                    g.vertex(2, 1);
                    g.vertex(2, 0);
                    g.translate(2, 0);
                }
                g.vertex(0, 3);
                g.vertex(2, 3);
                g.vertex(2, -2);
                g.vertex(0, -2);
            }
            g.popMatrix();
        }
    }

    void transformToEndpointLastPiece(AGraphics g, int ep) {
        transformEndpoint(g, ep);
        for (Tile p : endpoints[ep]) {
            transformPlacement(g, p.placement);
            g.translate(2, 0);
        }
    }

    synchronized int draw(APGraphics g, float vpWidth, float vpHeight, int pickX, int pickY, Tile dragging) {
        // choose an ortho that keeps the root in the middle and an edge around
        // that allows for a piece to be placed

        if (root == null)
            return -1;

        int picked = -1;
        g.pushMatrix();
        {
            g.begin();
            g.clearMinMax();
            genMinMaxPts(g);

            Vector2D minBR = saveMinV.minEq(g.getMinBoundingRect());
            Vector2D maxBR = saveMaxV.maxEq(g.getMaxBoundingRect());
            g.end();

            float maxPcW = Math.max(Math.abs(minBR.getX()), Math.abs(maxBR.getX()));
            float maxPcH = Math.max(Math.abs(minBR.getY()), Math.abs(maxBR.getY()));

            float dimW = vpWidth/(2*maxPcW);
            float dimH = vpHeight/(2*maxPcH);

            float DIM = Math.min(dimW, dimH);
            g.setPointSize(DIM/8);

            selectedMove = null;

            g.translate(vpWidth / 2, vpHeight / 2);
            g.scale(DIM, -DIM);

            // DEBUG outline the min/max bounding box
            if (false && AGraphics.DEBUG_ENABLED) {
                g.setColor(GColor.YELLOW);
                g.drawRect(minBR, maxBR, 1);
            }

            // DEBUG draw pickX, pickY in viewport coords
            if (false && AGraphics.DEBUG_ENABLED) {
                Vector2D mv = g.screenToViewport(pickX, pickY);
                g.setColor(GColor.YELLOW);
                g.drawCircle(mv.getX(), mv.getY(), 10);
            }

            g.pushMatrix();
            {
                g.translate(-1, -0.5f);
                drawTile(g, root.pip1, root.pip2, 1);
            }
            g.popMatrix();

            for (int i = 0; i < 4; i++) {
                g.pushMatrix();
                {
                    transformEndpoint(g, i);
                    for (Tile p : endpoints[i]) {
                        transformPlacement(g, p.placement);
                        drawTile(g, p.getClosedPips(), p.openPips, 1);
                        g.translate(2, 0);
                    }
                    picked = Math.max(picked, drawHighlighted(g, i, pickX, pickY, dragging));
                }
                g.popMatrix();
            }

            // DEBUG draw the rects
            if (false && AGraphics.DEBUG_ENABLED) {
                g.setColor(GColor.ORANGE);
                for (IVector2D [] r : rects) {
                    g.drawRect(r[0], r[1], 3);
                }
            }

        }

        Iterator<AAnimation<AGraphics>> it = animations.iterator();
        while (it.hasNext()) {
            AAnimation<AGraphics> a = it.next();
            if (a.isDone()) {
                it.remove();
            } else {
                a.update(g);
            }
        }
        g.popMatrix();
        return picked;
    }

    /**
     *
     * @param g
     * @param pips1
     * @param pips2
     * @param alpha [0-1] inclusive
     */
    static void drawTile(AGraphics g, int pips1, int pips2, float alpha) {
        g.pushMatrix();
        g.setColor(GColor.BLACK.withAlpha(alpha));
        g.drawFilledRoundedRect(0, 0, 2, 1, 0.25f);
        g.setColor(GColor.WHITE);
        g.drawRoundedRect(0, 0, 2, 1, 1, 0.25f);
        g.setColor(GColor.WHITE);
        g.drawLine(1, 0, 1, 1, 2);
        g.setColor(GColor.WHITE);
        drawDie(g, 0, 0, pips1);
        drawDie(g, 1, 0, pips2);
        if (false && AGraphics.DEBUG_ENABLED) {
            g.setColor(GColor.RED);
            g.drawDisk(0, 0, 4);
        }
        g.popMatrix();
    }

    static void drawDie(AGraphics g, float x, float y, int numDots) {
        float dd2 = 0.5f;
        float dd4 = 0.25f;
        float dd34 = 0.75f;//(dim*3)/4;
        float dd5 = 0.2f;//dim/5;
        float dd25 = 0.4f;//dim*2/5;
        float dd35 = 0.6f;//dim*3/5;
        float dd45 = 0.8f;//dim*4/5;
        g.begin();
        switch (numDots) {
            case 1:
                g.vertex(x+dd2, y+dd2);
                break;
            case 2:
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                break;
            case 3:
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd2, y+dd2);
                g.vertex(x+dd34, y+dd34);
                break;
            case 4:
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                break;
            case 5:
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                g.vertex(x+dd2, y+dd2);
                break;
            case 6:
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                g.vertex(x+dd4, y+dd2);
                g.vertex(x+dd34, y+dd2);
                break;
            case 7:
                g.vertex(x+dd2, y+dd2);
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                g.vertex(x+dd4, y+dd2);
                g.vertex(x+dd34, y+dd2);
                break;
            case 8:
                g.vertex(x+dd2, y+dd4);
                g.vertex(x+dd2, y+dd34);
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                g.vertex(x+dd4, y+dd2);
                g.vertex(x+dd34, y+dd2);
                break;
            case 9:
                g.vertex(x+dd2, y+dd2);
                g.vertex(x+dd2, y+dd4);
                g.vertex(x+dd2, y+dd34);
                g.vertex(x+dd4, y+dd4);
                g.vertex(x+dd34, y+dd34);
                g.vertex(x+dd4, y+dd34);
                g.vertex(x+dd34, y+dd4);
                g.vertex(x+dd4, y+dd2);
                g.vertex(x+dd34, y+dd2);
                break;
            case 10:
                g.vertex(x+dd4, y+dd5);
                g.vertex(x+dd4, y+dd25);
                g.vertex(x+dd4, y+dd35);
                g.vertex(x+dd4, y+dd45);

                g.vertex(x+dd2, y+dd5);
                g.vertex(x+dd2, y+dd45);

                g.vertex(x+dd34, y+dd5);
                g.vertex(x+dd34, y+dd25);
                g.vertex(x+dd34, y+dd35);
                g.vertex(x+dd34, y+dd45);
                break;
            case 11:
                g.vertex(x+dd4, y+dd5);
                g.vertex(x+dd4, y+dd25);
                g.vertex(x+dd4, y+dd35);
                g.vertex(x+dd4, y+dd45);

                g.vertex(x+dd2, y+dd5);
                g.vertex(x+dd2, y+dd2);
                g.vertex(x+dd2, y+dd45);

                g.vertex(x+dd34, y+dd5);
                g.vertex(x+dd34, y+dd2);
                g.vertex(x+dd34, y+dd35);
                g.vertex(x+dd34, y+dd45);
                break;
            case 12:
                g.vertex(x+dd4, y+dd5);
                g.vertex(x+dd4, y+dd25);
                g.vertex(x+dd4, y+dd35);
                g.vertex(x+dd4, y+dd45);

                g.vertex(x+dd2, y+dd5);
                g.vertex(x+dd2, y+dd25);
                g.vertex(x+dd2, y+dd35);
                g.vertex(x+dd2, y+dd45);

                g.vertex(x+dd34, y+dd5);
                g.vertex(x+dd34, y+dd25);
                g.vertex(x+dd34, y+dd35);
                g.vertex(x+dd34, y+dd45);
                break;
        }
        g.drawPoints();
        g.end();
    }

    final int computeEndpointsTotal() {
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

    final void highlightMoves(List<Move> moves) {
        for (int i=0; i<4; i++)
            highlightedMoves[i].clear();

        if (moves != null) {
            for (Move m : moves) {
                highlightedMoves[m.endpoint].add(m);
            }
        }
    }

    private boolean isInsideRects(IVector2D v) {
        for (IVector2D[] r : rects) {
            if (Utils.isPointInsideRect(v, r[0], r[1]))
                return true;
        }
        return false;
    }

    public int getOpenPips(int ep) {
        if (endpoints[ep].size() == 0) {
            if (ep == EP_LEFT || ep == EP_RIGHT)
                return root.pip1;
            return 0;
        }
        return endpoints[ep].getLast().openPips;
    }
}
