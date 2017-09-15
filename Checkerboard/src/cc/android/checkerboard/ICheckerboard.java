package cc.android.checkerboard;

import java.util.List;

public interface ICheckerboard {

	enum MoveType {
		END, SLIDE, JUMP, JUMP_CAPTURE, STACK
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
