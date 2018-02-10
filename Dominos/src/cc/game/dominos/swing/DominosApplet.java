package cc.game.dominos.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.List;

import javax.swing.JComponent;

import cc.game.dominos.core.*;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.utils.Reflector;

public class DominosApplet extends JComponent implements ActionListener, MouseListener, MouseMotionListener {

    static {
        Reflector.registerClass(PlayerConsole.class);
    }

    final static Object gameLock = new Object();

    public static class PlayerConsole extends Player {

        Tile tile = null;
        int endpoint = -1;

        @Override
        public Move chooseMove(List<Move> moves) {

            synchronized (gameLock) {
                try {
                    gameLock.wait();
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }

            if (tile != null) {
                for (Move m : moves) {
                    if (m.piece.equals(tile) && m.endpoint == endpoint) {
                        tile = null;
                        return m;
                    }
                }
            }

            return null;
        }

    }

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new DominosApplet();
    }

    boolean gameRunning = false;

    void startNewGame() {
        if (gameRunning) {
            gameRunning = false;
            synchronized (gameLock) {
                gameLock.notifyAll();
            }
            resetComponents();
        }
        dominos.newGame(6, 150);
        startGameThread();
        frame.repaint();
    }

    void resetComponents() {
        eastPlayer.clearAnimations();
        westPlayer.clearAnimations();
        southPlayer.clearAnimations();
        northPlayer.clearAnimations();
    }

    void startGameThread() {
        new Thread() {
            public void run() {
                gameRunning = true;
                while (gameRunning && !dominos.isGameOver()) {
                    dominos.runGame();
                    synchronized (this) {
                        try {
                            wait(1000);
                        } catch (Exception e) {}
                    }
                }
                gameRunning = false;
                Utils.println("Thread done.");
            }
        }.start();
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
                        startNewGame();
                        break;
                }
            }
        };
        frame.addMenuBarMenu("File", "New Game");
        frame.setLayout(new BorderLayout());
        frame.add(northPlayer = new PlayerComponent(this, Board.EP_UP), BorderLayout.NORTH);
        frame.add(eastPlayer = new PlayerComponent(this, Board.EP_RIGHT), BorderLayout.EAST);
        frame.add(westPlayer = new PlayerComponent(this, Board.EP_LEFT), BorderLayout.WEST);
        frame.add(southPlayer = new PlayerComponent(this, Board.EP_RIGHT), BorderLayout.SOUTH);

        northPlayer.setPreferredSize(new Dimension(600, 100));
        southPlayer.setPreferredSize(new Dimension(600, 100));
        eastPlayer.setPreferredSize(new Dimension(100, 600));
        westPlayer.setPreferredSize(new Dimension(100, 600));
        frame.add(this, BorderLayout.CENTER);
        try {
            dominos.loadFromFile(saveFile);
            for (int i=0; i<dominos.getNumPlayers(); i++) {
                if (dominos.getPlayer(i) instanceof PlayerConsole) {
                    user = (PlayerConsole)dominos.getPlayer(i);
                    break;
                }
            }
            if (user == null)
                throw new Exception("Cannot find user");
        } catch (Exception e) {
            user = new PlayerConsole();
            dominos.initPlayers(user, new Player(), new Player(), new Player());
            dominos.newGame(6, 150);
        }

        southPlayer.init(user, dominos.getBoard());
        eastPlayer.init(dominos.getPlayer(1), null);
        northPlayer.init(dominos.getPlayer(2), null);
        westPlayer.init(dominos.getPlayer(3), null);

        if (!frame.loadFromFile(new File("dominos.properties")))
            frame.centerToScreen(800, 600);
        addMouseListener(this);
        addMouseMotionListener(this);
        startGameThread();
        frame.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }


    File saveFile = new File("dominos.save");
    final Dominos dominos = new Dominos() {
        @Override
        protected void onPiecePlaced(Player player, Tile pc) {
            frame.repaint();
        }

        @Override
        public void onTilePlaced(Player p, Move mv) {
            frame.repaint();
        }

        @Override
        public void onTileFromPool(Player p, Tile pc) {
            frame.repaint();
        }

        @Override
        public void onKnock(Player p) {
            frame.repaint();
        }

        @Override
        protected void onPlayerPoints(Player p, int pts) {
            PlayerComponent comp = PlayerComponent.compMap.get(p);
            comp.startAddPointAnim(pts);
            frame.repaint();

            try {
                synchronized (gameLock) {
                    gameLock.wait(5000);
                }
            } catch (Exception e) {}
            frame.repaint();
        }
    };
    PlayerConsole user = null;
    PlayerComponent northPlayer = null;
    PlayerComponent southPlayer = null;
    PlayerComponent eastPlayer = null;
    PlayerComponent westPlayer = null;
    AWTGraphics G = null;
    int mouseX, mouseY;

    @Override
    public void paint(Graphics g) {
        if (G == null) {
            G = new AWTGraphics(g, this);
            G.setIdentity();
        } else {
            G.setGraphics(g);
            G.initViewport(getWidth(), getHeight());
        }

        G.clearScreen(G.GREEN);
        Board b = dominos.getBoard();
        if (b != null) {
            b.draw(G, G.getViewportWidth(), G.getViewportHeight(), mouseX, mouseY);
            G.setColor(G.BLUE);
            G.drawString("endpoints sum:" + b.computeEndpointsTotal(), 10, 10);
        }
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("click");
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (dominos.getBoard().getSelectedEndpoint() >= 0) {
            user.tile = dominos.getBoard().getHighlightedTile();
            user.endpoint = dominos.getBoard().getSelectedEndpoint();
            dominos.getBoard().clearSelection();
            synchronized (gameLock) {
                gameLock.notifyAll();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //System.out.println("entered");
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //System.out.println("exited");
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
