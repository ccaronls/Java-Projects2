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
import cc.lib.math.Vector2D;

public abstract class UIGame extends Game {

    static Logger log = LoggerFactory.getLogger(UIGame.class);

    private File saveFile;
    private boolean gameRunning = false;
    private boolean threadExitted = false;
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
            e.printStackTrace();
            setRules(new Chess());
            setPlayer(NEAR, new UIPlayer(UIPlayer.Type.USER));
            setPlayer(FAR, new UIPlayer(UIPlayer.Type.AI));
            newGame();
        }
        return false;
    }

    public final Move undoAndRefresh() {
        Move m = super.undo();
        selectedPiece = null;
        pickablePieces.clear();
        pickableMoves.clear();
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
        startGameThread();
        repaint();
        return m;
    }

    public abstract void repaint();

    @Omit
    private final Object EXIT_THREAD_MONITOR = new Object();

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        threadExitted = false;
        new Thread(() -> {
            Thread.currentThread().setName("runGame");
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
            threadExitted = true;
            synchronized (EXIT_THREAD_MONITOR) {
                EXIT_THREAD_MONITOR.notify();
            }
            log.debug("game thread stopped");
        }).start();
    }

    public synchronized void stopGameThread() {
        if (!threadExitted) {
            gameRunning = false;
            synchronized (RUNGAME_MONITOR) {
                RUNGAME_MONITOR.notifyAll();
            }
            for (int i=0; i<10 && !threadExitted; i++)
                Utils.waitNoThrow(EXIT_THREAD_MONITOR, 1000);
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
        drawCapturedPieces(g, WIDTH, HEIGHT/2-WIDTH/2, FAR, getPlayer(FAR).getCaptured());
        g.translate(0, HEIGHT/2+WIDTH/2);
        drawCapturedPieces(g, WIDTH, HEIGHT/2-WIDTH/2, NEAR, getPlayer(NEAR).getCaptured());
        if (getWinner() != null) {
            g.setColor(getWinner().getColor().color);
            g.drawJustifiedString(WIDTH - 10, 10, Justify.RIGHT, "Winner: " + getWinner().getColor());
        } else {
            g.drawJustifiedString(WIDTH - 10, 10, Justify.RIGHT, "Turn: " + getPlayer(getTurn()).getColor());
        }
    }

    private void drawLandscape(AGraphics g, int mx, int my) {
        drawBoard(g, HEIGHT, mx, my);
        g.pushMatrix();
        g.translate(HEIGHT, 0);
        drawPlayer((UIPlayer)getPlayer(FAR), g, WIDTH-HEIGHT, HEIGHT/2);
        g.translate(0, HEIGHT/2);
        drawPlayer((UIPlayer)getPlayer(NEAR), g, WIDTH-HEIGHT, HEIGHT/2);
        g.popMatrix();
        g.setColor(GColor.WHITE);
        if (getWinner() != null) {
            g.drawJustifiedString(WIDTH - 10, 10, Justify.RIGHT, "Winner: " + getWinner().getColor());
        } else {
            g.drawJustifiedString(WIDTH - 10, 10, Justify.RIGHT, "Turn: " + getPlayer(getTurn()).getColor());
        }
    }

    final int BORDER = 5;

    private void drawPlayer(UIPlayer player, AGraphics g, int width, int height) {
        if (player == null)
            return;
        g.pushMatrix();
        g.translate(BORDER, BORDER);
        width -= BORDER*2;
        height -= BORDER*2;

        // draw player info
        String info = player.getType().name();
        if (player.getThinkingTimeSecs() > 0) {
            int secs = player.getThinkingTimeSecs();
            int mins = secs / 60;
            secs -= mins * 60;
            info += String.format(" Thinking %d:%0d", mins, secs);
        }
        g.setColor(GColor.WHITE);
        float th = g.drawWrapString(0, 0, width, info).height;
        g.translate(0, th + BORDER);
        drawCapturedPieces(g, width, height, getOpponent(player.getPlayerNum()), player.getCaptured());
        g.popMatrix();
    }

    private void drawCapturedPieces(AGraphics g, int width, int height, int playerNum, List<PieceType> pieces) {
        if (pieces == null)
            return;
//        g.setClipRect(0, 0, width, height);
        float x = PIECE_RADIUS, y = PIECE_RADIUS;
        for (PieceType p :pieces) {
            g.pushMatrix();
            g.translate(x, y);
            drawPiece(g, p, playerNum, PIECE_RADIUS*2, PIECE_RADIUS*2);
            g.popMatrix();
            x += PIECE_RADIUS * 2;
            if (x >= width) {
                x = PIECE_RADIUS;
                y += PIECE_RADIUS*2;
            }
        }

//        g.clearClip();
    }

    protected abstract int getCheckerboardImageId();

    private void drawBoard(AGraphics g, float boarddim, int _mx, int _my) {

        g.pushMatrix();

        float cw = boarddim / getColumns();
        float ch = boarddim / getRanks();

        if (getRanks() == 8 && getColumns() == 8) {
            float boardImageDim = 545;
            float boardImageBorder = 24;
            float ratio = boardImageBorder / boardImageDim;

            g.drawImage(getCheckerboardImageId(), 0, 0, boarddim, boarddim);
            float t = ratio * boarddim;
            g.translate(t, t);
            boarddim -= t*2;
            cw = boarddim / getColumns();
            ch = boarddim / getRanks();

            //log.debug("draw ...");

        } else {
            g.setLineWidth(2);
            g.setColor(GColor.BLACK);
            for (int x=0; x<=getColumns(); x++) {
                g.drawLine(x* cw, 0, x* cw, boarddim);
            }
            for (int y=0; y<=getRanks(); y++) {
                g.drawLine(0, y* ch, boarddim, y* ch);
            }
        }

        PIECE_RADIUS = Math.min(cw, ch)/3;

        highlightedRank = highlightedCol = -1;
        int [] _selectedPiece = null;
        List<Piece> _pickablePieces = new ArrayList<>();
        List<Move> _pickableMoves = new ArrayList<>();
        final boolean isUser = isCurrentPlayerUser();

        if (isUser && !isGameOver()) {
            synchronized (this) {
                _selectedPiece = selectedPiece;
                _pickablePieces.addAll(pickablePieces);
                _pickableMoves.addAll(pickableMoves);
            }
        }

        if (_selectedPiece != null) {
            g.setColor(GColor.GREEN);
            float x = _selectedPiece[1]* cw + cw /2;
            float y = _selectedPiece[0]* ch + ch /2;
            g.drawFilledCircle(x, y, PIECE_RADIUS +5);
        }

        Vector2D mv = g.screenToViewport(_mx, _my);
        final float mx = mv.getX();
        final float my = mv.getY();

        for (int r=0; r<getRanks(); r++) {
            for (int c=0; c<getColumns(); c++) {
                Piece p = getPiece(r, c);
                float x = c * cw + cw / 2;
                float y = r * ch + ch / 2;
                if (isUser && Utils.isPointInsideRect(mx, my, c* cw, r* ch, cw, ch)) {
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
                if (p.getType() != PieceType.EMPTY && p.getPlayerNum() >= 0)
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

        if (isGameOver()) {
            if (getWinner() != null) {
                int x = g.getViewportWidth() / 2;
                int y = g.getViewportHeight() / 2;
                String txt = "G A M E   O V E R\nPlayer " + getWinner().getColor() + " Wins!";
                g.setColor(GColor.CYAN);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, txt);
            } else {
                int x = g.getViewportWidth() / 2;
                int y = g.getViewportHeight() / 2;
                String txt = "D R A W   G A M E";
                g.setColor(GColor.CYAN);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, txt);
            }
        }
        g.popMatrix();
    }

    public void doClick() {
        clicked = true;
        synchronized (RUNGAME_MONITOR) {
            RUNGAME_MONITOR.notifyAll();
        }
    }

    public boolean isCurrentPlayerUser() {
        return ((UIPlayer)getCurrentPlayer()).getType() == UIPlayer.Type.USER;
    }

    Piece choosePieceToMove(List<Piece> pieces) {
        //Utils.println("choosePieceToMove: " + pieces);
        clicked = false;
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickablePieces.addAll(pieces);
        }
        repaint();
        Utils.waitNoThrow(RUNGAME_MONITOR, -1);
        if (clicked) {
            for (Piece p : pieces) {
                if (p.getRank() == highlightedRank && p.getCol() == highlightedCol) {
                    return p;
                }
            }
        }
        return null;
    }

    Move chooseMoveForPiece(List<Move> moves) { clicked = false;
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickableMoves.addAll(moves);
        }
        repaint();
        Utils.waitNoThrow(RUNGAME_MONITOR, -1);
        if (clicked) {
            for (Move m : moves) {
                switch (m.getMoveType()) {
                    case END:
                        if (selectedPiece[0] == highlightedRank && selectedPiece[1] == highlightedCol) {
                            return m;
                        }
                        break;
                    case SLIDE:
                    case FLYING_JUMP:
                    case JUMP:
                    case CASTLE:
                    case STACK:
                        if (m.getEnd()[0] == highlightedRank && m.getEnd()[1] == highlightedCol) {
                            return m;
                        }
                        break;
                    case SWAP:
                        break;
                }

            }
        }
        return null;
    }

    private void drawPiece(AGraphics g, PieceType p, int playerNum, float w, float h) {
        if (playerNum < 0)
            return;
        Color color = getPlayer(playerNum).getColor();
        g.pushMatrix();
        switch (p) {
            case EMPTY:
                break;
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                drawPiece(g, PieceType.PAWN, color, w, h, null);
                break;
            case BISHOP:
            case KNIGHT:
            case QUEEN:
                drawPiece(g, p, color, w, h, null);
                break;
            case ROOK:
            case ROOK_IDLE:
                drawPiece(g, PieceType.ROOK, color, w, h, null);
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, color, w, h, GColor.RED);
                break;
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, color, w, h, null);
                break;
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                g.translate(0, -h/5);
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                break;
            case DAMA_MAN:
            case CHECKER:
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                break;
        }
        g.popMatrix();
    }

    public void drawPiece(AGraphics g, PieceType p, Color color, float w, float h, GColor outlineColor) {
        if (outlineColor != null) {
            g.setColor(outlineColor);
            g.drawCircleWithThickness(0, 0, w/2, 3);
        }
        int id = getPieceImageId(p, color);
        if (id >= 0) {
            g.drawImage(id, -w/2, -h/2, w, h);
        } else {
            g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, color.name() + "\n" + p.abbrev);
        }
    }

    @Override
    protected void onGameOver(Player winner) {
        super.onGameOver(winner);
    }

    @Override
    protected void onMoveChosen(Move m) {
        switch (m.getMoveType()) {

            case END:
                break;
            case SLIDE: {

                break;
            }
            case FLYING_JUMP:
                break;
            case JUMP:
                break;
            case STACK:
                break;
            case SWAP:
                break;
            case CASTLE:
                break;
        }
    }

    @Override
    protected void onPieceSelected(Piece p) {
        // TODO: Animation
        super.onPieceSelected(p);
    }

    /**
     * Return < 0 for no image or > 0 for a valid image id.
     * @param p
     * @param color
     * @return
     */
    public abstract int getPieceImageId(PieceType p, Color color);
}
