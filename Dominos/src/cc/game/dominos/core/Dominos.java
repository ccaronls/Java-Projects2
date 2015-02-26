package cc.game.dominos.core;

import java.util.*;

import cc.lib.game.AGraphics;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class Dominos extends Reflector<Dominos> {

	Player [] players;	
	List<Piece> pool = new LinkedList<Piece>();
	Piece root;
	int minX, minY;
	int maxX, maxY;
	
	public void initPieces(int maxNum) {
		pool.clear();
		for (int i=1; i<=maxNum; i++) {
			for (int ii=i; ii<=maxNum; ii++) {
				pool.add(new Piece(i, ii));
			}
		}
	}
	
	public void initPlayers(Player ... players) {
		
	}
	
	public void newGame() {
		
	}

	public void runGame() {
		
	}
	
	public void draw(AGraphics g) {
		// visit each piece and push into a list
		List<Piece> pieces = new ArrayList<Piece>();
		minX = minY = maxX = maxY = 0;
		addPieceR(root, pieces, 0, 0, 0, 0);
		
		// now grow the board by 4 units in each direction to make room for new pieces
		minX -= 4;
		minY -=4;
		maxX += 4;
		maxY += 4;
		
		for (Piece p : pieces) {
			p.x -= minX;
			p.y -= minY;
		}
		maxX -= minX;
		maxY -= minY;
		minX = minY= 0;
		
		g.ortho(0, maxX, 0, maxY);
		for (Piece p : pieces) {
			g.pushMatrix();
			g.translate(p.x, p.y);
			g.rotate(p.dir.angle);
			g.translate(-p.x, -p.y);
			drawPiece(g, p.x-2, p.y-1, 4, 2, p.num1, p.num2);
			g.popMatrix();
		}
	}
	
	private void addPieceR(Piece p, List<Piece> pieces, int x, int y, int xOffset, int yOffset) {
		if (p != null) {
			pieces.add(p);
			p.x = x;
			p.y = y;
			if (p.dir.horizontal) {
				p.x += xOffset;
				p.y += yOffset;
				minX = Math.min(minX, x-2);
				minY = Math.min(minY, y-1);
				maxX = Math.max(maxX, x+2);
				maxY = Math.max(maxY, y+1);
				addPieceR(p.left, 	pieces, x-3, y  , -1,  0);
				addPieceR(p.right, 	pieces, x+3, y  ,  1,  0);
				addPieceR(p.top, 	pieces, x  , y-3,  0,  0);
				addPieceR(p.bottom, pieces, x  , y+3,  0,  0);
			} else {
				minX = Math.min(minX, x-1);
				minY = Math.min(minY, y-2);
				maxX = Math.max(maxX, x+1);
				maxY = Math.max(maxY, y+2);
				addPieceR(p.left,   pieces, x-3, y  ,  0,  0);
				addPieceR(p.right,  pieces, x+3, y  ,  0,  0);
				addPieceR(p.top,    pieces, x  , y-4,  0,  1);
				addPieceR(p.bottom, pieces, x  , y+4,  0, -1);
			}
		}
	}
	
	protected void drawPiece(AGraphics g, int x, int y, int w, int h, int num1, int num2) {
		g.setColor(g.BLACK);
		float radius = 0.25f * Math.min(w, h);
		int thickness = 3;
		g.drawFilledRoundedRect(x, y, w, h, radius);
		g.setColor(g.WHITE);
		g.drawRoundedRect(x, y, w, h, thickness, radius);
		g.drawLine(x+w/2, y, x+w/2, y+h, thickness);
		g.pushMatrix();
		g.translate(x+w/4, y+h/2);
		drawArrangement(g, dotArrangement[num1]);
		g.translate(w/2, 0);
		drawArrangement(g, dotArrangement[num2]);
		g.popMatrix();
	}
	
	
	private void drawArrangement(AGraphics g, Arrangement arrangement) {
		// TODO Auto-generated method stub
		
	}


	public static class Arrangement {

		int [][] arr = new int[3][];
		
		public Arrangement(int i, int j, int k, int l, int m, int n, int o, int p, int q) {
			arr[0] = new int[] { i,j,k };
			arr[1] = new int[] { l,m,n };
			arr[2] = new int[] { o,p,q };
		}
		
	};
	
	public static final Arrangement [] dotArrangement = new Arrangement[] {
		new Arrangement( 0, 0, 0,
    				     0, 1, 0,
    				     0, 0, 0),
				     
	    new Arrangement( 1, 0, 0,
        			     0, 0, 0,
        			     0, 0, 1),
    			     
	    new Arrangement( 1, 0, 0,
    			     	 0, 1, 0,
    			     	 0, 0, 1),
			     	 
	    new Arrangement( 1, 0, 1,
    			     	 0, 0, 0,
    			     	 1, 0, 1),
			     	 
	    new Arrangement( 1, 0, 1,
    			     	 0, 1, 0,
    			     	 1, 0, 1),
			     	 
	    new Arrangement( 1, 0, 1,
    			     	 1, 0, 1,
    			     	 1, 0, 1),
			     	 
	    new Arrangement( 1, 0, 1,
    			     	 1, 1, 1,
    			     	 1, 0, 1),
			     	 
	    new Arrangement( 1, 1, 1,
    			     	 1, 0, 1,
    			     	 1, 1, 1),
			     	 
	    new Arrangement( 1, 1, 1,
    			     	 1, 1, 1,
    			     	 1, 1, 1),
	};
}
