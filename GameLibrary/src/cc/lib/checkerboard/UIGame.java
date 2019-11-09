package cc.lib.checkerboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public abstract class UIGame extends Game {

    static Logger log = LoggerFactory.getLogger(UIGame.class);

    final File saveFile;
    boolean gameRunning = false;
    final Object lock = new Object();

    public UIGame(File saveFile) {
        this.saveFile = saveFile;
        setRules(new Checkers());
        setPlayer(NEAR, new UIPlayer());
        setPlayer(FAR, new UIPlayer());
        newGame();
        tryLoadFromFile(saveFile);
    }

    public abstract void repaint();

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        new Thread() {
            public void run() {
                while (gameRunning) {
                    runGame();
                    if (isGameOver())
                        break;
                    trySaveToFile(saveFile);
                    repaint();
                }
                gameRunning = false;
            }
        }.start();
        gameRunning = true;
    }

    public void stopGameThread() {
        gameRunning = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    int highlightedRank, highlightedCol;
    final List<Piece> pickablePieces = new ArrayList<>();
    final List<Move>  pickableMoves  = new ArrayList<>();

    public void draw(AGraphics g, int mx, int my) {
        log.debug("draw ...");
        int cx = g.getViewportWidth() / cols;
        int cy = g.getViewportHeight() / ranks;

        int dim = Math.min(cx, cy)/3;
        highlightedRank = highlightedCol = -1;
        g.clearScreen(GColor.GRAY);
        g.setColor(GColor.BLACK);
        for (int x=0; x<=cols; x++) {
            g.drawLine(x*cx, 0, x*cx, g.getViewportHeight());
        }
        for (int y=0; y<=ranks; y++) {
            g.drawLine(0, y*cy, g.getViewportWidth(), y*cy);
        }

        for (int r=0; r<ranks; r++) {
            for (int c=0; c<cols; c++) {
                Piece p = getPiece(r, c);
                int x = c * cx + cx / 2;
                int y = r * cy + cy / 2;
                if (Utils.isPointInsideRect(mx, my, c*cx, r*cy, cx, cy)) {
                    g.setColor(GColor.CYAN);
                    g.drawRect(c*cx+1, r*cy+1, cx-2, cy-2);
                    highlightedRank = r;
                    highlightedCol = c;
                }

                for (Piece pp : pickablePieces) {
                    if (pp.getRank() == r && pp.getCol() == c) {
                        g.setColor(GColor.CYAN);
                        g.drawFilledCircle(x, y, dim+5);
                        break;
                    }
                }

                for (Move m : pickableMoves) {
                    if (m.getEnd() != null && m.getEnd()[0] == r && m.getEnd()[1] == c) {
                        g.setColor(GColor.CYAN);
                        g.drawCircle(x, y, dim);
                        break;
                    }
                }

                switch (p.getType()) {
                    case KING:
                        g.setColor(getPlayer(p.getPlayerNum()).getColor());
                        g.drawFilledCircle(x, y-cy/5, dim);
                    case CHECKER:
                        g.setColor(getPlayer(p.getPlayerNum()).getColor());
                        g.drawFilledCircle(x, y, dim);
                        break;
                }
            }
        }
    }

    public void doClick() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public final void endMove() {

    }

    public final boolean isGameOver() {
        return rules.computeMoves(this, false) == 0;
    }

    public Piece choosePieceToMove(List<Piece> pieces) {
        pickableMoves.clear();
        pickablePieces.clear();
        pickablePieces.addAll(pieces);
        repaint();
        Utils.waitNoThrow(lock, -1);
        for (Piece p : pieces) {
            if (p.getRank() == highlightedRank && p.getCol() == highlightedCol) {
                return p;
            }
        }
        return null;
    }

    public Move chooseMoveForPiece(List<Move> moves) {
        pickableMoves.clear();
        pickablePieces.clear();
        pickableMoves.addAll(moves);
        repaint();
        Utils.waitNoThrow(lock, -1);
        for (Move m : moves) {
            if (m.getEnd()[0] == highlightedRank && m.getEnd()[1] == highlightedCol) {
                return m;
            }
        }
        return null;
    }
}
