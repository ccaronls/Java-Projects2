package cc.android.checkerboard;

import java.util.List;

import cc.lib.utils.Reflector;

public interface ICheckerboard {

	class Piece extends Reflector<Piece> {

        static {
            addAllFields(Piece.class);
        }

		public int playerNum;
		public int stacks;

		public Piece(int playerNum, int stacks) {
			this.playerNum = playerNum;
			this.stacks = stacks;
		}
	}

	enum MoveType {
		END, SLIDE, JUMP, JUMP_CAPTURE, STACK
	}
	
	
	class Move extends Reflector<Move> {

        static {
            addAllFields(Move.class);
        }

		public final MoveType type;
		public final int startRank, startCol;
		public final int endRank, endCol;
		public final int captureRank, captureCol;
		public final int playerNum;

		public Move(MoveType type, int startRack, int startCol, int endRank, int endCol, int captureRank, int captureCol, int playerNum) {
			this.type = type;
			this.startRank = startRack;
			this.startCol = startCol;
			this.endRank = endRank;
			this.endCol = endCol;
			this.captureCol = captureCol;
			this.captureRank = captureRank;
			this.playerNum = playerNum;
		}
	}
	
	/**
	 * initialize the game
	 * 
	 */
	void newGame();
	
	/**
	 * Get a piece for a rank/column.  Implementor can attach its own meta data to each piece
	 * 
	 * @param rank
	 * @param column
	 * @return
	 */
	Piece getPiece(int rank, int column);
	
	int getRanks();
	
	int getColumns();

	int getNumPlayers();
	
	int getCurPlayerNum();
	
	/**
	 * List of moves available for a specific rank/column. Some moves may lead to new moves
	 * @param rank
	 * @param col
	 * @return
	 */
	List<Move> computeMoves(int rank, int col);
	
	/**
	 * Emmediately end the current player's turn and advance to next player
	 */
	void endTurn();
	
	/**
	 * Execute a move and return list of new moves for same player.  An empty list means no more moves.
	 * 
	 * @param move
	 * @return
	 */
	List<Move> executeMove(Move move);
	
	/**
	 * Return 0 based index of winning player or -1 for no winner
	 * 
	 * @return
	 */
	int getWinner();
	
	boolean isOnBoard(int rank, int col);
}
