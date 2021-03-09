package cc.lib.checkerboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;

public abstract class UIGame extends Game {

    static Logger log = LoggerFactory.getLogger(UIGame.class);

    private File saveFile;
    private boolean gameRunning = false;
    private final Object RUNGAME_MONITOR = new Object();
    private boolean clicked = false;

    int highlightedRank, highlightedCol;
    final List<Piece> pickablePieces = new ArrayList<>();
    final List<Move>  pickableMoves  = new ArrayList<>();

    float SQ_DIM, PIECE_RADIUS, BORDER_WIDTH;
    GDimension SCREEN_DIM, BOARD_DIM;

    public UIGame() {
    }

    public boolean init(File saveFile) {
        this.saveFile = saveFile;
        try {
            loadFromFile(saveFile);
            countPieceMoves();
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

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
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
            log.debug("game thread stopped");
        }).start();
    }

    public synchronized void stopGameThread() {
        if (gameRunning) {
            gameRunning = false;
            synchronized (RUNGAME_MONITOR) {
                RUNGAME_MONITOR.notifyAll();
            }
        }
    }

    public final void draw(AGraphics g, int mx, int my) {
        g.clearScreen(GColor.GRAY);
        if (!isInitialized())
            return;

        SCREEN_DIM = new GDimension(g.getViewportWidth(), g.getViewportHeight());
        SQ_DIM = Math.min(SCREEN_DIM.getWidth(), SCREEN_DIM.getHeight()) / Math.min(getRanks(), getColumns());
        BOARD_DIM = new GDimension(SQ_DIM * getColumns(), SQ_DIM * getRanks());

        if (false && SCREEN_DIM.getAspect() > 1) {
            // landscape draws board on the left and captured pieces on the right
            drawLandscape(g, mx, my);
        } else {
            // portrait draws board in center and captured pieces in front of each player
            drawPortrait(g, mx, my);
        }
    }

    private void drawPortrait(AGraphics g, int mx, int my) {
        g.pushMatrix();

        final float infoHgt = SCREEN_DIM.getHeight()/2-BOARD_DIM.getHeight()/2;

        GDimension infoDim = new GDimension(SCREEN_DIM.getWidth(), infoHgt);
        drawCapturedPieces(g, infoDim, FAR, getCapturedPieces(FAR));
        g.translate(0, infoHgt);
        drawBoard(g, mx, my);
        g.translate(0, BOARD_DIM.getHeight());
        drawCapturedPieces(g, infoDim, NEAR, getCapturedPieces(NEAR));
        if (getWinner() != null) {
            g.setColor(getWinner().getColor().color);
            g.drawJustifiedString(SCREEN_DIM.getWidth() - 10, 10, Justify.RIGHT, "Winner: " + getWinner().getColor());
        } else {
            g.drawJustifiedString(SCREEN_DIM.getWidth() - 10, 10, Justify.RIGHT, "Turn: " + getPlayer(getTurn()).getColor());
        }

        g.popMatrix();
    }

    private void drawLandscape(AGraphics g, int mx, int my) {
        /*
        final float infoHgt = HEIGHT/2-boardDim.getHeight()/2;

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
        }*/
    }

    private void drawPlayer(UIPlayer player, AGraphics g, float width, float height) {
        if (player == null)
            return;
        int BORDER = 5;
        g.pushMatrix();
        g.translate(BORDER, BORDER);
        width -= BORDER*2;
        height -= BORDER*2;

        // draw player info
        String info = player.getType().name();
        if (player.isThinking()) {
            int secs = player.getThinkingTimeSecs();
            int mins = secs / 60;
            secs -= mins * 60;
            info += String.format(" Thinking %d:%0$2d", mins, secs);
        }
        g.setColor(GColor.WHITE);
        float th = g.drawWrapString(0, 0, width, info).height;
        g.translate(0, th + BORDER);
        //drawCapturedPieces(g, width, height, getOpponent(player.getPlayerNum()), getCapturedPieces(player.getPlayerNum()));
        g.popMatrix();
    }

    private void drawCapturedPieces(AGraphics g, GDimension dim, int playerNum, List<PieceType> pieces) {
        if (pieces == null)
            return;
//        g.setClipRect(0, 0, width, height);
        float x = PIECE_RADIUS, y = PIECE_RADIUS;
        for (PieceType p :pieces) {
            g.pushMatrix();
            g.translate(x, y);
            Color color = getPlayer(playerNum).getColor();
            drawPiece(g, p, color, PIECE_RADIUS*2, PIECE_RADIUS*2, null);
            g.popMatrix();
            x += PIECE_RADIUS * 2;
            if (x >= dim.getWidth()) {
                x = PIECE_RADIUS;
                y += PIECE_RADIUS*2;
            }
        }

//        g.clearClip();
    }

    private void drawCheckerboardImage(AGraphics g, int id, float boardImageDim, float boardImageBorder) {
        BORDER_WIDTH = boardImageBorder;
        float boardWidth = BOARD_DIM.getWidth() - 2*boardImageBorder;
        float boardHeight = BOARD_DIM.getHeight() - 2*boardImageBorder;
        SQ_DIM = boardWidth / getColumns();
        g.drawImage(id, 0, 0, BOARD_DIM.getWidth(), BOARD_DIM.getHeight());
        g.translate(BORDER_WIDTH, BORDER_WIDTH);
    }

    protected abstract int getCheckerboardImageId();

    private void drawCheckerboard8x8(AGraphics g) {
        drawCheckerboardImage(g, getCheckerboardImageId(), 545, 24);
    }

    private void drawCheckboardBoard(AGraphics g, GColor dark, GColor light) {
        GColor [] color = {
                dark, light
        };

        int colorIdx = 0;

        g.pushMatrix();
        for (int i=0; i<getRanks(); i++) {
            g.pushMatrix();
            for (int ii=0; ii<getColumns(); ii++) {
                g.setColor(color[colorIdx]);
                colorIdx = (colorIdx+1) % 2;
                if (isOnBoard(i, ii))
                    g.drawFilledRect(0, 0, SQ_DIM, SQ_DIM);
                g.translate(SQ_DIM, 0);
            }
            g.popMatrix();
            g.translate(0, SQ_DIM);
            colorIdx = (colorIdx+1) % 2;
        }
        g.popMatrix();
    }

    protected abstract int getKingsCourtBoardId();

    private void drawKingsCourtBoard(AGraphics g) {
        drawCheckerboardImage(g, getKingsCourtBoardId(), 206, 16);
    }

    private final GColor DAMA_BACKGROUND_COLOR = new GColor(0xfffde9a9);

    private void drawDamaBoard(AGraphics g) {
        g.setColor(DAMA_BACKGROUND_COLOR);
        g.drawFilledRect(0, 0, BOARD_DIM.getWidth(), BOARD_DIM.getHeight());
        g.setColor(GColor.BLACK);
        for (int i=0; i<=getRanks(); i++) {
            g.drawLine(i*SQ_DIM, 0, i*SQ_DIM, BOARD_DIM.getHeight(), 3);
        }
        for (int i=0; i<=getColumns(); i++) {
            g.drawLine(0, i*SQ_DIM, BOARD_DIM.getWidth(), i*SQ_DIM, 3);
        }
    }

    private void drawBoard(AGraphics g, int _mx, int _my) {

        g.pushMatrix();

        if (getRules() instanceof KingsCourt) {
            drawKingsCourtBoard(g);
        } else if (getRules() instanceof Dama) {
            drawDamaBoard(g);
        } else if (getRanks() == 8 && getColumns() == 8) {
            drawCheckerboard8x8(g);
        } else {
            drawCheckboardBoard(g, GColor.BLACK, GColor.LIGHT_GRAY);
        }

        float cw = SQ_DIM;
        float ch = SQ_DIM;

        PIECE_RADIUS = SQ_DIM/3;

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

        for (Piece p : getPieces()) {
            int r = p.getRank();
            int c = p.getCol();
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
                drawPiece(g, p, PIECE_RADIUS *2, PIECE_RADIUS *2);
            g.popMatrix();
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
                String txt = "G A M E   O V E R\n" + getWinner().getColor() + " Wins!";
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(x, y, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 3);
            } else {
                int x = g.getViewportWidth() / 2;
                int y = g.getViewportHeight() / 2;
                String txt = "D R A W   G A M E";
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(x, y, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 3);
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
        if (!gameRunning)
            return null;
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

    private void drawPiece(AGraphics g, Piece pc, float w, float h) {
        if (pc.getPlayerNum() < 0)
            return;
        Color color = getPlayer(pc.getPlayerNum()).getColor();
        g.pushMatrix();
        switch (pc.getType()) {
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
                drawPiece(g, pc.getType(), color, w, h, null);
                break;
            case ROOK:
            case ROOK_IDLE:
            case DRAGON:
            case DRAGON_IDLE:
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
                if (pc.isStacked()) {
                    for (int s=pc.getStackSize()-1; s>=0; s--) {
                        drawPiece(g, PieceType.CHECKER, getPlayer(pc.getStackAt(s)).color, w, h, null);
                        g.translate(0, -h/5);
                    }
                }
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                g.translate(0, -h/5);
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                break;
            case CHIP_4WAY:
            case DAMA_MAN:
            case CHECKER:
                if (pc.isStacked()) {
                    for (int s=pc.getStackSize()-1; s>=0; s--) {
                        drawPiece(g, PieceType.CHECKER, getPlayer(pc.getStackAt(s)).color, w, h, null);
                        g.translate(0, -h/5);
                    }
                }
                drawPiece(g, PieceType.CHECKER, color, w, h, null);
                break;
        }
        g.popMatrix();
    }

    public void drawPiece(AGraphics g, PieceType p, Color color, float w, float h, GColor outlineColor) {
        if (outlineColor != null) {
            g.setColor(outlineColor);
            g.drawCircle(0, 0, w/2, 3);
        }
        int id = getPieceImageId(p, color);
        if (id >= 0) {
            g.drawImage(id, -w/2, -h/2, w, h);
        } else {
            //g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, color.name() + "\n" + p.abbrev);
            g.setColor(color.color);
            g.drawFilledCircle(0, 0, w/2);
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
