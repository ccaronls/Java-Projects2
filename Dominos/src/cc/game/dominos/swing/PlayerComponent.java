package cc.game.dominos.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import cc.game.dominos.core.Board;
import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Tile;
import cc.game.dominos.core.Player;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/2/18.
 */

public class PlayerComponent extends Component implements MouseMotionListener, MouseListener {

    Player player;
    AWTGraphics G = null;
    int picked = -1;
    Board board;
    final DominosApplet applet;

    PlayerComponent(DominosApplet applet) {
        this.applet = applet;
    }

    void init(Player player, Board board) {
        this.player = player;
        this.board = board;
        if (board != null) {
            addMouseMotionListener(this);
            addMouseListener(this);
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        if (player == null)
            return;

        if (G == null) {
            G = new AWTGraphics(g, this);
            G.ortho(0, getWidth(), 0, getHeight());
            G.setIdentity();
        } else {
            G.setGraphics(g);
        }

        G.setColor(G.BLACK);
        boolean vertical = getHeight() > getWidth();

        // compute number of rows needed
        //float dim = vertical ? getWidth()-10 : getHeight() - 10;

        float w = getWidth();
        float h = getHeight();

        float padding = 30;

        float tileD = Math.min(w, h)-padding;
        int tilesPerRow = (int)((Math.max(w, h)-padding) / (tileD * 2));

        int numRows = (int)Math.ceil(Math.sqrt((double) player.getTiles().size()/tilesPerRow));
        tilesPerRow *= numRows;
        tileD /= numRows;

        G.setColor(G.BLACK);
        G.drawString("Points: " + player.getScore(), 5, 5);

        G.begin();
        G.pushMatrix();
        G.clearMinMax();
        if (vertical) {
            G.translate(w, padding);
            G.rotate(90);
        } else {
            G.translate(padding, padding);
        }
        picked = -1;
        int tile = 0;
        List<IVector2D> pickedRect = null;
        for (int i=0; i<numRows; i++) {
            G.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < player.getTiles().size()) {
                    Tile t = player.getTiles().get(tile);
                    G.pushMatrix();
                    G.scale(0.95f, 0.95f);
                    if (board != null)
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
    }
}
