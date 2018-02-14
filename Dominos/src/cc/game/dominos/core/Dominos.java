package cc.game.dominos.core;

import java.util.*;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.utils.Reflector;

public abstract class Dominos extends Reflector<Dominos> {

    static {
        addAllFields(Dominos.class);
    }

    @Override
    protected int getMinVersion() {
        return 1;
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
        this.maxNum = maxPipNum;
        this.maxScore = maxScore;
        if (gameRunning) {
            gameRunning = false;
            synchronized (this) {
                this.notifyAll();
            }
        }
        newGame();
        startGameThread();
        redraw();
    }

    public final void startGameThread() {
        Utils.println("startGameThread, currently running=" + gameRunning);
        new Thread() {
            public void run() {
                Utils.println("Thread starting.");
                gameRunning = true;
                while (gameRunning && !isGameOver()) {
                    runGame();
                    if (gameRunning) {
                        synchronized (this) {
                            try {
                                wait(1000);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                gameRunning = false;
                Utils.println("Thread done.");
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
            p.tiles.clear();
            p.score = 0;
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
                    board.placeRootPiece(pc);
                    onPiecePlaced(players[p], pc);
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
                    newRound();
                    onEndRound();
                    return;
                } else {
                    // player knocks
                    onKnock(p);
                    break;
                }
            }

		    Tile pc = pool.removeFirst();
		    p.tiles.add(pc);
		    onTileFromPool(p, pc);
            moves.addAll(board.findMovesForPiece(pc));
        }

        if (moves.size() > 0) {
		    Move mv = p.chooseMove(this, moves);
		    if (mv == null)
		        return;
		    onTilePlaced(p, mv);
		    board.doMove(mv);
		    p.tiles.remove(mv.piece);
		    int pts = board.computeEndpointsTotal();
		    if (pts % 5 == 0) {
		        p.score += pts;
		        onPlayerPoints(p, pts);
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
                p.score += pts;
                onPlayerPoints(p, pts);
            }
            newRound();
            onEndRound();
        } else {
            nextTurn();
        }
	}

	public final boolean isGameOver() {
	    return getWinner() != null;
    }

    protected void onTilePlaced(Player p, Move mv) {
        redraw();
    }

    protected void onTileFromPool(Player p, Tile pc) {
        redraw();
    }

    protected void onKnock(Player p) {
        redraw();
    }

    protected void onEndRound() {
        redraw();
    }

    protected void onPlayerPoints(Player p, int pts) {
        // start an animation
        redraw();
    }

    public final   void draw(APGraphics g, int pickX, int pickY) {
        float w = g.getViewportWidth();
        float h = g.getViewportHeight();

        g.ortho(0, w, 0, h);

        // choose square for board and remainder to be used to display players stats

        final boolean portrait = h > w;
        final float boardDim = portrait ? w : h;

        g.clearScreen(g.GREEN);
        g.setColor(g.GREEN.darken(g, 0.2f));
        g.drawFilledRectf(0, 0, boardDim, boardDim);
        board.draw(g, boardDim, boardDim, pickX, pickY);

        g.pushMatrix();
        if (portrait) {
            // draw the non-visible player stuff on lower LHS and visible player stuff on lower RHS
            setupTextHeight(g, w/3, h-boardDim);
            g.translate(0, boardDim);
            drawInfo(g, w/3, h-boardDim);
            g.translate(w/3, 0);
            drawPlayer(g, w*2/3, h-boardDim, pickX, pickY);
        } else {
            // draw the non-visible player stuff on lower half of rhs and visible player stuff on
            // upper half of RHS
            setupTextHeight(g, w-boardDim, h/3);
            g.translate(boardDim, 0);
            drawPlayer(g, w-boardDim, h*2/3, pickX, pickY);
            g.translate(0, h*2/3);
            drawInfo(g, w-boardDim, h/3);
        }
        g.popMatrix();
    }

    void setupTextHeight(AGraphics g, float targetWidth, float targetHeight) {
        targetHeight -= 10;
        targetHeight /= 5;
        g.setTextHeight(targetHeight);

        // now size down until the string fits into the width
        for (int i=0; i<100; i++) {
            float width = g.getTextWidth("WWWWWWWWWWW");
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
        g.setColor(g.BLUE);
	    for (Player p : players) {
	        name++;
	        if (p.isPiecesVisible())
	            continue;
	        g.drawString(String.format("P%d X %d %d Points\n", name, p.getTiles().size(), p.getScore()), padding, y);
	        y += g.getTextHeight();
        }
        g.drawString(String.format("Pool X %d", pool.size()), padding, y);
    }

    private void drawPlayer(APGraphics g, float w, float h, int pickX, int pickY) {
        for (Player p : players) {
            if (p.isPiecesVisible()) {
                drawPlayer(g, p, w, h, pickX, pickY);
                break;
            }
        }
    }

    private void drawPlayer(APGraphics g, Player p, float w, float h, int pickX, int pickY) {
	    g.pushMatrix();
	    int padding = 5;
	    g.setColor(g.BLUE);
	    g.drawString(String.format("%d Points", p.getScore()), padding, padding);
	    g.translate(padding, g.getTextHeight() + padding*2);

	    h-= padding*2+g.getTextHeight();
	    w-= padding*2;

	    // find the best grid to use to
        drawPlayerTiles(g, p, true, w, h, pickX, pickY);
        g.popMatrix();
    }

    private void drawPlayerTiles(APGraphics g, Player p, boolean visible, float w, float h, int pickX, int pickY) {

	    int numRows = 1;
	    final int numTiles = p.getTiles().size();

	    if (numTiles == 0)
	        return;

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

        if (tileD*numRows > h) {
	        tileD = h/numRows;
        }

        highlightedPlayerTile = -1;
        int tile = 0;
        for (int i=0; i<numRows; i++) {
            g.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < p.tiles.size()) {
                    Tile t = p.tiles.get(tile);

                    g.setName(tile);
                    g.vertex(0, 0);
                    g.vertex(tileD*2, tileD);
                    int picked = g.pickRects(pickX, pickY);
                    if (picked >= 0) {
                        highlightedPlayerTile = picked;
                    }
                    g.pushMatrix();
                    g.scale(0.95f, 0.95f);
                    Board.drawTile(g, tileD, t.pip1, t.pip2);
                    g.popMatrix();
                    if (tile == selectedPlayerTile) {
                        g.setColor(g.RED);
                        g.drawRect(0, 0, tileD*2, tileD, 3);
                    } else if (tile == highlightedPlayerTile) {
                        g.setColor(g.YELLOW);
                        g.drawRect(0, 0, tileD*2, tileD, 3);
                    }
                    g.translate(tileD*2, 0);
                    tile++;
                }
            }
            g.popMatrix();
            g.translate(0, tileD);
        }
    }

    public static void drawTile(AGraphics g, Tile t, float dim, boolean visible) {
	    if (visible) {
	        Board.drawTile(g, dim, t.pip1, t.pip2);
        } else {
	        g.setColor(g.BLACK);
	        g.drawFilledRoundedRect(0, 0,dim*2, dim, dim/4);
	        g.setColor(g.WHITE);
	        g.drawRoundedRect(0, 0, dim*2, dim, 1, dim/4);
        }
    }

    public final Player getWinner() {
        for (Player p : players) {
            if (p.score >= maxScore) {
                return p;
            }
        }
        return null;
    }
    public void onClick() {
        PlayerUser user = (PlayerUser)players[0];
        if (highlightedPlayerTile >= 0) {
            if ((selectedPlayerTile = highlightedPlayerTile) >= 0)
                getBoard().highlightMovesForPiece(user.getTiles().get(selectedPlayerTile));
            else
                getBoard().highlightMovesForPiece(null);
        } else if (board.selectedEndpoint >= 0) {
            user.tile = board.highlightedTile;
            user.endpoint = board.selectedEndpoint;
            user.placement = board.selectedPlacement;
            getBoard().clearSelection();
            synchronized (this) {
                notifyAll();
            }
            selectedPlayerTile = -1;
        }
        redraw();
    }

    public void onDrag(int pickX, int pickY) {
    }

    public abstract void redraw();
}
