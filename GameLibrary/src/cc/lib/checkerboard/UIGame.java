package cc.lib.checkerboard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.utils.Lock;
import cc.lib.utils.Table;

public abstract class UIGame extends Game {

    static Logger log = LoggerFactory.getLogger(UIGame.class);

    private final static boolean DEBUG = false;

    File saveFile;
    boolean gameRunning = false;
    final Lock runLock = new Lock();
    final Lock animLock = new Lock();

    final static int MODE_CLICK = 1;
    final static int MODE_DRAG_START = 2;
    final static int MODE_DRAG_STOP = 3;

    int mode = 0;
    int draggingPos = -1;
    int highlightedPos = -1;
    final List<Piece> pickablePieces = new ArrayList<>();
    final List<Move>  pickableMoves  = new ArrayList<>();
    boolean showInstructions = true;

    float SQ_DIM, PIECE_RADIUS, BORDER_WIDTH;
    GDimension SCREEN_DIM, BOARD_DIM;

    final GColor DAMA_BACKGROUND_COLOR = new GColor(0xfffde9a9);
    final GColor BORDER_COLOR = new GColor(0xffd2b48c);

    //final List<PieceAnim> animations = Collections.synchronizedList(new ArrayList<>());
    final Map<Integer, PieceAnim> animations = Collections.synchronizedMap(new HashMap<>());

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

    @Override
    public void newGame() {
        super.newGame();
        pickablePieces.clear();
        pickableMoves.clear();
        animations.clear();
        mode = 0;
        draggingPos = -1;
        highlightedPos = -1;
        showInstructions = true;
    }

    public final Move undoAndRefresh() {
        Move m = super.undo();
        pickablePieces.clear();
        pickableMoves.clear();
        runLock.release();
        repaint(0);
        return m;
    }

    public abstract void repaint(long delayMs);

    public boolean isGameRunning() {
        return gameRunning;
    }

    public synchronized void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        new Thread(() -> {
            Thread.currentThread().setName("runGame");
            try {
                while (gameRunning) {
                    log.debug("run game");
                    runGame();
                    if (gameRunning) {
                        if (isGameOver())
                            break;
                        trySaveToFile(saveFile);
                        repaint(0);
                    } else {
                        log.debug("leaving");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            repaint(0);
            gameRunning = false;
            log.debug("game thread stopped");
        }).start();
    }

    public synchronized void stopGameThread() {
        if (gameRunning) {
            AIPlayer.cancel();
            gameRunning = false;
            runLock.reset();
        }
    }

    public final void draw(AGraphics g, int mx, int my) {
        g.clearScreen(GColor.GRAY);
        if (!isInitialized())
            return;

        SCREEN_DIM = new GDimension(g.getViewportWidth(), g.getViewportHeight());

        if (getRules() instanceof DragonChess) {
            // always landscape using the portion of the screen not used by the board
            SQ_DIM = Math.min(SCREEN_DIM.getWidth(), SCREEN_DIM.getHeight()) / Math.min(getRanks()+1, getColumns());
            BOARD_DIM = new GDimension(SQ_DIM * getColumns(), SQ_DIM * getRanks());

            g.pushMatrix();
            g.translate(0, SQ_DIM);
            drawDragonChess(g, mx, my);
            g.popMatrix();
        } else if (SCREEN_DIM.getAspect() > 1) {
            // landscape draws board on the left and captured pieces on the right
            SQ_DIM = Math.min(SCREEN_DIM.getWidth(), SCREEN_DIM.getHeight()) / Math.min(getRanks(), getColumns());
            BOARD_DIM = new GDimension(SQ_DIM * getColumns(), SQ_DIM * getRanks());
            drawLandscape(g, mx, my);
        } else {
            // portrait draws board in center and captured pieces in front of each player
            SQ_DIM = Math.min(SCREEN_DIM.getWidth(), SCREEN_DIM.getHeight()) / Math.min(getRanks(), getColumns());
            BOARD_DIM = new GDimension(SQ_DIM * getColumns(), SQ_DIM * getRanks());
            drawPortrait(g, mx, my);
        }
    }

    private void drawPortrait(AGraphics g, int mx, int my) {
        g.pushMatrix();

        final float infoHgt = SCREEN_DIM.getHeight()/2-BOARD_DIM.getHeight()/2;

        GDimension infoDim = new GDimension(SCREEN_DIM.getWidth(), infoHgt);
        drawPlayer((UIPlayer)getPlayer(FAR), g, infoDim);
        g.translate(0, infoHgt);
        drawBoard(g, mx, my);
        g.translate(0, BOARD_DIM.getHeight());
        drawPlayer((UIPlayer)getPlayer(NEAR), g, infoDim);

        g.popMatrix();
    }

    private void drawDragonChess(AGraphics g, int mx, int my) {
        GDimension info = new GDimension(SQ_DIM * 3 + SCREEN_DIM.getWidth() - BOARD_DIM.getWidth(), SQ_DIM * 3);

        drawBoard(g, mx, my);
        g.pushMatrix();
        g.translate(BOARD_DIM.getWidth() - SQ_DIM*3, 0);
        drawPlayer((UIPlayer)getPlayer(FAR), g, info);
        g.translate(0, info.getHeight() + SQ_DIM * 4);
        drawPlayer((UIPlayer)getPlayer(NEAR), g, info);
        g.popMatrix();

    }

    private void drawLandscape(AGraphics g, int mx, int my) {

        GDimension info = new GDimension(SCREEN_DIM.getWidth() - BOARD_DIM.getWidth(), SCREEN_DIM.getHeight()/2);

        drawBoard(g, mx, my);
        g.pushMatrix();
        g.translate(BOARD_DIM.getWidth(), 0);
        drawPlayer((UIPlayer)getPlayer(FAR), g, info);
        g.translate(0, info.getHeight());
        drawPlayer((UIPlayer)getPlayer(NEAR), g, info);
        g.popMatrix();
    }

    private void drawPlayer(UIPlayer player, AGraphics g, GDimension info) {
        if (player == null)
            return;
        int BORDER = 5;
        if (player.getPlayerNum() == getTurn()) {
            g.setColor(GColor.CYAN);
            g.drawRect(info, 3);
        }

        g.pushMatrix();
        g.translate(BORDER, BORDER);

        info = info.adjustedBy(-BORDER*2, -BORDER*2);

        drawCapturedPieces(g, info, getOpponent(player.getPlayerNum()), getCapturedPieces(player.getPlayerNum()));

        // draw player info
        String txt = player.getType().name();
        if (player.getType() == UIPlayer.Type.AI) {
            txt += " (";
            switch (player.getMaxSearchDepth()) {
                case 1:
                    txt += "Easy"; break;
                case 2:
                    txt += "Medium"; break;
                case 3:
                    txt += "Hard"; break;
                default:
                    txt += "MaxDepth: " + player.getMaxSearchDepth();
                    break;
            }
            txt += ")";
        }
        if (player.isThinking()) {
            int secs = player.getThinkingTimeSecs();
            int mins = secs / 60;
            secs -= mins * 60;
            txt += String.format("\nThinking %d:%02d", mins, secs);
            repaint(1000);
        }
        g.setColor(GColor.WHITE);
        g.drawJustifiedString(info.getWidth(), info.getHeight(), Justify.RIGHT, Justify.BOTTOM, txt);
        g.popMatrix();
    }

    private void drawCapturedPieces(AGraphics g, GDimension dim, int playerNum, List<PieceType> pieces) {
        if (getRules() instanceof Columns)
            return;
        if (pieces == null)
            return;
//        g.setClipRect(0, 0, width, height);
        float x = PIECE_RADIUS, y = PIECE_RADIUS*3;
        for (PieceType pt :pieces) {
            g.pushMatrix();
            g.translate(x, y);
            Color color = getPlayer(playerNum).getColor();
            drawPiece(g, pt, playerNum);//.getDisplayType(), color, PIECE_RADIUS*2, PIECE_RADIUS*2, null);
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

        BORDER_WIDTH = boardImageBorder * BOARD_DIM.getWidth() / boardImageDim;
        float boardWidth = BOARD_DIM.getWidth() - 2*BORDER_WIDTH;
        float boardHeight = BOARD_DIM.getHeight() - 2*BORDER_WIDTH;
        SQ_DIM = boardWidth / getColumns();
        g.drawImage(id, 0, 0, BOARD_DIM.getWidth(), BOARD_DIM.getHeight());
        g.translate(BORDER_WIDTH, BORDER_WIDTH);

        if (DEBUG) {
            g.setColor(GColor.RED);
            g.drawRect(0, 0, boardWidth, boardHeight);
        }

    }

    protected abstract int getCheckerboardImageId();

    private void drawCheckerboard8x8(AGraphics g) {
        drawCheckerboardImage(g, getCheckerboardImageId(), 545, 26);
    }

    private void drawCheckboardBoard(AGraphics g, GColor dark, GColor light) {
        GColor [] color = {
                dark, light
        };

        int colorIdx = 0;
        BORDER_WIDTH = SQ_DIM/2;
        float boardWidth = BOARD_DIM.getWidth() - 2*BORDER_WIDTH;
        float boardHeight = BOARD_DIM.getHeight() - 2*BORDER_WIDTH;
        SQ_DIM = boardWidth / getColumns();

        g.setColor(BORDER_COLOR);
        g.drawFilledRoundedRect(0, 0, BOARD_DIM.getWidth(), BOARD_DIM.getHeight(), BORDER_WIDTH/2);
        g.translate(BORDER_WIDTH, BORDER_WIDTH);
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

        if (DEBUG) {
            g.setColor(GColor.RED);
            g.drawRect(0, 0, boardWidth, boardHeight);
        }
        g.popMatrix();
    }

    protected abstract int getKingsCourtBoardId();

    private void drawKingsCourtBoard(AGraphics g) {
        drawCheckerboardImage(g, getKingsCourtBoardId(), 206, 7);
    }

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

    synchronized private void drawBoard(AGraphics g, int _mx, int _my) {

        log.verbose("drawBoard " + _mx + " x " + _my + " highlighted=" + highlightedPos + " selected=" + getSelectedPiece());
        g.pushMatrix();

        if (getRules() instanceof KingsCourt && getKingsCourtBoardId() != 0) {
            drawKingsCourtBoard(g);
        } else if (getRules() instanceof Dama) {
            drawDamaBoard(g);
        } else if (getRanks() == 8 && getColumns() == 8 && getCheckerboardImageId() != 0) {
            drawCheckerboard8x8(g);
        } else {
            drawCheckboardBoard(g, GColor.BLACK, GColor.LIGHT_GRAY);
        }

        float cw = SQ_DIM;
        float ch = SQ_DIM;

        PIECE_RADIUS = SQ_DIM/3;

        highlightedPos = -1;
        Piece _selectedPiece = null;
        List<Piece> _pickablePieces = new ArrayList<>();
        List<Move> _pickableMoves = new ArrayList<>();
        final boolean isUser = isCurrentPlayerUser();

        if (isUser && !isGameOver()) {
            synchronized (this) {
                _selectedPiece = getSelectedPiece();
                _pickablePieces.addAll(pickablePieces);
                _pickableMoves.addAll(pickableMoves);
            }
        }

        if (_selectedPiece != null && mode == 0 && animations.size() == 0) {
            g.setColor(GColor.GREEN);
            float x = _selectedPiece.getCol() * cw + cw /2;
            float y = _selectedPiece.getRank() * ch + ch /2;
            g.drawFilledCircle(x, y, PIECE_RADIUS +5);
        }

        Vector2D mv = g.screenToViewport(_mx, _my);
        final float mx = mv.getX();
        final float my = mv.getY();

        for (int r = 0; r <getRanks(); r++) {
            for (int c = 0; c <getColumns(); c++) {
                float x = c * cw + cw / 2;
                float y = r * ch + ch / 2;
                if (Utils.isPointInsideRect(mx, my, c* cw, r* ch, cw, ch)) {
                    if (isUser) {
                        g.setColor(GColor.CYAN);
                        g.drawRect(c * cw + 1, r * ch + 1, cw - 2, ch - 2);
                        highlightedPos = r << 8 | c;
                    }
                    if (DEBUG) {
                        g.setColor(GColor.YELLOW);
                        g.drawJustifiedStringOnBackground(mv, Justify.CENTER, Justify.CENTER, String.format("%d,%d\n%s", r, c, getPiece(r, c).getType()), GColor.BLACK, 5, 0);
                    }
                }
            }
        }

        for (Piece p : getPieces(-1)) {
            if (animations.containsKey(p.getPosition()))
                continue;
            int r = p.getRank();
            int c = p.getCol();
            float x = c * cw + cw / 2;
            float y = r * ch + ch / 2;
            if (mode == 0) {
                for (Piece pp : _pickablePieces) {
                    if (pp.getRank() == r && pp.getCol() == c) {
                        g.setColor(GColor.CYAN);
                        g.drawFilledCircle(x, y, PIECE_RADIUS + 5);
                        break;
                    }
                }
            }

            g.pushMatrix();
            if (p.isCaptured()) {
                g.setColor(GColor.RED);
                g.drawFilledCircle(x, y, PIECE_RADIUS +5);
            }
            if (p.getPosition() == draggingPos) {
                g.translate(mx, my);
            } else {
                g.translate(x, y);
            }
            drawPiece(g, p);
            g.popMatrix();
        }

        for (Move m : _pickableMoves) {
            if (m.getMoveType() == MoveType.END && _selectedPiece != null) {
                if (highlightedPos == _selectedPiece.getPosition()) {
                    g.setColor(GColor.YELLOW);
                } else {
                    g.setColor(GColor.GREEN);
                }
                float x = _selectedPiece.getCol() * cw + cw / 2;
                float y = _selectedPiece.getRank() * ch + ch / 2;
                g.setTextHeight(PIECE_RADIUS / 2);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, "END");
            } else if (m.getEnd() >= 0) {
                g.setColor(GColor.CYAN);
                float x = (m.getEnd() & 0xff) * cw + cw / 2;
                float y = (m.getEnd() >> 8) * ch + ch / 2;
                g.drawCircle(x, y, PIECE_RADIUS);
            }
        }

        synchronized (animations) {
            Iterator<Map.Entry<Integer, PieceAnim>> it = animations.entrySet().iterator();
            while (it.hasNext()) {
                PieceAnim a = it.next().getValue();
                if (a.isDone()) {
                    it.remove();
                } else {
                    a.update(g);
                    repaint(0);
                }
            }
        }

        g.popMatrix();

        float cx = BOARD_DIM.getWidth() / 2;
        float cy = BOARD_DIM.getHeight() / 2;
        if (isGameOver()) {
            if (getWinner() != null) {
                String txt = "G A M E   O V E R\n" + getWinner().getColor() + " Wins!";
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 20);
            } else {
                String txt = "D R A W   G A M E";
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 20);
            }
        } else if (!gameRunning) {
            g.setColor(GColor.YELLOW);
            if (showInstructions) {
                Table tab = getRules().getInstructions();
                tab.setModel(instructionsModel);
                tab.draw(g, cx, cy, Justify.CENTER, Justify.CENTER);
                showInstructions = false;
            } else {
                String txt = "P A U S E D";
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 3);
            }
        }
    }

    final Table.Model instructionsModel = new Table.Model() {
        @Override
        public GColor getBorderColor(AGraphics g) {
            return GColor.WHITE;
        }

        @Override
        public GColor getBackgroundColor() {
            return GColor.BLACK;
        }
    };

    public void doClick() {
        log.verbose("do Click");
        mode = MODE_CLICK;
        runLock.release();
    }

    public void startDrag() {
        log.verbose("start drag");
        mode = MODE_DRAG_START;
        runLock.release();
    }

    public void stopDrag() {
        log.verbose("stop drag");
        mode = MODE_DRAG_STOP;
        draggingPos = -1;
        runLock.release();
    }

    public void setShowInstructions(boolean showInstructions) {
        this.showInstructions = showInstructions;
    }

    public boolean isCurrentPlayerUser() {
        return ((UIPlayer)getCurrentPlayer()).getType() == UIPlayer.Type.USER;
    }

    Piece choosePieceToMove(List<Piece> pieces) {
        if (!gameRunning)
            return null;
        //Utils.println("choosePieceToMove: " + pieces);
        mode = 0;
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickablePieces.addAll(pieces);
        }
        repaint(0);
        runLock.acquireAndBlock();
        switch (mode) {
            case MODE_CLICK: {
                mode = 0;
                draggingPos = -1;
                for (Piece p : pieces) {
                    if (p.getPosition() == highlightedPos) {
                        return p;
                    }
                }
                break;
            }
            case MODE_DRAG_START:
            {
                for (Piece p : pieces) {
                    if (p.getPosition() == highlightedPos) {
                        draggingPos = p.getPosition();
                        return p;
                    }
                }
                break;
            }
            case MODE_DRAG_STOP:
        }
        return null;
    }

    Move chooseMoveForPiece(List<Move> moves) {
        synchronized (this) {
            pickableMoves.clear();
            pickablePieces.clear();
            pickableMoves.addAll(moves);
        }
        repaint(0);
        runLock.acquireAndBlock();
        switch (mode) {
            case MODE_CLICK:
                mode = 0;
            case MODE_DRAG_STOP: {
                for (Move m : moves) {
                    switch (m.getMoveType()) {
                        case SWAP:
                        case END:
                            if (getSelectedPiece() != null && getSelectedPiece().getPosition() == highlightedPos) {
                                return m;
                            }
                            break;
                        case SLIDE:
                        case FLYING_JUMP:
                        case JUMP:
                        case CASTLE:
                        case STACK:
                            if (m.getEnd() == highlightedPos) {
                                return m;
                            }
                            break;
                    }

                }
                break;
            }
        }
        return null;
    }

    private void drawPiece(AGraphics g, Piece pc) {
        if (pc.getPlayerNum() < 0)
            return;

        g.pushMatrix();
        g.translate(0, PIECE_RADIUS);
        float d = PIECE_RADIUS * 2;
        if (pc.isStacked()) {
            for (int s=pc.getStackSize()-1; s>=0; s--) {
                drawPiece(g, PieceType.CHECKER, getPlayer(pc.getStackAt(s)).color, d, d, null);
                g.translate(0, -d/5);
            }
        } else {
            drawPiece(g, pc.getType(), pc.getPlayerNum());
        }
        g.popMatrix();
    }

    private void drawPiece(AGraphics g, PieceType pt, int playerNum) {
        float d = PIECE_RADIUS * 2;
        Color color = getPlayer(playerNum).getColor();
        g.pushMatrix();
        switch (pt) {
            case EMPTY:
                break;
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                drawPiece(g, PieceType.PAWN, color, d, d, null);
                break;
            case BISHOP:
            case QUEEN:
            case KNIGHT_L:
            case KNIGHT_R:
                drawPiece(g, pt, color, d, d, null);
                break;
            case ROOK:
            case ROOK_IDLE:
                drawPiece(g, PieceType.ROOK, color, d, d, null);
                break;
            case DRAGON_R:
            case DRAGON_L:
            case DRAGON_IDLE_R:
            case DRAGON_IDLE_L:
                drawPiece(g, pt, color, d, d, null);
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, color, d, d, GColor.RED);
                break;
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                drawPiece(g, PieceType.KING, color, d, d, null);
                break;
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                drawPiece(g, PieceType.CHECKER, color, d, d, null);
                g.translate(0, -d/5);
                drawPiece(g, PieceType.CHECKER, color, d, d, null);
                break;
            case CHIP_4WAY:
            case DAMA_MAN:
            case CHECKER:
                drawPiece(g, PieceType.CHECKER, color, d, d, null);
                break;
        }
        g.popMatrix();
    }

    public void drawPiece(AGraphics g, PieceType p, Color color, float w, float h, GColor outlineColor) {
        int id = getPieceImageId(p, color);
        if (id > 0) {
            //Matrix3x3 M = new Matrix3x3();
            //g.getTransform(M);
            AImage img = g.getImage(id);
            float a = w/h;
            float aa = img.getAspect();
            //w = w * aa / a;
            h = h * a / aa;
            float imgWidth = img.getWidth();
            float imgHeight = img.getHeight();
            float xScale = w/imgWidth;
            float yScale = h/imgHeight;
            g.pushMatrix();
            if (p.drawFlipped()) {
                g.translate(w / 2, -h);
                g.scale(-xScale, yScale);
            } else {
                g.translate(-w / 2, -h);
                g.scale(xScale, yScale);
            }
            g.drawImage(id);//-w/2, -h/2, w, h);
            g.popMatrix();
        } else {
            g.setColor(color.color);
            g.drawFilledCircle(0, -h/2, w/2);
            float curHeight = g.getTextHeight();
            g.setTextHeight(SQ_DIM/2);
            g.setColor(color.color.inverted());
            g.drawJustifiedString(0, -h/2, Justify.CENTER, Justify.CENTER, p.abbrev);
            g.setTextHeight(curHeight);
        }
        if (outlineColor != null) {
            g.setColor(outlineColor);
            g.drawCircle(0, -h/2, w/2, 3);
        }
    }

    @Override
    protected void onGameOver(Player winner) {
        super.onGameOver(winner);
    }

    @Override
    protected void onMoveChosen(Move m) {
        if (mode == MODE_DRAG_STOP && m.getMoveType() != MoveType.STACK) {
            mode = 0;
            return; // no animations when drag-n-drop
        }
        Piece pc = getPiece(m.getStart());
        switch (m.getMoveType()) {

            case END:
                break;
            case SLIDE: {
                animations.put(pc.getPosition(), new SlideAnim(m.getStart(),m.getEnd()).start());
                break;
            }
            case FLYING_JUMP:
            case JUMP:
                animations.put(pc.getPosition(), new JumpAnim(m.getStart(), m.getEnd()).start());
                break;
            case STACK:
                animations.put(pc.getPosition(), new StackAnim(m.getStart()).start());
                break;
            case SWAP:
                break;
            case CASTLE:
                animations.put(pc.getPosition(), new SlideAnim(m.getStart(),m.getEnd()).start());
                animations.put(pc.getPosition(), new JumpAnim(m.getCastleRookStart(), m.getCastleRookEnd()).start());
                break;

        }

        repaint(0);
        animLock.block();
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

    abstract class PieceAnim extends AAnimation<AGraphics> {

        final float sx, sy, ex, ey;
        final int start;

        public PieceAnim(int start, int end, long durationMSecs) {
            this(start, SQ_DIM * (end & 0xff) + SQ_DIM / 2, SQ_DIM * (end >> 8) + SQ_DIM / 2, durationMSecs);
        }

        public PieceAnim(int start, float ex, float ey, long durationMSecs) {
            super(durationMSecs);
            this.start = start;
            this.sx = SQ_DIM * (start & 0xff) + SQ_DIM / 2;
            this.sy = SQ_DIM * (start >> 8) + SQ_DIM / 2;
            this.ex = ex;
            this.ey = ey;
            animLock.acquire();
        }

        @Override
        protected void onDone() {
            animLock.release();
        }
    }

    class SlideAnim extends PieceAnim {
        public SlideAnim(int start, int end) {
            super(start, end,1000);
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            float x = sx + (ex-sx) * position;
            float y = sy + (ey-sy) * position;
            g.pushMatrix();
            g.translate(x, y);
            drawPiece(g, getPiece(start));
            g.popMatrix();
        }
    }

    class JumpAnim extends SlideAnim {

        final Bezier curve;
        boolean upsidedown;

        private IVector2D[] computeJumpPoints(int playerNum) {

            float midx1 = sx + ((ex-sx) / 3);
            float midx2 = sx + ((ex-sx) * 2 / 3);
            float midy1 = sy + ((ey-sy) / 3);
            float midy2 = sy + ((ey-sy) * 2 / 3);
            float dist = -SQ_DIM;//-1;//getDir(playerNum);
            IVector2D [] v = {
                    new Vector2D(sx, sy),
                    new Vector2D(midx1, midy1+dist),
                    new Vector2D(midx2, midy2+dist),
                    new Vector2D(ex, ey),
            };
            return v;
        }

        public JumpAnim(int start, int end) {
            super(start, end);
            curve = new Bezier(computeJumpPoints(getPiece(start).getPlayerNum()));
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            IVector2D v = curve.getAtPosition(position);
            g.pushMatrix();
            g.translate(v);
            drawPiece(g, getPiece(start));
            g.popMatrix();

        }
    }

    class StackAnim extends PieceAnim {

        final float ssy, eey;

        public StackAnim(int pos) {
            super(pos, pos,1000);
            ssy = ey-SQ_DIM;
            eey = ey-PIECE_RADIUS/2;
        }

        @Override
        public void draw(AGraphics g, float position, float dt) {
            Piece p = getPiece(start);
            g.pushMatrix();
            g.translate(sx, sy);
            drawPiece(g, p);
            g.popMatrix();
            float scale = 1f + (1f-position);
            float x = sx + (ex-sx) * position;
            float y = ssy + (eey-ssy) * position;
            g.pushMatrix();
            g.translate(x, y);
            g.scale(scale, scale);
            drawPiece(g, getPiece(start));
            g.popMatrix();
        }

    }
}
