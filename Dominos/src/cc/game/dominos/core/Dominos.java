package cc.game.dominos.core;

import java.util.*;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Dominos extends Reflector<Dominos> {

    static {
        addAllFields(Dominos.class);
    }

    @Override
    protected int getMinVersion() {
        return 1;
    }

    Player [] players = new Player[0];
	LinkedList<Tile> pool = new LinkedList<Tile>();
    int maxNum;
    int maxScore;
    int turn;
    Board board = new Board();

	public void initPlayers(Player ... players) {
        this.players = players;
	}
	
	public void newGame(int maxPipNum, int maxScore) {
        this.maxNum = maxPipNum;
        this.maxScore = maxScore;
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
		    Move mv = p.chooseMove(moves);
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
            p.score += pts;
            onPlayerPoints(p, pts);
            newRound();
            onEndRound();
        } else {
            nextTurn();
        }
	}

	public final boolean isGameOver() {
	    for (Player p : players) {
	        if (p.score >= maxScore) {
	            return true;
            }
        }
        return false;
    }

    protected void onTilePlaced(Player p, Move mv) {
    }

    protected void onTileFromPool(Player p, Tile pc) {
    }

    protected void onKnock(Player p) {
    }

    protected void onEndRound() {}

    protected void onPlayerPoints(Player p, int pts) {}

    public final   void draw(AGraphics g) {
	    float w = g.getViewportWidth();
	    float h = g.getViewportHeight();

        g.ortho(0, w, 0, h);

        g.pushMatrix();
        {
            g.translate(w / 2, h / 2);

            // assume 4 players for now
            // allow 10% of screen for the players' tiles

            float tileH = 0.1f * Math.min(w, h);
            float tileY = Math.min(w, h) / 2 - tileH;
            float tileX = tileH;
            float tileW = w-tileH * 2;

            boolean visible = true;

            g.pushMatrix();
            for (int i = 0; i < 4; i++) {
                g.translate(tileX, tileY);
                drawPlayerTiles(g, players[i], visible, tileW, tileH);
                g.translate(-tileX, -tileY);
                g.rotate(90);
                visible = false;
            }
            g.popMatrix();
        }
        g.popMatrix();
    }

    private void drawPlayerTiles(AGraphics g, Player p, boolean visible, float w, float h) {
	    float tileD = h;
        int tilesPerRow = Math.round(w / (tileD * 2));

        int numRows = (int)Math.ceil(Math.sqrt((double) p.tiles.size()/tilesPerRow));
        tilesPerRow *= numRows;
        tileD /= numRows;

        int tile = 0;
        for (int i=0; i<numRows; i++) {
            g.pushMatrix();
            for (int ii=0; ii<tilesPerRow; ii++) {
                if (tile < p.tiles.size()) {
                    Tile t = p.tiles.get(tile++);
                    g.pushMatrix();
                    g.scale(0.95f, 0.95f);
                    Board.drawTile(g, tileD, t.pip1, t.pip2);
                    g.popMatrix();
                    g.translate(tileD*2, 0);
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
}
