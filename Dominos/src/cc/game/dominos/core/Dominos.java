package cc.game.dominos.core;

import java.util.*;

import cc.lib.game.AGraphics;
import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class Dominos extends Reflector<Dominos> {

	Player [] players;	
	LinkedList<Piece> pool = new LinkedList<Piece>();
    int maxNum;
    int turn;
    DominosBoard board = new DominosBoard();

	public void initPlayers(Player ... players) {
        this.players = players;
	}
	
	public void newGame(int maxNum) {
        this.maxNum = maxNum;
        pool.clear();
        for (int i=1; i<=maxNum; i++) {
            for (int ii=i; ii<=maxNum; ii++) {
                pool.add(new Piece(i, ii));
            }
        }
        for (Player p : players) {
            p.tiles.clear();
            p.score = 0;
        }
        board.clear();
    }

    public final int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public final DominosBoard getBoard() {
        return board;
    }

    public void setBoard(DominosBoard board) {
        this.board = board;
    }

    private void newRound() {

        for (Player p : players) {
            pool.addAll(p.tiles);
            p.tiles.clear();
        }

        pool.addAll(board.collectPieces());
        Utils.shuffle(pool);

        for (Player p : players) {
            for (int i=0; i<7; i++)
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
                Piece pc = players[p].removeTile(i, i);
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

    protected void onPiecePlaced(Player player, Piece pc) {}

	public void runGame() {
		Player p = players[turn];

		List<Move> moves = new ArrayList<>();
		for (Piece pc : p.tiles) {
		    moves.addAll(board.findMovesForPiece(pc));
        }

        while (moves.size() == 0) {
		    if (pool.size() == 0) {
                // player knocks
                onKnock(p);
                break;
            }

		    Piece pc = pool.removeFirst();
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
            }
        }
        nextTurn();
	}

    public void onTilePlaced(Player p, Move mv) {
    }

    public void onTileFromPool(Player p, Piece pc) {
    }

    public void onKnock(Player p) {
    }


}
