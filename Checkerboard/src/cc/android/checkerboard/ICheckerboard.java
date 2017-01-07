package cc.android.checkerboard;

import java.util.List;

public interface ICheckerboard {

	public static class Piece {
		public int playerNum;
		public int stacks;
		
		public Piece(int playerNum, int stacks) {
			super();
			this.playerNum = playerNum;
			this.stacks = stacks;
		}
	}

	enum MoveType {
		SLIDE, JUMP, JUMP_CAPTURE, STACK
	}
	
	
	public static class Move {
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
	
	void newGame();
	
	Piece getPiece(int rank, int column);
	
	int getRanks();
	
	int getColumns();

	int getNumPlayers();
	
	int getCurPlayerNum();
	
	List<Move> computeMoves(int rank, int col);
	
	void endTurn();
	
	List<Move> executeMove(Move move);
	
	int getWinner();
	
	boolean isOnBoard(int rank, int col);
}
