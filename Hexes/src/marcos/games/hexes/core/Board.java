package marcos.games.hexes.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.*;
import cc.lib.math.*;
import cc.lib.utils.Reflector;

public class Board extends Reflector<Board> {

	static {
		addAllFields(Board.class);
	}
	
	private List<Vertex> verts = new ArrayList<Vertex>();
	private List<Piece> pieces = new ArrayList<Piece>();
	private List<Edge> edges = new ArrayList<Edge>();
	private int minX, minY, maxX, maxY;
	private int highlightX, highlightY;
	private final IdGenerator gen = new IdGenerator();
	@Omit
	private List<Integer> pieceChoices = null;

	private static class Outline {
		HashSet<Edge> edges = new HashSet<Edge>();
		float cx, cy;
		int player;
		Shape shape;
	}

	@Omit
	private List<Outline> outlines = new ArrayList<Outline>();
	
	public synchronized void init() {
		verts.clear();
		verts.add(new Vertex(0,0));
		verts.add(new Vertex(2,0));
		verts.add(new Vertex(1,2));
		verts.add(new Vertex(1, -2));
		addPiece(0,1,2);
		addPiece(0,1,3);
		computeMinMax();
	}
	
	boolean isPieceUpward(Piece p) {
		return verts.get(p.v[0]).y < verts.get(p.v[2]).y;
	}
	
	synchronized void addPiece(int ... v) {
		int index = pieces.size();
		pieces.add(new Piece(v[0], v[1], v[2]));
		addEdge(v[0], v[1], index);
		addEdge(v[1], v[2], index);
		addEdge(v[2], v[0], index);
		verts.get(v[0]).addPiece(index);
		verts.get(v[1]).addPiece(index);
		verts.get(v[2]).addPiece(index);
	}
	
	synchronized void addEdge(int from, int to, int piece) {
		Edge e = new Edge(from, to);
		int index = Collections.binarySearch(edges, e);
		if (index < 0) {
			edges.add(-(index+1), e);
		} else {
			e = edges.get(index);
		}
		e.addPiece(piece);
	}

	synchronized Edge getEdge(int from, int to) {
		Edge e = new Edge(from, to);
		int index = Collections.binarySearch(edges, e);
		if (index < 0)
			return null;
		return edges.get(index);
	}
	
	private void computeMinMax() {
		minX=minY=maxX=maxY=0;
		for (Vertex v : verts) {
			minX = Math.min(minX, v.x);
			maxX = Math.max(maxX, v.x);
			minY = Math.min(minY, v.y);
			maxY = Math.max(maxY, v.y);
		}
	}
	
	public synchronized int draw(AGraphics g) {
		int highlighted = -1;
		float width = maxX-minX;
		float height = maxY-minY;
		float cx = minX + width/2;
		float cy = minY + height/2;
		float dim = Math.max(width, height);
		if (dim < 10)
			dim = 10;
		g.ortho(cx-dim/2, cx+dim/2, cy-dim/2, cy+dim/2);
//		g.ortho(minX, maxX, minY, maxY);
		Vector2D h = g.screenToViewport(highlightX, highlightY);
		int index = 0;
		for (Piece p: pieces) {
			if (drawPiece(g, p, h.getX(), h.getY())) {
				if (pieceChoices == null || pieceChoices.indexOf(index) >= 0)
					highlighted = index;
			}
			index++;
		}
		g.begin();
		for (Outline o : outlines) {
			g.pushMatrix();
			g.translate(o.cx, o.cy);
			g.scale(0.9f);
			switch (o.shape) {
				case DIAMOND:
					g.setColor(GColor.GREEN);
					break;
				case HEXAGON:
					g.setColor(GColor.ORANGE);
					break;
				case NONE:
					g.setColor(GColor.BLACK);
					break;
				case TRIANGLE:
					g.setColor(GColor.CYAN);
					break;
			}
			g.begin();
			for (Edge e : o.edges) {
				Vertex v = verts.get(e.from);
				g.vertex(v.getX() - o.cx, v.getY() - o.cy);
				v = verts.get(e.to);
				g.vertex(v.getX() - o.cx, v.getY() - o.cy);
			}
			g.drawLines();
			g.popMatrix();
		}
		
		if (highlighted >= 0) {
			Piece p = pieces.get(highlighted);
			g.setColor(GColor.MAGENTA);
			g.begin();
			for (int i=0; i<3; i++)
				g.vertex(verts.get(p.v[i]));
			g.setLineWidth(2);
			g.drawLineLoop();
		}
		return highlighted;
	}
	
	GColor getColorForPlayer(int playerNum, AGraphics g) {
		switch (playerNum) {
			case 1:
				return GColor.RED;
			case 2:
				return GColor.BLUE;
		}
		return GColor.BLACK;
	}
	
	private IVector2D getMidpoint(int ... vIndices) {
		MutableVector2D mv = new MutableVector2D();
		if (vIndices.length > 0) {
    		for (int vIndex : vIndices) {
    			IVector2D v = verts.get(vIndex);
    			mv.addEq(v);
    		}
    		mv.scaleEq(1.0f / vIndices.length);
		}
		return mv;
	}
	
	private boolean drawPiece(AGraphics g, Piece p, float hx, float hy) {
		g.clearMinMax();
		if (p.player > 0) {
			g.setColor(getColorForPlayer(p.player, g));
			g.begin();
			for (int v : p.v)
				g.vertex(verts.get(v));
			g.drawTriangles();
			g.setColor(GColor.YELLOW);
			IVector2D c = getMidpoint(p.v);
			switch (p.type) {
				case NONE:
					break;
				case DIAMOND: {
					float r = 0.2f;
					g.begin();
					g.vertex(c.getX(), c.getY()+r*2);
					g.vertex(c.getX()-r, c.getY());
					g.vertex(c.getX()+r, c.getY());
					g.vertex(c.getX(), c.getY()-r*2);
					g.setColor(GColor.YELLOW);
					g.drawTriangleStrip();
					break;
				}
				case TRIANGLE: {
					float r = 0.3f;
					g.begin();
					if (isPieceUpward(p)) {
						g.vertex(c.getX()-r, c.getY()-r);
						g.vertex(c.getX()+r, c.getY()-r);
						g.vertex(c.getX(), c.getY()+r*2);
					} else {
						g.vertex(c.getX()-r, c.getY()+r);
						g.vertex(c.getX()+r, c.getY()+r);
						g.vertex(c.getX(), c.getY()-r*2);
					}
					g.drawTriangles();
					break;
				}
				case HEXAGON: {
					float r = 0.2f;
					g.begin();
					g.vertex(c);
					g.vertex(c.getX()-r, c.getY());
					g.vertex(c.getX()-r/2, c.getY()-r);
					g.vertex(c.getX()+r/2, c.getY()-r);
					g.vertex(c.getX()+r, c.getY());
					g.vertex(c.getX()+r/2, c.getY()+r);
					g.vertex(c.getX()-r/2, c.getY()+r);
					g.vertex(c.getX()-r, c.getY());
					g.drawTriangleFan();
					break;
				}
			}
			// DEBUG
			//g.setColor(g.BLACK);
			//g.drawJustifiedString(c.getX(), c.getY(), Justify.CENTER, Justify.CENTER, "[" + p.getGroupId() + "]" + p.getGroupShape());  

		}
		g.begin();
		for (int v : p.v)
			g.vertex(verts.get(v));
		g.setColor(GColor.WHITE);
		g.setLineWidth(2);
		g.drawLineLoop();
		return Utils.isPointInsidePolygon(hx, hy, new IVector2D[] { verts.get(p.v[0]), verts.get(p.v[1]), verts.get(p.v[2]) }, 3);
	}

	/**
	 * Grow the board such that new new moves are available
	 */
	public synchronized void grow() {
		ArrayList<int []> newPieces = new ArrayList<int[]>();
		for (Piece p : pieces) {
			if (p.getPlayer() == 0)
				continue;
			//Vertex v0 = verts.get(p.v0);
			Vertex v1 = verts.get(p.v[1]);
			Vertex v2 = verts.get(p.v[2]);
			if (getEdge(p.v[0], p.v[1]).getNum() < 2) {
				int vIndex = -1;
				if (isPieceUpward(p)) {
					// grow down
					Utils.println("grow down");
					vIndex = getVertex(v2.x, v1.y-2);
				} else {
					// grow up
					Utils.println("grow up");
					vIndex = getVertex(v2.x, v1.y+2);
				}
				//addPiece(p.v0, p.v1, v);
				newPieces.add(new int[] { p.v[0], p.v[1], vIndex });
			}
			if (getEdge(p.v[1], p.v[2]).getNum()<2) {
				// grow right
				Utils.println("grow right");
				int v = getVertex(v2.x+2, v2.y);
				//addPiece(p.v2, v, p.v1);
				newPieces.add(new int[] { p.v[2], v, p.v[1] });

			}
			if (getEdge(p.v[2], p.v[0]).getNum()<2) {
				// grow left
				Utils.println("grow left");

				int v = getVertex(v2.x-2, v2.y);
				//addPiece(v, p.v2, p.v0);
				newPieces.add(new int[] { v, p.v[2], p.v[0] });
			}
		}
		for (int i=0; i<newPieces.size(); i++) {
			addPiece(newPieces.get(i));
		}
		computeMinMax();
		//Utils.println("verts");
		//Utils.printCollection(verts);
		//Utils.println("edges");
		//Utils.printCollection(edges);
		//Utils.println("pieces");
		//Utils.printCollection(pieces);
	}
	
	private int getVertex(int x, int y) {
		Vertex v = new Vertex(x,y);
		int index = verts.indexOf(v);
		if (index < 0) {
			index = verts.size();
			verts.add(v);
		}
		return index;
	}
	
	public synchronized void setHighlighted(int x, int y) {
		this.highlightX = x;
		this.highlightY = y;
	}

	public Piece getPiece(int index) {
		return pieces.get(index);
	}
	
	// identify all shapes for a player maximizing for points
	public synchronized void shapeSearch(final int player) {
		// initialize everything
		Iterator<Outline> it = outlines.iterator();
		while (it.hasNext()) {
			Outline o = it.next();
			if (o.player == player)
				it.remove();
		}
		for (Piece p : pieces) {
			if (p.player == player) {
				gen.putBack(p.groupId);
    			p.groupId = 0;
    			p.groupShape = Shape.NONE;
			}
		}
		// find all the hexagons
		while (true) {
			int bestHex = -1;
			int bestPts = 0;
    		for (Piece p : pieces) {
    			if (p.player == player) {
    				int [] pv = checkHexagon(p);
    				if (pv[0] > bestPts) {
    					bestHex = pv[1];
    					bestPts = pv[0];
    				}
    			}
    		}
    		if (bestHex >= 0) {
    			// mark the hex
    			Vertex v = verts.get(bestHex);
    			int id = gen.nextId();
    			for (int i=0; i<6; i++) {
    				Piece p = pieces.get(v.p[i]);
    				p.groupId = id;
    				p.groupShape = Shape.HEXAGON;
    				// add player edges that do not have bestHex as an endpoint
    				Outline o = new Outline();
    				outlines.add(o);
    				o.player = player;
    				o.shape = Shape.HEXAGON;
    				o.cx = v.getX();
    				o.cy = v.getY();
    				for (int ii=0; ii<3; ii++) {
    					int iii = (ii+1)%3;
    					if (p.v[ii] != bestHex && p.v[iii] != bestHex) {
    						o.edges.add(getEdge(p.v[ii], p.v[iii]));
    					}
    				}
    			}
    		} else {
    			break; // no more hexes
    		}
		}
		
		// find all the triangles
		while (true) {
			Piece bestTri = null;
			int bestPts = 0;
			for (Piece p :pieces) {
				if (p.player == player && p.groupId == 0) {
					int pts = checkTriangle(p);
					if (pts > bestPts) {
						bestTri = p;
						bestPts = pts;
					}
				}
			}
			if (bestTri != null) {
				// mark as a triangle
				int id = gen.nextId();
				bestTri.groupId = id;
				bestTri.groupShape = Shape.TRIANGLE;
				Outline o = new Outline();
				outlines.add(o);
				for (int i=0; i<3; i++) {
					Piece pp = getAdjacent(bestTri, i);
					pp.groupId = id;
					pp.groupShape = Shape.TRIANGLE;
					o.player = player;
					o.shape = Shape.TRIANGLE;
					IVector2D mp = getMidpoint(bestTri.v);
					o.cx = mp.getX();
					o.cy = mp.getY();
					o.edges.addAll(getPieceEdges(pp));
				}
				o.edges.removeAll(getPieceEdges(bestTri));
			} else {
				break;
			}
		}
		
		// find all the diamonds
		while (true) {
			Piece [] bestDiamond = null;
			int bestPts = 0;
			int edgeIndex=0; // edge in p to not include in playerShapes
			for (Piece p : pieces) {
				if (p.player == player && p.groupId == 0) {
					for (int i=0; i<3; i++) {
						Piece pp = getAdjacent(p, i);
						if (pp != null && pp.player == player && pp.groupId == 0) {
							int pts = p.type.ordinal() + pp.type.ordinal();
							if (pts > bestPts) {
								bestPts = pts;
								bestDiamond = new Piece[] { p, pp };
								edgeIndex = i;
							}
						}
					}
				}
			}
			if (bestDiamond != null) {
				// mark
				int id = gen.nextId();
				Outline o = new Outline();
				outlines.add(o);
				for (int i=0; i<2; i++) {
					bestDiamond[i].groupId = id;
					bestDiamond[i].groupShape = Shape.DIAMOND;
					o.player = player;
					o.shape = Shape.DIAMOND;
					o.edges.addAll(getPieceEdges(bestDiamond[i]));
				}
				Edge e = getPieceEdges(bestDiamond[0]).get(edgeIndex);
				IVector2D mp = getMidpoint(e.from, e.to);
				o.cx = mp.getX();
				o.cy = mp.getY();
				o.edges.remove(e);
			} else {
				break;
			}
		}
	}
	
	List<Edge> getPieceEdges(Piece p) {
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (int i=0; i<3; i++) {
			int ii = (i+1)%3;
			edges.add(getEdge(p.v[i], p.v[ii]));
		}
		return edges;
	}
	
	Piece getAdjacent(Piece p, int n) {
		int nn = (n+1)%3;
		Edge e = getEdge(p.v[n], p.v[nn]);
		Piece pp = pieces.get(e.p[0]);
		if (pp == p) {
			if (e.getNum() < 2)
				return null;
			pp = pieces.get(e.p[1]);
		}
		assert(pp != p);
		return pp;
	}
	
	private int checkTriangle(Piece p) {
		int pts = p.type.ordinal();
		for (int i=0; i<3; i++) {
			int ii=(i+1)%3;
			Edge e = getEdge(p.v[i], p.v[ii]);
			if (e.getNum() < 2)
				return 0;
			Piece p2 =  pieces.get(e.p[0]);
			if (p2 == p) {
				p2 = pieces.get(e.p[1]);
			}
			if (p2.player == p.player && p2.groupId == 0) {
				pts += p2.type.ordinal();
			} else {
				return 0;
			}
		}
		return pts;
	}

	// return vertex of highest value hexagon
	private int [] checkHexagon(Piece p) {
		int v = 0;
		int pts0 = checkHexagonVertex(p.v[0], p.player);
		int pts1 = checkHexagonVertex(p.v[1], p.player);
		int pts = 0;
		if (pts0 < pts1) {
			pts = pts1;
			v = p.v[1];
		} else {
			pts = pts0;
			v = p.v[0];
		}
		
		int pts2 = checkHexagonVertex(p.v[2], p.player);
		if (pts2 > pts) {
			v = p.v[2];
			pts = pts2;
		}
		return new int[] {  pts, v };
	}

	private int checkHexagonVertex(int vIndex, int playerNum) {
		Vertex v = verts.get(vIndex);
		if (v.getNum() < 6)
			return 0;
		int pts = 0;
		for (int i=0; i<v.getNum(); i++) {
			Piece p = pieces.get(v.p[i]);
			if (p.player == playerNum && p.groupId == 0) {
				pts += p.type.ordinal();
			} else {
				return 0;
			}
		}
		return pts;
	}
	
	public synchronized final int computePlayerPoints(int player) {
		int points = 0;
		for (Piece p : pieces) {
			if (p.player == player) {
				points += p.type.ordinal() * p.groupShape.ordinal();
			}
		}
		return points;
	}

	public final ArrayList<Integer> computeUnused() {
		ArrayList<Integer> choices = new ArrayList<Integer>();
		int index = 0;
		for (Piece p : pieces) {
			if (p.player == 0) {
				choices.add(index);
			}
			index++;
		}
		return choices;
	}

	synchronized final void setPieceChoices(List<Integer> choices) {
		pieceChoices = choices;
	}

	synchronized final void setPiece(int pIndex, int playerNum, Shape shape) {
		Piece p = pieces.get(pIndex);
		p.player = playerNum;
		p.type = shape;
	}
}
