package cc.lib.zombicide

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector
import cc.lib.utils.*
import cc.lib.utils.Grid.Pos
import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.zombicide.ZDir.Companion.getDirFrom
import cc.lib.zombicide.ZDir.Companion.valuesSorted
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class ZBoard : Reflector<ZBoard>, IDimension {
    companion object {
        private val log = LoggerFactory.getLogger(ZBoard::class.java)

	    init {
		    addAllFields(ZBoard::class.java)
	    }
    }

	private var grid: Grid<ZCell> = Grid()
	var zones: List<ZZone> = emptyList()
		private set

	private val actors = Collections.synchronizedMap(HashMap<String, ZActor>())

	constructor()
	constructor(grid: Grid<ZCell>, zones: List<ZZone>) {
		this.grid = grid
		this.zones = zones
		/*
		// collect all the doors
		val vaults = mutableMapOf<Int, MutableList<Pos>>()
		zones.forEach { zone ->
			zone.cells.forEach { pos ->
				grid[pos].takeIf { it.vaultId > 0 }?.let { cell ->
					vaults.getOrPut(cell.vaultId) { mutableListOf() }.add(pos)
				}
				ZDir.compassValues.forEach { dir ->
					if (grid[pos].getWallFlag(dir).isDoor) {
						zone.addDoorIfNeeded(this, ZDoor(pos, dir.getAdjacent(pos)!!, dir, GColor.BLACK))
					}
				}
			}
		}
		this.vaults = vaults.map { (id, list) ->
			require(list.size == 2)
			list.sortBy {
				grid[it].environment.getVaultDirection().ordinal
			}
			val ascending = ZDoor(list[0], list[1], ZDir.ASCEND, grid[list[0]].vaultType.color)
			val descending = ZDoor(list[1], list[0], ZDir.DESCEND, grid[list[1]].vaultType.color)
			id to Pair(ascending, descending)
		}.toMap()*/
	}

	val rows: Int
		get() = grid.rows
	val columns: Int
		get() = grid.cols

	private val hoard = mutableMapOf<ZZombieType, Int>()
	fun getHoard(): Map<ZZombieType, Int> = hoard

	fun addToHoard(type: ZZombieType, num: Int = 1) {
		hoard.increment(type, num)
	}

	fun getZone(index: Int): ZZone {
		return zones[index]
	}

	fun getNumZones(): Int {
		return zones.size
	}

	override fun getWidth(): Float {
		return columns.toFloat()
	}

	override fun getHeight(): Float {
		return rows.toFloat()
	}

	fun getActor(id: String?): ZActor? = actors[id ?: ""]

	override fun isEmpty(): Boolean = grid.isEmpty

	/**
	 * Get list of accessible zones
	 *
	 * @param fromZoneIndex
	 * @param minDist
	 * @param maxDist
	 * @return
	 */
	fun getAccessibleZones(
		actor: ZActor,
		minDist: Int,
		maxDist: Int,
		action: ZActionType
	): List<Int> {
		val fromZoneIndex = actor.occupiedZone
		if (maxDist == 0) return listOf(fromZoneIndex)
		val result: MutableSet<Int> = HashSet()
		val options = if (action === ZActionType.MOVE) ZDir.values() else compassValues
		val birdsEye =
			getZone(fromZoneIndex).type === ZZoneType.TOWER || (getZone(fromZoneIndex).isOutside && actor.hasSkill(
				ZSkill.Birds_eye_view
			))

		if (action.isProjectile && birdsEye) {
			// special case here
			// buildings do not block from being able to see beyond
			// can see into buildings with open door but only for a single zone
			for (cellPos in zones[fromZoneIndex].cells) {
				for (dir in compassValues) {
					var pos = cellPos
					var lastIndoorZone = -1
					if (grid[pos].getWallFlag(dir).openedForAction(action)) {
						for (i in 0 until minDist) {
							pos = getAdjacent(pos, dir)
							if (!grid.isOnGrid(pos)) {
								break
							}
						}
						for (i in minDist..maxDist) {
							if (!grid.isOnGrid(pos)) break
							val cell = grid[pos]
							val zone = getZone(cell.zoneIndex)
						    when (zone.type) {
							    ZZoneType.TOWER, ZZoneType.OUTDOORS -> {
								    lastIndoorZone = -1
								    result.add(cell.zoneIndex)
							    }
							    ZZoneType.BUILDING -> if (lastIndoorZone < 0) {
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
			            pos = getAdjacent(pos, dir)
			            dist++
                    }
                    result.add(grid[pos].zoneIndex)
                    while (dist < maxDist) {
                        var cell = grid[pos]
	                    if (!cell.getWallFlag(dir).openedForAction(action)) {
		                    break
	                    }
                        pos = getAdjacent(pos, dir)
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

	fun canSee(fromZone: Int, toZone: Int): Boolean {
		if (fromZone == toZone) return true
		for (pos0 in zones[fromZone].cells) {
			for (pos1 in zones[toZone].cells) {
				if (canSeeCell(pos0, pos1)) return true
			}
		}
		return false
	}

	fun canSeeCell(fromCell: Pos, toCell: Pos): Boolean {
		var fromCell = fromCell
		val dir = getDirFrom(fromCell, toCell) ?: return false
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
            fromCell = getAdjacent(fromCell, dir)
        }
        return true
    }

	/**
	 * Returns a list of directions the zombie can move
	 * @See DIR_NORTH, DIR_SOUTH, DIR_EAST, DIR_WEST
	 *
	 * @param fromPos
	 * @param toZoneIndex
	 * @return
	 */
	fun getShortestPathOptions(
		actor: ZActor,
		toZoneIndex: Int
	): List<List<ZDir>> {
		val fromPos = actor.occupiedCell
		if (grid[fromPos].zoneIndex == toZoneIndex) return emptyList()
		val toZone = zones[toZoneIndex]
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
		return allPaths.sortedBy { it.size }
	}

	private fun getShortestPathOptions(
		actor: ZActor,
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
		actor: ZActor,
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
        if (visited.contains(fromPos)) return
        visited.add(fromPos)
		val fromCell = grid[fromPos]
		val toPos = toZone.cells.first()
        for (dir in valuesSorted(fromPos, toPos)) {
	        if (!actor.isBlockedBy(fromCell.getWallFlag(dir))) {
		        val nextPos = getAdjacent(fromPos, dir)
		        if (visited.contains(nextPos)) continue

		        // is the cell full?
		        if (getCell(nextPos).isFull) continue
		        curPath.addLast(dir)
		        searchPathsR(actor, nextPos, toZone, maxDist, curPath, paths, visited)
		        curPath.removeLast()
	        }
        }
        val fromZone = zones[fromCell.zoneIndex]
        for (door in fromZone.doors) {
            if (door.cellPosStart == fromPos && !door.isClosed(this)) {
                curPath.addLast(door.moveDirection)
	            searchPathsR(actor, door.cellPosEnd, toZone, maxDist, curPath, paths, visited)
	            curPath.removeLast()
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

	fun getZone(pos: Pos): ZZone? = grid[pos]?.takeIf {
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
            if (cell.numSpawns == 0) {
                cell.spawns[cell.numSpawns++] = ZSpawnArea(pos, icon, ZDir.NORTH, canSpawnNecromancers, isEscapableForNecromancers, canBeRemovedFromBoard)
                return
            }
        }

        // we are adding a spawn to a cell that already has one 'GAH!' don't allow more than 2 in one cell and they
        // should be located across from each other
        for (pos in zone.getCells()) {
            val cell = getCell(pos)
            if (cell.numSpawns < 2) {
                val newDir = cell.spawns[0]!!.dir.opposite
                cell.spawns[cell.numSpawns++] = ZSpawnArea(pos, icon, newDir, canSpawnNecromancers, isEscapableForNecromancers, canBeRemovedFromBoard)
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

    fun getMaxNoiseLevelZones(): List<ZZone> {
	    val result = mutableListOf<ZZone>()
        var maxNoise = 1
        for (z in zones) {
            if (z.noiseLevel > maxNoise) {
                result.clear()
	            result.add(z)
                maxNoise = z.noiseLevel
            } else if (z.noiseLevel == maxNoise) {
                result.add(z)
            }
        }
        return result
    }

	fun spawnActor(actor: ZActor): Boolean {
		val zone = zones[actor.occupiedZone]
		for (c in zone.cells.indices) {
			val pos = zone.cells[zone.nextCellAndIncrement]
			val cell = getCell(pos)
			if (cell.isFull)
				continue
			val quadrant = actor.getSpawnQuadrant() ?: cell.findLowestPriorityOccupant(this)
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
	fun addActor(actor: ZActor, zoneIndex: Int, cellPos: Pos?): Boolean {
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

	fun moveActor(actor: ZActor, toZoneIndex: Int) {
		var targetPos: Pos? = null
		val fromZoneIndex = actor.occupiedZone
		val fromZone = zones[actor.occupiedZone]
		if (fromZoneIndex != toZoneIndex) {
			val toZone = zones[toZoneIndex]
			if (toZone.type === ZZoneType.VAULT || fromZone.type == ZZoneType.VAULT) {
				val dir = test(toZone.type === ZZoneType.VAULT, ZDir.ASCEND, ZDir.DESCEND)
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

	fun moveActor(actor: ZActor, cellPos: Pos) {
		val cell = getCell(actor.occupiedCell)
		cell.setQuadrant(null, actor.occupiedQuadrant)
		zones[cell.zoneIndex].addNoise(-actor.noise)
		addActorToCell(actor, cellPos)
	}

	/**
	 *
	 * @param actor
	 */
	fun removeActor(actor: ZActor) {
		val cell = getCell(actor.occupiedCell)
		cell.setQuadrant(null, actor.occupiedQuadrant)
		zones[cell.zoneIndex].addNoise(-actor.noise)
		actors.remove(actor.getId())
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
		for (c in getAllCharacters()) {
			removeActor(c)
		}
	}

    /**
     * Iterate over all cells
     * @return
     */
    fun getCellsIterator(): Grid.Iterator<ZCell> {
        return grid.iterator()
    }

    /**
     * Iterate over all cells
     * @return
     */
    fun getCells(): Iterable<ZCell> {
        return grid.cells
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

	fun getActorsInZone(zoneIndex: Int): List<ZActor> {
		if (zoneIndex !in zones.indices)
			return emptyList()
		val actors: MutableList<ZActor> = ArrayList()
		for (cellPos in zones[zoneIndex].cells) {
			getCell(cellPos).getOccupants(this).forEach {
				actors.add(it)
			}
		}
		return actors
	}

	fun getAllActors(): List<ZActor> = actors.values.toList()

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

	private fun addActorToCell(actor: ZActor, pos: Pos): Boolean {
		val cell = getCell(pos)
		var current: ZCellQuadrant? = if (actor.isOccupying()) actor.occupiedQuadrant else null
		if (current == null) {
			current = actor.getSpawnQuadrant()
		}
		if (current == null || cell.getOccupant(this, current) != null) {
			current = cell.findLowestPriorityOccupant(this)
		}
		cell.getOccupant(this, current)?.let {
			if (it.priority >= actor.priority)
				return false
		}
		actors[actor.getId()] = actor
		cell.setQuadrant(actor, current)
		if (actor.occupiedZone != cell.zoneIndex)
			actor.priorZone = actor.occupiedZone
		actor.occupiedZone = cell.zoneIndex
		actor.occupiedCell = pos
		actor.occupiedQuadrant = current
		zones[cell.zoneIndex].addNoise(actor.noise)
		return true
	}

	fun addActor(actor: ZActor) {
		actors[actor.getId()] = actor
		getCell(actor.occupiedCell).setQuadrant(actor, actor.occupiedQuadrant)
		actor.updateRect(this)
	}

	fun getUndiscoveredIndoorZones(startPos: Pos, undiscovered: MutableSet<Int>) {
		if (!grid.isOnGrid(startPos))
			return
		val cell = getCell(startPos)
		if (cell.discovered) return
		cell.discovered = true
		val zone = zones[cell.zoneIndex]
		if (!zone.isSearchable) return
		undiscovered.add(cell.zoneIndex)
		for (dir in ZDir.entries) {
			if (cell.getWallFlag(dir).openedForWalk)
				getUndiscoveredIndoorZones(getAdjacent(startPos, dir), undiscovered)
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
	fun canMove(actor: ZActor, dir: ZDir): Boolean {
		return getCell(actor.occupiedCell).getWallFlag(dir).openedForWalk
	}

	fun getMoveType(actor: ZActor, dir: ZDir): ZActionType? =
		when (getCell(actor.occupiedCell).getWallFlag(dir)) {
			ZWallFlag.WALL,
			ZWallFlag.CLOSED,
			ZWallFlag.LOCKED,
			ZWallFlag.RAMPART -> null

			ZWallFlag.NONE,
			ZWallFlag.OPEN,
			ZWallFlag.HEDGE -> ZActionType.MOVE

			ZWallFlag.LEDGE ->
				if (getZone(actor.occupiedZone).type == ZZoneType.WATER)
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

    fun getZonesOfType(type: ZZoneType): List<ZZone> {
        return zones.filter { zone -> zone.type === type }
    }

    fun getSpawnZones(): List<ZZone> {
        return zones.filter { zone -> isZoneSpawnable(zone.zoneIndex) }
    }

    fun isZoneSpawnable(zoneIndex: Int): Boolean {
        val zone = getZone(zoneIndex)
        for (pos in zone.getCells()) {
            val cell = getCell(pos)
	        if (cell.numSpawns > 0) return true
        }
	    return false
    }

	fun removeSpawn(spawn: ZSpawnArea) {
		val cell = getCell(spawn.cellPos)
		cell.removeSpawn(spawn.dir)
	}

	fun getActor(position: ZActorPosition): ZActor = getActorOrNull(position)!!

	fun getActorOrNull(position: ZActorPosition): ZActor? {
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

	fun getZombiePathTowardNearestSpawn(zombie: ZZombie): List<ZDir> {
		val pathsMap: MutableMap<Int, List<ZDir>> = HashMap()
		var shortestPath: Int? = null
		zones.filter { z: ZZone ->
			zombie.startZone != z.zoneIndex && isZoneEscapableForNecromancers(
				z.zoneIndex
			)
		}.forEach { zone ->
			val paths: List<List<ZDir>> =
				getShortestPathOptions(zombie, zone.zoneIndex)
			if (paths.isNotEmpty()) {
				pathsMap[zone.zoneIndex] = paths[0]
				if (shortestPath == null || paths.size < pathsMap[shortestPath]!!.size) {
					shortestPath = zone.zoneIndex
				}
			}
		}
		return shortestPath?.let { zoneIdx ->
			pathsMap[zoneIdx]?.also { list ->
				getZone(zoneIdx).getCells().forEach {
					zombie.escapeZone =
						getCell(it).spawnAreas.firstOrNull { it.isEscapableForNecromancers }
				}
			} ?: emptyList()
		} ?: emptyList()
	}

	fun getZombiePathTowardVisibleCharactersOrLoudestZone(zombie: ZZombie): List<ZDir> {
		// zombie will move toward players it can see first and then noisy areas second
		var maxNoise = 0
		var targetZone = -1
		getAllCharacters().filter { ch: ZCharacter -> !ch.isInvisible && ch.isAlive }.forEach { c ->
			if (canSee(zombie.occupiedZone, c.occupiedZone)) {
				val noiseLevel = getZone(c.occupiedZone).noiseLevel
				if (maxNoise < noiseLevel) {
					targetZone = c.occupiedZone
					maxNoise = noiseLevel
				}
			}
		}
		val paths: MutableList<List<ZDir>> = mutableListOf()
		if (targetZone < 0) {
			getMaxNoiseLevelZones().forEach {
				paths.addAll(getShortestPathOptions(zombie, it.zoneIndex))
			}
		} else {
			paths.addAll(getShortestPathOptions(zombie, targetZone))
		}
		return when (paths.size) {
			0 -> emptyList()
			1 -> paths.first()
			else -> {
				val min = paths.minOf { it.size }
				paths.filter { it.size == min }.random()
			}
		}
	}

	// return the center of all players, or the center of the start tile, or just the center
	fun getLogicalCenter(): IVector2D {
		with(getAllCharacters()) {
			if (size > 0) {
				val center = MutableVector2D()
				forEach {
					center.addEq(it.center)
				}
				return center.scaledBy(1.0f / size)
			}
		}

		with(getCellsOfType(ZCellType.START)) {
			if (size > 0) {
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
		val r0 = getZone(z0)
		val r1 = getZone(z1)
		var dx = (r0.getLeft() - r1.getLeft()).roundToInt()
		var dy = (r0.getTop() - r1.getTop()).roundToInt()
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
		if (zone.type == ZZoneType.HOARD && hoard.isNotEmpty())
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

	fun spawnHoardZombies(zoneIdx: Int, game: ZGame) {
		while (hoard.isNotEmpty()) {
			hoard.forEach { (type, _) ->
				val zombie = ZZombie(type, zoneIdx)
				if (spawnActor(zombie))
					game.onZombieSpawned(zombie)
				else
					return
				hoard.increment(type, -1)
			}
			hoard.removeAll { it.value <= 0 }
		}
	}

}