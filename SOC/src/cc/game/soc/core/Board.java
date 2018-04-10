package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.util.*;

import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

/**
 * Represents a board with some number of hexagonal cells.  A board must be 'finalized' before it can be used with SOC.
 * 
 * @author Chris Caron
 * 
 */
public class Board extends Reflector<Board> {

    static final Logger log = LoggerFactory.getLogger(Board.class);

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
	
	private final static int GD_N		= 1;
	private final static int GD_NE		= 2;
	private final static int GD_SE		= 4;
	private final static int GD_S		= 8;
	private final static int GD_SW		= 16;
	private final static int GD_NW		= 32;

	// optimization - To prevent excessive execution of the road len algorithm O(2^n) , we cache the result here.
	private final int [] playerRoadLenCache = new int[16];
	private int pirateRouteStartTile = -1; // when >= 0, then the pirate route starts at this tile.  each tile has a next index to form a route.
	private int numAvaialbleVerts = -1;

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
		for (int i = 0; i < v.getNumAdjacentVerts(); i++) {
			if (v.getAdjacentVerts()[i] == to)
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
    		v.setNumTiles(0);
    	}
        for (int i=0; i<getNumTiles(); i++) {
            Tile c = getTile(i);
            for (int ii: c.getAdjVerts()) { 
                Vertex v = getVertex(ii);
                v.addTile(i);
            }
        }
        // now we need to rewrite all the vertices such that the perimiter verts are at the ned on the list.
        // this way way can cull them from availablibilty and speed certain operations up.
        // Also players should not be able position vertices on the perimiter

        List<Vertex> moved = new ArrayList<>();

    	// mark all the vertex we want push to back of list
    	numAvaialbleVerts = 0;
        for (int i=verts.size()-1; i>=0; i--) {
    	    Vertex v = verts.get(i);
    	    if (v.getNumTiles() < 3) {
    	        moved.add(v);
            } else {
    	        numAvaialbleVerts++;
            }
        }

        // we want to reorder the vertices such that all the unusable verts are at the end of the list
        // need to track where they are now and where they get moved too
        Map<Vertex, Integer> vMap = new HashMap<>();
        for (int i=0; i<verts.size(); i++) {
            vMap.put(verts.get(i), i);
        }

        // this pushes all the moved verts to the end of the list
        verts.removeAll(moved);
        verts.addAll(moved);

        // map tells us how to remap the indices
        int [] map = new int[verts.size()];
        for (int i=0; i<map.length; i++) {
            Vertex v = verts.get(i);
            map[vMap.get(v)] = i;
        }

        // remap all the tile vertices
        for (Tile t : tiles) {
            assert(t.getNumAdj()==6);
            for (int v=0; v<t.adjVerts.length; v++) {
                t.adjVerts[v] = map[t.adjVerts[v]];
            }
        }

        // remap adjacency verts
        for (Vertex v : verts) {
            for (int ii=0; ii<v.getNumAdjacentVerts(); ii++) {
                v.getAdjacentVerts()[ii] = map[v.getAdjacentVerts()[ii]];
            }
        }

    }
    
    private void computeRoutes() {
    	routes.clear();
    	islands.clear();
		int ii;

		// clear vertex flags
        for (int i=0; i<getNumAvailableVerts(); i++) {
            Vertex v = getVertex(i);
			v.setAdjacentToLand(false);
			v.setAdjacentToWater(false);
		}
		
		for (int i = 0; i < tiles.size(); i++) {
			Tile c = getTile(i);
			c.setIslandNum(0);
			if (c.getType() != TileType.NONE) {
				for (ii = 0; ii < c.getNumAdj(); ii++) {
					int i2 = (ii + 1) % c.getNumAdj();
					int iv0 = c.getAdjVert(ii);
					int iv1 = c.getAdjVert(i2);

					if (iv0 >= numAvaialbleVerts || iv1 >= numAvaialbleVerts)
					    continue;

					if (c.isWater()) {
						getVertex(iv0).setAdjacentToWater(true);
						getVertex(iv1).setAdjacentToWater(true);
					} else if (c.isLand()) {
						getVertex(iv0).setAdjacentToLand(true);
						getVertex(iv1).setAdjacentToLand(true);
					}
					int edgeIndex = getRouteIndex(iv0, iv1);
					Route edge = null;
					if (edgeIndex < 0) {
						edge = new Route(Math.min(iv0, iv1), Math.max(iv0, iv1));
						routes.add(-(edgeIndex+1), edge);
					} else {
						edge = getRoute(edgeIndex);
					}
					edge.addTile(i);
					if (c.isWater()) {
						edge.setAdjacentToWater(true);
					} else if (c.isLand()) {
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
		
		//visit all vertices and remove adjacencies that dont exist
		for (int vIndex=0; vIndex<getNumAvailableVerts(); vIndex++) {
			Vertex v = getVertex(vIndex);
			for (int i=0; i<v.getNumAdjacentVerts(); ) {
				Route r = getRoute(vIndex, v.getAdjacentVerts()[i]);
				if (r == null) {
					v.removeAdjacency(i);
				} else {
					i++;
				}
			}
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
     * @param startCellIndex
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
    public Collection<Island> getIslands() {
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
    public Collection<Route> getTileRoutes(Tile tile) {
    	
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
    public Collection<Integer> getTileRouteIndices(Tile tile) {
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
			if (type == RouteType.OPEN) {
				edge.reset();
			} else {
    			edge.setPlayerDoNotUse(playerNum);
    			edge.setType(type);
			}
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
		if (num < v.getNumAdjacentVerts() && num >= 0)
			return v.getAdjacentVerts()[num];
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
//		int index = getRouteIndex(r);
		Vertex v = getVertex(r.getFrom());
		if (v.isStructure() && v.getPlayer() == r.getPlayer())
			return false;
		int numEnds = 0;
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int v2 = v.getAdjacentVerts()[i];
			if (v2 != r.getTo()) {
				Route r2 = getRoute(r.getFrom(), v2);
				if ((r.isVessel() && !r2.isVessel()) || (!r.isVessel() && r2.isVessel()))
					continue;
				if (r2 != null && r2.getPlayer() == r.getPlayer()) {
					numEnds++;
					break;
				}
			}
		}
		v = getVertex(r.getTo());
		if (v.isStructure() && v.getPlayer() == r.getPlayer())
			return false;
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int v2 = v.getAdjacentVerts()[i];
			if (v2 != r.getFrom()) {
				Route r2 = getRoute(r.getTo(), v2);
				if ((r.isVessel() && !r2.isVessel()) || (!r.isVessel() && r2.isVessel()))
					continue;
				if (r2 != null && r2.getPlayer() == r.getPlayer()) {
					numEnds++;
					break;
				}
			}
		}
		return numEnds < 2;
	}

	/**
	 * Clear the board of all data.  There will be no tiles, hence the board will be unplayable.
	 */
	public void clear() {
		clearPirateRoute();
		verts.clear();
		tiles.clear();
		islands.clear();
		name = "";
		robberTile = -1;
		pirateTile = -1;
		cw = ch = 0;
		clearRoutes();
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
            if (v.getType() != VertexType.OPEN_SETTLEMENT && v.getType() != VertexType.PIRATE_FORTRESS)
			    v.setOpen();
		}
		for (Route r : routes) {
			r.reset();
		}
	}
	
	public void clearIsland() {
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
		trim();
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
	 * @param dim
     * @param fillType
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
		log.info("Trim\n  Tiles: %d\n  Verts: %d\n  Routes:  %d\n  Available Verts: %d", tiles.size(), verts.size(), routes.size(), numAvaialbleVerts);
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
        

		Utils.shuffle(dieRolls);
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
		
		Utils.shuffle(resourceOptions);
		Utils.shuffle(portOptions);
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
				case UNDISCOVERED:
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
     * Returns a subset of the verts that are available for placement of a structure
     * @return
     */
	public int getNumAvailableVerts() {
	    if (numAvaialbleVerts < 0) {
	        // recompute if neccessary
	        numAvaialbleVerts = verts.size();
	        for (int i=0; i<verts.size(); i++) {
	            Vertex v = verts.get(i);
	            if (v.getNumTiles() < 3) {
	                numAvaialbleVerts = i;
	                break;
                }
            }
        }
        return numAvaialbleVerts;
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
	public Collection<Route> getRoutesAdjacentToVertex(int vIndex) {
		Vertex v = getVertex(vIndex);
		List<Route> routes = new ArrayList<Route>(3);
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int v2 = v.getAdjacentVerts()[i];
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
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int v2 = v.getAdjacentVerts()[i];
			int r = getRouteIndex(vIndex, v2);
			if (r >= 0)
				routes.add(r);
		}
		return routes;
	}
	
	/**
	 * Convenience method to get routes adjacent and a specific route
	 * @param r
	 * @return
	 */
	public Iterable<Integer> getRouteIndicesAdjacentToRoute(Route r) {
		List<Integer> routes = (List<Integer>)getRouteIndicesAdjacentToVertex(r.getFrom());
		routes.addAll((List<Integer>)getRouteIndicesAdjacentToVertex(r.getTo()));
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
     * @param types
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
        	for (int v2 : v.getAdjacentVerts()) {
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
	    for (int i=0; i<v.getNumAdjacentVerts(); i++) {
	        int v1 = v.getAdjacentVerts()[i];
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
    	    for (int i=0; i<getNumAvailableVerts(); i++) {
    	        Vertex v = getVertex(i);
    	        if (v.getType() != VertexType.OPEN && !v.isKnight()) // TOOD: should we check here for enableRoadBlock and only consider knights if true?
    	            continue; // skip past verts with settlements on them
    	        //Utils.fillArray(visited, false);
    	        Arrays.fill(visitedEdges, false);
    	        int [] len = {0,0,0};
    	        for (int ii=0; ii<v.getNumAdjacentVerts(); ii++) {
    	            int v1 = v.getAdjacentVerts()[ii];
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
	 * @param playerNum
	 * @return
	 */
	public int getNumStructuresForPlayer(int playerNum) {
		return getNumVertsOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_TRADE);
	}

	/**
	 * Count the number of vertices assigned to player that are settlements.
	 * @param playerNum
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
	public List<Integer> getVertIndicesOfType(int playerNum, VertexType ... types) {
		List<Integer> verts = new ArrayList<Integer>();
		List<VertexType> arr = Arrays.asList(types);
		for (int i = 0; i < getNumAvailableVerts(); i++) {
			Vertex v = getVertex(i);
			if ((playerNum == 0 || playerNum == v.getPlayer()) && arr.contains(v.getType()))
				verts.add(i);
		}
		return verts;
	}
	
	/**
	 * 
	 * @param playerNum
	 * @param types
	 * @return
	 */
	public List<Vertex> getVertsOfType(int playerNum, VertexType ... types) {
		List<Vertex> verts = new ArrayList<Vertex>();
		List<VertexType> arr = Arrays.asList(types);
		for (int i = 0; i < getNumAvailableVerts(); i++) {
			Vertex v = getVertex(i);
			if ((playerNum == 0 || playerNum == v.getPlayer()) && arr.contains(v.getType()))
				verts.add(v);
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
		return getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getCitiesForPlayer(int playerNum) {
		return getVertIndicesOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY);
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getSettlementsForPlayer(int playerNum) {
		return getVertIndicesOfType(playerNum, VertexType.SETTLEMENT);
	}

	/**
	 * 
	 * @param playerNum
	 * @return
	 */
	public List<Integer> getStructuresForPlayer(int playerNum) {
		return getVertIndicesOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
	}

	/**
	 * Return true if a player has any structure adjacent to a given cell.
	 * @param playerNum
	 * @param cellIndex
	 * @return
	 */
	public boolean isPlayerAdjacentToTile(int playerNum, int cellIndex) {
		Tile cell = getTile(cellIndex);
		//assert (cell.numAdj > 0 && cell.numAdj <= 6);
		for (int vIndex : cell.getAdjVerts()) {
			Vertex vertex = getVertex(vIndex);
			if (vertex.isStructure() && vertex.getPlayer() == playerNum)
				return true;
		}
		return false;
	}
	
	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edgeIndex
	 * @param playerNum
	 * @return
	 */
	public boolean isRouteAvailableForRoad(int edgeIndex, int playerNum) {
		return isRouteAvailableForRoad(getRoute(edgeIndex), playerNum);
	}
	
	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edge
	 * @param playerNum
	 * @return
	 */
	public boolean isRouteAvailableForRoad(Route edge, int playerNum) {
		if (edge.getPlayer() != 0 || !edge.isAdjacentToLand() || edge.isLocked() || edge.isClosed()) {
			 return false;
		 }

		// if either vertex is a structure then ok to place
		Vertex v0 = getVertex(edge.getFrom());
		Vertex v1 = getVertex(edge.getTo());

		// we can place a knight on an opponents knight if we have an adjacent knight that is of higher rank
        int knightRank = -1;
        if (v0.isKnight() && v0.getPlayer() == playerNum) {
            knightRank = v0.getType().getKnightLevel();
        }

        if (v1.isKnight() && v1.getPlayer() == playerNum) {
            knightRank = Math.max(knightRank, v1.getType().getKnightLevel());
        }

		if (v0.isKnight() && v0.getPlayer() != playerNum && v0.getType().getKnightLevel() > knightRank)
			return false;
		
		if (v1.isKnight() && v1.getPlayer() != playerNum && v1.getType().getKnightLevel() > knightRank)
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
     * @param rules
	 * @param edge
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
		if (v.isStructure() && v.getPlayer() == playerNum && isVertexAdjacentToWater(v))
			return true;
		
		v = getVertex(edge.getTo());
		if (v.isStructure() && v.getPlayer() == playerNum && isVertexAdjacentToWater(v))
			return true;
		
		if (rules.isEnableBuildShipsFromPort()) {
			for (Tile t : getRouteTiles(edge)) {
				if (t.isPort()) {
				    if (isVertexAdjacentToPlayerRoad(edge.getFrom(), playerNum) || isVertexAdjacentToPlayerRoad(edge.getTo(), playerNum))
				        return true;
                }
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

		for (int i = 0; i < v.getNumAdjacentVerts(); i++) {
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
	 * @param playerNum
     * @param flag
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
	 * @param playerNum
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
        for (int i=0; i<vertex.getNumAdjacentVerts(); i++) {
            int toVertexIndex = vertex.getAdjacentVerts()[i];
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
    	for (int i=0; i<v.getNumAdjacentVerts(); i++) {
    		Route r = getRoute(vIndex, v.getAdjacentVerts()[i]);
    		if (r.getType().isRoute && r.getPlayer() != v.getPlayer()) {
    			if (playerNum == 0)
    				playerNum = r.getPlayer();
    			else if (playerNum == r.getPlayer())
    				return playerNum;
    		}
    	}
    	return 0;
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
    	GRectangle minMax = computeMinMax();
    	Vector2D v = minMax.getCenter();
    	translate(v.getX(), v.getY());
    }

    public final GRectangle computeMinMax() {
    	MutableVector2D min = new MutableVector2D(Vector2D.MAX);
    	MutableVector2D max = new MutableVector2D(Vector2D.MIN);
    	for (Tile c : tiles) {
    		if (c.getType() == TileType.NONE)
    			continue;
            min.minEq(c);
            max.maxEq(c);
    	}
    	max.addEq(cw/2, ch/2);
    	min.subEq(cw/2, ch/2);
    	return new GRectangle(min, max);
    }
    
    /**
     * Fit all cells into the 0,0 x 1,1 rectangle
     */
    public final void fillFit() {
    	GRectangle minMax = computeMinMax();
    	//if (minMax.w <= 1 && minMax.h <= 1)
    	//    return;

    	Vector2D v = minMax.getCenter();
    	// center at 0,0
    	translate(v.getX(), v.getY());
    	// fill a 1,1 rect
    	scale(1.0f / minMax.w, 1.0f / minMax.h);
    	// move to 0.5, 0.5
    	translate(-v.getX(), -v.getY());

    	minMax = computeMinMax();
        translate(-minMax.x, -minMax.y);
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
     * @param vIndex
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
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	public int getNumShipsAdjacentTo(int vIndex, int playerNum) {
		int num = 0;
		Vertex v = getVertex(vIndex);
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			Route e = getRoute(vIndex, v.getAdjacentVerts()[i]);
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
	public Collection<Route> getVertexRoutes(int vIndex) {
		List<Route> edges = new ArrayList<Route>(3);
		Vertex v = getVertex(vIndex);
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			Route e = getRoute(vIndex, v.getAdjacentVerts()[i]);
			if (e != null)
				edges.add(e);
		}
		return edges;
	}

	/**
	 * Return the list of edges adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	public Collection<Integer> getVertexRouteIndices(int vIndex) {
		List<Integer> edges = new ArrayList<Integer>(3);
		Vertex v = getVertex(vIndex);
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int rIndex = getRouteIndex(vIndex, v.getAdjacentVerts()[i]);
			if (rIndex >= 0)
				edges.add(rIndex);
		}
		return edges;
	}

	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	public Collection<Tile> getVertexTiles(int vIndex) {
		return getVertexTiles(getVertex(vIndex));
	}

	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param v
	 * @return
	 */
	public Collection<Tile> getVertexTiles(Vertex v) {
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
	
	public int getKnightLevelForPlayer(int playerNum, boolean active, boolean inactive) {
		int level = 0;
		for (int kIndex : getKnightsForPlayer(playerNum)) {
			VertexType type = getVertex(kIndex).getType();
			if (type.isKnightActive() && active)
				level += getVertex(kIndex).getType().getKnightLevel();
			else if (!type.isKnightActive() && inactive)
				level += getVertex(kIndex).getType().getKnightLevel();
		}
		return level;
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

	@Omit
    private static final HashMap<String, IDistances> distancesCache = new HashMap<>();

	/**
	 * Return a structure that can compute the distance/path between any 2 vertices.  
	 * When transitioning from land to water, these routes must pass through a structure and/or port depending on rules.
	 * 
	 * @param rules
	 * @param playerNum
	 * @return
	 */
	public IDistances computeDistances(final Rules rules, final int playerNum) {
		
		if (rules.isEnableSeafarersExpansion()) {
			return computeDistancesLandWater(rules, playerNum);
		} else {
			final int numV = getNumAvailableVerts();
			final int [][] dist = new int[numV][numV];
			final int [][] next = new int[numV][numV];

			for (int i=0; i<numV; i++) {
				for (int ii=0; ii<numV; ii++) {
					dist[i][ii] = DistancesLandWater.DISTANCE_INFINITY;
					next[i][ii] = ii;
				}
				dist[i][i] = 0;
			}

			int [] rcache = new int[routes.size()];
			int rcacheLen = 0;

			// Assign distances to edges
			for (int rIndex=0; rIndex<routes.size(); rIndex++) {

			    Route r = routes.get(rIndex);
				final int v0 = r.getFrom();
				final int v1 = r.getTo();
				
				if (r.getPlayer() == 0) {
					dist[v0][v1] = dist[v1][v0] = 1;
				} else if (r.getPlayer() == playerNum) {
					switch (r.getType()) {
						case DAMAGED_ROAD:
						case ROAD:
							dist[v0][v1] = dist[v1][v0] = 0;
							rcache[rcacheLen++] = rIndex;
							break;
						case SHIP:
						case WARSHIP:
						case OPEN:
						default:
							assert(false);
						
					}
				}
				
			}

            // OPTIMIZATION: check if the current route configuration has already been done, then no need to do it again
            Arrays.sort(rcache, 0, rcacheLen);
            String str = Utils.toString(rcache, 0, rcacheLen);
            if (distancesCache.containsKey(str))
                return distancesCache.get(str);

            // All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
			// where every vertex has 2 or 3 edges.  The memory usage and complexity of a Dijkstra's make it less desirable.  
			for (int k=0; k<numV; k++) {
				for (int i=0; i<numV; i++) {
					for (int j=0; j<numV; j++) {
						int sum = dist[i][k] + dist[k][j];
						if (sum < dist[i][j]) {
							dist[i][j] = sum;
							next[i][j] = next[i][k];
						}
					}
				}
			}		
			
			IDistances d = new DistancesLand(dist, next);
			distancesCache.put(str, d);
			return d;
		}
	}

	private IDistances computeDistancesLandWater(final Rules rules, int playerNum) {
		
		final int numV = getNumAvailableVerts();
		final int [][] distLand = new int[numV][numV];
		final int [][] distAqua = new int[numV][numV];
		final int [][] nextLand = new int[numV][numV];
		final int [][] nextAqua = new int[numV][numV];

		for (int i=0; i<numV; i++) {
			for (int ii=0; ii<numV; ii++) {
				distLand[i][ii] = DistancesLandWater.DISTANCE_INFINITY;
				distAqua[i][ii] = DistancesLandWater.DISTANCE_INFINITY;
				nextLand[i][ii] = ii;
				nextAqua[i][ii] = ii;
			}
			distLand[i][i] = 0;
			distAqua[i][i] = 0;
		}

		int [] rcache = new int[routes.size()];
		int rcacheLen = 0;

		// Assign distances to edges
		for (int rIndex =0; rIndex<routes.size(); rIndex++) {

		    Route r= routes.get(rIndex);
			final int v0 = r.getFrom();
			final int v1 = r.getTo();
			
			if (r.getPlayer() == 0) {
				if (r.isAdjacentToLand()) {
					distLand[v0][v1] = distLand[v1][v0] = 1;
				}
				if (r.isAdjacentToWater()) {
					distAqua[v0][v1] = distAqua[v1][v0] = 1;
				}
			} else if (r.getPlayer() == playerNum) {
				switch (r.getType()) {
					case DAMAGED_ROAD:
					case ROAD:
						distLand[v0][v1] = distLand[v1][v0] = 0;
						rcache[rcacheLen++] = rIndex;
						break;
					case SHIP:
					case WARSHIP:
						distAqua[v0][v1] = distAqua[v1][v0] = 0;
						rcache[rcacheLen++] = rIndex*1000; // for water
						break;
					case OPEN:
					default:
						assert(false);
					
				}
			}
		}

		// OPTIMIZATION: check if the current route configuration has already been done, then no need to do it again
        Arrays.sort(rcache, 0, rcacheLen);
		String str = Utils.toString(rcache, 0, rcacheLen);
		if (distancesCache.containsKey(str))
		    return distancesCache.get(str);

		// All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
		// where every vertex has 2 or 3 edges.  The memory usage and complexity of a Dijkstra's make it less desirable.  
		for (int k=0; k<numV; k++) {
			for (int i=0; i<numV; i++) {
				for (int j=0; j<numV; j++) {
					int sum = distLand[i][k] + distLand[k][j];
					if (sum < distLand[i][j]) {
						distLand[i][j] = sum;
						nextLand[i][j] = nextLand[i][k];
					}
					sum = distAqua[i][k] + distAqua[k][j];
					if (sum < distAqua[i][j]) {
						distAqua[i][j] = sum;
						nextAqua[i][j] = nextAqua[i][k];
					}
				}
			}
		}
		
		Set<Integer> launchVerts = new HashSet<Integer>();
		
		// find all vertices where we can launch a ship from
		for (int vIndex=0; vIndex<getNumAvailableVerts(); vIndex++) {
			Vertex v = getVertex(vIndex);
			if (v.isAdjacentToWater()) {
    			if (v.isStructure() && v.getPlayer() == playerNum) {
    				launchVerts.add(vIndex);
    			} else if (v.getPlayer() == 0 && rules.isEnableBuildShipsFromPort() && isVertexAdjacentToPlayerRoute(vIndex, playerNum)) {
    				for (Tile t : getVertexTiles(vIndex)) {
    					if (t.getType().isPort) {
    						launchVerts.add(vIndex);
    						break;
    					}
    				}
    			}
			}
			
		}
		
		IDistances d = new DistancesLandWater(distLand, nextLand, distAqua, nextAqua, launchVerts);
		distancesCache.put(str, d);
		return d;
	}

	@Override
    public void loadFromFile(File file) throws IOException {
        super.loadFromFile(file);
        setName(file.getAbsolutePath());
    }
}


