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

    private File saveFile;
    private boolean gameRunning = false;
    private final Object RUNGAME_MONITOR = new Object();
    private boolean clicked = false;

    public UIGame() {
    }

    public boolean init(File saveFile) {
        this.saveFile = saveFile;
        try {
            loadFromFile(saveFile);
            return true;
        } catch (Exception e) {
            setRules(new Chess());
            setPlayer(NEAR, new UIPlayer());
            setPlayer(FAR, new UIPlayer());
            newGame();
        }
        return false;
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

    public void startGameThread() {
        if (gameRunning)
            return;
        new Thread() {
            public void run() {
                while (gameRunning) {
                    log.debug("run game");
                    runGame();
                    if (gameRunning) {
                        if (getWinner() != null)
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

    int WIDTH, HEIGHT;
    float PIECE_RADIUS;

    public final void draw(AGraphics g, int mx, int my) {
        g.clearScreen(GColor.GRAY);
        if (!isInitialized())
            return;

        WIDTH = g.getViewportWidth();
        HEIGHT = g.getViewportHeight();

        if (WIDTH > HEIGHT) {
            // landscape draws board on the left and captured pieces on the right
            drawLandscape(g, mx, my);
        } else {
            // portrait draws board in center and captured pieces in front of each player
            drawPortrait(g, mx, my);
        }
    }

    private void drawPortrait(AGraphics g, int mx, int my) {
        g.pushMatrix();
        g.translate(0, HEIGHT/2-WIDTH/2);
        drawBoard(g, WIDTH, mx, my);
        g.popMatrix();
        drawCapturedPieces(g, WIDTH, HEIGHT/2-WIDTH/2, FAR, getPlayer(FAR).captured);
        g.translate(0, HEIGHT/2+WIDTH/2);
        drawCapturedPieces(g, WIDTH, HEIGHT/2-WIDTH/2, NEAR, getPlayer(NEAR).captured);
    }

    private void drawLandscape(AGraphics g, int mx, int my) {
        drawBoard(g, HEIGHT, mx, my);
        drawCapturedPieces(g, WIDTH-HEIGHT, HEIGHT/2, FAR, getPlayer(FAR).captured);
        g.pushMatrix();
        g.translate(HEIGHT, HEIGHT/2+WIDTH/2);
        drawCapturedPieces(g, WIDTH, HEIGHT/2-WIDTH/2, NEAR, getPlayer(NEAR).captured);
        g.popMatrix();
    }

    int BORDER = 5;

    private void drawCapturedPieces(AGraphics g, int width, int height, int playerNum, List<PieceType> pieces) {
        g.setClipRect(0, 0, width, height);
        g.pushMatrix();
        g.translate(BORDER, BORDER);
        width -= BORDER*2;
        int x = 0, y = 0;
        for (PieceType p :pieces) {
            g.pushMatrix();
            g.translate(x, y);
            drawPiece(g, p, playerNum, PIECE_RADIUS, PIECE_RADIUS);
            g.popMatrix();
            x += PIECE_RADIUS * 2 / 3;
            if (x >= width) {
                x = 0;
                y += PIECE_RADIUS;
            }
        }

        g.popMatrix();
        g.clearClip();
    }

    protected abstract int getCheckerboardImageId();

    private void drawBoard(AGraphics g, float boarddim, int mx, int my) {

        g.pushMatrix();

        float boardImageDim = 545;
        float boardImageBorder = 24;
        float ratio = boardImageBorder / boardImageDim;

        g.drawImage(getCheckerboardImageId(), 0, 0, boarddim, boarddim);
        float t = ratio * boarddim;
        g.translate(t, t);
        boarddim -= t*2;

        //log.debug("draw ...");
        float cw = boarddim / getColumns();
        float ch = boarddim / getRanks();

        PIECE_RADIUS = Math.min(cw, ch)/3;
        highlightedRank = highlightedCol = -1;
/*
        g.setColor(GColor.BLACK);
        for (int x=0; x<=getColumns(); x++) {
            g.drawLine(x* cw, 0, x* cw, g.getViewportHeight());
        }
        for (int y=0; y<=getRanks(); y++) {
            g.drawLine(0, y* ch, g.getViewportWidth(), y* ch);
        }
*/
        int [] _selectedPiece;
        List<Piece> _pickablePieces = new ArrayList<>();
        List<Move> _pickableMoves = new ArrayList<>();

        synchronized (this) {
            _selectedPiece = selectedPiece;
            _pickablePieces.addAll(pickablePieces);
            _pickableMoves.addAll(pickableMoves);
        }

        if (_selectedPiece != null) {
            g.setColor(GColor.GREEN);
            float x = _selectedPiece[1]* cw + cw /2;
            float y = _selectedPiece[0]* ch + ch /2;
            g.drawFilledCircle(x, y, PIECE_RADIUS +5);
        }

        for (int r=0; r<getRanks(); r++) {
            for (int c=0; c<getColumns(); c++) {
                Piece p = getPiece(r, c);
                float x = c * cw + cw / 2;
                float y = r * ch + ch / 2;
                if (Utils.isPointInsideRect(mx, my, c* cw, r* ch, cw, ch)) {
                    g.setColor(GColor.CYAN);
                    g.drawRect(c* cw +1, r* ch +1, cw -2, ch -2);
                    highlightedRank = r;
                    highlightedCol = c;
                }

                for (Piece pp : _pickablePieces) {
                    if (pp.getRank() == r && pp.getCol() == c) {
                        g.setColor(GColor.CYAN);
                        g.drawFilledCircle(x, y, PIECE_RADIUS +5);
                        break;
                    }
                }

                for (Move m : _pickableMoves) {
                    if (m.getEnd() != null && m.getEnd()[0] == r && m.getEnd()[1] == c) {
                        g.setColor(GColor.CYAN);
                        g.drawCircle(x, y, PIECE_RADIUS);
                        break;
                    }
                }

                g.pushMatrix();
                g.translate(x, y);
                if (p.getType() != PieceType.EMPTY)
                    drawPiece(g, p.getType(), p.getPlayerNum(), PIECE_RADIUS *2, PIECE_RADIUS *2);
                g.popMatrix();
            }
        }

        for (Move m : _pickableMoves) {
            if (m.getMoveType() == MoveType.END && _selectedPiece != null) {
                if (highlightedCol == _selectedPiece[1] && highlightedRank == _selectedPiece[0]) {
                    g.setColor(GColor.YELLOW);
                } else {
                    g.setColor(GColor.GREEN);
                }
                float x = _selectedPiece[1]* cw + cw /2;
                float y = _selectedPiece[0]* ch + ch /2;
                g.setTextHeight(PIECE_RADIUS/2);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, "END");
            }
        }

        if (getWinner() != null) {
            int x = g.getViewportWidth()/2;
            int y = g.getViewportHeight()/2;
            String txt = "G A M E   O V E R\nPlayer " + getWinner().getColor() + " Wins!";
            g.setColor(GColor.CYAN);
            g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, txt);
        }
        g.popMatrix();
    }

    public void doClick() {
        clicked = true;
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
    }

    Piece choosePieceToMove(List<Piece> pieces) {
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickablePieces.addAll(pieces);
        }
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
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickableMoves.addAll(moves);
        }
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

    private void drawPiece(AGraphics g, PieceType p, int playerNum, float w, float h) {
        Color color = getPlayer(playerNum).getColor();
        g.pushMatrix();
        switch (p) {
            case EMPTY:
                break;
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                drawPiece(g, PieceType.PAWN, color, w, h);
                break;
            case BISHOP:
            case KNIGHT:
            case QUEEN:
                drawPiece(g, p, color, w, h);
                break;
            case ROOK:
            case ROOK_IDLE:
                drawPiece(g, PieceType.ROOK, color, w, h);
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, color, w, h);
                break;
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                drawPiece(g, PieceType.CHECKER, color, w, h);
                g.translate(0, h/5);
                drawPiece(g, PieceType.CHECKER, color, w, h);
                break;
            case DAMA_MAN:
            case CHECKER:
                drawPiece(g, PieceType.CHECKER, color, w, h);
                break;
        }
        g.popMatrix();
    }

    public void drawPiece(AGraphics g, PieceType p, Color color, float w, float h) {
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
