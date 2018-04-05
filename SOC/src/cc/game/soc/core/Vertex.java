package cc.game.soc.core;

import java.util.Collection;

import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
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
	//private final static int VERTEX_FLAG_PROMOTED_KNIGHT = 0x1 << 2;
	
	private float	x, y;						// position of this vertex
	private int		player;						// 0 when unowned, otherwise index to the player
	private VertexType type = VertexType.OPEN;
	private int[]   cells = new int[3];
	private int     numCells;
	private int[]	adjacent = new int[3];		// indices of adjacent verts
	private int		numAdj;						// number of adjacents verts (can be 2 or 3 for hexagons)
	private int 	flags;						// true when this vertex is valid for placing a structure
	private int		pirateHealth;				// meaningful only when type is PIRATE_FORTRESS

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
	 * @return
	 */
	public final int[] getAdjacentVerts() {
		return adjacent;
	}

	/**
	 * 
	 * @return
	 */
	public int getNumAdjacentVerts() {
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
	/*
	public void setPromotedKnight(boolean promoted) {
		setFlag(VERTEX_FLAG_PROMOTED_KNIGHT, promoted);
	}

	public boolean isPromotedKnight() {
		return 0 != (flags & VERTEX_FLAG_PROMOTED_KNIGHT);
	}*/

	public boolean isAdjacentToLand() {
		return 0 != (flags & VERTEX_FLAG_ADJACENT_TO_LAND);
	}
	
	public boolean isAdjacentToWater() {
		return 0 != (flags & VERTEX_FLAG_ADJACENT_TO_WATER);
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
		switch (type) {
			case CITY:
			case WALLED_CITY:
			case METROPOLIS_POLITICS:
			case METROPOLIS_SCIENCE:
			case METROPOLIS_TRADE:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * 
	 * @param isCity
	 */
	public void setPlayerAndType(int playerNum, VertexType type) {
		assert(type != null);
		assert(playerNum > 0);
		assert(type != VertexType.OPEN);
		this.player = playerNum;
		this.type = type;
	}
	
	void setType(VertexType type) {
		if (type == VertexType.OPEN) {
			setOpen();
		} else {
			setPlayerAndType(player, type);
		}
	}
	
	/**
	 * An open settlement is initialized on game startup to a random player.  There should be same number of open
	 * settlements as there are players.  If there are more open settlements than players, then extras are returned to open.
	 * If there are not enough for players then game will fail initialization.
	 */
	public void setOpenSettlement() {
		this.player = 0;
		this.type = VertexType.OPEN_SETTLEMENT;
	}
	
	/**
	 * 
	 */
	public void setOpen() {
		player = 0;
		type = VertexType.OPEN;
	}
	
	public void setPirateFortress() {
		player = 0;
		type = VertexType.PIRATE_FORTRESS;
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

	public Collection<Integer> getTiles() {
	    return Utils.asList(cells, 0, numCells);
	}

	public int getNumTiles() {
	    return numCells;
	}
	
	void setNumTiles(int num) {
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
		return type.isKnight();
	}
	
	public boolean isActiveKnight() {
		return type.isKnightActive();
	}
	
	public void activateKnight() {
		type = (type.activatedType());
	}

	public void deactivateKnight() {
		type = (type.deActivatedType());
	}
	
	public void promoteKnight() {
		type = (type.promotedType());
	}
	
	public void demoteKnight() {
		type = (type.demotedType());
	}
	
	public boolean isStructure() {
		return type.isStructure;
	}
	
	public final int getPirateHealth() {
		return pirateHealth;
	}

	public final void setPirateHealth(int pirateHealth) {
		this.pirateHealth = pirateHealth;
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
			case OPEN:
			case BASIC_KNIGHT_ACTIVE:
			case BASIC_KNIGHT_INACTIVE:
			case MIGHTY_KNIGHT_ACTIVE:
			case MIGHTY_KNIGHT_INACTIVE:
			case STRONG_KNIGHT_ACTIVE:
			case STRONG_KNIGHT_INACTIVE:
			case PIRATE_FORTRESS:
				return 0;
		}
		throw new RuntimeException("Ungandled case '" + getType() + "'");
	}
	
	@Override
	public boolean equals(Object o) {
		Vertex v = (Vertex)o;
		float dx = Math.abs(v.getX() - getX());
		float dy = Math.abs(v.getY() - getY());
		return dx < 0.001 && dy < 0.001;
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
//		if(isPromotedKnight())
//			r+= " promoted";
		if (pirateHealth > 0)
			r += " pirate health=" + pirateHealth;
		return r;
	}

	void removeAdjacency(int index) {
		adjacent[index] = adjacent[--numAdj];
	}
}
