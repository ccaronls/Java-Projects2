package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.utils.Reflector;

public class Board2 extends Reflector<Board2> {

	public static class Tree extends Reflector<Tree> {
		Piece p;
		Tree [] ch = new Tree[4]; // 0==right, 1==left, 2==top, 3==bottom
	}
	
	private Tree tree;
	
	static {
		addAllFields(Board.class);
	}
	
	public void draw(AGraphics g) {
	
	}
	
	public List<Move> computeMoves() {
		List<Move> moves = new ArrayList<Move>();
		
		if (tree == null) {
			moves.add(new Move("11,22,33,44,55,66"));
		} else {
			addMovesFromTreeR(moves, tree);
		}
		
		return moves;
	}
	
	private void addMovesFromTreeR(List<Move> moves, Tree t) {
		if (t == null)
			return;
		
		for (int i=0; i<4; i++) {
			
		}
		
	}
	
	/**
	 * 
	 * @param p piece to place
	 * @param x a x position on the board
	 * @param y a y position on the board
	 * @return the move that was replaced by the piece or null if piece could not be placed.
	 * 
	 */
	public void addPiece(Piece p, int x, int y) {
		
	}
	
	public static void drawDie(AGraphics g, float x, float y, float dim, int dotSize, int numDots) {
	    float dd2 = dim/2;
	    float dd4 = dim/4;
	    float dd34 = (dim*3)/4;
	    switch (numDots) {
	    case 1:	    	
	        drawDot(g, x+dd2, y+dd2, dotSize);	    	
	        break;
	    case 2:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        break;
	    case 3:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd2, y+dd2, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        break;
	    case 4:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        break;
	    case 5:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        drawDot(g, x+dd2, y+dd2, dotSize);
	        break;
	    case 6:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        drawDot(g, x+dd4, y+dd2, dotSize);
	        drawDot(g, x+dd34, y+dd2, dotSize);
	        break;
	    }
	}
	
	private static void drawDot(AGraphics g, float x, float y, int dotSize) {
		g.drawFilledOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}
}
