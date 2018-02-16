package cc.game.dominos.swing;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;

import javax.swing.JComponent;

import cc.game.dominos.core.*;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;

public class DominosApplet extends JComponent implements MouseListener, MouseMotionListener {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new DominosApplet();
    }

    final EZFrame frame;

    DominosApplet() {
        frame = new EZFrame("Dominos") {
            protected void onWindowClosing() {
                dominos.trySaveToFile(saveFile);
            }

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (subMenu) {
                    case "New Game":
                        dominos.startNewGame(9, 150);
                        dominos.startGameThread();
                        break;
                }
            }
        };
        frame.addMenuBarMenu("File", "New Game");
        frame.add(this, BorderLayout.CENTER);
        try {
            dominos.loadFromFile(saveFile);
        } catch (Exception e) {
            dominos.setNumPlayers(4);
            dominos.startNewGame(9, 150);
        }

        if (!frame.loadFromFile(new File("dominos.properties")))
            frame.centerToScreen(800, 600);
        addMouseListener(this);
        addMouseMotionListener(this);
        dominos.startGameThread();
    }

    File saveFile = new File("dominos.save");
    final Dominos dominos = new Dominos() {

        @Override
        public void redraw() {
            repaint();
        }
    };

    AWTGraphics G = null;
    int mouseX, mouseY;

    @Override
    public synchronized void paint(Graphics g) {
        if (G == null) {
            G = new AWTGraphics(g, this);
            G.setIdentity();
        } else {
            G.setGraphics(g);
            G.initViewport(getWidth(), getHeight());
        }
        dominos.draw(G, mouseX, mouseY);
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("click");
        repaint();
    }

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        dominos.onClick();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }
}
