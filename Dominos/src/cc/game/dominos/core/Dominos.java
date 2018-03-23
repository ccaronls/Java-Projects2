package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cc.lib.annotation.Keep;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;
import cc.lib.utils.Reflector;

/**
 * Dominos game biznezz logic.
 *
 * Example usage:
 *
 * D = new Dominos();
 * D.setNumPlayers(4);
 * D.startNewGame(6, 150);
 *
 * ...
 *
 * From ender thread:
 * D.draw(g, mouseX, mouseY);
 *
 * on mouse click events:
 * D.onClick();
 *
 * on mouse move event:
 * D.redraw()
 *
 * on drag start event:
 * D.startDrag(mouseX, mouseY)
 *
 * on drag end event
 * D.endDrag(mouseX, mouseY)
 */
public abstract class Dominos extends Reflector<Dominos> implements GameServer.Listener {

    public static float SPACING = 6;

    @Omit
    private final Logger log = LoggerFactory.getLogger(getClass());

    static {
        addAllFields(Dominos.class);
    }

    @Override
    protected int getMinVersion() {
        return super.getMinVersion();
    }

    @Omit
    public final GameServer server = new GameServer(
            getServerName(),
            MPConstants.PORT,
            MPConstants.CLIENT_READ_TIMEOUT,
            MPConstants.VERSION,
            MPConstants.getCypher(),
            MPConstants.MAX_CONNECTIONS);

    private Player [] players = new Player[0];
    private LinkedList<Tile> pool = new LinkedList<Tile>();
    private int maxNum;
    private int maxScore;
    private int turn;
    private int difficulty;
    private Board board = new Board();

    @Omit
    private int selectedPlayerTile = -1;
    @Omit
    private int highlightedPlayerTile = -1;
    @Omit
    public final Object gameLock = this;
    @Omit
    private boolean dragging = false;

    public Dominos() {
        server.addListener(this);
    }

    public void setNumPlayers(int num){
        if (gameRunning)
            throw new AssertionError();
        int d = difficulty;
        this.players = new Player[num];
        players[0] = new PlayerUser(0);
        for (int i=1; i<num; i++) {
            players[i] = new Player(i);
            if (d-- > 0) {
                players[i].smart = true;
            }
        }
	}

	protected String getServerName() {
        return System.getProperty("user.home");
    }

	public synchronized void clear() {
        stopGameThread();
        clearHighlghts();
        players = new Player[0];
        pool.clear();
        board.clear();
    }

	public void setPlayers(Player ... players) {
        if (gameRunning)
            throw new AssertionError();
        if (players.length <= 0 || players.length > 4)
            throw new AssertionError();
        this.players = players;
    }

	@Omit
    private boolean gameRunning = false;

	public final boolean isGameRunning() {
	    return gameRunning;
    }

    public void initGame(int maxPipNum, int maxScore, int difficulty) {
        if (gameRunning)
            throw new AssertionError();
        this.maxNum = maxPipNum;
        this.maxScore = maxScore;
        this.difficulty = difficulty;
    }

    public final void startNewGame() {
        if (players.length == 0)
            throw new RuntimeException("No players!");
        stopGameThread();
        newGame();
        redraw();
    }

    public final void stopGameThread() {
        if (gameRunning) {
            gameRunning = false;
            synchronized (gameLock) {
                gameLock.notifyAll();
            }
        }
    }

    @Keep
    protected void onGameOver(int playerNum) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, playerNum);
        final Player winner = players[playerNum];
        addAnimation(winner.getName() + "WINNER", new AAnimation<AGraphics>(1000, -1, true) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GColor c = new GColor(position, 1-position, position, position);
                g.drawJustifiedString(0, 0, Justify.CENTER, "WINNER!");
            }
        }, false);
    }

    private void startPlayerPtsAnim(final Player p, final int pts) {
        addAnimation(p.getName() + "PTS", new AAnimation<AGraphics>(2000) {

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                int curPts = p.score + Math.round(position * pts);
                g.drawJustifiedString(0, 0, Justify.RIGHT, String.valueOf(curPts));
            }

            @Override
            protected void onDone() {
                synchronized (gameLock) {
                    gameLock.notifyAll();
                }
            }
        }, false);
    }

    // return the height of used area (numLines * textHgt)
    private float drawPlayerInfo(AGraphics g, Player p, float maxWidth) {
        AAnimation<AGraphics> a = anims.get(p.getName() + "PTS");
        g.setColor(GColor.BLACK);
        if (a != null) {
            g.translate(maxWidth, 0);
            a.update(g);
            g.translate(-maxWidth, 0);
            g.setColor(GColor.TRANSPARENT);
        }
        GDimension dim = g.drawJustifiedString(maxWidth, 0, Justify.RIGHT, String.valueOf(p.getScore()));
	    g.setColor(GColor.BLUE);
        dim = g.drawWrapString(0, 0, maxWidth-dim.width-SPACING, p.getName());

        a = anims.get(p.getName() + "WINNER");
        if (a != null) {
            g.translate(maxWidth/2, 0);
            a.update(g);
            g.translate(-maxWidth/2, 0);
        } else {
            a = anims.get(p.getName() + "KNOCK");
            if (a != null) {
                g.translate(maxWidth/2, 0);
                a.update(g);
                g.translate(-maxWidth/2, 0);
            }
        }

        return dim.height;
    }

    public final boolean isInitialized() {
        if (players.length < 2)
            return false;
        if (getWinner() >= 0)
            return false;
        return true;
    }

    public final void startGameThread() {
        if (players.length < 2)
            throw new AssertionError("Game not initialized");
        log.debug("startGameThread, currently running=" + gameRunning);
        new Thread() {
            public void run() {
                log.debug("Thread starting.");
                gameRunning = true;
                while (gameRunning && !isGameOver()) {
                    synchronized (gameLock) {
                        runGame();
                        redraw();
                        if (gameRunning) {
                            Utils.waitNoThrow(gameLock, 100);
                        }
                    }
                }
                gameRunning = false;
                log.debug("Thread done.");
                int w = getWinner();
                if (w >= 0) {
                    onGameOver(w);
                }
            }
        }.start();
    }

    private void initPool() {
        pool.clear();
        for (int i=1; i<=maxNum; i++) {
            for (int ii=i; ii<=maxNum; ii++) {
                pool.add(new Tile(i, ii));
            }
        }
    }

	private void newGame() {
        anims.clear();
        pool.clear();
        for (Player p : players) {
            p.reset();
        }
        board.clear();
    }

    public final int getTurn() {
        return turn;
    }

    @Keep
    public synchronized final void setTurn(final int turn) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, turn);
        if (turn >= 0 && turn < players.length) {
            final Player fromPlayer = players[this.turn];
            final Player toPlayer = players[turn];
            addAnimation("TURN", new AAnimation<AGraphics>(1000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    g.setColor(GColor.YELLOW);
                    g.drawRect(fromPlayer.outlineRect.getInterpolationTo(toPlayer.outlineRect, position), 3);
                }

            }, true);
        }
        this.turn = turn;
    }

    public final Board getBoard() {
        return board;
    }

    public final void setBoard(Board board) {
        this.board = board;
    }

    public final int getNumPlayers() {
	    return players.length;
    }

    public final Player getPlayer(int num) {
	    return players[num];
    }

    public final Player getCurPlayer() {
	    return players[turn];
    }

    public final int getMaxPips() {
        return this.maxNum;
    }

    public final int getMaxScore() {
        return this.maxScore;
    }

    public final List<Tile> getPool() { return pool; }

    private void newRound() {

        Utils.shuffle(pool);
        int numPerPlayer = players.length == 2 ? 7 : 5;

        for (Player p : players) {
            for (int i=0; i<numPerPlayer; i++) {
                Tile t = pool.removeFirst();
                onTileFromPool(p.getPlayerNum(), t);
                p.tiles.add(t);
            }
        }

        if (!placeFirstTile()) {
            newRound();
        } else {
            nextTurn();
        }

    }

    private void nextTurn() {
	    setTurn((turn+1) % players.length);
    }

    private boolean placeFirstTile() {
        for (int i=maxNum; i>=1; i--) {
            for (int p=0; p<players.length; p++) {
                Tile t = players[p].findTile(i, i);
                if (t != null) {
                    onPlaceFirstTile(p, t);
                    setTurn(p);
                    redraw();
                    return true;
                }
            }
        }
        return false;
    }

    private List<Move> computePlayerMoves(Player p) {
        List<Move> moves = new ArrayList<>();
        synchronized (p.tiles) {
            for (Tile pc : p.tiles) {
                moves.addAll(board.findMovesForPiece(pc));
            }
        }
        return moves;
    }

    @Keep
    protected final void onPlaceFirstTile(int player, Tile t) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, t);
        players[player].tiles.remove(t);
        board.placeRootPiece(t);
    }

	public final void runGame() {
		if (pool.size() == 0 && board.getRoot() == null) {
            onNewRound();
        }

        Player p = players[turn];
		List<Move> moves = computePlayerMoves(p);

        while (moves.size() == 0) {
		    if (pool.size() == 0) {
		        // see if any player can move, otherwise new round
                boolean canMove = false;
                for (Player pp : players) {
                    if (computePlayerMoves(pp).size() > 0) {
                        canMove = true;
                        break;
                    }
                }

                if (!canMove) {
                    onNewRound();
                    return;
                } else {
                    // player knocks
                    onKnock(turn);
                    break;
                }
            }

		    Tile pc = pool.removeFirst();
		    onTileFromPool(turn, pc);
            p.tiles.add(pc);
            moves.addAll(board.findMovesForPiece(pc));
        }

        if (moves.size() > 0) {
		    Move mv = p.chooseMove(this, moves);
		    if (mv == null)
		        return;
		    onTilePlaced(turn, mv.piece, mv.endpoint, mv.placment);
		    int pts = board.computeEndpointsTotal();
		    if (pts > 0 && pts % 5 == 0) {
		        onPlayerPoints(turn, pts);
            }
        }

        if (p.tiles.size() == 0) {
		    // end of round
            // this player gets remaining tiles points from all other players rounded to nearest 5
            int pts = 0;
            for (Player pp : players) {
                for (Tile t : pp.tiles) {
                    pts += t.pip1 + t.pip2;
                }
            }
            pts = 5*((pts+4)/5);
            if (pts > 0) {
                onPlayerEndRoundPoints(turn, pts);
            }
            onNewRound();
        } else if (getWinner() < 0) {
            nextTurn();
        }

        redraw();
	}

	public final boolean isGameOver() {
	    return getWinner() >= 0;
    }

    @Keep
    protected final void onTilePlaced(int player, Tile tile, int endpoint, int placement) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, tile, endpoint, placement);
        board.doMove(tile, endpoint, placement);
        players[player].tiles.remove(tile);
        redraw();
    }

    @Keep
    protected final void onTileFromPool(int player, final Tile pc) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, pc);
        final Player p = players[player];
        addAnimation(p.getName() + "POOL", new AAnimation<AGraphics>(700) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {

                g.pushMatrix();
                g.translate(0, 0.5f);
                g.scale(1, Math.max(0.1f, position));
                g.translate(0, -0.5f);
                if (p.isPiecesVisible()) {
                    Board.drawTile(g, pc.pip1, pc.pip2, position / 2);
                } else {
                    Board.drawTile(g, 0, 0, 1);
                }
                g.popMatrix();
            }

        }, true);
        redraw();
    }

    @Keep
    protected void onKnock(int player) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player);
        Player p = players[player];
        addAnimation(p.getName() + "KNOCK", new AAnimation<AGraphics>(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.YELLOW.withAlpha(1f-position));
                g.drawJustifiedString(0, 0, Justify.CENTER, "KNOCK");
            }
        }, true);
        redraw();
    }

    @Omit
    private final Map<String, AAnimation<AGraphics>> anims = Collections.synchronizedMap(new HashMap<String, AAnimation<AGraphics>>());

    void addAnimation(String id, AAnimation<AGraphics> a, boolean block) {
        anims.put(id, a);
        a.start();
        redraw();
        if (block && a.getDuration() > 0) {
            Utils.waitNoThrow(gameLock, a.getDuration());
        }
    }


    class StackTilesAnim extends AAnimation<AGraphics> {

        final List<Tile> tiles;
        int rows, cols, num;
        float scale;
        final int pts;
        final Player p;

        static final int DELAY_BETWEEN = 700;

        StackTilesAnim(List<Tile> tiles, final Player p, final int pts) {
            super(tiles.size()*DELAY_BETWEEN + 3000);
            this.tiles = tiles;
            this.pts = pts;
            this.p = p;
            num = tiles.size();
            rows = tiles.size();
            cols = 1;
            if (rows > 5) {
                rows = 5;
                cols = 1 + (num -1)/ 5;
            }
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            scale = Math.min(boardDim / (float)(rows+2), boardDim / (float)(cols*2 + 2));
            int numToShow = Utils.clamp((int)(getElapsedTime()/DELAY_BETWEEN), 0, num);
            drawTiles(g, numToShow, 0);
            redraw();
        }

        void drawTiles(AGraphics g, int numToShow, float positionFromCenter) {
            g.setPointSize(scale/8);
            g.pushMatrix();
            g.translate(boardDim/2, boardDim/2);
            g.scale(scale, scale);
            final float startY = -0.5f * (rows-1);
            MutableVector2D pos = new MutableVector2D(-(cols-1), startY);

            int n = 0;
            for (int i=0; i<cols; i++) {
                for (int ii=0; ii<rows; ii++) {
                    if (n < numToShow) {
                        Tile t = tiles.get(n++);
                        g.pushMatrix();
                        g.translate(pos.scaledBy(1.0f-positionFromCenter));
                        g.translate(-1, -0.5f);
                        g.scale(0.9f, 0.9f);
                        Board.drawTile(g, t.pip1, t.pip2, 1.0f - positionFromCenter);
                        g.translate(1, 0.5f);
                        g.popMatrix();
                        pos.addEq(0, 1);
                    }
                }
                pos.setY(startY);
                pos.addEq(2, 0);
            }

            g.popMatrix();
        }

        @Override
        protected void onDone() {
            addAnimation("TILES", new AAnimation<AGraphics>(1000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    drawTiles(g, num, position);
                }

                @Override
                protected void onDone() {
                    startPlayerPtsBoardGraphicAnim(p, pts);
                }
            }, false);
        }
    }

    private void startPlayerPtsBoardGraphicAnim(final Player p, final int pts) {
        addAnimation("TILES", new AAnimation<AGraphics>(2000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                float hgtStart = boardDim/12;
                float hgtStop  = boardDim/6;

                g.setTextHeight(hgtStart + (hgtStop-hgtStart) * position);
                g.setColor(GColor.MAGENTA.withAlpha(1.0f-position));
                g.drawJustifiedString(boardDim/2, boardDim/2, Justify.CENTER, Justify.CENTER, "+"+pts);
            }
            @Override
            protected void onDone() {
                startPlayerPtsAnim(p, pts);
            }

        }, false);
    }

    @Keep
    protected final void onPlayerEndRoundPoints(final int player, final int pts) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, pts);
        Player p = players[player];

        // figure out how many pieces are left
        List<Tile> tiles = new ArrayList<>();
        for (int i=0; i<players.length; i++) {
            tiles.addAll(players[i].getTiles());
        }

        addAnimation("TILES", new StackTilesAnim(tiles, p, pts), false);
        Utils.waitNoThrow(gameLock, 30000);
        p.score += pts;
    }

    class GlowEndpointAnimation extends AAnimation<AGraphics> {

        final int endpoint;
        final Vector2D[] boundingRect = new Vector2D[2];

        GlowEndpointAnimation(int ep) {
            super(500, 1, true);
            this.endpoint = ep;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.pushMatrix();
            g.clearMinMax();
            board.transformToEndpointLastPiece(g, endpoint);
            g.translate(-1, 0);
            int pips = board.getOpenPips(endpoint);
            g.translate(0.5f, 0.5f);
            float scale = position/4 + 1;
            g.scale(scale, scale);
            g.translate(-0.5f, -0.5f);
            g.setColor(GColor.BLACK);
            g.drawFilledRoundedRect(0, 0, 1, 1, 0.25f);
            g.setColor(GColor.WHITE.interpolateTo(GColor.YELLOW, position));
            g.drawRoundedRect(0, 0, 1, 1, 2/*1+Math.round(position*3)*/, 0.25f);
            Board.drawDie(g, 0, 0, pips);
            boundingRect[0] = g.getMinBoundingRect();
            boundingRect[1] = g.getMaxBoundingRect();
            g.popMatrix();
        }
    };

    /**
     * Called when player was earned pts > 0
     * @param player
     * @param pts
     */
    @Keep
    protected final void onPlayerPoints(final int player, final int pts) {
        server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player,pts);
        final Player p = players[player];

        long delay = 0;
        for (int i=0; i<4; i++) {
            final boolean isFirst = i==0;
            if (board.getOpenPips(i) > 0) {
                board.addAnimation(new GlowEndpointAnimation(i) {

                    @Override
                    public void onDone() {
                        if (isFirst)
                            startPlayerPtsBoardGraphicAnim(p, pts);
                    }

                }.start(delay));
                delay += 300;
            }
        }

        redraw();
        Utils.waitNoThrow(gameLock, -1);
        p.score += pts;
    }

    protected final void onNewRound() {
        server.broadcastCommand(new GameCommand(MPConstants.SVR_TO_CL_INIT_ROUND).setArg("dominos", this));
        for (Player p : players) {
            p.tiles.clear();
        }
        board.clear();
        initPool();
        int expectedPoolSize = pool.size();
        board.startShuffleAnimation(gameLock, pool);
        redraw();
        Utils.waitNoThrow(gameLock, -1);
        while (pool.size() != expectedPoolSize) {
            log.error("Monitor notified too early");
            Utils.waitNoThrow(gameLock, 1000);
        }
        newRound();
    }

    @Omit
    private float boardDim = 0;

    public synchronized final void draw(APGraphics g, int pickX, int pickY) {
        float w = g.getViewportWidth();
        float h = g.getViewportHeight();

        g.ortho(0, w, 0, h);

        // choose square for board and remainder to be used to display players stats

        final boolean portrait = h > w;
        boardDim = portrait ? w : h;

        g.clearScreen(GColor.GREEN);
        g.setColor(GColor.GREEN.darkened(0.2f));
        g.drawFilledRect(0, 0, boardDim, boardDim);

        boolean drawDragged = false;
        AAnimation<AGraphics> anim = anims.get("TILES");
        if (anim != null) {
            board.draw(g, boardDim, boardDim, pickX, pickY, null);
            anim.update(g);
        } else {
            Tile dragging = null;
            if (this.dragging && selectedPlayerTile >= 0) {
                dragging = getUser().getTiles().get(selectedPlayerTile);
            }
            drawDragged = true;
            if (board.draw(g, boardDim, boardDim, pickX, pickY, dragging) >= 0)
                drawDragged = false;
        }

        g.pushMatrix();
        if (portrait) {
            // draw the non-visible player stuff on lower LHS and visible player stuff on lower RHS
            g.translate(0, boardDim);
            //setupTextHeight(g, w/3, (h-boardDim)/5);
            g.setTextHeight((h-boardDim)/8);
            drawInfo(g, w/3-SPACING, h-boardDim);
            g.translate(w/3, 0);
            drawPlayer(g, w*2/3, h-boardDim, pickX, pickY, drawDragged);
        } else {
            // draw the non-visible player stuff on lower half of rhs and visible player stuff on
            // upper half of RHS
            g.translate(boardDim, 0);
            //setupTextHeight(g, w-boardDim, h/5);
            g.setTextHeight(h/(3*6));
            drawPlayer(g, w-boardDim, h*2/3, pickX, pickY, drawDragged);
            g.translate(0, h*2/3+SPACING);
            drawInfo(g, w-boardDim, h/3-SPACING);
        }
        g.popMatrix();

        anim = anims.get("TURN");
        if (anim != null) {
            anim.update(g);
        }

        if (anim == null && turn >= 0 && turn < players.length) {
            g.setColor(GColor.YELLOW);
            g.drawRect(players[turn].outlineRect, 3);
        }

        synchronized (anims) {
            Iterator<String> it = anims.keySet().iterator();
            while (it.hasNext()) {
                if (anims.get(it.next()).isDone()) {
                    it.remove();
                }
            }
        }

        if (anims.size() > 0 || board.animations.size() > 0)
            redraw();
    }

    private void setupTextHeight(AGraphics g, float maxWidth, float maxHeight) {
        // now size down until the string fits into the width
        float height = maxHeight;

        String longest = "";
        for (Player p : players) {
            String s = String.format("%s X 99 999 PTS", p.getName());
            if (s.length() > longest.length()) {
                longest = s;
            }
        }

        for (int i=0; i<10; i++) {
            g.setTextHeight(height);
            float width = g.getTextWidth(longest);
            float delta = width / maxWidth;
            if (width > maxWidth) {
                //g.setFont(g.getFont().deriveFont(--targetHeight));
                height *= 1f/delta;
            } else if (width < maxWidth*0.8f && height < maxHeight * 0.8f) {
                height *= 1f/(delta*2);
            } else {
                break;
            }
        }
    }

    private void drawInfo(APGraphics g, float w, float h) {
	    GColor [] bk = {
	            GColor.GREEN.darkened(.25f),
                GColor.GREEN.darkened(.5f)
        };
	    g.pushMatrix();
	    int index = 0;
	    for (int i=0; i<players.length; i++) {
            Player p = players[i];
	        if (p.isPiecesVisible())
	            continue;

	        g.setColor(bk[index]);
	        index = (index+1) % bk.length;
	        g.drawFilledRect(0, 0, p.outlineRect.w, p.outlineRect.h);

            Vector2D outline1 = g.transform(0, 0);
	        float dy = drawPlayerInfo(g, p, w);
	        g.translate(0, dy);

            g.translate(0, SPACING);

            int numTiles = p.getTiles().size();
            final float tileDim = g.getTextHeight() / 2;
            AAnimation<AGraphics> anim = anims.get(p.getName() + "POOL");
            float width = tileDim * 2 * numTiles + ((numTiles-1)*SPACING);
            if (anim != null) {
                width += tileDim*2 + SPACING;
            }
            g.pushMatrix();
            {
                if (width > w) {
                    final float saveth = g.getTextHeight();
                    g.setTextHeight(saveth / 2);
                    g.pushMatrix();
                    {
                        g.scale(tileDim, tileDim);
                        Board.drawTile(g, 0, 0, 1);
                    }
                    g.popMatrix();
                    g.translate(tileDim * 2 + SPACING, 0);
                    if (anim != null) {
                        g.pushMatrix();
                        {
                            g.scale(tileDim, tileDim);
                            anim.update(g);
                        }
                        g.popMatrix();
                        g.translate(tileDim * 2 + SPACING, 0);
                    }
                    g.setColor(GColor.BLUE);
                    g.drawJustifiedString(0, tileDim / 2, Justify.LEFT, Justify.CENTER, "x " + numTiles);
                    g.setTextHeight(saveth);

                } else {
                    for (int t = 0; t < numTiles; t++) {
                        g.pushMatrix();
                        {
                            g.scale(tileDim, tileDim);
                            Board.drawTile(g, 0, 0, 1);
                        }
                        g.popMatrix();
                        g.translate(tileDim * 2 + SPACING, 0);
                    }
                    if (anim != null) {
                        g.pushMatrix();
                        {
                            g.scale(tileDim, tileDim);
                            anim.update(g);
                        }
                        g.popMatrix();
                    }
                }
            }
	        g.popMatrix();
            g.translate(0, tileDim+SPACING);
            Vector2D outline2 = g.transform(w, 0);
            p.outlineRect.set(outline1, outline2);
        }
        g.popMatrix();
        if (isInitialized()) {
            //g.drawJustifiedString(0, h, Justify.LEFT, Justify.BOTTOM, String.format("Pool X %d", pool.size()));
            g.pushMatrix();
            float dim = g.getTextHeight();
            g.translate(0, h-dim);
            g.scale(dim, dim);
            Board.drawTile(g, 0, 0, 1);
            g.popMatrix();
            g.setColor(GColor.BLUE);
            g.drawJustifiedString(dim*2+SPACING, h, Justify.LEFT, Justify.BOTTOM, String.format("x %d", pool.size()));
        }
    }

    private void drawPlayer(APGraphics g, float w, float h, int pickX, int pickY, boolean drawDragged) {
        String infoStr = "MENU"; // TODO: know when in shuffle so we can skip it
        g.setColor(GColor.WHITE);
        g.setName(1);
        g.begin();
        float tw = g.drawJustifiedString(w, 0, Justify.RIGHT, infoStr).width;
        int picked = g.pickRects(pickX, pickY);
        tw += tw/4;
        menuHighlighted = false;
        if (picked == 1) {
            menuHighlighted = true;
            g.setColor(GColor.DARK_GRAY);
            g.drawJustifiedString(w, 0, Justify.RIGHT, infoStr);
        }
        g.end();
        g.pushMatrix();
        for (int i=0; i<players.length; i++) {
            if (players[i].isPiecesVisible()) {
                Player p = players[i];
                p.outlineRect.set(g.transform(0, 0), g.transform(w, h));
                float dy = drawPlayerInfo(g, p, w-tw);
                dy += SPACING;
                g.translate(0, dy);
                h -= dy;
                drawPlayerTiles(g, p, w, h, pickX, pickY, drawDragged);
                break;
            }
        }
        g.popMatrix();
    }

    @Omit
    private boolean menuHighlighted = false;

    private final PlayerUser getUser() {
        for (Player p : players)
            if (p instanceof PlayerUser)
                return (PlayerUser)p;
        return null;
    }

    private void drawPlayerTiles(APGraphics g, Player p, float w, float h, int pickX, int pickY, boolean drawDragged) {
	    final int numPlayerTiles = p.getTiles().size();
        //int numVirtualTiles = numPlayerTiles;
        int numDrawnTiles = numPlayerTiles;

        final List<Tile> playertiles;
        synchronized (p.tiles) {
            playertiles = new ArrayList<>(p.tiles);
        }
        AAnimation<AGraphics> poolAnim = anims.get(p.getName() + "POOL");
	    if (poolAnim != null) {
	        numDrawnTiles++;
        }

	    if (numDrawnTiles == 0)
	        return;

        final float aspect = w/(h*2);

        int cols = 2*Utils.clamp((int)Math.floor(Math.sqrt(aspect*numDrawnTiles*2)), 1, 100);
        int rows = Utils.clamp((int)Math.ceil(2f * numDrawnTiles / cols),2, 100);

        if (rows * cols / 2 < numDrawnTiles) {
            System.err.println("oooops");
        }
        int tilesPerRow = cols/2;
        float tileD = Math.min(h/rows, w/cols);

        g.setPointSize(tileD/8);
        highlightedPlayerTile = -1;
        int tile = 0;
        g.pushMatrix();
        g.scale(tileD, tileD);
        for (int i=0; i<rows; i++) {
            g.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < numDrawnTiles) {

                    boolean anim = tile >= numPlayerTiles;

                    if (!anim) {
                        Tile t = playertiles.get(tile);
                        PlayerUser user = getUser();
                        boolean available = user != null && user.usable.contains(t);
                        if (available) {
                            g.setName(tile);
                            g.vertex(0, 0);
                            g.vertex(2, 1);
                            int picked = g.pickRects(pickX, pickY);
                            if (picked >= 0) {
                                highlightedPlayerTile = picked;
                            }
                        }
                        g.pushMatrix();
                        g.translate(0.04f, 0.02f);
                        g.scale(0.95f, 0.95f);
                        float alpha = available ? 1f : 0.5f;
                        if (tile == selectedPlayerTile && dragging) {
                            if (drawDragged) {
                                g.pushMatrix();
                                g.setIdentity();
                                g.translate(pickX, pickY);
                                g.translate(-tileD, -tileD / 2);
                                g.scale(tileD, tileD);
                                Board.drawTile(g, t.pip1, t.pip2, 1);
                                g.popMatrix();
                            }
                        } else {
                            Board.drawTile(g, t.pip1, t.pip2, alpha);
                        }
                        g.popMatrix();
                        if (tile == selectedPlayerTile) {
                            g.setColor(GColor.RED);
                            g.drawRect(0, 0, 2, 1, 3);
                        } else if (tile == highlightedPlayerTile) {
                            g.setColor(GColor.YELLOW);
                            g.drawRect(0, 0, 2, 1, 3);
                        }
                    } else {
                        if (poolAnim != null) {
                            poolAnim.update(g);
                        }
                    }
                    g.translate(2, 0);
                    tile++;
                }
            }
            g.popMatrix();
            g.translate(0, 1);
        }
        g.popMatrix();
    }

    public final int getWinner() {
        for (int i=0; i<players.length; i++) {
            Player p = players[i];
            if (p.score >= maxScore) {
                return i;
            }
        }
        return -1;
    }

    public final int getDifficulty() {
        return difficulty;
    }

    protected void onMenuClicked() {
        startNewGame();
        startGameThread();
    }

    public final void clearHighlghts() {
        menuHighlighted = false;
        highlightedPlayerTile = -1;
        selectedPlayerTile = -1;
        board.clearSelection();
        getBoard().highlightMoves(null);
    }

    public final synchronized void onClick() {
        if (menuHighlighted) {
            onMenuClicked();
            clearHighlghts();
        }
        PlayerUser user = getUser();
        if (user == null)
            return;
        if (highlightedPlayerTile >= 0) {
            selectedPlayerTile = highlightedPlayerTile;
            List<Move> highlightedMoves = new ArrayList<>();
            for (Move m : user.moves) {
                if (m.piece == user.tiles.get(selectedPlayerTile)) {
                    highlightedMoves.add(m);
                }
            }
            getBoard().highlightMoves(highlightedMoves);
        } else if (board.selectedMove != null) {
            user.setChoosedMove(board.selectedMove);
            synchronized (gameLock) {
                gameLock.notifyAll();
            }
            clearHighlghts();
        } else {
            clearHighlghts();
            log.debug("endpoints total: " + board.computeEndpointsTotal());
        }
        redraw();
    }

    public synchronized final void startDrag() {
        dragging = true;
        if (selectedPlayerTile < 0 && highlightedPlayerTile >= 0) {
            PlayerUser user = getUser();
            if (user != null) {
                selectedPlayerTile = highlightedPlayerTile;
                List<Move> highlightedMoves = new ArrayList<>();
                for (Move m : user.moves) {
                    if (m.piece.equals(user.tiles.get(selectedPlayerTile))) {
                        highlightedMoves.add(m);
                    }
                }
                getBoard().highlightMoves(highlightedMoves);
            }
        }
        redraw();
    }

    public synchronized final void stopDrag() {
        if (dragging) {
            if (selectedPlayerTile >= 0 && board.selectedMove != null) {
                PlayerUser user = getUser();
                if (user != null) {
                    user.setChoosedMove(board.selectedMove);
                    synchronized (gameLock) {
                        gameLock.notifyAll();
                    }
                }
            }
            dragging = false;
        }
        getBoard().clearSelection();
        selectedPlayerTile = -1;
        highlightedPlayerTile = -1;
        redraw();
    }

    @Override
    public final void onConnected(ClientConnection conn) {
        // if enough clients have connected then start the game
        Player remote = null;
        for (int i=1; i<getNumPlayers(); i++) {
            Player p = getPlayer(i);
            ClientConnection connection = p.getConnection();
            if (connection == null || !connection.isConnected()) {
                remote = p;
            } else if (connection.getName().equals(conn.getName())) {
                remote = p;
                break; // stop here if we have a reconnect
            }
        }
        if (remote != null) {
            remote.connect(conn);
            conn.sendCommand(new GameCommand(MPConstants.SVR_TO_CL_INIT_GAME)
                    .setArg("numPlayers", getNumPlayers())
                    .setArg("playerNum", remote.getPlayerNum())
                    .setArg("dominos", this));
            onPlayerConnected(remote);
        }
    }

    protected abstract void onPlayerConnected(Player player);

    @Override
    public void onReconnection(ClientConnection conn) {

    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {

    }

    public abstract void redraw();
}
