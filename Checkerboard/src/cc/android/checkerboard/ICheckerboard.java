package cc.android.checkerboard;

import java.util.List;

import cc.lib.game.AColor;

public interface ICheckerboard {

	public static class Piece {
		public final AColor color;
		public final int stacks;
		
		public Piece(AColor color, int stacks) {
			super();
			this.color = color;
			this.stacks = stacks;
		}
	}

	enum MoveType {
		
	}
	
	
	public static interface IMove {
		
	}
	
	void newGame();
	
	Piece getPiece(int rank, int column);
	
	int getRanks();
	
	int getColumns();

	int getNumPlayers();
	
	AColor getColor(int playerNum);
	
	int getCurPlayerNum();
	
	List<IMove> computeMoves(int rank, int col);
	
	void endTurn();
	
	List<IMove> executeMove(IMove move);
}
