package cc.game.soc.core;

import java.util.List;

import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;


/**
 * 
 * @author Chris Caron
 * 
 */
public final class Tile extends Reflector<Tile> implements IVector2D {
	
	static {
		addAllFields(Tile.class);
	}
	
	//	 position of cell
	private float	x, y;

	//	 indicies to vertices
	// that are adjacent to
	// this cell
	private int[] adjVerts	= new int[6];
	private int	numAdj;
	private int	dieNum;
	private int islandNum;
	private TileType type = TileType.NONE;
	private int pirateRouteNext = -1; // When 
	
	public Tile() {}
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	Tile(float x, float y, TileType type) {
		this.x = x;
		this.y = y;
		this.type = type;
	}

	void reset() {
	}

	/**
	 * @return Returns the num.
	 */
	public int getDieNum() {
		return dieNum;
	}
	
	/**
	 * 
	 * @param dieNum
	 */
	public void setDieNum(int dieNum) {
		this.dieNum = dieNum;
	}

	/**
	 * @return Returns the type.
	 */
	public TileType getType() {
		return type;
	}

	/**
	 * 
	 * @param type
	 */
	public void setType(TileType type) {
		this.type = type;
	}
	
	/**
	 * 
	 * @param type
	 * @param dieNum
	 */
	public void setType(TileType type, int dieNum) {
		this.type = type;
		this.dieNum = dieNum;
	}
	
	/**
	 * @return Returns the x.
	 */
	public float getX() {
		return x;
	}

	/**
	 * @return Returns the y.
	 */
	public float getY() {
		return y;
	}

	/**
	 * @return Returns the adjVerts.
	 */
	public List<Integer> getAdjVerts() {
		return Utils.asList(adjVerts, 0, numAdj);
	}

	/**
	 * @return Returns the numAdj.
	 */
	public int getNumAdj() {
		return numAdj;
	}
	
	public int getAdjVert(int index) {
		return adjVerts[index];
	}

    void setAdjVerts(int[] adjVerts) {
        assert(adjVerts.length == 6);
        this.adjVerts = adjVerts;
        this.numAdj = adjVerts.length;
    }
    
    void setX(float x) {
    	this.x = x;
    }
	
    void setY(float y) {
    	this.y = y;
    }

	public boolean isWater() {
		return type.isWater;
	}
	
	public boolean isLand() {
		return type.isLand;
	}
	
	public ResourceType getResource() {
		return getType().resource;
	}
	
	public CommodityType getCommodity() {
		return getType().commodity;
	}

	public final int getIslandNum() {
		return islandNum;
	}

	public final void setIslandNum(int islandNum) {
		this.islandNum = islandNum;
	}

	public final boolean isDistributionTile() {
		return getType().isDistribution;
	}
	
	public boolean isPort() {
		return getType().isPort;
	}
	
	@Override
	public String toString() {
		String r = type.name();
		if (isPort())
			r += " Port";
		if (islandNum > 0)
			r +=  "isle[" + islandNum + "]";
		if (dieNum > 0)
			r += " Die[" + dieNum + "]";
		return r;
	}
	
	@Override
	public boolean equals(Object o) {
		Tile t = (Tile)o;
		return t.getX() == getX() && t.getY() == getY();
	}

	public final int getPirateRouteNext() {
		return pirateRouteNext;
	}

	public final void setPirateRouteNext(int pirateRouteNext) {
		this.pirateRouteNext = pirateRouteNext;
	}

}
