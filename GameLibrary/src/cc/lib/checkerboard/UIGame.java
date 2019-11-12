package cc.lib.checkerboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public abstract class UIGame extends Game {

    static Logger log = LoggerFactory.getLogger(UIGame.class);

    final File saveFile;
    boolean gameRunning = false;
    final Object RUNGAME_MONITOR = new Object();

    public UIGame(File saveFile) {
        this.saveFile = saveFile;
        try {
            loadFromFile(saveFile);
        } catch (Exception e) {
            setRules(new Chess());
            setPlayer(NEAR, new UIPlayer());
            setPlayer(FAR, new UIPlayer());
            newGame();
        }
    }

    @Override
    public final Move undo() {
        Move m;
        m = super.undo();
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
        repaint();
        return m;
    }

    public abstract void repaint();

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        new Thread() {
            public void run() {
                while (gameRunning) {
                    log.debug("run game");
                    runGame();
                    if (gameRunning) {
                        if (isGameOver())
                            break;
                        trySaveToFile(saveFile);
                        repaint();
                    } else {
                        log.debug("leaving");
                    }
                }
                gameRunning = false;
                log.debug("game thread stopped");
            }
        }.start();
        gameRunning = true;
    }

    public void stopGameThread() {
        gameRunning = false;
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
    }

    int highlightedRank, highlightedCol;
    final List<Piece> pickablePieces = new ArrayList<>();
    final List<Move>  pickableMoves  = new ArrayList<>();

    public final void draw(AGraphics g, int mx, int my) {
        if (!initialized)
            return;
        //log.debug("draw ...");
        int cw = g.getViewportWidth() / cols;
        int ch = g.getViewportHeight() / ranks;

        int dim = Math.min(cw, ch)/3;
        highlightedRank = highlightedCol = -1;
        g.clearScreen(GColor.GRAY);
        g.setColor(GColor.BLACK);
        for (int x=0; x<=cols; x++) {
            g.drawLine(x* cw, 0, x* cw, g.getViewportHeight());
        }
        for (int y=0; y<=ranks; y++) {
            g.drawLine(0, y* ch, g.getViewportWidth(), y* ch);
        }

        if (selectedPiece != null) {
            g.setColor(GColor.GREEN);
            int x = selectedPiece[1]* cw + cw /2;
            int y = selectedPiece[0]* ch + ch /2;
            g.drawFilledCircle(x, y, dim+5);
        }

        for (int r=0; r<ranks; r++) {
            for (int c=0; c<cols; c++) {
                Piece p = getPiece(r, c);
                if (p == null)
                    continue;
                int x = c * cw + cw / 2;
                int y = r * ch + ch / 2;
                if (Utils.isPointInsideRect(mx, my, c* cw, r* ch, cw, ch)) {
                    g.setColor(GColor.CYAN);
                    g.drawRect(c* cw +1, r* ch +1, cw -2, ch -2);
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

                g.pushMatrix();
                g.translate(x, y);
                drawPiece(g, p, dim*2, dim*2);
                g.popMatrix();
            }
        }

        for (Move m : pickableMoves) {
            if (m.getMoveType() == MoveType.END && selectedPiece != null) {
                if (highlightedCol == selectedPiece[1] && highlightedRank == selectedPiece[0]) {
                    g.setColor(GColor.YELLOW);
                } else {
                    g.setColor(GColor.GREEN);
                }
                int x = selectedPiece[1]* cw + cw /2;
                int y = selectedPiece[0]* ch + ch /2;
                g.setTextHeight(dim/2);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, "END");
            }
        }

        if (isGameOver()) {
            int x = g.getViewportWidth()/2;
            int y = g.getViewportHeight()/2;
            String txt = "G A M E   O V E R\nPlayer " + getWinner().getColor() + " Wins!";
            g.setColor(GColor.CYAN);
            g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, txt);
        }
    }

    @Omit
    private boolean clicked = false;

    public void doClick() {
        clicked = true;
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
    }

    public final boolean isGameOver() {
        return rules.computeMoves(this, false) == 0;
    }

    Piece choosePieceToMove(List<Piece> pieces) {
        pickableMoves.clear();
        pickablePieces.clear();
        pickablePieces.addAll(pieces);
        repaint();
        Utils.waitNoThrow(RUNGAME_MONITOR, -1);
        if (clicked) {
            clicked = false;
            for (Piece p : pieces) {
                if (p.getRank() == highlightedRank && p.getCol() == highlightedCol) {
                    return p;
                }
            }
        }
        return null;
    }

    Move chooseMoveForPiece(List<Move> moves) {
        pickableMoves.clear();
        pickablePieces.clear();
        pickableMoves.addAll(moves);
        repaint();
        Utils.waitNoThrow(RUNGAME_MONITOR, -1);
        if (clicked) {
            clicked = false;
            for (Move m : moves) {
                if (m.getMoveType() == MoveType.END && selectedPiece[0] == highlightedRank && selectedPiece[1] == highlightedCol) {
                    return m;
                }
                if (m.getEnd() != null && m.getEnd()[0] == highlightedRank && m.getEnd()[1] == highlightedCol) {
                    return m;
                }
            }
        }
        return null;
    }

    private void drawPiece(AGraphics g, Piece p, int w, int h) {
        if (p == null || p.getPlayerNum() < 0)
            return;
        g.pushMatrix();
        switch (p.getType()) {
            case UNAVAILABLE:
            case EMPTY:
                break;
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                drawPiece(g, PieceType.PAWN, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
            case BISHOP:
            case KNIGHT:
            case QUEEN:
                drawPiece(g, p.getType(), p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
            case ROOK:
            case ROOK_IDLE:
                drawPiece(g, PieceType.ROOK, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                drawPiece(g, PieceType.CHECKER, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                g.translate(0, h/5);
                drawPiece(g, PieceType.CHECKER, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
            case DAMA_MAN:
            case CHECKER:
                drawPiece(g, PieceType.CHECKER, p.getPlayerNum(), getPlayer(p.getPlayerNum()).color, w, h);
                break;
        }
        g.popMatrix();
    }

    public void drawPiece(AGraphics g, PieceType p, int playerNum, Color color, int w, int h) {
        int id = getPieceImageId(p, color);
        if (id >= 0) {
            g.drawImage(id, -w/2, -h/2, w, h);
        } else {
            g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, color.name() + "\n" + p.abbrev);
        }
    }

    /**
     * Return < 0 for no image or > 0 for a valid image id.
     * @param p
     * @param color
     * @return
     */
    public abstract int getPieceImageId(PieceType p, Color color);
}
