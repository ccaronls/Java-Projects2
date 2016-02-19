package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.utils.Reflector;

public class Board extends Reflector<Board> {
	
	static {
		addAllFields(Board.class);
	}
	
	public void draw(AGraphics g) {
		for (int i=1; i<numSquares; i++) {
			boolean done = false;
			for (int ii=0; ii<DIM && !done; ii++) {
				for (int iii=0; iii<DIM && !done; iii++) {
					if (board[ii][iii] == i) {
						drawDie(g, ii, iii, 2, 3, square[i]);
						done = true;
						break;
					}
				}
			}
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
		
		if (x<0 || y<0 || x>=DIM || y>=DIM)
			return;
		
		if (moves.size() == 0) {
			// x,y is the center of the piece
			int [] xywh = { x,y,4,2 };
			findSquare(xywh);
			placePiece(xywh[0], xywh[1], p.num1, xywh[0]+2, xywh[1], p.num2, 2);
			return;
		}

		int [] mv = null;
		int bestD = -1;
		for (int [] m : moves) {
			int mx = m[0];
			int my = m[1];
			int mn = m[2];
			if (mn == p.num1 || mn == p.num2) {
    			int dx = Math.abs(mx - x);
    			int dy = Math.abs(my - y);
    			
    			int d2 = dx*dx + dy*dy;
    			if (d2 < 8) {
    				if (mv == null || d2 < bestD) {
    					if (isOpen(mx, my, 2, 4) ||
        	    		    isOpen(mx, my, 4, 2) ||
        	    		    isOpen(mx-2, my, 4, 2) ||
        	    		    isOpen(mx, my-2, 2, 4)) {
        					mv = m;
        					bestD = d2;
    					}
    				}
    			}
			}			
		}
		
		if (mv != null) {

			int mx = mv[0];
			int my = mv[1];
			int mn = mv[2];

			int n0,n1;
			
			if (mn==p.num1) {
				n0=p.num1;
				n1=p.num2;
			} else {
				n0=p.num2;
				n1=p.num1;
			}
			
			moves.remove(mv);
			// if our x,y is 'above' or 'below' mv.xy, then try to place vertically
			int dx = x-mx;
			int dy = y-my;
			
			if (Math.abs(dx) > Math.abs(dy)) {
				// try to place horizontally
				if (dx < 0) {
					// left
					placePiece(mx-2, my, false, n0, n1);
				} else {
					// right
					placePiece(mx,my, false, n0, n1);
				}
			} else {
				// try to place vertically
				if (dy < 0) {
					// above
					placePiece(mx, my-2, true, n0, n1);
				} else {
					// below
					placePiece(mx, my, true, n0, n1);
				}
			}
		}
	}

	private void placePiece(int x, int y, boolean vertical, int n0, int n1) {
		int w,h;
		if (vertical) {
			w=2;h=4;
		} else {
			w=4;h=2;
		}
		int [] xywh = { x,y,w,h };
		findSquare(xywh);
		if (vertical) {
			placePiece(xywh[0], xywh[1], n1, xywh[0], xywh[1]+2, n1, 2);
		} else {
			placePiece(xywh[0], xywh[1], n0, xywh[0]+2, xywh[1], n1, 2);
		}
	}
	
	private boolean isOpen(int x, int y, int w, int h) {
		if (x<1 || y<1 || x+w>=DIM-1 || y+h >= DIM-1)
			return false;
		
		for (int i=0; i<w; i++) {
			for (int ii=0; ii<h; ii++) {
				if (board[x+i][y+ii] != 0)
					return false;
			}
		}
		
		return true;
	}
	
	
	private boolean isLegal(int x, int y, int w, int h) {
		if (x<1 || y<1 || x+w>=DIM-1 || y+h >= DIM-1)
			return false;
		
		for (int i=0; i<w; i++) {
			for (int ii=0; ii<h; ii++) {
				if (board[x+i][y+ii] > 0)
					return false;
			}
		}
		
		return true;
	}
	
	/*
	 * 
	 */
	private void placePiece(int x0, int y0, int n0, int x1, int y1, int n1, int d) {
		
		square[numSquares] = n0;
		
		for (int i=0; i<d; i++) {
			for (int ii=0; ii<d; ii++) {
				assert(board[x0+i][y0+ii] == 0);
				board[x0+i][y0+ii] = numSquares; 
			}
		}
		numSquares++;
		square[numSquares] = n1;
		for (int i=0; i<d; i++) {
			for (int ii=0; ii<d; ii++) {
				assert(board[x1+i][y1+ii] == 0);
				board[x1+i][y1+ii] = numSquares; 
			}
		}
		numSquares++;
		// put a barrier around the piece to prevent adjacencies
		borderRect(x0, y0, d);
		borderRect(x1, y1, d);
		
		if (x0 == x1) {
			// vertical piece, check above and below
			maybeAddMove(x0,y0-d,d,n0);
			maybeAddMove(x1,y1+d,d,n1);
			// check for doubles
			if (n0 == n1) {
				maybeAddMove(x0-d,y0+d/2,d,n0);
				maybeAddMove(x0+d,y0+d/2,d,n1);
			}
		} else if (y0 == y1) {
			// horz piece, check to right and left
			maybeAddMove(x0-d,y0,d,n0);
			maybeAddMove(x1+d,y1,d,n1);
			
			if (n0 == n1) {
				maybeAddMove(x0+d/2,y0-d,d,n0);
				maybeAddMove(x0+d/2,y0+2,d,n1);
			}
		}
	}
	
	private void maybeAddMove(int x, int y, int d, int n) {
		if (isLegal(x,y,d,d)) {
			setRect(x,y,d,d,0);
			moves.add(new int [] {x,y,n});
		}
	}

	private void borderRect(int x, int y, int d) {
		for (int i=-1; i<d+1; i++) {
			if (board[x+i][y-1] == 0)
				board[x+1][y-1] = -1;
			if (board[x+i][y+d+1] == 0)
				board[x+i][y+d+1] = -1;
			if (board[x-1][y+i] == 0)
				board[x-1][y+i] = -1;
			if (board[x+d+1][y+i] == 0)
				board[x+d+1][y+i] = -1;
		}
	}
	
	private void setRect(int x, int y, int w, int h, int n) {
		for (int i=0; i<w; i++) {
			for (int ii=0; ii<h; ii++) {
				board[x+i][y+ii] = n;
			}
		}
	}
	
	/*
	 * return x,y,w,h
	 */
	private void findSquare(int [] xywh) {
		int x = xywh[0];
		int y = xywh[1];
		int w = xywh[2];
		int h = xywh[3];
		
		if (x<1)
			x=1;
		else if (x+w>=DIM-1)
			x=DIM-2-w;
		if (y<1)
			y=1;
		else if (y+h>=DIM-1)
			y=DIM-2-h;
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
	
	enum Direction {
		UP,DOWN,RIGHT,LEFT;
	}
	
	private final List<int[]> moves = new ArrayList<int[]>();
	
	int [][] board = new int[DIM][DIM]; // unique indices into square
	
	int [] square = new int[DIM*2];
	int numSquares = 1;
	
	final static int DIM = 100;
}
