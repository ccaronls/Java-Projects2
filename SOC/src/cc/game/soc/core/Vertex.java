package cc.game.soc.core;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;

/**
 * 
 * @author Chris Caron
 * 
 */
public final class Vertex extends Reflector<Vertex> implements IVector2D {

	static {
		addAllFields(Vertex.class);
	}
	
	private final static int VERTEX_FLAG_ADJACENT_TO_LAND = 0x1 << 0;
	private final static int VERTEX_FLAG_ADJACENT_TO_WATER = 0x1 << 1;
	private final static int VERTEX_FLAG_PROMOTED_KNIGHT = 0x1 << 2;
	
	private float	x, y;						// position of this vertex
	private int		player;						// 0 when unowned, otherwise index to the player
	private VertexType type = VertexType.OPEN;
	private int[]   cells = new int[3];
	private int     numCells;
	private int[]	adjacent = new int[3];		// indices of adjacent verts
	private int		numAdj;						// number of adjacents verts (can be 2 or 3 for hexagons)
	private int 	flags;						// true when this vertex is valid for placing a structure

	public Vertex() {}
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	Vertex(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * 
	 * 
	 */
	void reset() {
		player = 0;
		type = VertexType.OPEN;
	}

	/**
	 * 
	 * @return
	 */
	public final int[] getAdjacent() {
		return adjacent;
	}

	/**
	 * 
	 * @return
	 */
	public int getNumAdjacent() {
		return numAdj;
	}

	/**
	 * 
	 * @return
	 */
	public boolean canPlaceStructure() {
		return 0 != (flags & VERTEX_FLAG_ADJACENT_TO_LAND);
	}

	public void setAdjacentToLand(boolean adjacent) {
		setFlag(VERTEX_FLAG_ADJACENT_TO_LAND, adjacent);
	}

	public void setAdjacentToWater(boolean adjacent) {
		setFlag(VERTEX_FLAG_ADJACENT_TO_WATER, adjacent);
	}
	
	public void setPromotedKnight(boolean promoted) {
		setFlag(VERTEX_FLAG_PROMOTED_KNIGHT, promoted);
	}
	
	public boolean isAdjacentToLand() {
		return 0 != (flags & VERTEX_FLAG_ADJACENT_TO_LAND);
	}
	
	public boolean isAdjacentToWater() {
		return 0 != (flags & VERTEX_FLAG_ADJACENT_TO_WATER);
	}
	
	public boolean isPromotedKnight() {
		return 0 != (flags & VERTEX_FLAG_PROMOTED_KNIGHT);
	}

	private void setFlag(int flag, boolean on) {
		if (on) {
			this.flags |= flag;
		} else {
			this.flags &= ~flag;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isCity() {
		return type == VertexType.CITY || type == VertexType.WALLED_CITY;
	}
	
	/**
	 * 
	 * @param isCity
	 */
	public void setType(VertexType type) {
		this.type = type;
	}

	/**
	 * 
	 * @return
	 */
	public int getPlayer() {
		return player;
	}
	
	/**
	 * 
	 * @param p
	 */
	public void setPlayer(int p) {
		player = p;
	}

	/**
	 * 
	 * @return
	 */
	public float getX() {
		return x;
	}

	/**
	 * 
	 * @return
	 */
	public float getY() {
		return y;
	}

	public int getTile(int index) {
	    return cells[index];
	}
	
	public int getNumTiles() {
	    return numCells;
	}
	
	void setNumCells(int num) {
		this.numCells = num;
	}
	
	void addTile(int tIndex) {
		cells[numCells++] = tIndex;
	}
	
	void addAdjacentVertex(int vIndex) {
		adjacent[numAdj++] = vIndex;
	}
	
	void setX(float x) {
		this.x = x;
	}
	
	void setY(float y) {
		this.y = y;
	}

	public final VertexType getType() {
		return type;
	}
	
	public boolean isKnight() {
		return type.knightLevel > 0;
	}
	
	public void activateKnight() {
		switch (getType()) {
			case BASIC_KNIGHT_INACTIVE:
				setType(VertexType.BASIC_KNIGHT_ACTIVE);
				break;
			case MIGHTY_KNIGHT_INACTIVE:
				setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
				break;
			case STRONG_KNIGHT_INACTIVE:
				setType(VertexType.STRONG_KNIGHT_ACTIVE);
				break;
			default:
				throw new RuntimeException("Not an valid kight to activate");
		}
	}

	public void deactivateKnight() {
		switch (getType()) {
			case BASIC_KNIGHT_ACTIVE:
				setType(VertexType.BASIC_KNIGHT_INACTIVE);
				break;
			case MIGHTY_KNIGHT_ACTIVE:
				setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
				break;
			case STRONG_KNIGHT_ACTIVE:
				setType(VertexType.STRONG_KNIGHT_INACTIVE);
				break;
			default:
				throw new RuntimeException("Not an valid kight to activate");
		}
	}
	
	public void promoteKnight() {
		switch (getType()) {
			case BASIC_KNIGHT_ACTIVE:
				setType(VertexType.STRONG_KNIGHT_ACTIVE);
				break;
			case BASIC_KNIGHT_INACTIVE:
				setType(VertexType.STRONG_KNIGHT_INACTIVE);
				break;
			case STRONG_KNIGHT_ACTIVE:
				setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
				break;
			case STRONG_KNIGHT_INACTIVE:
				setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
				break;
			default:
				throw new RuntimeException("Not a valid knight to promote");
		}
	}
	
	public void demoteKnight() {
		switch (getType()) {
			case BASIC_KNIGHT_ACTIVE:
			case BASIC_KNIGHT_INACTIVE:
				// ignore
				break;
			case STRONG_KNIGHT_ACTIVE:
				setType(VertexType.BASIC_KNIGHT_ACTIVE);
				break;
			case STRONG_KNIGHT_INACTIVE:
				setType(VertexType.BASIC_KNIGHT_INACTIVE);
				break;
			case MIGHTY_KNIGHT_ACTIVE:
				setType(VertexType.STRONG_KNIGHT_ACTIVE);
				break;
			case MIGHTY_KNIGHT_INACTIVE:
				setType(VertexType.STRONG_KNIGHT_INACTIVE);
				break;
			default:
				throw new RuntimeException("Not a valid knight to promote");
		}
	}
	
	public boolean isStructure() {
		return type.isStructure;
	}

	public int getPointsValue(Rules rules) {
		switch (getType()) {
			case SETTLEMENT:
				return rules.getPointsPerSettlement();
			case CITY:
			case WALLED_CITY:
				return rules.getPointsPerCity();
			case METROPOLIS_POLITICS:
			case METROPOLIS_SCIENCE:
			case METROPOLIS_TRADE:
				return rules.getPointsPerMetropolis();
			case BASIC_KNIGHT_ACTIVE:
			case BASIC_KNIGHT_INACTIVE:
			case MIGHTY_KNIGHT_ACTIVE:
			case MIGHTY_KNIGHT_INACTIVE:
			case OPEN:
			case STRONG_KNIGHT_ACTIVE:
			case STRONG_KNIGHT_INACTIVE:
				return 0;
		}
		throw new RuntimeException("Ungandled case '" + getType() + "'");
	}
	
	@Override
	public boolean equals(Object o) {
		Vertex v = (Vertex)o;
		return v.getX() == getX() && v.getY() == getY();
	}
	
	@Override
	public String toString() {
		String r = type.name();
		if (player > 0)
			r+= " player[" + player + "]";
		if (isAdjacentToLand())
			r+= " land";
		if(isAdjacentToWater())
			r+= " water";
		if(isPromotedKnight())
			r+= " promoted";
		return r;
	}
}
