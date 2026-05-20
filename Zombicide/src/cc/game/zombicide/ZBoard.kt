package cc.game.zombicide

import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.game.IVector2D
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Alternate
import cc.lib.reflector.DirtyGrid
import cc.lib.reflector.DirtyHashMap
import cc.lib.reflector.DirtyList
import cc.lib.reflector.DirtyReflector
import cc.lib.reflector.Omit
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.allMaxOf
import cc.lib.utils.allMinOf
import cc.lib.utils.increment
import cc.lib.utils.removeAll
import cc.lib.utils.test
import cc.lib.utils.transform
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// TODO: Should take non-dirty variations?
class ZBoard(val grid: DirtyGrid<ZCell>, val zones: DirtyList<ZZone>) : DirtyReflector<ZBoard>(), IDimension {
    companion object {
        private val log = LoggerFactory.getLogger(ZBoard::class.java)

	    init {
		    addAllFields(ZBoard::class.java)
	    }
    }

	private val _actors = DirtyHashMap(ConcurrentHashMap<String, ZActor<*>>())

	@Alternate("hoard")
	private val _hoard = DirtyHashMap(mutableMapOf<ZZombieType, Int>())

	constructor() : this(DirtyGrid(1, 1) { ZCell() }, DirtyList())

	val hoard: Map<ZZombieType, Int>
		get() = _hoard

	val actors: Map<String, ZActor<*>>
		get() = _actors

	val rows: Int
		get() = grid.rows
	val columns: Int
		get() = grid.cols


	override val width: Float
		get() = columns.toFloat()
	override val height: Float
		get() = rows.toFloat()


	override val isEmpty: Boolean
		get() = grid.isEmpty()

	fun addToHoard(type: ZZombieType, num: Int = 1) {
		_hoard.increment(type, num)
	}

	fun getZone(index: Int): ZZone {
		return zones[index]
	}

	fun getNumZones(): Int {
		return zones.size
	}

	fun getActor(id: String?): ZActor<*>? = _actors[id ?: ""]

	/**
	 * Get list of accessible zones
	 *
	 * @param fromZoneIndex
	 * @param minDist
	 * @param maxDist
	 * @return
	 */
	fun getAccessibleZones(
		actor: ZActor<*>,
		minDist: Int,
		maxDist: Int,
		action: ZActionType
	): List<Int> {
		val fromZoneIndex = actor.occupiedZone
		if (maxDist == 0) return listOf(fromZoneIndex)
		val result: MutableSet<Int> = HashSet()
		val options = if (action === ZActionType.MOVE) ZDir.values() else ZDir.compassValues
		val birdsEye =
			getZone(fromZoneIndex).type === ZEnvironmentType.TOWER || (getZone(fromZoneIndex).isOutside && actor.hasSkill(
				ZSkill.Birds_eye_view
			))

		if (action.isProjectile && birdsEye) {
			// special case here
			// buildings do not block from being able to see beyond
			// can see into buildings with open door but only for a single zone
			for (cellPos in zones[fromZoneIndex].cells) {
				for (dir in ZDir.compassValues) {
					var pos = cellPos
					var lastIndoorZone = -1
					if (grid[pos].getWallFlag(dir).openedForAction(action)) {
						for (i in 0 until minDist) {
							pos = getAdjacentOrNull(pos, dir) ?: break
						}
						for (i in minDist..maxDist) {
							if (!grid.isOnGrid(pos)) break
							val cell = grid[pos]
							when (getZone(cell.zoneIndex).type) {
								ZEnvironmentType.TOWER, ZEnvironmentType.OUTDOORS -> {
									lastIndoorZone = -1
									result.add(cell.zoneIndex)
								}

								ZEnvironmentType.BUILDING -> if (lastIndoorZone < 0) {
									lastIndoorZone = cell.zoneIndex
									if (cell.getWallFlag(dir.opposite).openedForAction(action)) {
										result.add(cell.zoneIndex)
									}
								}
							    else -> Unit
						    }
						    pos = getAdjacent(pos, dir)
					    }
				    }
			    }
            }
        } else {
			for (cellPos in zones[fromZoneIndex].cells) {
				// fan out in all direction to given distance
				outer@ for (dir in options) {
					var pos = cellPos
					var dist = 0
					var buildingZoneIdx = -1
					while (dist < minDist) {
						val cell = grid[pos]
						if (!cell.getWallFlag(dir).openedForAction(action)) {
							continue@outer
						}
						pos = getAdjacentOrNull(pos, dir) ?: break
			            dist++
                    }
                    result.add(grid[pos].zoneIndex)
                    while (dist < maxDist) {
                        var cell = grid[pos]
	                    if (!cell.getWallFlag(dir).openedForAction(action)) {
		                    break
	                    }
	                    pos = getAdjacentOrNull(pos, dir) ?: break
	                    cell = grid[pos]
                        if (cell.isInside) {
	                        if (buildingZoneIdx < 0)
		                        buildingZoneIdx = cell.zoneIndex
	                        else if (cell.zoneIndex != buildingZoneIdx)
		                        break
                        }
                        result.add(cell.zoneIndex)
	                    if (action.isMovement && getNumZombiesInZone(cell.zoneIndex) > 0) {
		                    break
	                    }
	                    dist++
                    }
				}
			}
		}
		return ArrayList(result)
	}

	fun getAdjacentOrNull(from: Pos, dir: ZDir): Pos? = when (dir) {
		ZDir.NORTH, ZDir.SOUTH, ZDir.EAST, ZDir.WEST -> {
			val pos = Pos(from.row + dir.dy, from.column + dir.dx)
			if (grid.isOnGrid(pos) && getCell(pos).environment != ZEnvironmentType.NOTHING)
				pos
			else null
		}

		ZDir.ASCEND, ZDir.DESCEND -> findDoorOrNull(from, dir)?.cellPosEnd
	}


	fun getAdjacent(from: Pos, dir: ZDir): Pos = when (dir) {
		ZDir.NORTH, ZDir.SOUTH, ZDir.EAST, ZDir.WEST -> Pos(from.row + dir.dy, from.column + dir.dx)
		ZDir.ASCEND, ZDir.DESCEND -> findDoorOrNull(from, dir)?.cellPosEnd ?: Pos(-1, -1)
	}

	fun findDoor(pos: Pos, dir: ZDir): ZDoor {
		val zone = zones[getCell(pos).zoneIndex]
		zone.doors.firstOrNull { door -> door.cellPosStart == pos && door.moveDirection === dir }?.let {
			return it
		}
		throw GException("No door found at $pos, $dir")
	}

	private fun findDoor(zone: ZZone, dir: ZDir): ZDoor {
		for (door in zone.doors) {
			if (door.moveDirection === dir) {
				return door
			}
		}
		throw GException("No door found at zone ${zone.zoneIndex}, $dir")
	}

	fun findDoorOrNull(pos: Pos, dir: ZDir): ZDoor? = zones[getCell(pos).zoneIndex].doors.firstOrNull {
		it.cellPosStart == pos && it.moveDirection === dir
	}

	/**
	 * Gets all doors including duplicates
	 */
	fun getDoors(): List<ZDoor> = zones.map {
		it.doors
	}.flatten()

	fun findVault(id: Int): ZDoor {
		var numIds = 0
		val ids = arrayOf(Pos(), Pos())
		var color: GColor? = null
		zones.forEach { zone ->
			zone.cells.forEach { pos ->
				with(getCell(pos)) {
					if (vaultId == id) {
						if (color == null) {
							color = this.vaultType.color
						}
						if (this.isVault) {
							ids[1] = pos
						} else {
							ids[0] = pos
						}
						numIds++
					}
				}
			}
		}
		require(numIds == 2)
		return ZDoor(ids[0], ids[1], ZDir.DESCEND, requireNotNull(color))
	}

	@Omit
	private var canSeeCache: Array<Array<Boolean?>>? = null

	fun canSee(fromZone: Int, toZone: Int): Boolean {
		if (fromZone == toZone) return true
		canSeeCache?.get(fromZone)?.get(toZone)?.let {
			return it
		}

		if (canSeeCache == null) {
			canSeeCache = Array(getNumZones()) { Array(getNumZones()) { null } }
		}

		var canSee = false
		outer@ for (pos0 in zones[fromZone].cells) {
			for (pos1 in zones[toZone].cells) {
				if (canSeeCell(pos0, pos1)) {
					canSee = true
					break@outer
				}
			}
		}
		canSeeCache!![fromZone][toZone] = canSee
		return canSee
	}

	fun canSeeCell(fromCell: Pos, toCell: Pos): Boolean {
		var fromCell = fromCell
		val dir = ZDir.getDirFromOrNull(fromCell, toCell) ?: return false
		var zoneChanges = 0
		var curZoneId = grid[fromCell].zoneIndex
		while (fromCell != toCell) {
			val cell = grid[fromCell]
			// can only see 1 one zone difference
			if (cell.isInside && cell.zoneIndex != curZoneId) {
				if (++zoneChanges > 1) return false
				curZoneId = cell.zoneIndex
			}
			if (!cell.getWallFlag(dir).lineOfSight) {
				return false
			}
			fromCell = getAdjacentOrNull(fromCell, dir) ?: break
		}
		return true
	}

	@Omit
	private val shortestPathCache = mutableMapOf<Pos, MutableMap<Int, List<List<ZDir>>>>()

	/**
	 * Returns a list of directions the zombie can move
	 * @See DIR_NORTH, DIR_SOUTH, DIR_EAST, DIR_WEST
	 *
	 * @param fromPos
	 * @param toZoneIndex
	 * @return
	 */
	fun getShortestPath(
		actor: ZActor<*>,
		toZoneIndex: Int
	): List<ZDir> {
		val fromPos = actor.occupiedCell
		if (grid[fromPos].zoneIndex == toZoneIndex) return emptyList()
		val allPaths = shortestPathCache.getOrPut(fromPos) {
			mutableMapOf()
		}.getOrPut(toZoneIndex) {
			val allPaths: MutableList<List<ZDir>> = ArrayList()
			var maxDist = (grid.rows + grid.cols)
			val visited: MutableSet<Pos> = HashSet()
			getShortestPathOptions(actor, fromPos, getZone(toZoneIndex), visited, maxDist).also { paths ->
				for (l in paths) {
					maxDist = maxDist.coerceAtMost(l.size)
				}
				allPaths.addAll(paths)
			}
			allPaths.removeIf { it.size > maxDist }
			allPaths
		}

		return when (allPaths.size) {
			0 -> emptyList()
			1 -> allPaths.first()
			else -> {
				allPaths.allMinOf {
					it.size
				}.random()
			}
		}
	}

	fun getShortestPathOrNull(
		actor: ZActor<*>,
		toZoneIndex: Int
	): List<ZDir>? = getShortestPath(actor, toZoneIndex).takeIf { it.isNotEmpty() }

	fun isZoneReachable(actor: ZActor<*>, targetZone: Int) = getShortestPathOrNull(actor, targetZone) != null

	private fun getShortestPathOptions(
		actor: ZActor<*>,
		fromCell: Pos,
		toZone: ZZone,
		visited: MutableSet<Pos>,
		maxDist: Int
	): List<List<ZDir>> {
		val paths: MutableList<List<ZDir>> = ArrayList()
		searchPathsR(actor, fromCell, toZone, intArrayOf(maxDist), LinkedList(), paths, visited)
		return paths
	}

	private fun searchPathsR(
		actor: ZActor<*>,
		fromPos: Pos,
		toZone: ZZone,
		maxDist: IntArray,
		curPath: LinkedList<ZDir>,
		paths: MutableList<List<ZDir>>,
		visited: MutableSet<Pos>
	) {
		if (getCell(fromPos).zoneIndex == toZone.zoneIndex) {
			if (curPath.size > 0) {
				paths.add(ArrayList(curPath))
				maxDist[0] = Math.min(maxDist[0], curPath.size)
			}
			return
		}
		if (curPath.size >= maxDist[0]) {
			if (paths.isEmpty()) {
				paths.add(ArrayList(curPath))
			}
        	return
        }
		if (visited.contains(fromPos))
			return
        visited.add(fromPos)
		val fromCell = grid[fromPos]
		val toPos = toZone.cells.first()
		for (dir in ZDir.valuesSorted(fromPos, toPos)) {
			if (actor.actionToCross(fromCell.getWallFlag(dir)).isMovement) {
				getAdjacentOrNull(fromPos, dir)?.let { nextPos ->
					if (!visited.contains(nextPos) && !getCell(nextPos).isFull) {
						curPath.addLast(dir)
						searchPathsR(actor, nextPos, toZone, maxDist, curPath, paths, visited)
						curPath.removeLast()
					}
				}
			}
        }
	}

	fun getCell(pos: Pos): ZCell {
		return grid[pos]
	}

	fun getCellOrNull(pos: Pos): ZCell? = if (grid.isOnGrid(pos) && !grid[pos].isCellTypeEmpty)
		grid[pos]
	else
		null

	fun getZone(pos: Pos): ZZone? = grid[pos].takeIf {
		it.zoneIndex >= 0
	}?.transform {
		getZone(it.zoneIndex)
	}

	fun getAllDoors(): List<ZDoor> = zones.map {
		it.doors
	}.flatten()

	fun setObjective(pos: Pos, type: ZCellType) {
		val cell = getCell(pos)
		cell.setCellType(type, true)
		getZone(cell.zoneIndex).isObjective = true
	}

	fun getCell(row: Int, col: Int): ZCell {
		return grid[row, col]
	}

    fun getDoor(door: ZDoor): ZWallFlag {
        return getCell(door.cellPosStart).getWallFlag(door.moveDirection)
    }

    fun setDoor(door: ZDoor, flag: ZWallFlag) {
        getCell(door.cellPosStart).setWallFlag(door.moveDirection, flag)
	    with(door.getOtherSide(this)) {
		    getCell(cellPosStart).setWallFlag(moveDirection, flag)
	    }
    }

    fun setDoorLocked(door: ZDoor) {
	    addLockedDoor(door)
	    val other = ZDoor(door.cellPosEnd, door.cellPosStart, door.moveDirection.opposite, door.lockedColor)
	    addLockedDoor(other)
    }

    private fun addLockedDoor(door: ZDoor) {
	    val cell = getCell(door.cellPosStart)
	    val zone = zones[cell.zoneIndex]
	    require(!zone.doors.contains(door))
	    cell.setWallFlag(door.moveDirection, ZWallFlag.LOCKED)
	    getCell(door.cellPosEnd).setWallFlag(door.moveDirection.opposite, ZWallFlag.LOCKED)
	    zone.addDoorIfNeeded(this, door)
	    require(door in zone.doors)
    }

    fun setSpawnZone(zoneIdx: Int, icon: ZIcon, canSpawnNecromancers: Boolean, isEscapableForNecromancers: Boolean, canBeRemovedFromBoard: Boolean) {
        val zone = zones[zoneIdx]
        // find a cell in the zone without a spawn
        for (pos in zone.getCells()) {
            val cell = getCell(pos)
	        if (cell.spawns.isEmpty()) {
		        cell.addSpawn(ZSpawnArea(pos, icon, ZDir.NORTH, canSpawnNecromancers, isEscapableForNecromancers, canBeRemovedFromBoard, false))
                return
            }
        }

        // we are adding a spawn to a cell that already has one 'GAH!' don't allow more than 2 in one cell and they
        // should be located across from each other
        for (pos in zone.getCells()) {
            val cell = getCell(pos)
	        if (cell.spawns.size < 2) {
		        val newDir = cell.spawns[0].dir.opposite
		        cell.addSpawn(ZSpawnArea(pos, icon, newDir, canSpawnNecromancers, isEscapableForNecromancers, canBeRemovedFromBoard, false))
                break
            }
        }
    }

	fun getMaxNoiseLevel(): Int {
		var maxNoise = 0
		for (z in zones) {
			if (z.noiseLevel > maxNoise) {
				maxNoise = z.noiseLevel
			}
		}
		return maxNoise
	}

	fun clearCaches() {
		maxNoiseLevelZoneCache.clear()
		canSeeCache = null
		shortestPathCache.clear()
	}

	@Omit
	private val maxNoiseLevelZoneCache = mutableListOf<ZZone>()

	fun getMaxNoiseLevelZones(): List<ZZone> {
		maxNoiseLevelZoneCache.takeIf { it.isNotEmpty() }?.let {
			return it
		}
		maxNoiseLevelZoneCache.addAll(zones.filter {
			it.noiseLevel > 0
		}.allMaxOf {
			it.noiseLevel
		})
		return maxNoiseLevelZoneCache
	}

	fun spawnActor(actor: ZActor<*>): Boolean {
		val zone = zones[actor.occupiedZone]
		for (c in zone.cells.indices) {
			val pos = zone.cells[zone.nextCellAndIncrement]
			val cell = getCell(pos)
			if (cell.isFull)
				continue
			val quadrant = actor.getSpawnQuadrant(this) ?: cell.findLowestPriorityOccupant(this)
			if (cell.getOccupant(this, quadrant) == null) {
				actor.occupiedCell = pos
				actor.occupiedQuadrant = quadrant
				addActor(actor)
				return true
			}
		}
        return false
    }

	/**
	 *
	 * @param actor
	 * @param zoneIndex
	 */
	fun addActor(actor: ZActor<*>, zoneIndex: Int, cellPos: Pos?): Boolean {
		var cellPos: Pos? = cellPos
		val zone = zones[zoneIndex]
		for (c in zone.cells.indices) {
			if (cellPos == null) {
				cellPos = zone.cells[zone.nextCellAndIncrement]
			}
			if (getCell(cellPos).isFull) {
				cellPos = null
				continue
			}
			addActorToCell(actor, cellPos)
            return true
        }
        if (actor.priority > 2) {
            //throw new AssertionError("Failed to add Actor");
            log.warn("Zone $zoneIndex is full!")
	        var minPriority = 100
	        var minPos: Pos? = null
            for (pos in zone.cells) {
	            val cell = getCell(pos)
	            val q = cell.findLowestPriorityOccupant(this)
	            val priority = cell.getOccupant(this, q)?.priority ?: 0
	            if (priority < minPriority) {
		            minPriority = priority
		            minPos = pos
	            }
            }
            minPos?.let {
                return addActorToCell(actor, it)
            }
        }
        return false
    }

	fun moveActor(actor: ZActor<*>, toZoneIndex: Int) {
		var targetPos: Pos? = null
		val fromZoneIndex = actor.occupiedZone
		val fromZone = zones[actor.occupiedZone]
		if (fromZoneIndex != toZoneIndex) {
			val toZone = zones[toZoneIndex]
			if (toZone.type === ZEnvironmentType.VAULT || fromZone.type == ZEnvironmentType.VAULT) {
				val dir = test(toZone.type === ZEnvironmentType.VAULT, ZDir.ASCEND, ZDir.DESCEND)
				toZone.doors.first {
					it.moveDirection == dir && getCell(it.cellPosEnd).zoneIndex == fromZoneIndex
				}.also {
					targetPos = it.cellPosStart
				}
			}
		}
        val fromCell = getCell(actor.occupiedCell)
        fromCell.setQuadrant(null, actor.occupiedQuadrant)
        fromZone.addNoise(-actor.noise)
        // if we are moving in or out of a vault, make so the cellPos moving is the opposing door
        addActor(actor, toZoneIndex, targetPos)
    }

	fun moveActor(actor: ZActor<*>, cellPos: Pos) {
		val cell = getCell(actor.occupiedCell)
		cell.setQuadrant(null, actor.occupiedQuadrant)
		zones[cell.zoneIndex].addNoise(-actor.noise)
		addActorToCell(actor, cellPos)
	}

	/**
	 *
	 * @param actor
	 */
	fun removeActor(actor: ZActor<*>) {
		val cell = getCell(actor.occupiedCell)
		cell.setQuadrant(null, actor.occupiedQuadrant)
		zones[cell.zoneIndex].addNoise(-actor.noise)
		_actors.remove(actor.id)
	}

	fun removeActor(id: String) {
		getActor(id)?.let {
			removeActor(it)
		}
	}

	/**
	 *
	 */
	fun removeCharacters() {
		_actors.removeAll { it.value is ZCharacter }
	}

    /**
     * Iterate over all cells
     * @return
     */
    fun getCellsIterator(): Grid.GridIterator<ZCell> {
        return grid.iterator()
    }

    /**
     * Iterate over all cells
     * @return
     */
    fun getCells(): Iterable<ZCell> {
        return grid.cells
    }

	fun getCells(zoneIdx: Int): Iterable<ZCell> {
		return getZone(zoneIdx).cells.map {
			getCell(it)
		}
	}

    fun getZombiesInZone(zoneIdx: Int): List<ZZombie> {
        return getActorsInZone(zoneIdx).filterIsInstance<ZZombie>().filter { it.isAlive }
    }

    fun getNumZombiesInZone(zoneIdx: Int): Int {
        return getActorsInZone(zoneIdx).count { it is ZZombie && it.isAlive }
    }

    fun getCharactersInZone(zoneIdx: Int): List<ZCharacter> {
        return getActorsInZone(zoneIdx).filterIsInstance<ZCharacter>()
    }

	fun getActorsInZone(zoneIndex: Int): List<ZActor<*>> {
		if (zoneIndex !in zones.indices)
			return emptyList()
		val actors: MutableList<ZActor<*>> = ArrayList()
		for (cellPos in zones[zoneIndex].cells) {
			getCell(cellPos).getOccupants(this).forEach {
				actors.add(it)
			}
		}
		return actors
	}

	fun getAllActors(): List<ZActor<*>> = _actors.values.toList()

	fun getCharacterFamiliars(name: ZPlayerName): List<ZFamiliar> = _actors.values
		.filterIsInstance<ZFamiliar>().filter { it.handler == name }

	inline fun <reified T : ZActor<*>> getAllActorsOfType(): List<T> = actors.values.filterIsInstance<T>()

	fun getAllZombies(vararg types: ZZombieType): List<ZZombie> {
		val all = getAllActors().filterIsInstance<ZZombie>()
		if (types.isNotEmpty()) {
			return all.filter { types.contains(it.type) }
		}
		return all
	}

	fun getAllCharacters(): List<ZCharacter> {
		return getAllActors().filterIsInstance<ZCharacter>()
	}

	private fun addActorToCell(actor: ZActor<*>, pos: Pos): Boolean {
		val cell = getCell(pos)
		var current: ZCellQuadrant? = if (actor.isOccupying()) actor.occupiedQuadrant else null
		if (current == null) {
			current = actor.getSpawnQuadrant(this)
		}
		if (current == null || cell.getOccupant(this, current) != null) {
			current = cell.findLowestPriorityOccupant(this)
		}
		cell.getOccupant(this, current)?.let {
			if (it.priority >= actor.priority)
				return false
		}
		_actors[actor.id] = actor
		cell.setQuadrant(actor, current)
		if (actor.occupiedZone != cell.zoneIndex)
			actor.priorZone = actor.occupiedZone
		actor.occupiedZone = cell.zoneIndex
		actor.occupiedCell = pos
		actor.occupiedQuadrant = current
		zones[cell.zoneIndex].addNoise(actor.noise)
		return true
	}

	fun addActor(actor: ZActor<*>) {
		_actors[actor.id] = actor
		getCell(actor.occupiedCell).setQuadrant(actor, actor.occupiedQuadrant)
		actor.updateRect(this)
	}

	fun getUndiscoveredZones(startPos: Pos, undiscovered: MutableSet<Int>) {
		if (!grid.isOnGrid(startPos))
			return
		val cell = getCell(startPos)
		if (cell.discovered) return
		cell.discovered = true
		val zone = zones[cell.zoneIndex]
		undiscovered.add(cell.zoneIndex)
		for (dir in ZDir.entries) {
			if (cell.getWallFlag(dir).openedForWalk)
				getAdjacentOrNull(startPos, dir)?.let {
					getUndiscoveredZones(it, undiscovered)
				}
		}
	}

    fun getCellsOfType(type: ZCellType): List<ZCell> {
        val start: MutableList<ZCell> = ArrayList()
        for (cell in getCells()) {
            if (cell.isCellType(type)) {
                start.add(cell)
            }
        }
        return start
    }

    fun resetNoise() {
        for (zone in zones) {
            zone.noiseLevel = 0
            for (pos in zone.cells) {
	            for (a in getCell(pos).getOccupants(this)) {
		            if (a.isNoisy) {
			            zone.addNoise(1)
		            }
	            }
            }
        }
    }

	/**
	 *
	 * @param actor
	 * @param dir
	 * @return
	 */
	fun canMove(actor: ZActor<*>, dir: ZDir): Boolean {
		return getCell(actor.occupiedCell).getWallFlag(dir).openedForWalk
	}

	fun getMoveType(actor: ZActor<*>, dir: ZDir): ZActionType? =
		when (getCell(actor.occupiedCell).getWallFlag(dir)) {
			ZWallFlag.WALL,
			ZWallFlag.CLOSED,
			ZWallFlag.LOCKED,
			ZWallFlag.RAMPART -> null

			ZWallFlag.NONE,
			ZWallFlag.OPEN,
			ZWallFlag.HEDGE -> ZActionType.MOVE

			ZWallFlag.LEDGE ->
				if (getZone(actor.occupiedZone).type == ZEnvironmentType.WATER)
					ZActionType.CLIMB
				else ZActionType.MOVE
		}

	/**
	 *
	 * @return
	 */
	fun getDimension(): GDimension {
		if (grid.isEmpty()) return GDimension(0f, 0f)
		val br = grid[grid.rows - 1, grid.cols - 1].bottomRight
		return GDimension(br.x, br.y)
    }

    fun getDirection(fromZone: Int, toZone: Int): ZDir {
        val start = getZone(fromZone)
        val end = getZone(toZone)
        val dv: Vector2D = end.center.sub(start.center)
        if (dv.isZero) return ZDir.EAST
        val angle = dv.angleOf()
        if (angle > 270 - 45 && angle < 270 + 45) return ZDir.NORTH
        if (angle > 180 - 45 && angle < 180 + 45) return ZDir.WEST
        return if (angle > 90 - 45 && angle < 90 + 45) ZDir.SOUTH else ZDir.EAST
    }

	fun getZonesOfType(type: ZEnvironmentType): List<ZZone> {
        return zones.filter { zone -> zone.type === type }
    }

    fun getSpawnZones(): List<ZZone> {
        return zones.filter { zone -> isZoneSpawnable(zone.zoneIndex) }
    }

    fun isZoneSpawnable(zoneIndex: Int): Boolean {
	    return getCells(zoneIndex).firstOrNull { it.spawns.isNotEmpty() } != null
    }

	fun removeSpawn(spawn: ZSpawnArea) {
		val cell = getCell(spawn.cellPos)
		cell.removeSpawn(spawn.dir)
	}

	fun getActor(position: ZActorPosition): ZActor<*> = getActorOrNull(position)!!

	fun getActorOrNull(position: ZActorPosition): ZActor<*>? {
		return grid[position.pos].getOccupant(this, position.quadrant)
	}

	fun getCharacter(type: ZPlayerName): ZCharacter = getActor(type.name) as ZCharacter

	fun getCharacterOrNull(type: ZPlayerName?): ZCharacter? = getActor(type?.name) as ZCharacter?

	fun isZoneEscapableForNecromancers(zoneIdx: Int): Boolean {
		val zone = getZone(zoneIdx)
		for (pos: Pos in zone.getCells()) {
			for (area: ZSpawnArea in getCell(pos).spawnAreas) {
				if (area.isEscapableForNecromancers) return true
			}
		}
		return false
	}

	fun canZoneSpawnNecromancers(zoneIdx: Int): Boolean {
		getZone(zoneIdx).getCells().forEach { pos ->
			if (getCell(pos).spawnAreas.firstOrNull { !it.isCanSpawnNecromancers } != null)
				return false
		}
		return true
	}

	// return the center of all players, or the center of the start tile, or just the center
	fun getLogicalCenter(): IVector2D {
		with(getAllCharacters()) {
			if (isNotEmpty()) {
				val center = MutableVector2D()
				forEach {
					center.addEq(it.center)
				}
				return center.scaledBy(1.0f / size)
			}
		}

		with(getCellsOfType(ZCellType.START)) {
			if (isNotEmpty()) {
				val center = MutableVector2D()
				forEach {
					center.addEq(it.center)
				}
				return center.scaledBy(1.0f / size)
			}
		}
		return center
	}

	fun getDistanceBetweenZones(z0: Int, z1: Int): Int {
		val r0 = getZone(z0).enclosingRect()
		val r1 = getZone(z1).enclosingRect()
		var dx = (r0.left - r1.left).roundToInt()
		var dy = (r0.top - r1.top).roundToInt()
		if (dx < 0)
			dx = abs(dx) - r0.width.roundToInt()
		else
			dx -= r1.width.roundToInt()
		if (dy < 0)
			dy = abs(dy) - r0.height.roundToInt()
		else
			dy -= r1.height.roundToInt()
		return max(dx, dy)
	}

	fun isZoneTargetForCatapult(zone: ZZone): Boolean {
		if (zone.type == ZEnvironmentType.HOARD && hoard.isNotEmpty())
			return true
		for (pos in zone.cells) {
			getCell(pos).spawnAreas.forEach {
				if (it.isCanBeDestroyedByCatapult)
					return true
			}
		}
		return getNumZombiesInZone(zone.zoneIndex) > 0
	}

	fun isZoneObserved(zoneIdx: Int): Boolean {
		getAllCharacters().filter { it.isAlive }.forEach {
			if (canSee(it.occupiedZone, zoneIdx))
				return true
		}
		return false
	}

	suspend fun spawnHoardZombies(zoneIdx: Int, game: ZGame) {
		while (hoard.isNotEmpty()) {
			hoard.forEach { (type, _) ->
				val zombie = type.spawn(zoneIdx)
				if (spawnActor(zombie))
					game.onZombieSpawned(zombie)
				else
					return
				_hoard.increment(type, -1)
			}
			_hoard.removeAll { it.value <= 0 }
		}
	}

	@Synchronized
	fun toString2(): String = StringBuffer().also {
		for (r in 0 until rows) {
			var cell: ZCell = grid[0, 0]
			for (c in 0 until columns) {
				cell = grid.get(r, c)
				it.append("+")
				it.append(cell.getWallFlag(ZDir.NORTH).codeH)
			}
			it.append("+\n")
			for (c in 0 until columns) {
				cell = grid.get(r, c)
				it.append(String.format("%cZ%-2d%2s", cell.getWallFlag(ZDir.WEST).codeV[0], cell.zoneIndex, cell.environment.code))
			}
			it.append(cell.getWallFlag(ZDir.EAST).codeV[0]).append("\n")
			for (c in 0 until columns) {
				cell = grid.get(r, c)
				it.append(String.format("%c%5s", cell.getWallFlag(ZDir.WEST).codeV[1], cell.codes.take(4)))
			}
			it.append(cell.getWallFlag(ZDir.EAST).codeV[1]).append("\n")
			for (c in 0 until columns) {
				cell = grid.get(r, c)
				it.append(String.format("%c%5s", cell.getWallFlag(ZDir.WEST).codeV[2], cell.codes.takeLast(4)))
			}
			it.append(cell.getWallFlag(ZDir.EAST).codeV[2]).append("\n")
		}
		for (c in 0 until columns) {
			val cell = grid.get(rows - 1, c)
			it.append("+")
			it.append(cell.getWallFlag(ZDir.SOUTH).codeH)
		}
		it.append("+").append("\n")
	}.toString()
}