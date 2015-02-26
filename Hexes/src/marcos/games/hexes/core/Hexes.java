package marcos.games.hexes.core;

import java.util.*;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Hexes extends Reflector<Hexes> {

	public String getVersion() {
		return "1.0";
	}

	static {
		omitField(Hexes.class, "playerChoice");
		addAllFields(Hexes.class);
	}
	
	public final static int MAX_PLAYERS = 4;
	
	private final Player [] players = new Player[MAX_PLAYERS];
	private int numPlayers=0;
	private int curPlayer=0;
	private final Board board = new Board();
	private State state = State.READY;
	
	public Hexes() {}
	
	public final void initPlayers(Player ... players) {
		Utils.println("init players");
		if (state != State.READY)
			throw new RuntimeException("Already in a game");
		if (players.length < 2 || players.length > MAX_PLAYERS)
			throw new RuntimeException("Invalid number of players '" + players.length + "'");
		numPlayers = 0;
		for (Player p : players) {
			this.players[numPlayers++] = p;
		}
	}
	
	public final void newGame() {
		Utils.println("New Game");
		if (numPlayers < 2)
			throw new RuntimeException("Not enough players");
		state = State.READY;
		board.init();
		curPlayer = 0;
		for (int i=0; i<numPlayers; i++)
			players[i].init();
	}
	
	private int playerChoice = -1;
	public final void runGame() {
		if (isGameOver()) {
			state = State.READY;
			return;
		}
		if (state == State.READY)
			state = State.FIRST_MOVE;
		Player player = players[curPlayer];
		if (playerChoice < 0) {
    		ArrayList<Integer> choices = board.computeUnused();
    		// reduce the choices
    		for (int pIndex : choices) {
    			Piece p = board.getPiece(pIndex);
    			for (int i=0; i<3; i++) {
    				Piece pp = board.getAdjacent(p, i);
    				if (pp != null) {
    					// TODO: 
    				}
    			}
    		}
    		board.setPieceChoices(choices);
    		int pIndex = player.choosePiece(this, choices);
    		if (choices.indexOf(pIndex) >= 0) {
    			playerChoice = pIndex;
        		board.setPieceChoices(null);
    		}
		} else {
			ArrayList<Shape> choices = computeShapeChoices(playerChoice);
			Iterator<Shape> it = choices.iterator();
			while (it.hasNext()) {
				Shape s = it.next();
				if (player.getShapeCount(s) == 0)
					it.remove();
			}
			Shape shape = null;
			if (choices.size() == 1) {
				shape = choices.get(0);
			} else if (choices.size() > 1) {
				shape = player.chooseShape(this, choices.toArray(new Shape[choices.size()]));
			} else {
				cancel();
			}
			if (shape != null && playerChoice >= 0 && player.getShapeCount(shape) > 0) {
				board.setPiece(playerChoice, curPlayer+1, shape);
				onPiecePlayed(playerChoice);
				board.shapeSearch(curPlayer+1);
				playerChoice = -1;
				player.score = board.computePlayerPoints(curPlayer+1);
				curPlayer = (curPlayer+1) % numPlayers;
				if (curPlayer == 0)
					state = State.PLAYING;
				player.decrementPiece(shape);
				if (isGameOver()) {
					int winner = 0;
					int winnerScore = players[0].score;
					for (int i=1; i<numPlayers; i++) {
						if (players[i].score > winnerScore) {
							winner = i;
							winnerScore = players[i].score;
						}
					}
					onGameOver(winner+1);
				} else if (state == State.PLAYING) {
					board.grow();
				}
			}
		}

	}
	
	private ArrayList<Shape> computeShapeChoices(int pieceIndex) {
		Piece p = board.getPiece(pieceIndex);
		ArrayList<Shape> choices = new ArrayList<Shape>();
		choices.add(Shape.DIAMOND);
		choices.add(Shape.TRIANGLE);
		choices.add(Shape.HEXAGON);
		for (int i=0; i<3; i++) {
			Piece pp = board.getAdjacent(p, i);
			if (pp != null && pp.player > 0) {
				choices.remove(pp.type);
			}
		}
		return choices;
	}

	public final boolean isGameOver() {
		return players[curPlayer].countPieces() == 0;
	}

	public final int getNumPlayers() {
		return numPlayers;
	}

	public final Player getCurPlayer() {
		if (curPlayer < 0)
			return null;
		return players[curPlayer];
	}

	public final Board getBoard() {
		return board;
	}
	
	/**
	 * Cancel the piece choice the player has made.  Only valid when player.chooseShape is processing (which must return 
	 */
	public void cancel() {
		playerChoice = -1;
	}
	
	/**
	 * Return player.  The first is 1
	 * @param num
	 * @return
	 */
	public final Player getPlayer(int num) {
		return players[num-1];
	}
	
	protected void onGameOver(int winner) {}
	
	protected void onPiecePlayed(int pieceIndex) {}

}
