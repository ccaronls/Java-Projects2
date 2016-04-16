package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.util.*;

import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

/**
 * Represents a board with some number of hexagonal cells.  A board must be 'finalized' before it can be used with SOC.
 * 
 * @author Chris Caron
 * 
 */
public final class Board extends Reflector<Board> {
	
	static {
		addAllFields(Board.class);
	}
	
	private Vector<Tile>	tiles		= new Vector<Tile>();
	private Vector<Vertex>	verts		= new Vector<Vertex>();
	private Vector<Route>	routes		= new Vector<Route>();
	private Vector<Island>	islands		= new Vector<Island>();
	private int				robberTile	= -1;
	private int				pirateTile  = -1;
	private int				merchantTile = -1;
	private int				merchantPlayer = 0; // player num who last placed the merchant or 0 is not played
	private String			name		= "";
	private float           cw, ch;
	private final List<Integer>	undiscoveredCells = new ArrayList<Integer>(); // this is here so we can reset the board to original state
	private final List<Integer> pirateFortresses = new ArrayList<Integer>();
	
	private final static int GD_N		= 1;
	private final static int GD_NE		= 2;
	private final static int GD_SE		= 4;
	private final static int GD_S		= 8;
	private final static int GD_SW		= 16;
	private final static int GD_NW		= 32;

	// optimization - To prevent excessive execution of the road len algorithm O(2^n) , we cache the result here.
	private final int [] playerRoadLenCache = new int[16];
	private int pirateRouteStartTile = -1; // when >= 0, then the pirate route starts at this tile.  each tile has a next index to form a route.
	
	/**
	 * Create an empty board
	 */
	public Board() {
		Utils.fillArray(playerRoadLenCache, -1);
	}
	
	private void generateR2(float cx, float cy, float w, float h, float z, int depth, int dir, boolean allowDup, TileType type) {
		if (depth <= 0)
			return;

		if (!makeTile(cx, cy, w, h, z, type) && !allowDup)
			return; // duplicate, dont recurse
		if ((dir & GD_N) != 0)
			generateR2(cx, cy - h, w, h, z, depth - 1, dir, false, type);
		if ((dir & GD_S) != 0)
			generateR2(cx, cy + h, w, h, z, depth - 1, dir, false, type);
		if ((dir & GD_NE) != 0)
			generateR2(cx + w / 2 + z / 2, cy - h / 2, w, h, z, depth - 1, dir, false, type);
		if ((dir & GD_SE) != 0)
			generateR2(cx + w / 2 + z / 2, cy + h / 2, w, h, z, depth - 1, dir, false, type);
		if ((dir & GD_NW) != 0)
			generateR2(cx - w / 2 - z / 2, cy - h / 2, w, h, z, depth - 1, dir, false, type);
		if ((dir & GD_SW) != 0)
			generateR2(cx - w / 2 - z / 2, cy + h / 2, w, h, z, depth - 1, dir, false, type);
	}

	private void generateR(float cx, float cy, float w, float h, float z, TileType type) {
		if (cx - w / 2 < 0 || cx + w / 2 > 1)
			return;
		if (cy - h / 2 < 0 || cy + h / 2 > 1)
			return;
		if (!makeTile(cx, cy, w, h, z, type))
			return; // duplicate, dont recurse
		generateR(cx, cy - h, w, h, z, type);
		generateR(cx, cy + h, w, h, z, type);
		generateR(cx + w / 2 + z / 2, cy - h / 2, w, h, z, type);
		generateR(cx + w / 2 + z / 2, cy + h / 2, w, h, z, type);
		generateR(cx - w / 2 - z / 2, cy - h / 2, w, h, z, type);
		generateR(cx - w / 2 - z / 2, cy + h / 2, w, h, z, type);
	}

	private boolean makeTile(float cx, float cy, float w, float h, float z, TileType type) {
		int a = addVertex(cx - w / 2, cy);
		int b = addVertex(cx - z / 2, cy - h / 2);
		int c = addVertex(cx + z / 2, cy - h / 2);
		int d = addVertex(cx + w / 2, cy);
		int e = addVertex(cx + z / 2, cy + h / 2);
		int f = addVertex(cx - z / 2, cy + h / 2);
		boolean added = false;
		if (addAdjacency(a, b))
			added = true;
		if (addAdjacency(b, c))
			added = true;
		if (addAdjacency(c, d))
			added = true;
		if (addAdjacency(d, e))
			added = true;
		if (addAdjacency(e, f))
			added = true;
		if (addAdjacency(f, a))
			added = true;
		if (!added)
			return false;

		Tile cell = new Tile(cx, cy, type);
		tiles.add(cell);

		int [] adjVerts = {a,b,c,d,e,f};
		cell.setAdjVerts(adjVerts);
		
		return true;

	}

	private int addVertex(float x, float y) {
		int index = getVertex(x, y);
		if (index < 0) {
			index = verts.size();
			verts.add(new Vertex(x, y));
		}
		return index;

	}

	private boolean addAdjacency(int v0, int v1) {
		boolean added = false;
		if (addAdjacent(v0, v1))
			added = true;
		if (addAdjacent(v1, v0))
			added = true;
		return added;
	}

	private boolean addAdjacent(int from, int to) {
		Vertex v = getVertex(from);
		for (int i = 0; i < v.getNumAdjacent(); i++) {
			if (v.getAdjacent()[i] == to)
				return false;
		}
		v.addAdjacentVertex(to);
		return true;
	}
	
	/**
	 * Get the width of a single hexagon cell
	 * @return
	 */
	public float getTileWidth() {
        return cw;
    }

    /**
     * Get the height of a single hexagon cell
     * @return
     */
    public float getTileHeight() {
        return ch;
    }

    // compute the cell lookups for each vertex
    private void computeVertexTiles() {
    	for (Vertex v : verts) {
    		v.setNumCells(0);
    		v.setOpen();
    	}
        for (int i=0; i<getNumTiles(); i++) {
            Tile c = getTile(i);
            for (int ii: c.getAdjVerts()) { 
                Vertex v = getVertex(ii);
                v.addTile(i);
            }
        }
    }
    
    private void computeRoutes() {
    	routes.clear();
    	islands.clear();
		int ii;

		// clear vertex flags
		for (Vertex v : getVerticies()) {
			v.setAdjacentToLand(false);
			v.setAdjacentToWater(false);
		}
		
		for (int i = 0; i < tiles.size(); i++) {
			Tile c = getTile(i);
			c.setIslandNum(0);
			if (c.getType() != TileType.NONE) {
				for (ii = 0; ii < c.getNumAdj(); ii++) {
					int i2 = (ii + 1) % c.getNumAdj();
					int v0 = c.getAdjVert(ii);
					int v1 = c.getAdjVert(i2);
					if (c.isWater()) {
						getVertex(v0).setAdjacentToWater(true);
						getVertex(v1).setAdjacentToWater(true);
					} else if (c.isLand()) {
						getVertex(v0).setAdjacentToLand(true);
						getVertex(v1).setAdjacentToLand(true);
					}
					int edgeIndex = getRouteIndex(v0, v1);
					Route edge = null;
					if (edgeIndex < 0) {
						edge = new Route(Math.min(v0, v1), Math.max(v0,  v1));
						routes.add(-(edgeIndex+1), edge);
					} else {
						edge = getRoute(edgeIndex);
					}
					edge.addTile(i);
					if (c.isWater()) {
						edge.setAdjacentToWater(true);
					} else {
						edge.setAdjacentToLand(true);
					}
				}
			}
		}
		
		// Very important for edges to be sorted as this is a big bottleneck area.  Need O(lg n) or better access to to look up an edge.
		Collections.sort(routes);
		
		// make sure edges are unique and in ascending order
		// TODO: do we need this sanity check anymore?
		for (int i=1; i<routes.size(); i++) {
			Route e0 = getRoute(i-1);
			Route e1 = getRoute(i);
			if (e1.compareTo(e0)<=0)
				throw new RuntimeException("Edges out of order or not unique");
		}
	}

    /**
     * 
     * @param r
     * @return
     */
    public Iterable<Tile> getRouteTiles(final Route r) {
    	return new Iterable<Tile>() {

    		int index = 0;
    		
			@Override
			public Iterator<Tile> iterator() {
				return new Iterator<Tile>() {
					
					@Override
					public void remove() {
						throw new AssertionError("Cannot remove");
					}
					
					@Override
					public Tile next() {
						return getTile(r.getTile(index++));
					}
					
					@Override
					public boolean hasNext() {
						return index < 2 && r.getTile(index) >= 0;
					}
				};
			}
		};
    }
    
    /**
     * Iterate over cells are count the number of islands
     * 
     * @return
     */
    public int getNumIslands() {
    	return islands.size();
    }
    
    /**
     * 
     * @param startCellIndex
     * @return
     */
    public List<Integer> findIslandTiles(int startCellIndex) {
    	Tile t = getTile(startCellIndex);
    	if (t.getIslandNum() > 0) {
    		return getIsland(t.getIslandNum()).tiles;
    	}
    	boolean [] visited = new boolean[tiles.size()];
    	List<Integer> tiles = new ArrayList<Integer>();
    	computeIslandTilesDFS(startCellIndex, visited, tiles);
    	return tiles;
    }
    
    private void computeIslandTilesDFS(int start, boolean [] visited, List<Integer> cells) {
    	if (visited[start])
    		return;
    	visited[start] = true;
    	Tile cell = getTile(start);
    	if (cell.isWater())
    		return;
    	cells.add(start);
    	for (int vIndex : cell.getAdjVerts()) {
    		Vertex v = getVertex(vIndex);
    		for (int cIndex=0; cIndex<v.getNumTiles(); cIndex++) {
    			computeIslandTilesDFS(v.getTile(cIndex), visited, cells);
    		}
    	}
    }

    
    /**
     * Find all adjacent cells from a starting cell that form an island.
     * If start is already an island, then just return is num > 0 
     *  
     *  Return 0 means start cell was not land.
     *  
     * @param start
     */
    public int createIsland(int startCellIndex) {
    	Tile start = getTile(startCellIndex);
    	if (start.getIslandNum() > 0)
    		return start.getIslandNum();
    	if (start.isWater())
    		return 0;
    	int num = islands.size()+1;
    	Island island = new Island(num);
    	for (int cIndex : findIslandTiles(startCellIndex)) {
    		getTile(cIndex).setIslandNum(num);
    		island.tiles.add(cIndex);
    		for (int rIndex : getTileRouteIndices(getTile(cIndex))) {
    			if (getRoute(rIndex).isShoreline())
    				island.borderRoute.add(rIndex);
    		}
    	}
    	islands.add(island);
    	return num;
    }
    
    /**
     * 
     * @param islandNum
     */
    public void removeIsland(int islandNum) {
    	if (islandNum < 0 || islandNum > islands.size())
    		return;
    	islands.remove(islandNum-1);
    	int num = 1;
    	for (Island i : islands) {
    		i.num = num++;
    	}
    	for (Tile t : getTiles()) {
    		num = t.getIslandNum();
    		if (num == islandNum) {
    			t.setIslandNum(0);
    		} else if (num > islandNum) {
    			t.setIslandNum(num-1);
    		}
    	}
    }
    
    /**
     * Get an island by island num.  
     * @param num value >= 1
     * @return
     */
    public Island getIsland(int num) {
    	return islands.get(num-1);
    }
    
    /**
     * 
     * @return
     */
    public Iterable<Island> getIslands() {
    	return islands;
    }
    
    /**
     * 
     * @param playerNum
     * @return
     */
    public int getNumPlayerDiscoveredIslands(int playerNum) {
    	int num = 0;
    	for (Island i : getIslands()) {
    		if (i.isDiscoveredBy(playerNum))
    			num++;
    	}
    	return num;
    }

    /**
     * 
     * @param startCellIndex
     * @return
     */
    public Collection<Integer> findIslandShoreline(int startCellIndex) {
    	Tile tile = getTile(startCellIndex);
    	if (tile.getIslandNum() > 0)
    		return getIsland(tile.getIslandNum()).borderRoute;
    	
    	Set<Integer> edges = new HashSet<Integer>();
    	boolean [] visited = new boolean[tiles.size()];
    	findIslandEdgesDFS(startCellIndex, visited, edges);
    	return edges;
    }

    private void findIslandEdgesDFS(int start, boolean [] visited, Set<Integer> edges) {
    	if (visited[start])
    		return;
    	visited[start] = true;
    	Tile c = getTile(start);
    	if (c.isWater())
    		return;
    	for (int eIndex : getTileRouteIndices(c)) {
    		Route e = getRoute(eIndex);
    		if (e.isShoreline()) {
    			edges.add(eIndex);
    		}
    	}
    	for (int vIndex: c.getAdjVerts()) {
    		Vertex v = getVertex(vIndex);
    		for (int i = 0; i<v.getNumTiles(); i++) {
    			findIslandEdgesDFS(v.getTile(i), visited, edges);
    		}
    	}
    }
    
    /**
     * 
     * @param tile
     * @return
     */
    public Iterable<Route> getTileRoutes(Tile tile) {
    	
    	List<Route> edges = new ArrayList<Route>();
    	for (int i=0; i<tile.getNumAdj(); i++) {
    		int v0 = tile.getAdjVert(i);
    		int v1 = tile.getAdjVert((i+1) % tile.getNumAdj());
    		int eIndex = getRouteIndex(v0, v1);
    		if (eIndex >= 0) {
    			edges.add(getRoute(eIndex));
    		}
    	}
    	return edges;
    }
    
    public List<Route> getTileRoutesOfType(Tile tile, RouteType type) {
    	List<Route> edges = new ArrayList<Route>();
    	for (int i=0; i<tile.getNumAdj(); i++) {
    		int v0 = tile.getAdjVert(i);
    		int v1 = tile.getAdjVert((i+1) % tile.getNumAdj());
    		int eIndex = getRouteIndex(v0, v1);
    		if (eIndex >= 0) {
    			Route r = getRoute(eIndex);
    			if (r.getType() == type)
    				edges.add(getRoute(eIndex));
    		}
    	}
    	return edges;
    }
    
    /**
     * 
     * @param tile
     * @return
     */
    public Iterable<Integer> getTileRouteIndices(Tile tile) {
    	List<Integer> edges = new ArrayList<Integer>();
    	for (int i=0; i<tile.getNumAdj(); i++) {
    		int v0 = tile.getAdjVert(i);
    		int v1 = tile.getAdjVert((i+1) % tile.getNumAdj());
    		int eIndex = getRouteIndex(v0, v1);
    		if (eIndex >= 0) {
    			edges.add(eIndex);
    		}
    	}
    	return edges;
    }
    
	/**
     * Return the index of a vertex.  v may be a copy.
     * @param v
     * @return
     */
	public int getVertexIndex(Vertex v) {
		return verts.indexOf(v);
	}

	/**
	 * Return the index of an edge.  e may be a copy
	 * @param e
	 * @return
	 */
	public int getRouteIndex(Route e) {
		return routes.indexOf(e);
	}
	
	/**
	 * Return the index of a cell.  c may be a copy
	 * @param c
	 * @return
	 */
	public int getTileIndex(Tile c) {
		return tiles.indexOf(c);
	}
	
	/**
	 * Use this to remove a player from a route
	 * @param r
	 */
	public final void setRouteOpen(Route r) {
		setPlayerForRoute(r, 0, RouteType.OPEN);
	}
	
	/**
	 * 
	 * @param edge
	 * @param playerNum
	 */
	public final void setPlayerForRoute(Route edge, int playerNum, RouteType type) {
		if (edge.getPlayer() != playerNum) {
			if (playerNum == 0) {
				assert(type == RouteType.OPEN);
				playerRoadLenCache[edge.getPlayer()] = -1;
			}
			else {
				assert(type != RouteType.OPEN);
				playerRoadLenCache[playerNum] = -1;
			}
			edge.setPlayerDoNotUse(playerNum);
			edge.setType(type);
		}
	}
	
	/**
	 * Set the cell the robber is assigned to.  Setting to -1 means no robber
	 * @param cellNum
	 */
	public void setRobber(int cellNum) {
		robberTile = cellNum;
	}
	
	/**
	 * 
	 * @param cellNum
	 */
	public void setPirate(int cellNum) {
		if (pirateTile != cellNum) {
    		if (pirateTile >= 0) {
    			setTileRoutesImmobile(pirateTile, false);
    		}
    		if (cellNum >= 0) {
    			setTileRoutesImmobile(cellNum, true);
    		}
    		pirateTile = cellNum;
		}
	}
	
	private void setTileRoutesImmobile(int pirateCell, boolean isImmobile) {
		Tile cell = getTile(pirateCell);
		for (int eIndex : getTileRouteIndices(cell)) {
			getRoute(eIndex).setAttacked(isImmobile);
		}
	}

	/**
	 * Convenience method to set the robber using a cell.  cell can be null and indicates the robber is unassigned (-1)
	 * @param cell
	 */
	public void setRobberTile(Tile cell) {
	    if (cell == null)
	        robberTile = -1;
	    else
	        robberTile = getTileIndex(cell);
	}
	
	/**
	 * 
	 * @param cell
	 */
	public void setPirateTile(Tile cell) {
		setPirate(cell == null ? -1 : getTileIndex(cell));
	}

	/**
	 * Get the cell index the robber is assigned to
	 * @return
	 */
	public int getRobberTileIndex() {
		return robberTile;
	}
	
	/**
	 * 
	 * @return
	 */
	public Tile getRobberTile() {
		if (robberTile < 0)
			return null;
		return getTile(robberTile);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getPirateTileIndex() {
		return pirateTile;
	}

	/**
	 * 
	 * @return
	 */
	public Tile getPirateTile() {
		if (pirateTile < 0)
			return null;
		return getTile(pirateTile);
	}

	/**
	 * 
	 * @return
	 */
	public final int getMerchantTileIndex() {
		return merchantTile;
	}

	/**
	 * 
	 * @return
	 */
	public Tile getMerchantTile() {
		if (merchantTile < 0)
			return null;
		return getTile(merchantTile);
	}

	/**
	 * 
	 * @param merchantTile
	 */
	public final void setMerchant(int merchantTile, int playerNum) {
		this.merchantTile = merchantTile;
		this.merchantPlayer = playerNum;
	}

	/**
	 * 
	 * @return
	 */
	public final int getMerchantPlayer() {
		return merchantPlayer;
	}

	/**
	 * Get the name of the board
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the board
	 * @param name
	 */
	public void setName(String name) {
	    this.name = name;
	}

	/**
	 * Get the vertex at point x,y such that |P(x,y) - P(v)| < epsilon
	 * @param x
	 * @param y
	 * @return
	 */
	public int getVertex(float x, float y) {
	    double minD = 0.001;
	    int index = -1;
		for (int i = 0; i < getNumVerts(); i++) {
			Vertex v = verts.get(i);
			double dx = Math.abs(v.getX() - x);
			double dy = Math.abs(v.getY() - y);
			double d = (dx*dx) + (dy*dy);
			//if (dx < 1 && dy < 1)
			if (d < minD) {
				minD = d;
				index = i;
			}
		}
		return index;
	}

	/**
	 * Get the edge between 2 vertex indices
	 * @param from
	 * @param to
	 * @return
	 */
	public int getRouteIndex(int from, int to) {
		if (from > to) {
			int t = from;
			from = to;
			to = t;
		}
		Route key = new Route(from, to);
		int index = Collections.binarySearch(routes, key);
		return index;
	}
		
	/**
	 * Get the index of the next adjacent vertex after num of vert.  This allows iteration over adjacent verts.
	 * Example:
	 * for (int i=0; ; i++) {
	 *     int cur = findAdjacentVertex(vert, i);
	 *     if (cur >= 0) {
	 *         doSomething(getVertex(cur));
	 *     }
	 * }
	 * @param vert
	 * @param num
	 * @return
	 */
	public int findAdjacentVertex(int vert, int num) {
		Vertex v = getVertex(vert);
		if (num < v.getNumAdjacent() && num >= 0)
			return v.getAdjacent()[num];
		return -1;
	}

	/**
	 * Return true if 2 edges share a SINGLE endpoint.  
	 * @param e0
	 * @param e1
	 * @return
	 */
	public boolean isRoutesAdjacent(int e0, int e1) {
		if (e0 == e1)
			return false;

		Route E0 = routes.get(e0);
		Route E1 = routes.get(e1);

		if (E0.getFrom() == E1.getFrom() || E0.getFrom() == E1.getTo() || E0.getTo() == E1.getFrom() || E0.getTo() == E1.getTo())
			return true;

		return false;

	}
	
	public boolean isRouteOpenEnded(Route r) {
		assert(r.getPlayer() != 0);
		Vertex v = getVertex(r.getFrom());
		if (v.isStructure() && v.getPlayer() == r.getPlayer())
			return false;
		int numEnds = 0;
		for (int i=0; i<v.getNumAdjacent(); i++) {
			int v2 = v.getAdjacent()[i];
			if (v2 != r.getTo()) {
				Route r2 = getRoute(r.getFrom(), v2);
				if (r2 != null && r2.getPlayer() == r.getPlayer()) {
					numEnds++;
					break;
				}
			}
		}
		v = getVertex(r.getTo());
		if (v.isStructure() && v.getPlayer() == r.getPlayer())
			return false;
		for (int i=0; i<v.getNumAdjacent(); i++) {
			int v2 = v.getAdjacent()[i];
			if (v2 != r.getFrom()) {
				Route r2 = getRoute(r.getTo(), v2);
				if (r2 != null && r2.getPlayer() == r.getPlayer()) {
					numEnds++;
					break;
				}
			}
		}
		return numEnds < 2;
	}

	/**
	 *  This function only works for default (hexagon shaped) board types.
	 * @param c
	 * @return
	 *
	@Deprecated
	public boolean isPerimeterTile(Tile c) {
	    for (int vIndex: c.getAdjVerts()) {
	        Vertex v = getVertex(vIndex);
	        if (v.getNumAdjacent() != 3)
	            return true;
	    }
	    return false;
	}
	
	/**
	 * Clear the board of all data.  There will be no tiles, hence the board will be unplayable.
	 */
	public void clear() {
		verts.clear();
		tiles.clear();
		islands.clear();
		name = "";
		robberTile = -1;
		pirateTile = -1;
		cw = ch = 0;
		clearRoutes();
		clearPirateRoute();
	}
	
	/**
	 * Clear the board of all routes (edges).  isInitialized will be false after this call.
	 */
	public void clearRoutes() {
		routes.clear();
		clearRouteLenCache();
	}
	
	/**
	 * Remove the pirate route chain.
	 */
	public void clearPirateRoute() {
		while (pirateRouteStartTile >= 0) {
			Tile t = getTile(pirateRouteStartTile);
			pirateRouteStartTile = t.getPirateRouteNext();
			t.setPirateRouteNext(-1);
		}
		pirateRouteStartTile = -1;
	}
	
	public void addPirateRoute(int tileIndex) {
		if (pirateRouteStartTile < 0) {
			pirateRouteStartTile = tileIndex;
		} else {
			int tIndex = pirateRouteStartTile;
			while (true) {
				int index = getTile(tIndex).getPirateRouteNext();
				if (index < 0 || index == pirateRouteStartTile)
					break;
				tIndex = index;
			}
			if (tileIndex != tIndex) {
				getTile(tIndex).setPirateRouteNext(tileIndex);
			}
		}
	}

	/**
	 * Reset the board to its initial playable state.
	 */
	public void reset() {
		robberTile = -1;
		pirateTile = -1;
		merchantTile = -1;
		merchantPlayer = -1;
		clearRouteLenCache();
		for (Tile c : tiles) {
			c.reset();
		}
		for (Vertex v : verts) {
			v.reset();
		}
		for (Route r : routes) {
			r.reset();
		}
		for (int tile : undiscoveredCells) {
			getTile(tile).setType(TileType.UNDISCOVERED);
			getTile(tile).setDieNum(0);
		}
		for (int vertex : pirateFortresses) {
			getVertex(vertex).setPirateFortress();
		}
		for (Island i : islands) {
			Arrays.fill(i.discovered, false);
		}
	}

	/**
	 * Generate the original game play board
	 */
	public void generateDefaultBoard() {
		generateHexBoard(4, TileType.NONE);
		// creating the board using this link:
		//  http://www.lifeisbetterthanchocolate.com/blog/wp-content/uploads/2011/09/Sample-Setup.jpg
		
		// Sort the cells such that the 1st cell is at leftmost top position
		//  and the last cell is at the right most bottom position
		
		Vector<Tile> copyCells = new Vector<Tile>(tiles);
		
		Collections.sort(copyCells, new Comparator<Tile>() {

			@Override
			public int compare(Tile c0, Tile c1) {
				float dx = c0.getX() - c1.getX();
				float dy = c0.getY() - c1.getY();
				
				if (Math.abs(dx) < 0.001) {
					return dy < 0 ? -1 : 1;
				}
				
				return dx < 0 ? -1 : 1;
			}
			
		});

		// first column
		copyCells.get(0).setType(TileType.WATER);
		copyCells.get(1).setType(TileType.PORT_ORE);
		copyCells.get(2).setType(TileType.WATER);
		copyCells.get(3).setType(TileType.PORT_MULTI);
	
		// 2nd column
		copyCells.get(4).setType(TileType.PORT_WHEAT);
		copyCells.get(5).setType(TileType.FOREST, 11);
		copyCells.get(6).setType(TileType.HILLS, 4);
		copyCells.get(7).setType(TileType.FOREST, 8);
		copyCells.get(8).setType(TileType.WATER);
		
		// 3rd column
		copyCells.get(9).setType(TileType.WATER);
		copyCells.get(10).setType(TileType.MOUNTAINS, 12);
		copyCells.get(11).setType(TileType.FIELDS, 6);
		copyCells.get(12).setType(TileType.PASTURE, 3);
		copyCells.get(13).setType(TileType.DESERT);
		copyCells.get(14).setType(TileType.PORT_ORE);
		
		// 4th (center) column
		copyCells.get(15).setType(TileType.PORT_MULTI);
		copyCells.get(16).setType(TileType.FIELDS, 2);
		copyCells.get(17).setType(TileType.FOREST, 5);
		copyCells.get(18).setType(TileType.MOUNTAINS, 11);
		copyCells.get(19).setType(TileType.PASTURE, 10);
		copyCells.get(20).setType(TileType.PASTURE, 5);
		copyCells.get(21).setType(TileType.WATER);
		
		// 5th column
		copyCells.get(22).setType(TileType.WATER);
		copyCells.get(23).setType(TileType.HILLS, 11);
		copyCells.get(24).setType(TileType.MOUNTAINS, 4);
		copyCells.get(25).setType(TileType.FIELDS, 2);
		copyCells.get(26).setType(TileType.MOUNTAINS, 2);
		copyCells.get(27).setType(TileType.PORT_WOOD);
		
		// 6th column
		copyCells.get(28).setType(TileType.PORT_MULTI);
		copyCells.get(29).setType(TileType.PASTURE, 8);
		copyCells.get(30).setType(TileType.FIELDS, 3);
		copyCells.get(31).setType(TileType.FOREST, 6);
		copyCells.get(32).setType(TileType.WATER);
		
		// 7th column
		copyCells.get(33).setType(TileType.WATER);
		copyCells.get(34).setType(TileType.PORT_SHEEP);
		copyCells.get(35).setType(TileType.WATER);
		copyCells.get(36).setType(TileType.PORT_MULTI);

		assignRandom();
	}
	
	/**
	 * Generate a playable hexagonal shaped board with sideLen hexagons at each side.  For instance, if sideLen == 3, then will generate
	 * a board with 3 + 4 + 5 + 4 + 3 = 19 cells.  Recommended values for sideLen is between 4 and 6.
	 * @param sideLen
	 */
	public void generateHexBoard(int sideLen, TileType fillType) {
		clear();
		float rows = 2*sideLen - 1;
		ch = 1.0f / rows; 
		cw = ch;
		//int ch = cellDim;
		float zeta = cw / 2;
		float cx = ch / 2 + (sideLen-1) * (zeta + ch/2 - zeta/2);
		float cy = ch / 2 + ch * (sideLen - 1);
		generateR2(cx, cy, cw, ch, zeta, sideLen, GD_NE | GD_SE, true, fillType);
		generateR2(cx, cy, cw, ch, zeta, sideLen, GD_NW | GD_SW, true, fillType);
		generateR2(cx, cy, cw, ch, zeta, sideLen, GD_N | GD_NE | GD_NW, true, fillType);
		generateR2(cx, cy, cw, ch, zeta, sideLen, GD_S | GD_SE | GD_SW, true, fillType);
	}

	/**
	 * Generate a rectangular board with dim*dim cells. 
	 * @param rows
	 */
	public void generateRectBoard(int dim, TileType fillType) {
		clear();
		float cellDim = 1.0f / dim; 
		float cx = cellDim / 2;
		float cy = cellDim / 2;
		cw = cellDim;
		ch = cellDim;
		float zeta = cellDim / 2;
		generateR(cx, cy, cw, ch, zeta, fillType);
	}

	private void shuffle(Object [] arr) {
	    for (int i=0;  i<1000; i++) {
	         int a = Utils.rand() % arr.length;
	         int b = Utils.rand() % arr.length;
	         Object t = arr[a];
	         arr[a] = arr[b];
	         arr[b] = t;
	    }
	}

	/**
	 * 
	 */
	public void trim() {
		for (int i = 0; i < getNumTiles();) {
			Tile cell = getTile(i);
			if (cell.getType() == TileType.NONE) {
				// delete
				tiles.set(i, tiles.lastElement());
				tiles.removeElementAt(tiles.size() - 1);
			} else {
				i++;
			}
		}
		
		fillFit();
		computeVertexTiles();
		computeRoutes();
	}
	
    /**
     * Visit all the cells and assign die values when necessary and apply cell types to random cell.
     */
	public void assignRandom() {
		// make sure a reasonable number of cells
		if (getNumTiles() < 7)
			return;

		Integer dieRolls[] = 
		{ 
	        2, 
	        3, 3, 
	        4, 4, 4, 
	        5, 5, 5, 5, 
	        6, 6, 6, 6, 
	        8, 8, 8, 8, 
	        9, 9, 9, 9, 
	        10, 10, 10, 
	        11, 11, 
	        12 
		};
        

		shuffle(dieRolls);
		int curDieRoll = Utils.rand() % dieRolls.length;

		TileType [] resourceOptions = {
				TileType.FIELDS,
				TileType.FOREST,
				TileType.HILLS,
				TileType.MOUNTAINS,
				TileType.PASTURE,
				/*
				TileType.FIELDS,
				TileType.FOREST,
				TileType.HILLS,
				TileType.MOUNTAINS,
				TileType.PASTURE,
				TileType.GOLD,
				*/
		};

		TileType [] portOptions = {
				TileType.PORT_BRICK,
				TileType.PORT_ORE,
				TileType.PORT_SHEEP,
				TileType.PORT_WHEAT,
				TileType.PORT_WOOD,
		};
		
		shuffle(resourceOptions);
		shuffle(portOptions);
		int curResourceOption = 0;
		int curPortOption = 0;
		final int minDeserts = 1; // make configurable
		int numDeserts = 0;
		Vector<Integer> desertOptions = new Vector<Integer>();

		// iterate over all the cells and finalize if they are random
		//int numActualCells = 0;
		for (int i = 0; i < getNumTiles();) {
			Tile cell = getTile(i);
			switch (cell.getType()) {
				case NONE:
					break;
    			// mark all edges as available for road
    			case DESERT:
    				//numActualCells++;
    				numDeserts++;
    				break;
    
    			// nothing to do
    			case WATER:
    			case PORT_MULTI:
        		case PORT_ORE:
        		case PORT_SHEEP:
        		case PORT_WHEAT:
        		case PORT_WOOD:
        		case PORT_BRICK:
        			break;
    
    			case RANDOM_RESOURCE_OR_DESERT:
    				desertOptions.add(i);
    			// fallthrough
    			case RANDOM_RESOURCE: {
    				cell.setType(resourceOptions[curResourceOption]);
    				curResourceOption = (curResourceOption + 1) % resourceOptions.length;
    			}
    			// fallthrough
    
    			case GOLD:
    			case FOREST:
    			case PASTURE:
    			case FIELDS:
    			case HILLS:
    			case MOUNTAINS:
    				if (cell.getDieNum() == 0) {
    					cell.setDieNum(dieRolls[curDieRoll]);
    					curDieRoll = (curDieRoll + 1) % dieRolls.length;
    				}
    				break;
    
    			case RANDOM_PORT_OR_WATER:
    			case RANDOM_PORT:
    				switch (Utils.rand() % (cell.getType() == TileType.RANDOM_PORT_OR_WATER ? 2 : 1)) {
    				case 0:
    					if (curPortOption >= portOptions.length) {
    						cell.setType(TileType.PORT_MULTI);
    					} else {
    						cell.setType(portOptions[curPortOption++]);
    					}
    					//curPortOption = (curPortOption + 1) % portOptions.length;
    					break;
    
    				case 1:
    					cell.setType(TileType.WATER);
    					break;
    				}
    				break;
    				
    			case UNDISCOVERED:
    				undiscoveredCells.add(i);
    				break;
			}
			i++;
		}
		// assign the deserts
		if (desertOptions.size() > 0) {
			Utils.shuffle(desertOptions);
			while (numDeserts < minDeserts && numDeserts < desertOptions.size()) {
				Tile cell = getTile(desertOptions.get(numDeserts++));
				cell.setType(TileType.DESERT);
				cell.setDieNum(0);
			}
		}
	}

	/**
	 * Get the number of cells.
	 * @return
	 */
	public int getNumTiles() {
		return tiles.size();
	}

	/**
	 * Get the cell assigned to index 
	 * @param index
	 * @return
	 */
	public Tile getTile(int index) {
		return tiles.get(index);
	}

	/**
	 * Get the number of vertices
	 * @return
	 */
	public int getNumVerts() {
		return verts.size();
	}

	/**
	 * Get the vertex assigned to index
	 * @param index
	 * @return
	 */
	public Vertex getVertex(int index) {
		return verts.get(index);
	}

	/**
	 * Get the number of edges.
	 * @return
	 */
	public int getNumRoutes() {
		return routes.size();
	}

	/**
	 * Get the edge assigned to index.
	 * @param index
	 * @return
	 */
	public Route getRoute(int index) {
		return routes.get(index);
	}
	
	/**
	 * 
	 * @param vIndex
	 * @return
	 */
	public Iterable<Route> getRoutesAdjacentToVertex(int vIndex) {
		Vertex v = getVertex(vIndex);
		List<Route> routes = new ArrayList<Route>(3);
		for (int i=0; i<v.getNumAdjacent(); i++) {
			int v2 = v.getAdjacent()[i];
			Route r = getRoute(vIndex, v2);
			if (r != null)
				routes.add(r);
		}
		return routes;
	}

	/**
	 * 
	 * @param vIndex
	 * @return
	 */
	public Iterable<Integer> getRouteIndicesAdjacentToVertex(int vIndex) {
		Vertex v = getVertex(vIndex);
		List<Integer> routes = new ArrayList<Integer>(3);
		for (int i=0; i<v.getNumAdjacent(); i++) {
			int v2 = v.getAdjacent()[i];
			int r = getRouteIndex(vIndex, v2);
			if (r >= 0)
				routes.add(r);
		}
		return routes;
	}

	/**
	 * Get the edge between 2 vertices null when DNE.
	 * @param from
	 * @param to
	 * @return
	 */
    public Route getRoute(int from, int to) {
        int index = getRouteIndex(from, to);
        if (index < 0)
            return null;
        return getRoute(index);
    }

    /**
     * 
     * @return
     */
    public Iterable<Vertex> getVerticies() {
        return this.verts;
    }
    
    /**
     * 
     * @return
     */
    public Iterable<Route> getRoutes() {
        return this.routes;
    }
    
    /**
     * 
     * @return
     */
    public Iterable<Tile> getTiles() {
        return this.tiles;
    }
    
    /**
     * 
     * @param player
     * @param rt
     * @return
     */
    public List<Route> getRoutesOfType(int player, RouteType ... types) {
    	List<Route> routes = new ArrayList<Route>();
    	List<RouteType> arr = Arrays.asList(types);
    	for (int i=0; i<getNumRoutes(); i++) {
    		Route r = getRoute(i);
    		if (arr.contains(r.getType()) && (player == 0 || r.getPlayer() == player)) {
    			routes.add(r);
    		}
    	}
    	return routes;
    }

    public List<Integer> getRoutesIndicesOfType(int player, RouteType ... types) {
    	List<Integer> routes = new ArrayList<Integer>();
    	List<RouteType> arr = Arrays.asList(types);
    	for (int i=0; i<getNumRoutes(); i++) {
    		Route r = getRoute(i);
    		if (arr.contains(r.getType()) && (player == 0 || r.getPlayer() == player)) {
    			routes.add(i);
    		}
    	}
    	return routes;
    }

    /**
     * Use BFS to find closest ships.  Ships must be adjacent to the vertex at vIndex.
     * @param vIndex
     * @param numShips
     */
    public void removeShipsClosestToVertex(int vIndex, int playerNum, int numShips) {
    	Queue<Integer> Q = new LinkedList<Integer>();
    	Q.add(vIndex);
    	while (!Q.isEmpty() && numShips > 0) {
        	vIndex = Q.remove();
        	Vertex v = getVertex(vIndex);
        	for (int v2 : v.getAdjacent()) {
        		Route r = getRoute(vIndex, v2);
        		if (r.getPlayer() == playerNum && r.getType().isVessel) {
        			setRouteOpen(r);
        			numShips -= 1;
            		Q.add(v2);
        		}
        	}
    	}
    }
    
    /**
     * 
     * @param type
     * @return
     */
    public List<Integer> getTilesOfType(TileType type) {
    	List<Integer> tiles = new ArrayList<Integer>();
    	for (int i=0; i<getNumTiles(); i++) {
    		if(getTile(i).getType()==type) {
    			tiles.add(i);
    		}
    	}
    	return tiles;
    }
    
    /**
     * Return whether the board has been initialized.
     * @return
     */
	public boolean isReady() {
		if (routes.size() == 0)
			return false;
		
		if (verts.size() == 0)
			return false;

		int numCells = 0;
		for (Tile cell : tiles) {
			switch (cell.getType()) {
				case NONE:
					return false;
				case GOLD:
    			case FOREST:
    			case FIELDS:
    			case HILLS:
    			case MOUNTAINS:
    			case PASTURE:
    			case RANDOM_PORT:
    			case RANDOM_PORT_OR_WATER:
    			case RANDOM_RESOURCE:
    			case RANDOM_RESOURCE_OR_DESERT:
				case DESERT:
				case PORT_MULTI:
	    		case PORT_ORE:
	    		case PORT_SHEEP:
	    		case PORT_WHEAT:
	    		case PORT_WOOD:
	    		case PORT_BRICK:
	    		case UNDISCOVERED:
				case WATER:
					numCells++;
					break;
    				
			}
			
		}
		return numCells > 10;
	}

	private int traversePath(int from, int v0, boolean [] visitedEdges, int playerNum, int depth, boolean enableRoadBlock) {
	    Vertex v = getVertex(v0);
	    if (enableRoadBlock && v.getPlayer() > 0 && v.getPlayer() != playerNum)
	    	return depth;
	    int max = depth;
	    for (int i=0; i<v.getNumAdjacent(); i++) {
	        int v1 = v.getAdjacent()[i];
	        if (v1 == from) {
	            continue;
	        }
	        int eIndex = getRouteIndex(v0, v1);
	        if (eIndex < 0)
	            break;
	        if (visitedEdges[eIndex])
	        	continue;
	        visitedEdges[eIndex] = true;
	        //assert(eIndex >= 0);
	        Route e = getRoute(eIndex);
	        if (e.getPlayer() != playerNum) {
	            continue;
	        }
	        int len = traversePath(v0, v1, visitedEdges, playerNum, depth+1, enableRoadBlock);
	        if (len > max) {
	            max= len;
	        }
	    }
	    return max;
	}

	/**
	 * O(2^n) algorithm to solve the longest path problem.
	 * @param playerNum
	 * @return
	 */
	public int computeMaxRouteLengthForPlayer(int playerNum, boolean enableRoadBlock)
	{
		if (playerRoadLenCache[playerNum] >= 0) {
			//System.out.println("DEBUG: Road len for player " + playerNum + " cached too: " + playerRoadLenCache[playerNum]);
			return playerRoadLenCache[playerNum];
		}
		
		//System.out.println("Recompute road length");
	    //if (Profiler.ENABLED) Profiler.push("SOCBoard::computeMaxRoadLengthForPlayer");
	    try {
    	    boolean [] visitedEdges = new boolean[getNumRoutes()];
    	    int max = 0;
    	    for (int i=0; i<getNumVerts(); i++) {
    	        Vertex v = getVertex(i);
    	        if (v.getType() != VertexType.OPEN && !v.isKnight()) // TOOD: should we check here for enableRoadBlock and only consider knights if true?
    	            continue; // skip past verts with settlements on them
    	        //Utils.fillArray(visited, false);
    	        Arrays.fill(visitedEdges, false);
    	        int [] len = {0,0,0};
    	        for (int ii=0; ii<v.getNumAdjacent(); ii++) {
    	            int v1 = v.getAdjacent()[ii];
    	            int eIndex = getRouteIndex(i, v1);
    	            if (eIndex < 0)
    	                continue;
    	            if (visitedEdges[eIndex])
    	            	continue;
    	            visitedEdges[eIndex] = true;
    	            Route e = getRoute(eIndex);
    	            if (e.getPlayer() != playerNum)
    	                continue;
    	            //System.out.println("traverse path starting at edge " + eIndex);
    	            len[ii] = traversePath(i, v1, visitedEdges, playerNum, 1, enableRoadBlock);
    	            //System.out.println("Road len from vertex " + i + " = " + len[ii]);
    	        }
    	        int a = len[0] + len[1];
    	        int b = len[1] + len[2];
    	        int c = len[0] + len[2];
    
    	        int m = a > b ? a : b;
    	        m = m > c ? m : c;
    	        if (m > max) {
    	            max = m;
    	        }
    	    }
    
    	    //System.out.println("Computed max road len for player " + playerNum + " = " + max);
    	    playerRoadLenCache[playerNum] = max;
    	    return max;
	    } finally {
	        //if (Profiler.ENABLED) Profiler.pop("SOCBoard::computeMaxRoadLengthForPlayer");	        
	    }
	}
		
	/**
	 * Find the nth cell that is adjactent to an edge.
	 * There are at most 2 cells to an edge.  
	 * Algorithm Requires testing 2 vertices, each with 3 cells adjacent
	 * The cell indices adjacent to the verts at our endpoints indicate
	 * a cell we are adjacent 2.  O(3)^2
	 * @param edge the edge to test
	 * @param fromIndex 0 = first, 1 = second cell
	 * @return the index of the cell adjacent to edge or -1 if not found
	 */
	public int findAdjacentTile(Route edge, int fromIndex) {
	    Vertex v0 = getVertex(edge.getFrom());
	    Vertex v1 = getVertex(edge.getTo());
	    for (int i=0; i<v0.getNumTiles(); i++) {
	        for (int ii=0; ii<v1.getNumTiles(); ii++) {
	            if (v0.getTile(i) == v1.getTile(ii)) {
	                if (fromIndex-->0)
	                    continue;
	                return v0.getTile(i);
	            }
	        }
	    }
	    return -1;
	}
	
	/**
	 * Count the number of vertices assigned to a player.
	 * @param player
	 * @return
	 */
	public int getNumStructuresForPlayer(int playerNum) {
		return getNumVertsOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_TRADE);
	}

	/**
	 * Count the number of vertices assigned to player that are settlements.
	 * @param player
	 * @return
	 */
	public int getNumSettlementsForPlayer(int playerNum) {
		return getNumVertsOfType(playerNum, VertexType.SETTLEMENT);
	}

	/**
	 * Count of vertices of a set of types restricted to a certain player.
	 * When playerNum == 0, then there is no restriction on player
	 * @param playerNum
	 * @param types
	 * @return
	 */
	public int getNumVertsOfType(int playerNum, VertexType ... types) {
		int num = 0;
		List<VertexType> arr = Arrays.asList(types);
		for (Vertex v : verts) {
			if ((playerNum == 0 || playerNum == v.getPlayer()) && arr.contains(v.getType())) {
				num++;
			}
		}
		return num;
	}
	
	/**
	 * Get a list of vertices restricted to a set of types and a player num.
	 * When player num == 0, then no player restriction
	 * @param playerNum
	 * @param types
	 * @return
	 */
	public List<Integer> getVertsOfType(int playerNum, VertexType ... types) {
		List<Integer> verts = new ArrayList<Integer>();
		List<VertexType> arr = Arrays.asList(types);
		for (int i = 0; i < getNumVerts(); i++) {
			Vertex v = getVertex(i);
			if ((playerNum == 0 || playerNum == v.getPlayer()) && arr.contains(v.getType()))
				verts.add(i);
		}
		return verts;
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public int getNumKnightsForPlayer(int playerNum) {
		return getNumVertsOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
	}
	
	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getKnightsForPlayer(int playerNum) {
		return getVertsOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
	}
	
    /**
     * Count the number of vertices assigned to player that are cities.
     * @param player
     * @return
     *
	public int getNumCitiesForPlayer(int playerNum) {
		return getNumVertsOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY);
	}
	
	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getCitiesForPlayer(int playerNum) {
		return getVertsOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY);
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getSettlementsForPlayer(int playerNum) {
		return getVertsOfType(playerNum, VertexType.SETTLEMENT);
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getStructuresForPlayer(int playerNum) {
		return getVertsOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
	}

	/**
	 * Return true if a player has any structure adjacent to a given cell.
	 * @param player
	 * @param cellIndex
	 * @return
	 */
	public boolean isPlayerAdjacentToTile(int playerNum, int cellIndex) {
		Tile cell = getTile(cellIndex);
		//assert (cell.numAdj > 0 && cell.numAdj <= 6);
		for (int vIndex : cell.getAdjVerts()) {
			Vertex vertex = getVertex(vIndex);
			if (vertex.getPlayer() == playerNum)
				return true;
		}
		return false;
	}
	
	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edgeIndex
	 * @param p
	 * @return
	 */
	public boolean isRouteAvailableForRoad(int edgeIndex, int playerNum) {
		return isRouteAvailableForRoad(getRoute(edgeIndex), playerNum);
	}
	
	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edgeIndex
	 * @param p
	 * @return
	 */
	public boolean isRouteAvailableForRoad(Route edge, int playerNum) {
		if (edge.getPlayer() != 0 || !edge.isAdjacentToLand() || edge.isLocked() || edge.isClosed()) {
			 return false;
		 }

		// if either vertex is a structure then ok to place
		Vertex v0 = getVertex(edge.getFrom());
		Vertex v1 = getVertex(edge.getTo());
		
		if (v0.isKnight() && v0.getPlayer() != playerNum)
			return false;
		
		if (v1.isKnight() && v1.getPlayer() != playerNum)
			return false;
		
		if (v0.isStructure() && v0.getPlayer() == playerNum) {
			return true;
		}

		if (v1.isStructure() && v1.getPlayer() == playerNum) {
			return true;
		}

		// check if the adjacent edges have one of our roads
		if (isVertexAdjacentToPlayerRoad(edge.getFrom(), playerNum) || isVertexAdjacentToPlayerRoad(edge.getTo(), playerNum)) {
			return true;
		}

		return false;
	}
	
	/**
	 * 
	 * @param edgeIndex
	 * @param playerNum
	 * @return
	 */
	public boolean isRouteAvailableForShip(Rules rules, Route edge, int playerNum) {
		if (edge.getPlayer() != 0 || !edge.isAdjacentToWater() || edge.isAttacked() || edge.isLocked() || edge.isClosed())
			return false;
		
		// check if the adjacent edges have one of our ships
		if (isVertexAdjacentToPlayerShip(edge.getFrom(), playerNum) || isVertexAdjacentToPlayerShip(edge.getTo(), playerNum)) {
			return true;
		}

		Vertex v = getVertex(edge.getFrom());
		if (v.getPlayer() == playerNum && isVertexAdjacentToWater(v))
			return true;
		
		v = getVertex(edge.getTo());
		if (v.getPlayer() == playerNum && isVertexAdjacentToWater(v))
			return true;
		
		if (rules.isEnableBuildShipsFromPort()) {
			for (Tile t : getRouteTiles(edge)) {
				if (t.isPort())
					return true;
			}
		}
		
		return false;
	}

	/**
	 * 
	 * @param edgeIndex
	 * @param playerNum
	 * @return
	 */
	public boolean isRouteAvailableForShip(Rules rules, int edgeIndex, int playerNum) {
		return isRouteAvailableForShip(rules, getRoute(edgeIndex), playerNum);
	}
	
	/**
	 * Return true if a vertex is available for a structure
	 * @param vIndex
	 * @return
	 */
	public boolean isVertexAvailbleForSettlement(int vIndex) {
		Vertex v = getVertex(vIndex);
		if (v.getPlayer() > 0 || !v.canPlaceStructure())
			return false;

		for (int i = 0; i < v.getNumAdjacent(); i++) {
			int v2 = findAdjacentVertex(vIndex, i);
			if (v2 >= 0) {
				int ie = getRouteIndex(vIndex, v2);
				if (ie >= 0) {
					Route e = getRoute(ie);
					if (getVertex(e.getFrom()).isStructure() || getVertex(e.getTo()).isStructure())
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Return true if a vertex has any edge assigned to player.
	 * @param vIndex
	 * @param player
	 * @return
	 */
	private boolean isVertexAdjacentToRoute(int vIndex, int playerNum, int flag) {
		for (int i = 0; i < 3; i++) {
			int v2 = findAdjacentVertex(vIndex, i);
			if (v2 >= 0) {
				int ie = getRouteIndex(vIndex, v2);
				if (ie >= 0) {
					Route e = getRoute(ie);
					int eFlag = e.getType().isVessel ? Route.EDGE_FLAG_WATER : Route.EDGE_FLAG_LAND;
					if (e.getPlayer() == playerNum && 0 != (flag & eFlag))
						return true;
				}
			} else {
				break;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	public final boolean isVertexAdjacentToPlayerRoad(int vIndex, int playerNum) {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_LAND);
	}
	
	/**
	 * 
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	public final boolean isVertexAdjacentToPlayerShip(int vIndex, int playerNum) {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_WATER);
	}
	
	/**
	 * 
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	public final boolean isVertexAdjacentToPlayerRoute(int vIndex, int playerNum) {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_LAND | Route.EDGE_FLAG_WATER);
	}

	/**
	 * Count the number of edges assigned to player
	 * @param player
	 * @return
	 */
    public final int getNumRoadsForPlayer(int playerNum) {
        int num = 0;
        for (int i=0; i<getNumRoutes(); i++) {
            Route e = getRoute(i);
            if (e.getPlayer() == playerNum) {
            	switch (e.getType()) {
					case DAMAGED_ROAD:
					case ROAD:
						num++;
						break;
					case OPEN:
					case SHIP:
					case WARSHIP:
						break;
            	}
            }
        }
        return num;
    }
    
    /**
     * Convenience method to get a iterable over a players roads.  The roads are not ordered
     * 
     * @param playerNum
     * @return
     */
    public final Iterable<Route> getRoutesForPlayer(int playerNum) {
        List<Route> edges = new ArrayList<Route>();
        for (int i=0; i<getNumRoutes(); i++) {
            Route e = getRoute(i);
            if (e.getPlayer() == playerNum) {
                edges.add(e);
            }
        }
        return edges;
    }
    
    /**
     * 
     * @param playerNum
     * @return
     */
    public final Iterable<Integer> getRouteIndicesForPlayer(int playerNum) {
    	List<Integer> routes = new ArrayList<Integer>();
    	for (int rIndex =0; rIndex<getNumRoutes(); rIndex++) {
    		if (getRoute(rIndex).getPlayer() == playerNum)
    			routes.add(rIndex);
    	}
    	return routes;
    }

    /**
     * Interface to be used in conjunction with walkEdgeTree
     * @author ccaron
     *
     */
    public interface IVisitor {
        /**
         * Visit an edge 
         *  
         * @param edge
         * @param depth current depth of recursion
         * @return false to terminate the recursion, true to continue the recursion
         */
        boolean visit(Route edge, int depth);
        
        /**
         * Determine advancement of recursion on a vertex
         * @param vertexIndex
         * @return
         */
        boolean canRecurse(int vertexIndex);
    }
    
    /**
     * Recursive DFS search through edges.  
     * visit is called once for each edge.  
     * recursion is stopped if IVisitor.visit returns false
     * recursion will not advance if IVisitor.canRecurse(vertex) returns false
     * 
     * @param startVertex
     * @param visitor
     */
    public void walkRouteTree(int startVertex, IVisitor visitor) {
        if (startVertex < 0 || visitor == null)
            return;
        boolean [] visitedEdges = new boolean[getNumRoutes()];
        walkRouteTreeR(startVertex, visitor, visitedEdges, 0);
    }

    private void walkRouteTreeR(int startVertex, IVisitor visitor, boolean [] usedEdges, int depth) {
        assert(startVertex >= 0);
        Vertex vertex = getVertex(startVertex);
        for (int i=0; i<vertex.getNumAdjacent(); i++) {
            int toVertexIndex = vertex.getAdjacent()[i];
            if (toVertexIndex != startVertex) {
                int edgeIndex = getRouteIndex(startVertex, toVertexIndex);
                if (usedEdges[edgeIndex])
                    continue;
                if (!visitor.canRecurse(toVertexIndex))
                    continue;
                usedEdges[edgeIndex] = true;
                Route edge = getRoute(edgeIndex);
                if (visitor.visit(edge, depth+1)) {
                    walkRouteTreeR(toVertexIndex, visitor, usedEdges, depth+1);
                }
            }
        }
    }
    
    /**
     * Return the single player num whose road has route has been blocked due to the positioning of
     * a structure or knight at vertex vIndex or 0 if no blocking occurred.  Note that in order for
     * there to be a blocking event then a player must have 2 edges adjacent to the vertex
     * 
     * @param vIndex
     * @return
     */
    public int checkForPlayerRouteBlocked(int vIndex) {
    	if (vIndex < 0)
    		return 0;
    	Vertex v = getVertex(vIndex);
    	int playerNum = 0;
    	for (int i=0; i<v.getNumAdjacent(); i++) {
    		Route r = getRoute(vIndex, v.getAdjacent()[i]);
    		if (r.getType().isRoute && r.getPlayer() != v.getPlayer()) {
    			if (playerNum == 0)
    				playerNum = r.getPlayer();
    			else if (playerNum == r.getPlayer())
    				return playerNum;
    		}
    	}
    	return 0;
    }

	public void load(String string) throws IOException {
		loadFromFile(new File(string));
		Collections.sort(routes);
	}

	public void save(String string) throws IOException {
		saveToFile(new File(string));
	}

	public void translate(float dx, float dy) {
		for (Vertex v : verts) {
			v.setX(v.getX() + dx);
			v.setY(v.getY() + dy);
		}
		for (Tile c : tiles) {
			c.setX(c.getX() + dx);
			c.setY(c.getY() + dy);
		}
	}

	public void scale(float sx, float sy) {
		for (Vertex v : verts) {
			v.setX(v.getX() * sx);
			v.setY(v.getY() * sy);
		}
		for (Tile c : tiles) {
			c.setX(c.getX() * sx);
			c.setY(c.getY() * sy);
		}
		cw *= sx;
		ch *= sy;
	}

	/**
	 * Center the board inside the 0,0 x 1,1 rectangle
	 */
    public final void center() {
    	Vector2D [] minMax = computeMinMax();
    	Vector2D v = minMax[0].add(minMax[1]).scale(-0.5f);
    	translate(v.getX() + 0.5f, v.getY() + 0.5f);
    }

    public final Vector2D [] computeMinMax() {
    	MutableVector2D min = new MutableVector2D(Vector2D.MAX);
    	MutableVector2D max = new MutableVector2D(Vector2D.MIN);
    	for (Tile c : tiles) {
    		if (c.getType() == TileType.NONE)
    			continue;
    		min.minEq(c);
    		max.maxEq(c);
    	}
    	max.addEq(Vector2D.newTemp(cw/2, ch/2));
    	min.subEq(Vector2D.newTemp(cw/2, ch/2));
    	return new Vector2D [] { min, max };
    }
    
    /**
     * Fit all cells into the 0,0 x 1,1 rectangle
     */
    public final void fillFit() {
    	Vector2D [] minMax = computeMinMax();
    	Vector2D v = minMax[0].add(minMax[1]).scale(-0.5f);
    	Vector2D d = minMax[1].sub(minMax[0]);
    	// center at 0,0
    	translate(v.getX(), v.getY());
    	// fill a 1,1 rect
    	scale(1.0f / d.getX(), 1.0f / d.getY());
    	// move to 0.5, 0.5
    	translate(0.5f, 0.5f);
    }
    
    /**
     * Resets all routes to have player 0
     */
    public final void resetRoutes() {
    	for (Route e : routes) {
    		setRouteOpen(e);
    	}
    }
    
    /**
     * Resets all structures to have player 0
     */
    public final void resetStructures() {
    	for (Vertex v : verts) {
    		v.setOpen();
    	}
    }
    
    /**
     * Removes all islands references and sets island num of all tiles to 0
     */
    public final void clearIslands() {
    	for (Tile t : tiles) {
    		t.setIslandNum(0);
    	}
    	islands.clear();
    }

    /**
     * Convenience method to get iterable over the cells adjacent to a vertex
     * @param v
     * @return
     */
	public final Iterable<Tile> getTilesAdjacentToVertex(Vertex v) {
		ArrayList<Tile> a = new ArrayList<Tile>();
		for (int i=0; i<v.getNumTiles(); i++) {
			a.add(getTile(v.getTile(i)));
		}
		return a;
	}
	
	/**
	 * 
	 * @param v
	 * @return
	 */
	public final Iterable<Integer> getTileIndicesAdjacentToVertex(Vertex v) {
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int i=0; i<v.getNumTiles(); i++) {
			a.add(v.getTile(i));
		}
		return a;
	}
	
	/**
	 * 
	 * @param t
	 * @return
	 */
	public final Iterable<Integer> getTilesAdjacentToTile(Tile t) {
		HashSet<Integer> result = new HashSet<Integer>();
		for (int vIndex : t.getAdjVerts()) {
			for (int tIndex : getTileIndicesAdjacentToVertex(getVertex(vIndex))) {
				if (getTile(tIndex) == t)
					continue;
				result.add(tIndex);
			}
		}
		return result;
	}
	
	/**
     * Convenience method to get iterable over the cells adjacent to a vertex
     * @param v
     * @return
     */
	public final Iterable<Tile> getTilesAdjacentToVertex(int vIndex) {
		return getTilesAdjacentToVertex(getVertex(vIndex));
	}

	/**
	 * 
	 */
	public void clearRouteLenCache() {
		Arrays.fill(playerRoadLenCache, -1);
	}
	
	public final boolean isVertexAdjacentToWater(Vertex v) {
		for (Tile cell : getTilesAdjacentToVertex(v)) {
			if (cell.isWater())
				return true;
		}
		return false;
	}

	/**
	 * Return true if any edge adjacent to e is a ship and owned by playerNum
	 * @param e
	 * @param playerNum
	 * @return
	 */
	public int getNumShipsAdjacentTo(int vIndex, int playerNum) {
		int num = 0;
		Vertex v = getVertex(vIndex);
		for (int i=0; i<v.getNumAdjacent(); i++) {
			Route e = getRoute(vIndex, v.getAdjacent()[i]);
			if (e != null) {
				if (e.getType().isVessel && e.getPlayer() == playerNum)
					num++;
			}
		}
		return num;
	}

	/**
	 * 
	 * @param edge
	 * @return
	 */
	public Vector2D getRouteMidpoint(Route edge) {
		Vertex v0 = getVertex(edge.getFrom());
		Vertex v1 = getVertex(edge.getTo());
		float mx = (v0.getX() + v1.getX()) / 2;
		float my = (v0.getY() + v1.getY()) / 2;
		return new Vector2D(mx, my);
	}

	/**
	 * Return the list of edges adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	public Iterable<Route> getVertexRoutes(int vIndex) {
		List<Route> edges = new ArrayList<Route>(3);
		Vertex v = getVertex(vIndex);
		for (int i=0; i<v.getNumAdjacent(); i++) {
			Route e = getRoute(vIndex, v.getAdjacent()[i]);
			if (e != null)
				edges.add(e);
		}
		return edges;
	}

	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	public Iterable<Tile> getVertexTiles(int vIndex) {
		return getVertexTiles(getVertex(vIndex));
	}
	
	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	public Iterable<Tile> getVertexTiles(Vertex v) {
		List<Tile> options = new ArrayList<Tile>(3);
		for (int i=0; i<v.getNumTiles(); i++) {
			options.add(getTile(v.getTile(i)));
		}
		return options;
	}


	public boolean isIslandDiscovered(int playerNum, int islandNum) {
		return getIsland(islandNum).discovered[playerNum];
	}
	
	public void setIslandDiscovered(int playerNum, int islandNum, boolean discovered) {
		getIsland(islandNum).discovered[playerNum] = discovered;
	}
	
	public int getNumDiscoveredIslands(int playerNum) {
		int num = 0;
		for (Island i : islands) {
			if (i.discovered[playerNum])
				num++;
		}
		return num;
	}
	
	/**
	 * Return a list of vertices that is the shortest path from v0 to v1.  Both v0 and v1 are included
	 * in the list.  when v0 or v1 are equal of out of range then empty list.
	 * 
	 * @param vIndex0
	 * @param vIndex1
	 * @return
	 *
	public List<Integer> findShortestPath(int vIndex0, int vIndex1) {
		List<Integer> path = new LinkedList<Integer>();
		if (vIndex0 == vIndex1 || vIndex0 < 0 || vIndex1 < 0) {
			return path;
		}
		path.add(vIndex0);
		findShortestPathR(vIndex0, vIndex1, path, 0);
		return path;
	}
	
	/**
	 * Find the shortest path from v0 to v1.
	 * If playerNum > 0, then only routes owned by playerNum are considered
	 * If playerNum == 0, then any route is considered
	 * If playerNum < 0, then path is any route owned by -playerNum or not owned.
	 * 
	 * @param vIndex0
	 * @param vIndex1
	 * @param playerNum
	 * @return
	 */
	public List<Integer> findShortestRoute(int vIndex0, int vIndex1, int playerNum) {
		List<Integer> path = new LinkedList<Integer>();
		if (vIndex0 == vIndex1 || vIndex0 < 0 || vIndex1 < 0) {
			return path;
		}
		boolean [] visited = new boolean[verts.size()];
		path.add(vIndex0);
		findShortestPathR(vIndex0, vIndex1, path, playerNum, visited);
		System.out.println("Path=" + path);
		return path;
	}
	
	private boolean findShortestPathR(int vIndex0, int vIndex1, List<Integer> path, int playerNum, boolean [] visited) {

		visited[vIndex0] = true;
		int rIndex = getRouteIndex(vIndex0,  vIndex1);
		if (rIndex >= 0) {
			if (playerNum == 0) {
				path.add(vIndex1);
				return true;
			}
			Route r = getRoute(rIndex);
			if (playerNum > 0) {
				if (r.getPlayer() == playerNum) {
    				path.add(vIndex1);
    				return true;
				}
			} else if (r.getPlayer() == -playerNum || r.getPlayer() == 0) {
				path.add(vIndex1);
				return true;
			}
		}
		
		if (path.size() > 100)
			return false; // defensive programming yeah!
		
		Vertex v0 = getVertex(vIndex0);
		Vertex v1 = getVertex(vIndex1);
		
		Vector2D dv = new MutableVector2D(v1).subEq(v0);
		
		float [] dots = new float[3];
		int [] indices = new int[3];
		int numEdges = 0;
		
		for (int i=0; i<v0.getNumAdjacent(); i++) {
			int vIndex = v0.getAdjacent()[i];
			if (visited[vIndex])
				continue;

			if (playerNum != 0) {
				Route r = getRoute(vIndex0, vIndex);
				if (playerNum > 0) {
					if (r.getPlayer() != playerNum)
						continue;
				} else if (-playerNum != r.getPlayer() && r.getPlayer() != 0)
					continue;
			}
			
			Vertex v = getVertex(vIndex);
			Vector2D dv2 = new MutableVector2D(v).subEq(v0);

			float dot = dv.dot(dv2);
			dots[numEdges] = dot;
			indices[numEdges] = vIndex;
			
			numEdges++;
		}		
		
		// now sort the dots along with their idic
		{
			boolean swapped = true;
			do {
				
				swapped = false;
				for (int i=1; i<numEdges; i++) {
					if (dots[i-1] > dots[i]) {
						float t = dots[i];
						dots[i] = dots[i-1];
						dots[i-1] = t;
						int tt = indices[i];
						indices[i] = indices[i-1];
						indices[i-1] = tt;
						swapped = true;
					}
				}
				
			} while (swapped);
		}
		
		for (int i=numEdges-1; i>=0; i--) {
			path.add(indices[i]);
			if (findShortestPathR(indices[i], vIndex1, path, playerNum, visited))
				return true;
			
			path.remove(path.size()-1);
		}	
		
		return false;
	}
	
	public void findAllPairsShortestPathToDiscoverables(int playerNum) {
		boolean [] discoverable = new boolean[getNumVerts()];
		
		// find the verts we can touch
		for (Tile t : getTiles()) {
			boolean addIt = false;
			if (t.getType() == TileType.UNDISCOVERED)
				addIt = true;
			else {
				if (t.getIslandNum() > 0 && !isIslandDiscovered(playerNum, t.getIslandNum())) {
					addIt = true;
				}
			}
			
			if (addIt) {
				for (int i=0; i<t.getNumAdj(); i++) {
					Vertex v = getVertex(t.getAdjVert(i));
					if (v.getPlayer() == 0) {
						discoverable[t.getAdjVert(i)] = true;
					}
				}
			}
		}
		
		// Floyd-Marshall Algorithm.  Find distances without path knowledge O(|V|^3)
		int [][] dist = new int[getNumVerts()][getNumVerts()];
		for (int i=0; i<dist.length; i++) {
			Arrays.fill(dist[i], 10000);
		}
		// compute the edges.  Only unowned edges are considered 
		for (Route r : getRoutes()) {
			//if (isRouteAvailableForRoad(r, p.getPlayerNum()) || isRouteAvailableForShip(r, p.getPlayerNum())) {
			if (r.getPlayer() == 0) {
				dist[r.getFrom()][r.getTo()] = 1;
				dist[r.getTo()][r.getFrom()] = 1;
			}
		}
		
		for (int k=0; k<dist.length; k++) {
			for (int i=0; i<dist.length; i++) {
				for (int j=0; j<dist.length; j++) {
					int d = dist[i][k] + dist[k][j];
					if (dist[i][j] > d) {
						dist[i][j] = d;
					}
				}
			}
		}	
	}

	public List<Integer> getOpenKnightVertsForPlayer(int playerNum) {
		HashSet<Integer> verts = new HashSet<Integer>();
		for (int eIndex=0; eIndex<getNumRoutes(); eIndex++) {
			Route r = getRoute(eIndex);
			if (r.getPlayer() != playerNum)
				continue;
			Vertex v = getVertex(r.getFrom());
			if (v.getPlayer() == 0)
				verts.add(r.getFrom());
			v = getVertex(r.getTo());
			if (v.getPlayer() == 0)
				verts.add(r.getTo());
		}
		return new ArrayList<Integer>(verts);
	}

	public List<Vertex> getTileVertices(Tile t) {
		List<Vertex> verts = new ArrayList<Vertex>();
		for (int i=0; i<t.getNumAdj(); i++) {
			verts.add(getVertex(t.getAdjVert(i)));
		}
		return verts;
	}
/*
	public void setRoutesLocked(Iterable<Integer> routes, boolean locked) {
		for (int rIndex : routes) {
			Route r = getRoute(rIndex);
			r.setLocked(locked);
		}
	}*/
	
	public int getIslandAdjacentToVertex(Vertex v) {
		for (int tIndex =0; tIndex<v.getNumTiles(); tIndex++) {
			Tile t = getTile(v.getTile(tIndex));
			if (t.getIslandNum() > 0)
				return t.getIslandNum();
		}
		return 0;
	}

	public final int getPirateRouteStartTile() {
		return pirateRouteStartTile;
	}

	public final void setPirateRouteStartTile(int pirateRouteStartTile) {
		this.pirateRouteStartTile = pirateRouteStartTile;
	}
	
	
}


