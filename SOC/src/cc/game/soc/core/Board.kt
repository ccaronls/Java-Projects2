package cc.game.soc.core

import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.LRUCache
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a board with some number of hexagonal cells.  A board must be 'finalized' before it can be used with SOC.
 *
 * @author Chris Caron
 */
class Board : Reflector<Board>() {
	companion object {
		val log = LoggerFactory.getLogger(Board::class.java)
		private const val GD_N = 1
		private const val GD_NE = 2
		private const val GD_SE = 4
		private const val GD_S = 8
		private const val GD_SW = 16
		private const val GD_NW = 32

		init {
			addAllFields(Board::class.java)
		}
	}

	private val tiles = Vector<Tile>()
	private val verts = Vector<Vertex>()
	private val routes = Vector<Route>()
	private val islands = Vector<Island>()

	/**
	 * Get the cell index the robber is assigned to
	 * @return
	 */
	var robberTileIndex = -1
		private set

	/**
	 *
	 * @return
	 */
	var pirateTileIndex = -1
		private set

	/**
	 *
	 * @return
	 */
	var merchantTileIndex = -1
		private set

	/**
	 *
	 * @return
	 */
	var merchantPlayer = 0 // player num who last placed the merchant or 0 is not played
		private set
	/**
	 * Get the name of the board
	 * @return
	 */
	/**
	 * Set the name of the board
	 * @param name
	 */
	var name = ""

	var displayName: String = "Unnamed"

	/**
	 * Get the width of a single hexagon cell
	 * @return
	 */
	var tileWidth = 0f
		private set

	/**
	 * Get the height of a single hexagon cell
	 * @return
	 */
	var tileHeight = 0f
		private set

	// optimization - To prevent excessive execution of the road len algorithm O(2^n) , we cache the result here.
	@Omit
	private val playerRoadLenCache = IntArray(16)
	var pirateRouteStartTile = -1 // when >= 0, then the pirate route starts at this tile.  each tile has a next index to form a route.
	private var numAvaialbleVerts = -1

	@Omit
	private val distancesCache = LRUCache<String, IDistances>(100)

	/**
	 * Reset the board to its initial playable state.
	 */
	fun reset() {
		robberTileIndex = -1
		pirateTileIndex = -1
		merchantTileIndex = -1
		merchantPlayer = -1
		clearRouteLenCache()
		for (c in tiles) {
			c.reset()
		}
		for (v in verts) {
			if (v.type !== VertexType.OPEN_SETTLEMENT && v.type !== VertexType.PIRATE_FORTRESS) v.setOpen()
		}
		for (r in routes) {
			r.reset()
		}
		distancesCache.clear()
	}

	private fun generateR2(cx: Float, cy: Float, w: Float, h: Float, z: Float, depth: Int, dir: Int, allowDup: Boolean, type: TileType) {
		if (depth <= 0) return
		if (!makeTile(cx, cy, w, h, z, type) && !allowDup) return  // duplicate, dont recurse
		if (dir and GD_N != 0) generateR2(cx, cy - h, w, h, z, depth - 1, dir, false, type)
		if (dir and GD_S != 0) generateR2(cx, cy + h, w, h, z, depth - 1, dir, false, type)
		if (dir and GD_NE != 0) generateR2(cx + w / 2 + z / 2, cy - h / 2, w, h, z, depth - 1, dir, false, type)
		if (dir and GD_SE != 0) generateR2(cx + w / 2 + z / 2, cy + h / 2, w, h, z, depth - 1, dir, false, type)
		if (dir and GD_NW != 0) generateR2(cx - w / 2 - z / 2, cy - h / 2, w, h, z, depth - 1, dir, false, type)
		if (dir and GD_SW != 0) generateR2(cx - w / 2 - z / 2, cy + h / 2, w, h, z, depth - 1, dir, false, type)
	}

	private fun generateR(cx: Float, cy: Float, w: Float, h: Float, z: Float, type: TileType) {
		if (cx - w / 2 < 0 || cx + w / 2 > 1) return
		if (cy - h / 2 < 0 || cy + h / 2 > 1) return
		if (!makeTile(cx, cy, w, h, z, type)) return  // duplicate, dont recurse
		generateR(cx, cy - h, w, h, z, type)
		generateR(cx, cy + h, w, h, z, type)
		generateR(cx + w / 2 + z / 2, cy - h / 2, w, h, z, type)
		generateR(cx + w / 2 + z / 2, cy + h / 2, w, h, z, type)
		generateR(cx - w / 2 - z / 2, cy - h / 2, w, h, z, type)
		generateR(cx - w / 2 - z / 2, cy + h / 2, w, h, z, type)
	}

	private fun makeTile(cx: Float, cy: Float, w: Float, h: Float, z: Float, type: TileType): Boolean {
		val a = addVertex(cx - w / 2, cy)
		val b = addVertex(cx - z / 2, cy - h / 2)
		val c = addVertex(cx + z / 2, cy - h / 2)
		val d = addVertex(cx + w / 2, cy)
		val e = addVertex(cx + z / 2, cy + h / 2)
		val f = addVertex(cx - z / 2, cy + h / 2)
		var added = false
		if (addAdjacency(a, b)) added = true
		if (addAdjacency(b, c)) added = true
		if (addAdjacency(c, d)) added = true
		if (addAdjacency(d, e)) added = true
		if (addAdjacency(e, f)) added = true
		if (addAdjacency(f, a)) added = true
		if (!added) return false
		val cell = Tile(cx, cy, type)
		tiles.add(cell)
		val adjVerts = intArrayOf(a, b, c, d, e, f)
		cell.setAdjVerts(adjVerts)
		return true
	}

	private fun addVertex(x: Float, y: Float): Int {
		var index = getVertex(x, y)
		if (index < 0) {
			index = verts.size
			verts.add(Vertex(x, y))
		}
		return index
	}

	private fun addAdjacency(v0: Int, v1: Int): Boolean {
		var added = false
		if (addAdjacent(v0, v1)) added = true
		if (addAdjacent(v1, v0)) added = true
		return added
	}

	private fun addAdjacent(from: Int, to: Int): Boolean {
		val v = getVertex(from)
		for (i in 0 until v.numAdjacentVerts) {
			if (v.adjacentVerts[i] == to) return false
		}
		v.addAdjacentVertex(to)
		return true
	}

	// compute the cell lookups for each vertex
	private fun computeVertexTiles() {
		for (v in verts) {
			v.numTiles = 0
		}
		for (i in 0 until numTiles) {
			val c = getTile(i)
			for (ii in c.getAdjVerts()) {
				val v = getVertex(ii)
				v.addTile(i)
			}
		}
		// now we need to rewrite all the vertices such that the perimiter verts are at the ned on the list.
		// this way way can cull them from availablibilty and speed certain operations up.
		// Also players should not be able position vertices on the perimiter
		val moved: MutableList<Vertex> = ArrayList()

		// mark all the vertex we want push to back of list
		numAvaialbleVerts = 0
		for (i in verts.indices.reversed()) {
			val v = verts[i]
			if (v.numTiles < 3) {
				moved.add(v)
			} else {
				numAvaialbleVerts++
			}
		}

		// we want to reorder the vertices such that all the unusable verts are at the end of the list
		// need to track where they are now and where they get moved too
		val vMap: MutableMap<Vertex, Int> = HashMap()
		for (i in verts.indices) {
			vMap[verts[i]] = i
		}

		// this pushes all the moved verts to the end of the list
		verts.removeAll(moved)
		verts.addAll(moved)

		// map tells us how to remap the indices
		val map = IntArray(verts.size)
		for (i in map.indices) {
			val v = verts[i]
			map[vMap[v]!!] = i
		}

		// remap all the tile vertices
		for (t in tiles) {
			assert(t.numAdj == 6)
			for (v in 0 until t.adjVerts.size) {
				t.adjVerts[v] = map[t.adjVerts[v]]
			}
		}

		// remap adjacency verts
		for (v in verts) {
			for (ii in 0 until v.numAdjacentVerts) {
				v.adjacentVerts[ii] = map[v.adjacentVerts[ii]]
			}
		}
	}

	private fun computeRoutes() {
		routes.clear()
		islands.clear()
		var ii: Int

		// clear vertex flags
		for (i in 0 until numAvailableVerts) {
			val v = getVertex(i)
			v.isAdjacentToLand = false
			v.isAdjacentToWater = false
		}
		for (i in tiles.indices) {
			val c = getTile(i)
			c.islandNum = 0
			if (c.type !== TileType.NONE) {
				ii = 0
				while (ii < c.numAdj) {
					val i2 = (ii + 1) % c.numAdj
					val iv0 = c.getAdjVert(ii)
					val iv1 = c.getAdjVert(i2)
					if (iv0 >= numAvaialbleVerts || iv1 >= numAvaialbleVerts) {
						ii++
						continue
					}
					if (c.isWater) {
						getVertex(iv0).isAdjacentToWater = true
						getVertex(iv1).isAdjacentToWater = true
					} else if (c.isLand) {
						getVertex(iv0).isAdjacentToLand = true
						getVertex(iv1).isAdjacentToLand = true
					}
					val edgeIndex = getRouteIndex(iv0, iv1)
					var edge = if (edgeIndex < 0) {
						Route(min(iv0, iv1), max(iv0, iv1)).also {
							routes.add(-(edgeIndex + 1), it)
						}
					} else {
						getRoute(edgeIndex)
					}
					edge.addTile(i)
					if (c.isWater) {
						edge.isAdjacentToWater = true
					} else if (c.isLand) {
						edge.isAdjacentToLand = true
					}
					ii++
				}
			}
		}

		// Very important for edges to be sorted as this is a big bottleneck area.  Need O(lg n) or better access to to look up an edge.
		Collections.sort(routes)

		// make sure edges are unique and in ascending order
		if (Utils.isDebugEnabled()) {
			// TODO: do we need this sanity check anymore?
			for (i in 1 until routes.size) {
				val e0 = getRoute(i - 1)
				val e1 = getRoute(i)
				if (e1.compareTo(e0) <= 0) throw RuntimeException("Edges out of order or not unique")
			}
		}
		//visit all vertices and remove adjacencies that dont exist
		for (vIndex in 0 until numAvailableVerts) {
			val v = getVertex(vIndex)
			var i = 0
			while (i < v.numAdjacentVerts) {
				val r = getRoute(vIndex, v.adjacentVerts[i])
				if (r == null) {
					v.removeAdjacency(i)
				} else {
					i++
				}
			}
		}
	}

	/**
	 *
	 * @param r
	 * @return
	 */
	fun getRouteTiles(r: Route): Iterable<Tile> {
		return object : Iterable<Tile> {
			var index = 0
			override fun iterator(): Iterator<Tile> {
				return object : MutableIterator<Tile> {
					override fun remove() {
						throw GException("Cannot remove")
					}

					override fun next(): Tile {
						return getTile(r.getTile(index++))
					}

					override fun hasNext(): Boolean {
						return index < 2 && r.getTile(index) >= 0
					}
				}
			}
		}
	}

	/**
	 * Iterate over cells are count the number of islands
	 *
	 * @return
	 */
	val numIslands: Int
		get() = islands.size

	/**
	 *
	 * @param startCellIndex
	 * @return
	 */
	fun findIslandTiles(startCellIndex: Int): List<Int> {
		val t = getTile(startCellIndex)
		if (t.islandNum > 0) {
			return getIsland(t.islandNum).tiles
		}
		val visited = BooleanArray(tiles.size)
		val tiles: MutableList<Int> = ArrayList()
		computeIslandTilesDFS(startCellIndex, visited, tiles)
		return tiles
	}

	private fun computeIslandTilesDFS(start: Int, visited: BooleanArray, cells: MutableList<Int>) {
		if (visited[start]) return
		visited[start] = true
		val cell = getTile(start)
		if (cell.isWater) return
		cells.add(start)
		for (vIndex in cell.getAdjVerts()) {
			val v = getVertex(vIndex)
			for (cIndex in 0 until v.numTiles) {
				computeIslandTilesDFS(v.getTile(cIndex), visited, cells)
			}
		}
	}

	/**
	 * Find all adjacent cells from a starting cell that form an island.
	 * If start is already an island, then just return is num > 0
	 *
	 * Return 0 means start cell was not land.
	 *
	 * @param startCellIndex
	 */
	fun createIsland(startCellIndex: Int): Int {
		val start = getTile(startCellIndex)
		if (start.islandNum > 0) return start.islandNum
		if (start.isWater) return 0
		val num = islands.size + 1
		val island = Island(num)
		for (cIndex in findIslandTiles(startCellIndex)) {
			getTile(cIndex).islandNum = num
			island.tiles.add(cIndex)
			for (rIndex in getTileRouteIndices(getTile(cIndex))) {
				if (getRoute(rIndex).isShoreline) island.borderRoute.add(rIndex)
			}
		}
		islands.add(island)
		return num
	}

	/**
	 *
	 * @param islandNum
	 */
	fun removeIsland(islandNum: Int) {
		if (islandNum < 0 || islandNum > islands.size) return
		islands.removeAt(islandNum - 1)
		var num = 1
		for (i in islands) {
			i.num = num++
		}
		for (t in getTiles()) {
			num = t.islandNum
			if (num == islandNum) {
				t.islandNum = 0
			} else if (num > islandNum) {
				t.islandNum = num - 1
			}
		}
	}

	/**
	 * Get an island by island num.
	 * @param num value >= 1
	 * @return
	 */
	fun getIsland(num: Int): Island {
		return islands[num - 1]
	}

	/**
	 *
	 * @return
	 */
	fun getIslands(): Collection<Island> {
		return islands
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getNumPlayerDiscoveredIslands(playerNum: Int): Int {
		var num = 0
		for (i in getIslands()) {
			if (i.isDiscoveredBy(playerNum)) num++
		}
		return num
	}

	/**
	 *
	 * @param startCellIndex
	 * @return
	 */
	fun findIslandShoreline(startCellIndex: Int): Collection<Int> {
		val tile = getTile(startCellIndex)
		if (tile.islandNum > 0) return getIsland(tile.islandNum).borderRoute
		val edges: MutableSet<Int> = HashSet()
		val visited = BooleanArray(tiles.size)
		findIslandEdgesDFS(startCellIndex, visited, edges)
		return edges
	}

	private fun findIslandEdgesDFS(start: Int, visited: BooleanArray, edges: MutableSet<Int>) {
		if (visited[start]) return
		visited[start] = true
		val c = getTile(start)
		if (c.isWater) return
		for (eIndex in getTileRouteIndices(c)) {
			val e = getRoute(eIndex)
			if (e.isShoreline) {
				edges.add(eIndex)
			}
		}
		for (vIndex in c.getAdjVerts()) {
			val v = getVertex(vIndex)
			for (i in 0 until v.numTiles) {
				findIslandEdgesDFS(v.getTile(i), visited, edges)
			}
		}
	}

	/**
	 *
	 * @param tile
	 * @return
	 */
	fun getTileRoutes(tile: Tile): Collection<Route> {
		val edges: MutableList<Route> = ArrayList()
		for (i in 0 until tile.numAdj) {
			val v0 = tile.getAdjVert(i)
			val v1 = tile.getAdjVert((i + 1) % tile.numAdj)
			val eIndex = getRouteIndex(v0, v1)
			if (eIndex >= 0) {
				edges.add(getRoute(eIndex))
			}
		}
		return edges
	}

	fun getTileRoutesOfType(tile: Tile, type: RouteType): List<Route> {
		val edges: MutableList<Route> = ArrayList()
		for (i in 0 until tile.numAdj) {
			val v0 = tile.getAdjVert(i)
			val v1 = tile.getAdjVert((i + 1) % tile.numAdj)
			val eIndex = getRouteIndex(v0, v1)
			if (eIndex >= 0) {
				val r = getRoute(eIndex)
				if (r.type === type) edges.add(getRoute(eIndex))
			}
		}
		return edges
	}

	/**
	 *
	 * @param tile
	 * @return
	 */
	fun getTileRouteIndices(tile: Tile): Collection<Int> {
		val edges: MutableList<Int> = ArrayList()
		for (i in 0 until tile.numAdj) {
			val v0 = tile.getAdjVert(i)
			val v1 = tile.getAdjVert((i + 1) % tile.numAdj)
			val eIndex = getRouteIndex(v0, v1)
			if (eIndex >= 0) {
				edges.add(eIndex)
			}
		}
		return edges
	}

	/**
	 * Return the index of a vertex.  v may be a copy.
	 * @param v
	 * @return
	 */
	fun getVertexIndex(v: Vertex): Int {
		return verts.indexOf(v)
	}

	/**
	 * Return the index of an edge.  e may be a copy
	 * @param e
	 * @return
	 */
	fun getRouteIndex(e: Route): Int {
		return routes.indexOf(e)
	}

	/**
	 * Return the index of a cell.  c may be a copy
	 * @param c
	 * @return
	 */
	fun getTileIndex(c: Tile): Int {
		return tiles.indexOf(c)
	}

	/**
	 * Use this to remove a player from a route
	 * @param r
	 */
	fun setRouteOpen(r: Route) {
		setPlayerForRoute(r, 0, RouteType.OPEN)
	}

	/**
	 *
	 * @param edge
	 * @param playerNum
	 */
	fun setPlayerForRoute(edge: Route, playerNum: Int, type: RouteType) {
		if (edge.player != playerNum) {
			if (playerNum == 0) {
				assert(type === RouteType.OPEN)
				playerRoadLenCache[edge.player] = -1
			} else {
				assert(type !== RouteType.OPEN)
				playerRoadLenCache[playerNum] = -1
			}
			if (type === RouteType.OPEN) {
				edge.reset()
			} else {
				edge.setPlayerDoNotUse(playerNum)
				edge.type = type
			}
		}
	}

	/**
	 * Set the cell the robber is assigned to.  Setting to -1 means no robber
	 * @param cellNum
	 */
	fun setRobber(cellNum: Int) {
		robberTileIndex = cellNum
	}

	/**
	 *
	 * @param cellNum
	 */
	fun setPirate(cellNum: Int) {
		if (pirateTileIndex != cellNum) {
			if (pirateTileIndex >= 0) {
				setTileRoutesImmobile(pirateTileIndex, false)
			}
			if (cellNum >= 0) {
				setTileRoutesImmobile(cellNum, true)
			}
			pirateTileIndex = cellNum
		}
	}

	private fun setTileRoutesImmobile(pirateCell: Int, isImmobile: Boolean) {
		val cell = getTile(pirateCell)
		for (eIndex in getTileRouteIndices(cell)) {
			getRoute(eIndex).isAttacked = isImmobile
		}
	}

	/**
	 * Convenience method to set the robber using a cell.  cell can be null and indicates the robber is unassigned (-1)
	 * @param cell
	 */
	fun setRobberTile(cell: Tile?) {
		robberTileIndex = if (cell == null) -1 else getTileIndex(cell)
	}

	/**
	 *
	 * @param cell
	 */
	fun setPirateTile(cell: Tile?) {
		setPirate(cell?.let { getTileIndex(it) } ?: -1)
	}

	/**
	 *
	 * @return
	 */
	fun getRobberTile(): Tile? {
		return if (robberTileIndex < 0) null else getTile(robberTileIndex)
	}

	/**
	 *
	 * @return
	 */
	fun getPirateTile(): Tile? {
		return if (pirateTileIndex < 0) null else getTile(pirateTileIndex)
	}

	/**
	 *
	 * @return
	 */
	fun getMerchantTile(): Tile? {
		return if (merchantTileIndex < 0) null else getTile(merchantTileIndex)
	}

	/**
	 *
	 * @param merchantTile
	 */
	fun setMerchant(merchantTile: Int, playerNum: Int) {
		merchantTileIndex = merchantTile
		merchantPlayer = playerNum
	}

	/**
	 *
	 * @return
	 *
	fun getDisplayName(): String {
		return displayName?:run {
			if (Utils.isEmpty(name)) return "Unnamed"
			val file = File(name)
			if (file.isFile) Utils.toPrettyString(file.name) else ""
		}
	}

	/**
	 *
	 * @param displayName
	 */
	fun setDisplayName(displayName: String) {
		this.displayName = displayName
	}*/

	/**
	 * Get the vertex at point x,y such that |P(x,y) - P(v)| < epsilon
	 * @param x
	 * @param y
	 * @return
	 */
	fun getVertex(x: Float, y: Float): Int {
		var minD = 0.001
		var index = -1
		for (i in 0 until numVerts) {
			val v = verts[i]
			val dx = Math.abs(v.x - x).toDouble()
			val dy = Math.abs(v.y - y).toDouble()
			val d = dx * dx + dy * dy
			//if (dx < 1 && dy < 1)
			if (d < minD) {
				minD = d
				index = i
			}
		}
		return index
	}

	/**
	 * Get the edge between 2 vertex indices
	 * @param from
	 * @param to
	 * @return
	 */
	fun getRouteIndex(from: Int, to: Int): Int {
		var from = from
		var to = to
		if (from > to) {
			val t = from
			from = to
			to = t
		}
		val key = Route(from, to)
		return Collections.binarySearch(routes, key)
	}

	/**
	 * Get the index of the next adjacent vertex after num of vert.  This allows iteration over adjacent verts.
	 * Example:
	 * for (int i=0; ; i++) {
	 * int cur = findAdjacentVertex(vert, i);
	 * if (cur >= 0) {
	 * doSomething(getVertex(cur));
	 * }
	 * }
	 * @param vert
	 * @param num
	 * @return
	 */
	fun findAdjacentVertex(vert: Int, num: Int): Int {
		val v = getVertex(vert)
		return if (num < v.numAdjacentVerts && num >= 0) v.adjacentVerts[num] else -1
	}

	/**
	 * Return true if 2 edges share a SINGLE endpoint.
	 * @param e0
	 * @param e1
	 * @return
	 */
	fun isRoutesAdjacent(e0: Int, e1: Int): Boolean {
		if (e0 == e1) return false
		val E0 = routes[e0]
		val E1 = routes[e1]
		return E0.from == E1.from || E0.from == E1.to || E0.to == E1.from || E0.to == E1.to
	}

	fun isRouteOpenEnded(r: Route): Boolean {
		assert(r.player != 0)
		//		int index = getRouteIndex(r);
		var v = getVertex(r.from)
		if (v.isStructure && v.player == r.player) return false
		var numEnds = 0
		for (i in 0 until v.numAdjacentVerts) {
			val v2 = v.adjacentVerts[i]
			if (v2 != r.to) {
				val r2 = getRoute(r.from, v2)
				if (r.isVessel && !r2!!.isVessel || !r.isVessel && r2!!.isVessel) continue
				if (r2 != null && r2.player == r.player) {
					numEnds++
					break
				}
			}
		}
		v = getVertex(r.to)
		if (v.isStructure && v.player == r.player) return false
		for (i in 0 until v.numAdjacentVerts) {
			val v2 = v.adjacentVerts[i]
			if (v2 != r.from) {
				val r2 = getRoute(r.to, v2)
				if (r.isVessel && !r2!!.isVessel || !r.isVessel && r2!!.isVessel) continue
				if (r2 != null && r2.player == r.player) {
					numEnds++
					break
				}
			}
		}
		return numEnds < 2
	}

	/**
	 * Clear the board of all data.  There will be no tiles, hence the board will be unplayable.
	 */
	fun clear() {
		clearPirateRoute()
		verts.clear()
		tiles.clear()
		islands.clear()
		robberTileIndex = -1
		pirateTileIndex = -1
		tileHeight = 0f
		tileWidth = tileHeight
		clearRoutes()
		displayName = ""
		distancesCache.clear()
		merchantPlayer = -1
		merchantTileIndex = -1
		name = ""
		numAvaialbleVerts = -1
		pirateRouteStartTile = -1
	}

	/**
	 * Clear the board of all routes (edges).  isInitialized will be false after this call.
	 */
	fun clearRoutes() {
		routes.clear()
		clearRouteLenCache()
	}

	/**
	 * Remove the pirate route chain.
	 */
	fun clearPirateRoute() {
		while (pirateRouteStartTile >= 0) {
			val t = getTile(pirateRouteStartTile)
			pirateRouteStartTile = t.pirateRouteNext
			t.pirateRouteNext = -1
		}
		pirateRouteStartTile = -1
	}

	fun addPirateRoute(tileIndex: Int) {
		if (pirateRouteStartTile < 0) {
			pirateRouteStartTile = tileIndex
		} else {
			var tIndex = pirateRouteStartTile
			while (true) {
				val index = getTile(tIndex).pirateRouteNext
				if (index < 0 || index == pirateRouteStartTile) break
				tIndex = index
			}
			if (tileIndex != tIndex) {
				getTile(tIndex).pirateRouteNext = tileIndex
			}
		}
	}

	fun clearIsland() {
		for (i in islands) {
			Arrays.fill(i.discovered, false)
		}
	}

	/**
	 * Generate the original game play board
	 */
	fun generateDefaultBoard() {
		generateHexBoard(4, TileType.NONE)
		// creating the board using this link:
		//  http://www.lifeisbetterthanchocolate.com/blog/wp-content/uploads/2011/09/Sample-Setup.jpg

		// Sort the cells such that the 1st cell is at leftmost top position
		//  and the last cell is at the right most bottom position
		val copyCells = Vector(tiles)
		Collections.sort(copyCells, Comparator { c0, c1 ->
			val dx = c0.x - c1.x
			val dy = c0.y - c1.y
			if (Math.abs(dx) < 0.001) {
				return@Comparator if (dy < 0) -1 else 1
			}
			if (dx < 0) -1 else 1
		})

		// first column
		copyCells[0].type = TileType.WATER
		copyCells[1].type = TileType.PORT_ORE
		copyCells[2].type = TileType.WATER
		copyCells[3].type = TileType.PORT_MULTI

		// 2nd column
		copyCells[4].type = TileType.PORT_WHEAT
		copyCells[5].setType(TileType.FOREST, 11)
		copyCells[6].setType(TileType.HILLS, 4)
		copyCells[7].setType(TileType.FOREST, 8)
		copyCells[8].type = TileType.WATER

		// 3rd column
		copyCells[9].type = TileType.WATER
		copyCells[10].setType(TileType.MOUNTAINS, 12)
		copyCells[11].setType(TileType.FIELDS, 6)
		copyCells[12].setType(TileType.PASTURE, 3)
		copyCells[13].type = TileType.DESERT
		copyCells[14].type = TileType.PORT_ORE

		// 4th (center) column
		copyCells[15].type = TileType.PORT_MULTI
		copyCells[16].setType(TileType.FIELDS, 2)
		copyCells[17].setType(TileType.FOREST, 5)
		copyCells[18].setType(TileType.MOUNTAINS, 11)
		copyCells[19].setType(TileType.PASTURE, 10)
		copyCells[20].setType(TileType.PASTURE, 5)
		copyCells[21].type = TileType.WATER

		// 5th column
		copyCells[22].type = TileType.WATER
		copyCells[23].setType(TileType.HILLS, 11)
		copyCells[24].setType(TileType.MOUNTAINS, 4)
		copyCells[25].setType(TileType.FIELDS, 2)
		copyCells[26].setType(TileType.MOUNTAINS, 2)
		copyCells[27].type = TileType.PORT_WOOD

		// 6th column
		copyCells[28].type = TileType.PORT_MULTI
		copyCells[29].setType(TileType.PASTURE, 8)
		copyCells[30].setType(TileType.FIELDS, 3)
		copyCells[31].setType(TileType.FOREST, 6)
		copyCells[32].type = TileType.WATER

		// 7th column
		copyCells[33].type = TileType.WATER
		copyCells[34].type = TileType.PORT_SHEEP
		copyCells[35].type = TileType.WATER
		copyCells[36].type = TileType.PORT_MULTI
		assignRandom()
		trim()
	}

	/**
	 * Generate a playable hexagonal shaped board with sideLen hexagons at each side.  For instance, if sideLen == 3, then will generate
	 * a board with 3 + 4 + 5 + 4 + 3 = 19 cells.  Recommended values for sideLen is between 4 and 6.
	 * @param sideLen
	 */
	fun generateHexBoard(sideLen: Int, fillType: TileType) {
		clear()
		val rows = (2 * sideLen - 1).toFloat()
		tileHeight = 1.0f / rows
		tileWidth = tileHeight
		//int ch = cellDim;
		val zeta = tileWidth / 2
		val cx = tileHeight / 2 + (sideLen - 1) * (zeta + tileHeight / 2 - zeta / 2)
		val cy = tileHeight / 2 + tileHeight * (sideLen - 1)
		generateR2(cx, cy, tileWidth, tileHeight, zeta, sideLen, GD_NE or GD_SE, true, fillType)
		generateR2(cx, cy, tileWidth, tileHeight, zeta, sideLen, GD_NW or GD_SW, true, fillType)
		generateR2(cx, cy, tileWidth, tileHeight, zeta, sideLen, GD_N or GD_NE or GD_NW, true, fillType)
		generateR2(cx, cy, tileWidth, tileHeight, zeta, sideLen, GD_S or GD_SE or GD_SW, true, fillType)
	}

	/**
	 * Generate a rectangular board with dim*dim cells.
	 * @param dim
	 * @param fillType
	 */
	fun generateRectBoard(dim: Int, fillType: TileType) {
		clear()
		val cellDim = 1.0f / dim
		val cx = cellDim / 2
		val cy = cellDim / 2
		tileWidth = cellDim
		tileHeight = cellDim
		val zeta = cellDim / 2
		generateR(cx, cy, tileWidth, tileHeight, zeta, fillType)
	}

	/**
	 *
	 */
	fun trim() {
		var i = 0
		while (i < numTiles) {
			val cell = getTile(i)
			if (cell.type === TileType.NONE) {
				// delete
				tiles[i] = tiles.lastElement()
				tiles.removeElementAt(tiles.size - 1)
			} else {
				i++
			}
		}
		fillFit()
		computeVertexTiles()
		computeRoutes()
		log.info("Trim\n  Tiles: %d\n  Verts: %d\n  Routes:  %d\n  Available Verts: %d", tiles.size, verts.size, routes.size, numAvaialbleVerts)
	}

	/**
	 * Visit all the cells and assign die values when necessary and apply cell types to random cell.
	 */
	fun assignRandom() {
		// make sure a reasonable number of cells
		if (numTiles < 7) return
		val dieRolls = arrayOf(
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
		)
		Utils.shuffle(dieRolls)
		var curDieRoll = Utils.rand() % dieRolls.size
		val resourceOptions = arrayOf(
			TileType.FIELDS,
			TileType.FOREST,
			TileType.HILLS,
			TileType.MOUNTAINS,
			TileType.PASTURE)
		val portOptions = arrayOf(
			TileType.PORT_BRICK,
			TileType.PORT_ORE,
			TileType.PORT_SHEEP,
			TileType.PORT_WHEAT,
			TileType.PORT_WOOD)
		Utils.shuffle(resourceOptions)
		Utils.shuffle(portOptions)
		var curResourceOption = 0
		var curPortOption = 0
		val minDeserts = 1 // make configurable
		var numDeserts = 0
		val desertOptions = Vector<Int>()

		// iterate over all the cells and finalize if they are random
		//int numActualCells = 0;
		var i = 0
		while (i < numTiles) {
			val cell = getTile(i)
			when (cell.type) {
				TileType.UNDISCOVERED, TileType.NONE -> {
				}
				TileType.DESERT ->                    //numActualCells++;
					numDeserts++
				TileType.WATER, TileType.PORT_MULTI, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD, TileType.PORT_BRICK -> {
				}
				TileType.RANDOM_RESOURCE_OR_DESERT -> {
					desertOptions.add(i)
					run {
						cell.type = resourceOptions[curResourceOption]
						curResourceOption = (curResourceOption + 1) % resourceOptions.size
					}
					if (cell.dieNum == 0) {
						assert(dieRolls[curDieRoll] > 0)
						cell.dieNum = dieRolls[curDieRoll]
						curDieRoll = (curDieRoll + 1) % dieRolls.size
					}
				}
				TileType.RANDOM_RESOURCE -> {
					run {
						cell.type = resourceOptions[curResourceOption]
						curResourceOption = (curResourceOption + 1) % resourceOptions.size
					}
					if (cell.dieNum == 0) {
						assert(dieRolls[curDieRoll] > 0)
						cell.dieNum = dieRolls[curDieRoll]
						curDieRoll = (curDieRoll + 1) % dieRolls.size
					}
				}
				TileType.GOLD, TileType.FOREST, TileType.PASTURE, TileType.FIELDS, TileType.HILLS, TileType.MOUNTAINS -> if (cell.dieNum == 0) {
					assert(dieRolls[curDieRoll] > 0)
					cell.dieNum = dieRolls[curDieRoll]
					curDieRoll = (curDieRoll + 1) % dieRolls.size
				}
				TileType.RANDOM_PORT_OR_WATER, TileType.RANDOM_PORT -> when (Utils.rand() % (if (cell.type === TileType.RANDOM_PORT_OR_WATER) 2 else 1)) {
					0 -> if (curPortOption >= portOptions.size) {
						cell.type = TileType.PORT_MULTI
					} else {
						cell.type = portOptions[curPortOption++]
					}
					1 -> cell.type = TileType.WATER
				}
			}
			i++
		}
		// assign the deserts
		if (desertOptions.size > 0) {
			Utils.shuffle(desertOptions)
			while (numDeserts < minDeserts && numDeserts < desertOptions.size) {
				val cell = getTile(desertOptions[numDeserts++])
				cell.type = TileType.DESERT
				cell.dieNum = 0
			}
		}
	}

	/**
	 * Get the number of cells.
	 * @return
	 */
	val numTiles: Int
		get() = tiles.size

	/**
	 * Get the cell assigned to index
	 * @param index
	 * @return
	 */
	fun getTile(index: Int): Tile {
		return tiles[index]
	}

	fun setTile(index: Int, tile: Tile) {
		tiles[index] = tile
	}

	/**
	 * Get the number of vertices
	 * @return
	 */
	val numVerts: Int
		get() = verts.size// recompute if neccessary

	/**
	 * Returns a subset of the verts that are available for placement of a structure
	 * @return
	 */
	val numAvailableVerts: Int
		get() {
			if (numAvaialbleVerts < 0) {
				// recompute if neccessary
				numAvaialbleVerts = verts.size
				for (i in verts.indices) {
					val v = verts[i]
					if (v.numTiles < 3) {
						numAvaialbleVerts = i
						break
					}
				}
			}
			return numAvaialbleVerts
		}

	/**
	 * Get the vertex assigned to index
	 * @param index
	 * @return
	 */
	fun getVertex(index: Int): Vertex {
		return verts[index]
	}

	fun setVertex(index: Int, v: Vertex) {
		verts[index] = v
	}

	/**
	 * Get the number of edges.
	 * @return
	 */
	val numRoutes: Int
		get() = routes.size

	/**
	 * Get the edge assigned to index.
	 * @param index
	 * @return
	 */
	fun getRoute(index: Int): Route {
		return routes[index]
	}

	fun setRoute(index: Int, r: Route) {
		routes[index] = r
	}

	/**
	 *
	 * @param vIndex
	 * @return
	 */
	fun getRoutesAdjacentToVertex(vIndex: Int): Collection<Route> {
		val v = getVertex(vIndex)
		val routes: MutableList<Route> = ArrayList(3)
		for (i in 0 until v.numAdjacentVerts) {
			val v2 = v.adjacentVerts[i]
			val r = getRoute(vIndex, v2)
			if (r != null) routes.add(r)
		}
		return routes
	}

	/**
	 *
	 * @param vIndex
	 * @return
	 */
	fun getRouteIndicesAdjacentToVertex(vIndex: Int): Iterable<Int> {
		val v = getVertex(vIndex)
		val routes: MutableList<Int> = ArrayList(3)
		for (i in 0 until v.numAdjacentVerts) {
			val v2 = v.adjacentVerts[i]
			val r = getRouteIndex(vIndex, v2)
			if (r >= 0) routes.add(r)
		}
		return routes
	}

	/**
	 * Convenience method to get routes adjacent and a specific route
	 * @param r
	 * @return
	 */
	fun getRouteIndicesAdjacentToRoute(r: Route): Iterable<Int> {
		val routes = getRouteIndicesAdjacentToVertex(r.from) as MutableList<Int>
		routes.addAll((getRouteIndicesAdjacentToVertex(r.to) as List<Int>))
		return routes
	}

	/**
	 * Get the edge between 2 vertices null when DNE.
	 * @param from
	 * @param to
	 * @return
	 */
	fun getRoute(from: Int, to: Int): Route? {
		val index = getRouteIndex(from, to)
		return if (index < 0) null else getRoute(index)
	}

	/**
	 *
	 * @return
	 */
	fun getRoutes(): Iterable<Route> {
		return routes
	}

	/**
	 *
	 * @return
	 */
	fun getTiles(): Iterable<Tile> {
		return tiles
	}

	/**
	 *
	 * @param player
	 * @param types
	 * @return
	 */
	fun getRoutesOfType(player: Int, vararg types: RouteType): List<Route> {
		val routes: MutableList<Route> = ArrayList()
		val arr: List<RouteType> = Arrays.asList(*types)
		for (i in 0 until numRoutes) {
			val r = getRoute(i)
			if (arr.contains(r.type) && (player == 0 || r.player == player)) {
				routes.add(r)
			}
		}
		return routes
	}

	fun getRoutesIndicesOfType(player: Int, vararg types: RouteType): List<Int> {
		val routes: MutableList<Int> = ArrayList()
		val arr: List<RouteType> = Arrays.asList(*types)
		for (i in 0 until numRoutes) {
			val r = getRoute(i)
			if (arr.contains(r.type) && (player == 0 || r.player == player)) {
				routes.add(i)
			}
		}
		return routes
	}

	/**
	 * Use BFS to find closest ships.  Ships must be adjacent to the vertex at vIndex.
	 * @param vIndex
	 * @param numShips
	 */
	fun removeShipsClosestToVertex(vIndex: Int, playerNum: Int, numShips: Int) {
		var vIndex = vIndex
		var numShips = numShips
		val Q: Queue<Int> = LinkedList()
		Q.add(vIndex)
		while (!Q.isEmpty() && numShips > 0) {
			vIndex = Q.remove()
			val v = getVertex(vIndex)
			for (v2 in v.adjacentVerts) {
				val r = getRoute(vIndex, v2)
				if (r!!.player == playerNum && r.type.isVessel) {
					setRouteOpen(r)
					numShips -= 1
					Q.add(v2)
				}
			}
		}
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getTilesOfType(type: TileType): List<Int> {
		val tiles: MutableList<Int> = ArrayList()
		for (i in 0 until numTiles) {
			if (getTile(i).type === type) {
				tiles.add(i)
			}
		}
		return tiles
	}

	/**
	 * Return whether the board has been initialized.
	 * @return
	 */
	val isReady: Boolean
		get() {
			if (routes.size == 0) return false
			if (verts.size == 0) return false
			var numCells = 0
			for (cell in tiles) {
				when (cell.type) {
					TileType.NONE -> return false
					TileType.GOLD, TileType.FOREST, TileType.FIELDS, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE, TileType.RANDOM_PORT, TileType.RANDOM_PORT_OR_WATER, TileType.RANDOM_RESOURCE, TileType.RANDOM_RESOURCE_OR_DESERT, TileType.DESERT, TileType.PORT_MULTI, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD, TileType.PORT_BRICK, TileType.UNDISCOVERED, TileType.WATER -> numCells++
				}
			}
			return numCells > 10
		}

	private fun traversePath(from: Int, v0: Int, visitedEdges: BooleanArray, playerNum: Int, depth: Int, enableRoadBlock: Boolean): Int {
		val v = getVertex(v0)
		if (enableRoadBlock && v.player > 0 && v.player != playerNum) return depth
		var max = depth
		for (i in 0 until v.numAdjacentVerts) {
			val v1 = v.adjacentVerts[i]
			if (v1 == from) {
				continue
			}
			val eIndex = getRouteIndex(v0, v1)
			if (eIndex < 0) break
			if (visitedEdges[eIndex]) continue
			visitedEdges[eIndex] = true
			//assert(eIndex >= 0);
			val e = getRoute(eIndex)
			if (e.player != playerNum) {
				continue
			}
			val len = traversePath(v0, v1, visitedEdges, playerNum, depth + 1, enableRoadBlock)
			if (len > max) {
				max = len
			}
		}
		return max
	}

	/**
	 * O(2^n) algorithm to solve the longest path problem.
	 * @param playerNum
	 * @return
	 */
	fun computeMaxRouteLengthForPlayer(playerNum: Int, enableRoadBlock: Boolean): Int {
		return if (playerRoadLenCache[playerNum] >= 0) {
			//System.out.println("DEBUG: Road len for player " + playerNum + " cached too: " + playerRoadLenCache[playerNum]);
			playerRoadLenCache[playerNum]
		} else try {
			val visitedEdges = BooleanArray(numRoutes)
			var max = 0
			for (i in 0 until numAvailableVerts) {
				val v = getVertex(i)
				if (v.type !== VertexType.OPEN && !v.isKnight) // TOOD: should we check here for enableRoadBlock and only consider knights if true?
					continue  // skip past verts with settlements on them
				//Utils.fillArray(visited, false);
				Arrays.fill(visitedEdges, false)
				val len = intArrayOf(0, 0, 0)
				for (ii in 0 until v.numAdjacentVerts) {
					val v1 = v.adjacentVerts[ii]
					val eIndex = getRouteIndex(i, v1)
					if (eIndex < 0) continue
					if (visitedEdges[eIndex]) continue
					visitedEdges[eIndex] = true
					val e = getRoute(eIndex)
					if (e.player != playerNum) continue
					//System.out.println("traverse path starting at edge " + eIndex);
					len[ii] = traversePath(i, v1, visitedEdges, playerNum, 1, enableRoadBlock)
					//System.out.println("Road len from vertex " + i + " = " + len[ii]);
				}
				val a = len[0] + len[1]
				val b = len[1] + len[2]
				val c = len[0] + len[2]
				var m = if (a > b) a else b
				m = if (m > c) m else c
				if (m > max) {
					max = m
				}
			}

			//System.out.println("Computed max road len for player " + playerNum + " = " + max);
			playerRoadLenCache[playerNum] = max
			max
		} finally {
			//if (Profiler.ENABLED) Profiler.pop("SOCBoard::computeMaxRoadLengthForPlayer");
		}

		//System.out.println("Recompute road length");
		//if (Profiler.ENABLED) Profiler.push("SOCBoard::computeMaxRoadLengthForPlayer");
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
	fun findAdjacentTile(edge: Route, fromIndex: Int): Int {
		var fromIndex = fromIndex
		val v0 = getVertex(edge.from)
		val v1 = getVertex(edge.to)
		for (i in 0 until v0.numTiles) {
			for (ii in 0 until v1.numTiles) {
				if (v0.getTile(i) == v1.getTile(ii)) {
					if (fromIndex-- > 0) continue
					return v0.getTile(i)
				}
			}
		}
		return -1
	}

	/**
	 * Count the number of vertices assigned to a player.
	 * @param playerNum
	 * @return
	 */
	fun getNumStructuresForPlayer(playerNum: Int): Int {
		return getNumVertsOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_TRADE)
	}

	/**
	 * Count the number of vertices assigned to player that are settlements.
	 * @param playerNum
	 * @return
	 */
	fun getNumSettlementsForPlayer(playerNum: Int): Int {
		return getNumVertsOfType(playerNum, VertexType.SETTLEMENT)
	}

	/**
	 * Count of vertices of a set of types restricted to a certain player.
	 * When playerNum == 0, then there is no restriction on player
	 * @param playerNum
	 * @param types
	 * @return
	 */
	fun getNumVertsOfType(playerNum: Int, vararg types: VertexType): Int {
		var num = 0
		val arr = Arrays.asList(*types)
		for (v in verts) {
			if ((playerNum == 0 || playerNum == v.player) && arr.contains(v.type)) {
				num++
			}
		}
		return num
	}

	/**
	 * Get a list of vertices restricted to a set of types and a player num.
	 * When player num == 0, then no player restriction
	 * @param playerNum
	 * @param types
	 * @return
	 */
	fun getVertIndicesOfType(playerNum: Int, vararg types: VertexType): List<Int> {
		val verts: MutableList<Int> = ArrayList()
		val arr = Arrays.asList(*types)
		for (i in 0 until numAvailableVerts) {
			val v = getVertex(i)
			if ((playerNum == 0 || playerNum == v.player) && arr.contains(v.type)) verts.add(i)
		}
		return verts
	}

	/**
	 *
	 * @param playerNum
	 * @param types
	 * @return
	 */
	fun getVertsOfType(playerNum: Int, vararg types: VertexType): List<Vertex> {
		val verts: MutableList<Vertex> = ArrayList()
		val arr = Arrays.asList(*types)
		for (i in 0 until numAvailableVerts) {
			val v = getVertex(i)
			if ((playerNum == 0 || playerNum == v.player) && arr.contains(v.type)) verts.add(v)
		}
		return verts
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getNumKnightsForPlayer(playerNum: Int): Int {
		return getNumVertsOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getKnightsForPlayer(playerNum: Int): List<Int> {
		return getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getCitiesForPlayer(playerNum: Int): List<Int> {
		return getVertIndicesOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY)
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getSettlementsForPlayer(playerNum: Int): List<Int> {
		return getVertIndicesOfType(playerNum, VertexType.SETTLEMENT)
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getStructuresForPlayer(playerNum: Int): List<Int> {
		return getVertIndicesOfType(playerNum, VertexType.SETTLEMENT, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE)
	}

	/**
	 * Return true if a player has any structure adjacent to a given cell.
	 * @param playerNum
	 * @param cellIndex
	 * @return
	 */
	fun isPlayerAdjacentToTile(playerNum: Int, cellIndex: Int): Boolean {
		val cell = getTile(cellIndex)
		//assert (cell.numAdj > 0 && cell.numAdj <= 6);
		for (vIndex in cell.getAdjVerts()) {
			val vertex = getVertex(vIndex)
			if (vertex.isStructure && vertex.player == playerNum) return true
		}
		return false
	}

	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edgeIndex
	 * @param playerNum
	 * @return
	 */
	fun isRouteAvailableForRoad(edgeIndex: Int, playerNum: Int): Boolean {
		return isRouteAvailableForRoad(getRoute(edgeIndex), playerNum)
	}

	/**
	 * Return true if an edge can legally be used as a road for a given player.
	 * @param edge
	 * @param playerNum
	 * @return
	 */
	fun isRouteAvailableForRoad(edge: Route, playerNum: Int): Boolean {
		if (edge.player != 0 || !edge.isAdjacentToLand || edge.isLocked || edge.isClosed) {
			return false
		}

		// if either vertex is a structure then ok to place
		val v0 = getVertex(edge.from)
		val v1 = getVertex(edge.to)

		// we can place a knight on an opponents knight if we have an adjacent knight that is of higher rank
		var knightRank = -1
		if (v0.isKnight && v0.player == playerNum) {
			knightRank = v0.type!!.knightLevel
		}
		if (v1.isKnight && v1.player == playerNum) {
			knightRank = Math.max(knightRank, v1.type!!.knightLevel)
		}
		if (v0.isKnight && v0.player != playerNum && v0.type!!.knightLevel > knightRank) return false
		if (v1.isKnight && v1.player != playerNum && v1.type!!.knightLevel > knightRank) return false
		if (v0.isStructure && v0.player == playerNum) {
			return true
		}
		if (v1.isStructure && v1.player == playerNum) {
			return true
		}

		// check if the adjacent edges have one of our roads
		return if (isVertexAdjacentToPlayerRoad(edge.from, playerNum) || isVertexAdjacentToPlayerRoad(edge.to, playerNum)) {
			true
		} else false
	}

	/**
	 *
	 * @param rules
	 * @param edge
	 * @param playerNum
	 * @return
	 */
	fun isRouteAvailableForShip(rules: Rules, edge: Route, playerNum: Int): Boolean {
		if (edge.player != 0 || !edge.isAdjacentToWater || edge.isAttacked || edge.isLocked || edge.isClosed) return false

		// check if the adjacent edges have one of our ships
		if (isVertexAdjacentToPlayerShip(edge.from, playerNum) || isVertexAdjacentToPlayerShip(edge.to, playerNum)) {
			return true
		}
		var v = getVertex(edge.from)
		if (v.isStructure && v.player == playerNum && isVertexAdjacentToWater(v)) return true
		v = getVertex(edge.to)
		if (v.isStructure && v.player == playerNum && isVertexAdjacentToWater(v)) return true
		if (rules.isEnableBuildShipsFromPort) {
			for (t in getRouteTiles(edge)) {
				if (t.isPort) {
					if (isVertexAdjacentToPlayerRoad(edge.from, playerNum) || isVertexAdjacentToPlayerRoad(edge.to, playerNum)) return true
				}
			}
		}
		return false
	}

	/**
	 *
	 * @param edgeIndex
	 * @param playerNum
	 * @return
	 */
	fun isRouteAvailableForShip(rules: Rules, edgeIndex: Int, playerNum: Int): Boolean {
		return isRouteAvailableForShip(rules, getRoute(edgeIndex), playerNum)
	}

	/**
	 * Return true if a vertex is available for a structure
	 * @param vIndex
	 * @return
	 */
	fun isVertexAvailbleForSettlement(vIndex: Int): Boolean {
		val v = getVertex(vIndex)
		if (v.player > 0 || !v.canPlaceStructure()) return false
		for (i in 0 until v.numAdjacentVerts) {
			val v2 = findAdjacentVertex(vIndex, i)
			if (v2 >= 0) {
				val ie = getRouteIndex(vIndex, v2)
				if (ie >= 0) {
					val e = getRoute(ie)
					if (getVertex(e.from).isStructure || getVertex(e.to).isStructure) return false
				}
			}
		}
		return true
	}

	/**
	 * Return true if a vertex has any edge assigned to player.
	 * @param vIndex
	 * @param playerNum
	 * @param flag
	 * @return
	 */
	private fun isVertexAdjacentToRoute(vIndex: Int, playerNum: Int, flag: Int): Boolean {
		for (i in 0..2) {
			val v2 = findAdjacentVertex(vIndex, i)
			if (v2 >= 0) {
				val ie = getRouteIndex(vIndex, v2)
				if (ie >= 0) {
					val e = getRoute(ie)
					val eFlag = if (e.type.isVessel) Route.EDGE_FLAG_WATER else Route.EDGE_FLAG_LAND
					if (e.player == playerNum && 0 != flag and eFlag) return true
				}
			} else {
				break
			}
		}
		return false
	}

	/**
	 *
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	fun isVertexAdjacentToPlayerRoad(vIndex: Int, playerNum: Int): Boolean {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_LAND)
	}

	/**
	 *
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	fun isVertexAdjacentToPlayerShip(vIndex: Int, playerNum: Int): Boolean {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_WATER)
	}

	/**
	 *
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	fun isVertexAdjacentToPlayerRoute(vIndex: Int, playerNum: Int): Boolean {
		return isVertexAdjacentToRoute(vIndex, playerNum, Route.EDGE_FLAG_LAND or Route.EDGE_FLAG_WATER)
	}

	/**
	 * Count the number of edges assigned to player
	 * @param playerNum
	 * @return
	 */
	fun getNumRoutesOfType(playerNum: Int, vararg types: RouteType): Int {
		var num = 0
		for (i in 0 until numRoutes) {
			val e = getRoute(i)
			if (e.player == playerNum) {
				for (rt in types) {
					if (rt === e.type) {
						num++
						break
					}
				}
			}
		}
		return num
	}

	/**
	 * Convenience method to get a iterable over a players roads.  The roads are not ordered
	 *
	 * @param playerNum
	 * @return
	 */
	fun getRoutesForPlayer(playerNum: Int): Iterable<Route> {
		val edges: MutableList<Route> = ArrayList()
		for (i in 0 until numRoutes) {
			val e = getRoute(i)
			if (e.player == playerNum) {
				edges.add(e)
			}
		}
		return edges
	}

	/**
	 *
	 * @param playerNum
	 * @return
	 */
	fun getRouteIndicesForPlayer(playerNum: Int): Iterable<Int> {
		val routes: MutableList<Int> = ArrayList()
		for (rIndex in 0 until numRoutes) {
			if (getRoute(rIndex).player == playerNum) routes.add(rIndex)
		}
		return routes
	}

	/**
	 * Interface to be used in conjunction with walkEdgeTree
	 * @author ccaron
	 */
	interface IVisitor {
		/**
		 * Visit an edge
		 *
		 * @param edge
		 * @param depth current depth of recursion
		 * @return false to terminate the recursion, true to continue the recursion
		 */
		fun visit(edge: Route?, depth: Int): Boolean

		/**
		 * Determine advancement of recursion on a vertex
		 * @param vertexIndex
		 * @return
		 */
		fun canRecurse(vertexIndex: Int): Boolean
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
	fun walkRouteTree(startVertex: Int, visitor: IVisitor) {
		if (startVertex < 0 || visitor == null) return
		val visitedEdges = BooleanArray(numRoutes)
		walkRouteTreeR(startVertex, visitor, visitedEdges, 0)
	}

	private fun walkRouteTreeR(startVertex: Int, visitor: IVisitor, usedEdges: BooleanArray, depth: Int) {
		assert(startVertex >= 0)
		val vertex = getVertex(startVertex)
		for (i in 0 until vertex.numAdjacentVerts) {
			val toVertexIndex = vertex.adjacentVerts[i]
			if (toVertexIndex != startVertex) {
				val edgeIndex = getRouteIndex(startVertex, toVertexIndex)
				if (usedEdges[edgeIndex]) continue
				if (!visitor.canRecurse(toVertexIndex)) continue
				usedEdges[edgeIndex] = true
				val edge = getRoute(edgeIndex)
				if (visitor.visit(edge, depth + 1)) {
					walkRouteTreeR(toVertexIndex, visitor, usedEdges, depth + 1)
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
	fun checkForPlayerRouteBlocked(vIndex: Int): Int {
		if (vIndex < 0) return 0
		val v = getVertex(vIndex)
		var playerNum = 0
		for (i in 0 until v.numAdjacentVerts) {
			val r = getRoute(vIndex, v.adjacentVerts[i])
			if (r!!.type.isRoute && r.player != v.player) {
				if (playerNum == 0) playerNum = r.player else if (playerNum == r.player) return playerNum
			}
		}
		return 0
	}

	fun translate(dx: Float, dy: Float) {
		for (v in verts) {
			v.x = v.x + dx
			v.y = v.y + dy
		}
		for (c in tiles) {
			c.x = c.x + dx
			c.y = c.y + dy
		}
	}

	fun scale(sx: Float, sy: Float) {
		for (v in verts) {
			v.x = v.x * sx
			v.y = v.y * sy
		}
		for (c in tiles) {
			c.x = c.x * sx
			c.y = c.y * sy
		}
		tileWidth *= sx
		tileHeight *= sy
	}

	/**
	 * Center the board inside the 0,0 x 1,1 rectangle
	 */
	fun center() {
		val minMax = computeMinMax()
		val v = minMax.center
		translate(v.x, v.y)
	}

	fun computeMinMax(): GRectangle {
		val min = MutableVector2D(Vector2D.MAX)
		val max = MutableVector2D(Vector2D.MIN)
		for (c in tiles) {
			if (c.type === TileType.NONE) continue
			min.minEq(c)
			max.maxEq(c)
		}
		max.addEq(tileWidth / 2, tileHeight / 2)
		min.subEq(tileWidth / 2, tileHeight / 2)
		return GRectangle(min, max)
	}

	/**
	 * Fit all cells into the 0,0 x 1,1 rectangle
	 */
	fun fillFit() {
		var minMax = computeMinMax()
		//if (minMax.w <= 1 && minMax.h <= 1)
		//    return;
		val v = minMax.center
		// center at 0,0
		translate(v.x, v.y)
		// fill a 1,1 rect
		scale(1.0f / minMax.w, 1.0f / minMax.h)
		// move to 0.5, 0.5
		translate(-v.x, -v.y)
		minMax = computeMinMax()
		translate(-minMax.x, -minMax.y)
	}

	/**
	 * Resets all routes to have player 0
	 */
	fun resetRoutes() {
		for (e in routes) {
			setRouteOpen(e)
		}
	}

	/**
	 * Resets all structures to have player 0
	 */
	fun resetStructures() {
		for (v in verts) {
			v.setOpen()
		}
	}

	/**
	 * Removes all islands references and sets island num of all tiles to 0
	 */
	fun clearIslands() {
		for (t in tiles) {
			t.islandNum = 0
		}
		islands.clear()
	}

	/**
	 * Convenience method to get iterable over the cells adjacent to a vertex
	 * @param v
	 * @return
	 */
	fun getTilesAdjacentToVertex(v: Vertex): Iterable<Tile> {
		val a = ArrayList<Tile>()
		for (i in 0 until v.numTiles) {
			a.add(getTile(v.getTile(i)))
		}
		return a
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	fun getTileIndicesAdjacentToVertex(v: Vertex): Iterable<Int> {
		val a = ArrayList<Int>()
		for (i in 0 until v.numTiles) {
			a.add(v.getTile(i))
		}
		return a
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	fun getTilesAdjacentToTile(t: Tile): Iterable<Int> {
		val result = HashSet<Int>()
		for (vIndex in t.getAdjVerts()) {
			for (tIndex in getTileIndicesAdjacentToVertex(getVertex(vIndex))) {
				if (getTile(tIndex) === t) continue
				result.add(tIndex)
			}
		}
		return result
	}

	/**
	 * Convenience method to get iterable over the cells adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	fun getTilesAdjacentToVertex(vIndex: Int): Iterable<Tile> {
		return getTilesAdjacentToVertex(getVertex(vIndex))
	}

	/**
	 *
	 */
	fun clearRouteLenCache() {
		Arrays.fill(playerRoadLenCache, -1)
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	fun isVertexAdjacentToWater(v: Vertex): Boolean {
		for (cell in getTilesAdjacentToVertex(v)) {
			if (cell.isWater) return true
		}
		return false
	}

	/**
	 *
	 * @param vIndex
	 * @return
	 */
	fun isVertexAdjacentToPirateRoute(vIndex: Int): Boolean {
		if (pirateRouteStartTile >= 0) {
			val v = getVertex(vIndex)
			var i = pirateRouteStartTile
			do {
				if (v.isAdjacentToTile(i)) return true
				i = getTile(i).pirateRouteNext
			} while (i != pirateRouteStartTile)
		}
		return false
	}

	/**
	 * Return true if any edge adjacent to e is a ship and owned by playerNum
	 * @param vIndex
	 * @param playerNum
	 * @return
	 */
	fun getNumShipsAdjacentTo(vIndex: Int, playerNum: Int): Int {
		var num = 0
		val v = getVertex(vIndex)
		for (i in 0 until v.numAdjacentVerts) {
			val e = getRoute(vIndex, v.adjacentVerts[i])
			if (e != null) {
				if (e.type.isVessel && e.player == playerNum) num++
			}
		}
		return num
	}

	/**
	 *
	 * @param edge
	 * @return
	 */
	fun getRouteMidpoint(edge: Route): Vector2D {
		val v0 = getVertex(edge.from)
		val v1 = getVertex(edge.to)
		val mx = (v0.x + v1.x) / 2
		val my = (v0.y + v1.y) / 2
		return Vector2D(mx, my)
	}

	/**
	 * Return the list of edges adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	fun getVertexRoutes(vIndex: Int): Collection<Route> {
		val edges: MutableList<Route> = ArrayList(3)
		val v = getVertex(vIndex)
		for (i in 0 until v.numAdjacentVerts) {
			val e = getRoute(vIndex, v.adjacentVerts[i])
			if (e != null) edges.add(e)
		}
		return edges
	}

	/**
	 * Return the list of edges adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	fun getVertexRouteIndices(vIndex: Int): Collection<Int> {
		val edges: MutableList<Int> = ArrayList(3)
		val v = getVertex(vIndex)
		for (i in 0 until v.numAdjacentVerts) {
			val rIndex = getRouteIndex(vIndex, v.adjacentVerts[i])
			if (rIndex >= 0) edges.add(rIndex)
		}
		return edges
	}

	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param vIndex
	 * @return
	 */
	fun getVertexTiles(vIndex: Int): Collection<Tile> {
		return getVertexTiles(getVertex(vIndex))
	}

	/**
	 * Convenience to get the cells adjacent to a vertex
	 * @param v
	 * @return
	 */
	fun getVertexTiles(v: Vertex): Collection<Tile> {
		val options: MutableList<Tile> = ArrayList(3)
		for (i in 0 until v.numTiles) {
			options.add(getTile(v.getTile(i)))
		}
		return options
	}

	fun isIslandDiscovered(playerNum: Int, islandNum: Int): Boolean {
		return getIsland(islandNum).discovered[playerNum]
	}

	fun setIslandDiscovered(playerNum: Int, islandNum: Int, discovered: Boolean) {
		getIsland(islandNum).discovered[playerNum] = discovered
	}

	fun getNumDiscoveredIslands(playerNum: Int): Int {
		var num = 0
		for (i in islands) {
			if (i.discovered[playerNum]) num++
		}
		return num
	}

	fun getOpenKnightVertsForPlayer(playerNum: Int): List<Int> {
		val verts = HashSet<Int>()
		for (eIndex in 0 until numRoutes) {
			val r = getRoute(eIndex)
			if (r.player != playerNum) continue
			var v = getVertex(r.from)
			if (v.player == 0) verts.add(r.from)
			v = getVertex(r.to)
			if (v.player == 0) verts.add(r.to)
		}
		return ArrayList(verts)
	}

	fun getKnightLevelForPlayer(playerNum: Int, active: Boolean, inactive: Boolean): Int {
		var level = 0
		for (kIndex in getKnightsForPlayer(playerNum)) {
			val type = getVertex(kIndex).type
			if (type!!.isKnightActive && active) level += getVertex(kIndex).type!!.knightLevel else if (!type.isKnightActive && inactive) level += getVertex(kIndex).type!!.knightLevel
		}
		return level
	}

	fun getTileVertices(t: Tile): List<Vertex> {
		val verts: MutableList<Vertex> = ArrayList()
		for (i in 0 until t.numAdj) {
			verts.add(getVertex(t.getAdjVert(i)))
		}
		return verts
	}

	/*
	public void setRoutesLocked(Iterable<Integer> routes, boolean locked) {
		for (int rIndex : routes) {
			Route r = getRoute(rIndex);
			r.setLocked(locked);
		}
	}*/
	fun getIslandAdjacentToVertex(v: Vertex): Int {
		for (tIndex in 0 until v.numTiles) {
			val t = getTile(v.getTile(tIndex))
			if (t.islandNum > 0) return t.islandNum
		}
		return 0
	}

	/**
	 * Return a structure that can compute the distance/path between any 2 vertices.
	 * When transitioning from land to water, these routes must pass through a structure and/or port depending on rules.
	 *
	 * @param rules
	 * @param playerNum
	 * @return
	 */
	fun computeDistances(rules: Rules, playerNum: Int): IDistances {
		return if (rules.isEnableSeafarersExpansion) {
			try {
				computeDistancesLandWater(rules, playerNum)
			} catch (e: OutOfMemoryError) {
				System.gc()
				computeDistancesLandWater(rules, playerNum)
			}
		} else {
			val rcache = IntArray(routes.size)
			var rcacheLen = 0

			// Assign distances to edges
			for (rIndex in routes.indices) {
				val r = routes[rIndex]
				val v0 = r.from
				val v1 = r.to
				if (r.player == playerNum) {
					when (r.type) {
						RouteType.DAMAGED_ROAD, RouteType.ROAD -> rcache[rcacheLen++] = rIndex
						RouteType.SHIP, RouteType.WARSHIP, RouteType.OPEN -> assert(false)
						else                                              -> assert(false)
					}
				}
			}

			// OPTIMIZATION: check if the current route configuration has already been done, then no need to do it again
			//Arrays.sort(rcache, 0, rcacheLen); // do we really need to sort?
			val str = Utils.toString(rcache, 0, rcacheLen) // decompose the array into a hashable string
			distancesCache[str]?.let { return it }
			val numV = numAvailableVerts
			assert(numV <= 100)
			val dist = Array(numV) { ByteArray(numV) }
			val next = Array(numV) { ByteArray(numV) }
			for (i in 0 until numV) {
				for (ii in 0 until numV) {
					dist[i][ii] = IDistances.DISTANCE_INFINITY
					next[i][ii] = ii.toByte()
				}
				dist[i][i] = 0
			}
			for (rIndex in routes.indices) {
				val r = routes[rIndex]
				val v0 = r.from
				val v1 = r.to
				if (r.player == 0) {
					dist[v1][v0] = 1
					dist[v0][v1] = dist[v1][v0]
				} else if (r.player == playerNum) {
					when (r.type) {
						RouteType.DAMAGED_ROAD, RouteType.ROAD -> {
							dist[v1][v0] = 0
							dist[v0][v1] = dist[v1][v0]
						}
						RouteType.SHIP, RouteType.WARSHIP, RouteType.OPEN -> assert(false)
						else                                              -> assert(false)
					}
				}
			}


			// All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
			// where every vertex has 2 or 3 edges.  The memory usage and complexity of a Dijkstra's make it less desirable.
			// However this is very expensive computationally especially on lesser tablets, hence the optimization above.
			for (k in 0 until numV) {
				for (i in 0 until numV) {
					for (j in 0 until numV) {
						val sum = dist[i][k] + dist[k][j]
						if (sum < dist[i][j]) {
							assert(sum < IDistances.DISTANCE_INFINITY)
							dist[i][j] = sum.toByte()
							next[i][j] = next[i][k]
						}
					}
				}
			}
			val d: IDistances = DistancesLand(dist, next)
			distancesCache[str] = d
			d
		}
	}

	private fun computeDistancesLandWater(rules: Rules, playerNum: Int): IDistances {
		val rcache = IntArray(routes.size)
		var rcacheLen = 0

		// Assign distances to edges
		for (rIndex in routes.indices) {
			val r = routes[rIndex]
			if (r.player == playerNum) {
				when (r.type) {
					RouteType.DAMAGED_ROAD, RouteType.ROAD -> rcache[rcacheLen++] = rIndex
					RouteType.SHIP, RouteType.WARSHIP -> rcache[rcacheLen++] = rIndex * 1000 // for water we scale by 1000 so that it produces a different distanceCache string
					RouteType.OPEN -> assert(false)
					else                                   -> assert(false)
				}
			}
		}

		// OPTIMIZATION: check if the current route configuration has already been done, then no need to do it again
		Arrays.sort(rcache, 0, rcacheLen)
		val str = Utils.toString(rcache, 0, rcacheLen)
		distancesCache[str]?.let { return it }
		val numV = numAvailableVerts
		val distLand = Array(numV) { ByteArray(numV) }
		val distAqua = Array(numV) { ByteArray(numV) }
		val nextLand = Array(numV) { ByteArray(numV) }
		val nextAqua = Array(numV) { ByteArray(numV) }
		for (i in 0 until numV) {
			for (ii in 0 until numV) {
				distLand[i][ii] = IDistances.DISTANCE_INFINITY
				distAqua[i][ii] = IDistances.DISTANCE_INFINITY
				nextLand[i][ii] = ii.toByte()
				nextAqua[i][ii] = ii.toByte()
			}
			distLand[i][i] = 0
			distAqua[i][i] = 0
		}

		// Assign distances to edges
		for (rIndex in routes.indices) {
			val r = routes[rIndex]
			val v0 = r.from
			val v1 = r.to
			if (r.player == 0) {
				if (r.isAdjacentToLand) {
					distLand[v1][v0] = 1
					distLand[v0][v1] = distLand[v1][v0]
				}
				if (r.isAdjacentToWater) {
					distAqua[v1][v0] = 1
					distAqua[v0][v1] = distAqua[v1][v0]
				}
			} else if (r.player == playerNum) {
				when (r.type) {
					RouteType.DAMAGED_ROAD, RouteType.ROAD -> {
						distLand[v1][v0] = 0
						distLand[v0][v1] = distLand[v1][v0]
					}
					RouteType.SHIP, RouteType.WARSHIP -> {
						distAqua[v1][v0] = 0
						distAqua[v0][v1] = distAqua[v1][v0]
					}
					RouteType.OPEN -> assert(false)
				}
			}
		}

		// All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
		// where every vertex has 2 or 3 edges.  The memory usage and complexity of a Dijkstra's make it less desirable.
		for (k in 0 until numV) {
			for (i in 0 until numV) {
				for (j in 0 until numV) {
					// this is wrong!
					var sum: Int = max(IDistances.DISTANCE_INFINITY.toInt(), distLand[i][k] + distLand[k][j])
					if (sum < distLand[i][j]) {
						distLand[i][j] = sum.toByte()
						nextLand[i][j] = nextLand[i][k]
					}
					sum = distAqua[i][k] + distAqua[k][j]
					if (sum < distAqua[i][j]) {
						distAqua[i][j] = sum.toByte()
						nextAqua[i][j] = nextAqua[i][k]
					}
				}
			}
		}
		val launchVerts: MutableSet<Int> = HashSet()

		// find all vertices where we can launch a ship from
		for (vIndex in 0 until numAvailableVerts) {
			val v = getVertex(vIndex)
			if (v.isAdjacentToWater) {
				if (v.isStructure && v.player == playerNum) {
					launchVerts.add(vIndex)
				} else if (v.player == 0 && rules.isEnableBuildShipsFromPort && isVertexAdjacentToPlayerRoute(vIndex, playerNum)) {
					for (t in getVertexTiles(vIndex)) {
						if (t.type.isPort) {
							launchVerts.add(vIndex)
							break
						}
					}
				}
			}
		}
		val d: IDistances = DistancesLandWater(distLand, nextLand, distAqua, nextAqua, launchVerts)
		distancesCache[str] = d
		return d
	}

	@Throws(IOException::class)
	override fun loadFromFile(file: File) {
		super.loadFromFile(file)
		name = file.absolutePath
	}

	fun tryRefreshFromFile(): Boolean {
		return try {
			loadFromFile(File(name))
			true
		} catch (e: Exception) {
			e.printStackTrace()
			false
		}
	}

	/**
	 * Create an empty board
	 */
	init {
		Arrays.fill(playerRoadLenCache, -1)
	}
}