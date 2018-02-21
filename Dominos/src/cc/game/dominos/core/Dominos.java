package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AAnimation;
import cc.lib.game.GColor;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.Utils;
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
public abstract class Dominos extends Reflector<Dominos> {

    static {
        addAllFields(Dominos.class);
    }

    @Override
    protected int getMinVersion() {
        return super.getMinVersion();
    }

    private Player [] players = new Player[0];
    private LinkedList<Tile> pool = new LinkedList<Tile>();
    private int maxNum;
    private int maxScore;
    private int turn;
    private Board board = new Board();

    @Omit
    private int selectedPlayerTile = -1;
    @Omit
    private int highlightedPlayerTile = -1;
    @Omit
    private Object gameLock = this;
    @Omit
    private boolean dragging = false;

    public void setNumPlayers(int num){
        this.players = new Player[num];
        players[0] = new PlayerUser();
        for (int i=1; i<num; i++) {
            players[i] = new Player();
        }
	}

	@Omit
    private boolean gameRunning = false;

	public final boolean isGameRunning() {
	    return gameRunning;
    }

    public final void startNewGame(int maxPipNum, int maxScore) {
	    if (players.length == 0)
	        throw new RuntimeException("No players!");
        this.maxNum = maxPipNum;
        this.maxScore = maxScore;
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

    public final void startGameThread() {
        Utils.println("startGameThread, currently running=" + gameRunning);
        new Thread() {
            public void run() {
                Utils.println("Thread starting.");
                gameRunning = true;
                while (gameRunning && !isGameOver()) {
                    synchronized (gameLock) {
                        runGame();
                        redraw();
                        if (gameRunning) {
                            try {
                                gameLock.wait(1000);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                gameRunning = false;
                Utils.println("Thread done.");
                if (getWinner() != null) {
                    getWinner().textAnimation = new AAnimation<AGraphics>(1000, -1, true) {
                        @Override
                        protected void draw(AGraphics g, float position, float dt) {
                            GColor c = new GColor(position, 1-position, position, position);
                            if (getWinner().isPiecesVisible()) {
                                g.drawAnnotatedString(String.format("%sPoints %d WINNER!!", c, getWinner().getScore()), 0, 0);
                            } else {
                                g.drawAnnotatedString(String.format("%sP%d Points %d WINNNER!!", c, turn, getWinner().getScore()), 0, 0);
                            }
                            redraw();
                        }
                    }.start();
                    redraw();
                }
            }
        }.start();
    }

	private void newGame() {
        pool.clear();
        for (int i=1; i<=maxNum; i++) {
            for (int ii=i; ii<=maxNum; ii++) {
                pool.add(new Tile(i, ii));
            }
        }
        for (Player p : players) {
            p.reset();
        }
        board.clear();
        newRound();
    }

    public final int getTurn() {
        return turn;
    }

    public final void setTurn(int turn) {
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

    private void newRound() {

        for (Player p : players) {
            pool.addAll(p.tiles);
            p.tiles.clear();
        }

        pool.addAll(board.collectPieces());
        board.clear();
        Utils.shuffle(pool);

        Utils.println("Total number of tiles: " + pool.size());

        int numPerPlayer = players.length == 2 ? 7 : 5;

        for (Player p : players) {
            for (int i=0; i<numPerPlayer; i++)
                p.tiles.add(pool.removeFirst());
        }

        if (!placeFirstTile()) {
            newRound();
        } else {
            nextTurn();
        }

    }

    private void nextTurn() {
	    turn = (turn+1) % players.length;
    }

    private boolean placeFirstTile() {
        for (int i=maxNum; i>=1; i--) {
            for (int p=0; p<players.length; p++) {
                Tile pc = players[p].removeTile(i, i);
                if (pc != null) {
                    onPiecePlaced(players[p], pc);
                    board.placeRootPiece(pc);
                    redraw();
                    turn = p;
                    return true;
                }
            }
        }
        return false;
    }

    protected void onPiecePlaced(Player player, Tile pc) {}

    private List<Move> computrePlayerMoves(Player p) {
        List<Move> moves = new ArrayList<>();
        for (Tile pc : p.tiles) {
            moves.addAll(board.findMovesForPiece(pc));
        }
        return moves;
    }

	public final void runGame() {
		Player p = players[turn];

		List<Move> moves = computrePlayerMoves(p);

        while (moves.size() == 0) {
		    if (pool.size() == 0) {
		        // see if any player can move, otherwise new round
                boolean canMove = false;
                for (Player pp : players) {
                    if (computrePlayerMoves(pp).size() > 0) {
                        canMove = true;
                        break;
                    }
                }

                if (!canMove) {
                    onEndRound();
                    newRound();
                    redraw();
                    return;
                } else {
                    // player knocks
                    onKnock(p);
                    redraw();
                    break;
                }
            }

		    Tile pc = pool.removeFirst();
		    onTileFromPool(p, pc);
            p.tiles.add(pc);
            moves.addAll(board.findMovesForPiece(pc));
        }

        if (moves.size() > 0) {
		    Move mv = p.chooseMove(this, moves);
		    if (mv == null)
		        return;
		    onTilePlaced(p, mv);
		    board.doMove(mv);
            redraw();
            p.tiles.remove(mv.piece);
		    int pts = board.computeEndpointsTotal();
		    if (pts > 0 && pts % 5 == 0) {
		        onPlayerPoints(p, pts);
                p.score += pts;
            }
        }

        if (p.tiles.size() == 0) {
		    // end of round
            // this player gets remaining tiles points from all other players rounded to nearest 5
            int pts = 0;
            for (Player pp : players) {
                for (Tile t : p.tiles) {
                    pts += t.pip1 + t.pip2;
                }
            }
            pts = 5*((pts+2)/5);
            if (pts > 0) {
                onPlayerPoints(p, pts);
                p.score += pts;
            }
            newRound();
            onEndRound();
        } else if (getWinner() == null) {
            nextTurn();
        }
	}

	public final boolean isGameOver() {
	    return getWinner() != null;
    }

    protected void onTilePlaced(Player p, Move mv) {
    }

    protected void onTileFromPool(Player p, final Tile pc) {
	    if (p.isPiecesVisible()) {
	        poolAnimation = new AAnimation<AGraphics>(2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {

                    g.pushMatrix();
                    g.translate(0, 0.5f);
                    g.scale(1, position);
                    g.translate(0, -0.5f);
                    Board.drawTile(g, pc.pip1, pc.pip2, position/2);
                    g.popMatrix();
                    redraw();
                }

                @Override
                protected void onDone() {
                    poolAnimation = null;
                    synchronized (gameLock) {
                        gameLock.notifyAll();
                    }
                    redraw();
                }
            }.start();
	        redraw();
	        synchronized (gameLock) {
	            try {
	                gameLock.wait();
                } catch (Exception e) {}
            }
        } else {
            redraw();
        }


    }

    protected void onKnock(Player p) {
    }

    protected void onEndRound() {
    }

    @Omit
    private AAnimation<AGraphics> poolAnimation = null;


    /**
     * Called when player was earned pts > 0
     * @param p
     * @param pts
     */
    protected void onPlayerPoints(final Player p, final int pts) {
        p.textAnimation = new AAnimation<AGraphics>(2000) {

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                int curPts = p.score + Math.round(position * pts);
                String alphaRed = GColor.RED.setAlpha(1.0f-position).toString();
                if (p.isPiecesVisible())
                    g.drawAnnotatedString(String.format("%d PTS %s+%d", curPts, alphaRed, pts), 0, 0);
                else
                    g.drawAnnotatedString(String.format("P%d X %d %s%d PTS %s+%d", turn+1, p.getTiles().size(), GColor.BLACK, curPts, alphaRed, pts), 0, 0);
                redraw();
            }

            @Override
            protected void onDone() {
                p.textAnimation = null;
                synchronized (gameLock) {
                    gameLock.notifyAll();
                }
                redraw();
            }
        }.start();

        redraw();
        synchronized (gameLock) {
            try {
                gameLock.wait();
            } catch (Exception e) {}
        }
    }

    public synchronized final void draw(APGraphics g, int pickX, int pickY) {
        float w = g.getViewportWidth();
        float h = g.getViewportHeight();

        g.ortho(0, w, 0, h);

        // choose square for board and remainder to be used to display players stats

        final boolean portrait = h > w;
        final float boardDim = portrait ? w : h;

        g.clearScreen(GColor.GREEN);
        g.setColor(GColor.GREEN.darkened(0.2f));
        g.drawFilledRectf(0, 0, boardDim, boardDim);
        Tile dragging = null;
        if (this.dragging && selectedPlayerTile >= 0) {
            dragging = getUser().getTiles().get(selectedPlayerTile);
        }
        boolean drawDragged = true;
        if (board.draw(g, boardDim, boardDim, pickX, pickY, dragging) >= 0)
            drawDragged = false;

        g.pushMatrix();
        if (portrait) {
            // draw the non-visible player stuff on lower LHS and visible player stuff on lower RHS
            setupTextHeight(g, w/3, h-boardDim);
            g.translate(0, boardDim);
            drawInfo(g, w/3, h-boardDim);
            g.translate(w/3, 0);
            drawPlayer(g, w*2/3, h-boardDim, pickX, pickY, drawDragged);
        } else {
            // draw the non-visible player stuff on lower half of rhs and visible player stuff on
            // upper half of RHS
            setupTextHeight(g, w-boardDim, h/3);
            g.translate(boardDim, 0);
            drawPlayer(g, w-boardDim, h*2/3, pickX, pickY, drawDragged);
            g.translate(0, h*2/3);
            drawInfo(g, w-boardDim, h/3);
        }
        g.popMatrix();
    }

    private void setupTextHeight(AGraphics g, float targetWidth, float targetHeight) {

        targetHeight -= 10;
        targetHeight /= 5;
        g.setTextHeight(targetHeight);

        // now size down until the string fits into the width
        for (int i=0; i<100; i++) {
            float width = g.getTextWidth("WWWWWWWWWWWWW");
            if (width > targetWidth) {
                //g.setFont(g.getFont().deriveFont(--targetHeight));
                g.setTextHeight(--targetHeight);
            } else {
                break;
            }
        }
    }

    private void drawInfo(APGraphics g, float w, float h) {
	    int name = 0;
	    float padding = 5;
	    float y = padding;
        g.setColor(GColor.BLUE);
	    for (Player p : players) {
	        name++;
	        if (p.isPiecesVisible())
	            continue;
	        g.pushMatrix();
	        g.translate(padding, y);
	        if (p.textAnimation != null)
	            p.textAnimation.update(g);
	        else
                g.drawAnnotatedString(String.format("P%d X %d [0,0,0]%d PTS", name, p.getTiles().size(), p.getScore()), 0, 0);
	        g.popMatrix();
	        y += g.getTextHeight();
        }
        g.drawString(String.format("Pool X %d", pool.size()), padding, y);
    }

    private void drawPlayer(APGraphics g, float w, float h, int pickX, int pickY, boolean drawDragged) {
        for (Player p : players) {
            if (p.isPiecesVisible()) {
                drawPlayer(g, p, w, h, pickX, pickY, drawDragged);
                break;
            }
        }
    }

    private void drawPlayer(APGraphics g, Player p, float w, float h, int pickX, int pickY, boolean drawDragged) {
	    g.pushMatrix();
	    int padding = 5;
	    g.setColor(GColor.BLUE);
	    g.translate(padding, padding);
	    if (p.textAnimation != null)
	        p.textAnimation.update(g);
	    else
	        g.drawString(String.format("%d PTS", p.getScore()), 0, 0);
	    g.translate(0, g.getTextHeight() + padding);

	    h-= padding*2+g.getTextHeight();
	    w-= padding*2;

	    // find the best grid to use to
        drawPlayerTiles(g, p, w, h, pickX, pickY, drawDragged);
        g.popMatrix();
    }

    private PlayerUser getUser() {
        return (PlayerUser)players[0];
    }

    private void drawPlayerTiles(APGraphics g, Player p, float w, float h, int pickX, int pickY, boolean drawDragged) {

	    int numRows = 1;
	    int numTiles = p.getTiles().size();

	    if (poolAnimation != null) {
	        numTiles++;
        }

	    if (numTiles == 0)
	        return;

        if (numTiles < 4)
            numTiles = 4;

	    float tileD = Math.min(w/(2*numTiles), h);
	    int tilesPerRow = numTiles;

	    while (tilesPerRow > 1) {
	        if (tileD * 2 * tilesPerRow * tileD * numRows < (w*h)/2) {
                tilesPerRow -= 1;
                numRows++;
                tileD = w / (2 * tilesPerRow);
            } else {
	            break;
            }
        }

        final int numT = p.tiles.size() + (poolAnimation == null ? 0 : 1);

        if (tileD*numRows > h) {
	        tileD = h/numRows;
        }

        g.setPointSize(tileD/8);
        highlightedPlayerTile = -1;
        int tile = 0;
        g.pushMatrix();
        g.scale(tileD, tileD);
        for (int i=0; i<numRows; i++) {
            g.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < numT) {

                    boolean anim = tile >= p.tiles.size();

                    if (!anim) {
                        Tile t = p.tiles.get(tile);
                        boolean available = getUser().usable.contains(t);
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
                        poolAnimation.update(g);
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

    public final Player getWinner() {
        for (Player p : players) {
            if (p.score >= maxScore) {
                return p;
            }
        }
        return null;
    }

    public synchronized final void onClick() {
        PlayerUser user = getUser();
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
            user.choosedMove = board.selectedMove;
            getBoard().clearSelection();
            synchronized (gameLock) {
                gameLock.notifyAll();
            }
            selectedPlayerTile = -1;
        } else {
            getBoard().highlightMoves(null);
            selectedPlayerTile = -1;
            Utils.println("endpoints total: " + board.computeEndpointsTotal());
        }
        redraw();
    }

    public synchronized final void startDrag() {
        dragging = true;
        if (selectedPlayerTile < 0 && highlightedPlayerTile >= 0) {
            PlayerUser user = getUser();
            selectedPlayerTile = highlightedPlayerTile;
            List<Move> highlightedMoves = new ArrayList<>();
            for (Move m : user.moves) {
                if (m.piece == user.tiles.get(selectedPlayerTile)) {
                    highlightedMoves.add(m);
                }
            }
            getBoard().highlightMoves(highlightedMoves);
        }
    }

    public synchronized final void stopDrag() {
        if (dragging) {
            if (selectedPlayerTile >= 0 && board.selectedMove != null) {
                getUser().choosedMove = board.selectedMove;
                getBoard().clearSelection();
                synchronized (gameLock) {
                    gameLock.notifyAll();
                }
                selectedPlayerTile = -1;
            }
            dragging = false;
        }
    }

    public abstract void redraw();
}
