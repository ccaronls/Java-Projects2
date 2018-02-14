package cc.game.dominos.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.game.dominos.core.Board;
import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Tile;
import cc.game.dominos.core.Player;
import cc.lib.game.AAnimation;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/2/18.
 */

public class PlayerComponent {
    /*
    extends
} Component implements MouseMotionListener, MouseListener {

    Player player;
    AWTGraphics G = null;
    int picked = -1;
    Board board;
    final DominosApplet applet;
    final int layoutPosition;

    private AAnimation<AWTGraphics> animation = null;

    final static Map<Player, PlayerComponent> compMap = new HashMap<>();

    private class TextPulseAnim extends AAnimation<AWTGraphics> {

        final String text;
        float tileD = 1;

        TextPulseAnim(String text) {
            super(700, 1, true);
            this.text = text;
        }

        @Override
        protected void draw(AWTGraphics g, float position, float dt) {
            Font curFont = g.getFont();
            float startSize = curFont.getSize();
            float maxSize = startSize * 1.5f;

            float targetSize = startSize + (maxSize-startSize)*position;
            Font newFont = curFont.deriveFont(targetSize);

            g.setFont(newFont);
            g.drawJustifiedString(0, 0, Justify.LEFT, Justify.TOP, text);
            g.setFont(curFont);

            repaint();
        }

        @Override
        protected void onDone() {
            animation = null;
            synchronized (applet.gameLock) {
                applet.gameLock.notify();
            }
        }
    };

    class NewTileAnim extends AAnimation<AWTGraphics> {

        final Tile tile;
        float tileD = 1;

        NewTileAnim(Tile tile) {
            super(1000);
            this.tile = tile;
        }

        @Override
        protected void draw(AWTGraphics g, float position, float dt) {
            g.pushMatrix();
            g.translate(tileD, tileD/2);
            g.scale(position, position);
            g.translate(-tileD, -tileD/2);
            if (board != null)
                Board.drawTile(G, tileD, tile.pip1, tile.pip2);
            else
                G.drawFilledRoundedRect(0, 0, tileD * 2, tileD, tileD/ 4);
            g.popMatrix();
            repaint();
        }

        @Override
        protected void onDone() {
            animation = null;
            synchronized (applet.gameLock) {
                applet.gameLock.notify();
            }
            repaint();
        }
    }

    void clearAnimations() {
        animation = null;
    }

    void startAddPointAnim(int pts) {
        animation = new TextPulseAnim("Points: " + player.getScore() + "\n+" + pts).start();
    }

    PlayerComponent(DominosApplet applet, int layoutPosition) {
        this.applet = applet;
        this.layoutPosition = layoutPosition;
    }

    void init(Player player, Board board) {
        this.player = player;
        this.board = board;
        if (board != null) {
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        compMap.put(player, this);
    }

    @Override
    public void paint(Graphics g) {
        if (player == null)
            return;

        if (G == null) {
            G = new AWTGraphics(g, this);
            G.ortho(0, getWidth(), 0, getHeight());
            G.setIdentity();
        } else {
            G.setGraphics(g);
        }

        G.setColor(G.GREEN.darken(G, 0.2f));
        G.drawFilledRect(0, 0, getWidth(), getHeight());

        G.setColor(G.BLACK);
        boolean vertical = getHeight() > getWidth();

        // compute number of rows needed
        //float dim = vertical ? getWidth()-10 : getHeight() - 10;

        float w = getWidth();
        float h = getHeight();

        if (applet.dominos.getCurPlayer() == player) {
            G.setColor(G.BLUE);
            G.drawRect(2, 2, w-4, h-4, 4);
        } else if (player.getScore() >= 150) {
            int red = (int)System.currentTimeMillis() % 256;
            int grn = (int)(System.currentTimeMillis() -857634567) % 256;
            int blu = (int)(System.currentTimeMillis() -3225299) % 256;

            AColor c = G.makeColor(red,grn,blu);
            G.setColor(c);
            G.drawRect(2, 2, w-4, h-4, 4);
            repaint();
        }

        float padding = 30;
        float offset = 5;

        float tileD = Math.min(w, h)-padding;
        int tilesPerRow = (int)((Math.max(w, h)-padding) / (tileD * 2));

        int numRows = (int)Math.ceil(Math.sqrt((double) player.getTiles().size()/tilesPerRow));
        tilesPerRow *= numRows;
        tileD /= numRows;

        G.translate(offset, offset);
        if (animation != null && (animation instanceof TextPulseAnim)) {
            G.setColor(G.RED);
            animation.update(G);
        } else {
            G.setColor(G.BLACK);
            G.drawString("Points: " + player.getScore(),0, 0);
        }
        G.translate(-offset, -offset);

        G.begin();
        G.pushMatrix();
        G.clearMinMax();
        if (vertical) {
            G.translate(w-offset, padding);
            G.rotate(90);
        } else {
            G.translate(padding-offset, padding-offset);
        }
        picked = -1;
        int tile = 0;
        for (int i=0; i<numRows; i++) {
            G.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < player.getTiles().size()) {
                    Tile t = player.getTiles().get(tile);
                    G.pushMatrix();
                    G.scale(0.95f, 0.95f);
                    G.setColor(G.BLACK);
                    if (animation != null && (animation instanceof NewTileAnim) && ((NewTileAnim)animation).tile == t) {
                        animation.update(G);
                    }
                    else if (board != null)
                        Board.drawTile(G, tileD, t.pip1, t.pip2);
                    else
                        G.drawFilledRoundedRect(0, 0, tileD * 2, tileD, tileD/ 4);
                    G.popMatrix();
                    G.setName(tile++);
                    G.end();
                    if (picked < 0) {
                        G.begin();
                        G.vertex(0, 0);
                        G.vertex(tileD * 2, tileD);
                        picked = G.pickRects(mx, my);
                        G.end();
                        if (picked >=0) {
                            G.setColor(G.RED);
                            G.drawRect(0, 0, tileD*2, tileD, 3);
                        }
                    }
                    G.translate(tileD*2, 0);
                }
            }
            G.popMatrix();
            G.translate(0, tileD);
        }

        G.popMatrix();
        G.end();
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    int mx,my;

    @Override
    public void mouseMoved(MouseEvent e) {
        mx = e.getX();
        my = e.getY();
        repaint();
    }

    @Override
    public synchronized void mouseClicked(MouseEvent e) {
        if (picked >= 0)
            board.highlightMovesForPiece(player.getTiles().get(picked));
        else
            board.highlightMovesForPiece(null);
        applet.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        repaint();
    }*/
}
