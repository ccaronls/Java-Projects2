package cc.lib.zombicide

import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.Reflector
import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.zombicide.ZDir.Companion.getDirFrom
import cc.lib.zombicide.ZDir.Companion.valuesSorted
import java.util.*

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

    constructor() {}
    constructor(grid: Grid<ZCell>, zones: List<ZZone>) {
        this.grid = grid
        this.zones = zones
    }

    val rows: Int
        get() = grid.rows
    val columns: Int
        get() = grid.cols

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

    /**
     * Get list of accessable zones
     *
     * @param zoneIndex
     * @param minDist
     * @param maxDist
     * @return
     */
    fun getAccessableZones(zoneIndex: Int, minDist: Int, maxDist: Int, action: ZActionType): List<Int> {
        if (maxDist == 0) return listOf(zoneIndex)
        val result: MutableSet<Int> = HashSet()
        val options = if (action === ZActionType.MOVE) ZDir.values() else compassValues
        if (getZone(zoneIndex).type === ZZoneType.TOWER && action.isProjectile) {
            // special case here
            // buildings do not block from being able to see beyond
            // can see into buildings with open door but only for a single zone
            for (cellPos in zones[zoneIndex].cells) {
                for (dir in compassValues) {
                    var pos = cellPos
                    var lastIndoorZone = -1
                    if (grid[pos].getWallFlag(dir).openForProjectile) {
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
                                    if (cell.getWallFlag(dir.opposite).opened) {
                                        result.add(cell.zoneIndex)
                                    }
                                }
                            }
                            pos = getAdjacent(pos, dir)
                        }
                    }
                }
            }
        } else {
            for (cellPos in zones[zoneIndex].cells) {
                // fan out in all direction to given distance
                //for (int dir = 0; dir <4; dir++) {
                for (dir in options) {
                    var pos = cellPos
                    var open = true
                    var dist = 0
                    var buildingZoneIdx = -1
                    while (dist < minDist) {
                        val cell = grid[pos]
                        if (!cell.getWallFlag(dir).opened) {
                            open = false
                            break
                        }
                        pos = getAdjacent(pos, dir)
                        dist++
                    }
                    if (!open) continue
                    result.add(grid[pos].zoneIndex)
                    while (dist < maxDist) {
                        var cell = grid[pos]
                        if (!cell.getWallFlag(dir).opened) {
                            break
                        }
                        pos = getAdjacent(pos, dir)
                        cell = grid[pos]
                        if (cell.isInside) {
                            if (buildingZoneIdx < 0) buildingZoneIdx = cell.zoneIndex else if (cell.zoneIndex != buildingZoneIdx) break
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

    fun getAdjacent(from: Grid.Pos, dir: ZDir): Grid.Pos {
        when (dir) {
            ZDir.NORTH, ZDir.SOUTH, ZDir.EAST, ZDir.WEST -> return Grid.Pos(from.row + dir.dy, from.column + dir.dx)
            ZDir.ASCEND, ZDir.DESCEND -> return findDoor(from, dir).cellPosEnd
        }
    }

    @Synchronized
    fun findDoor(pos: Grid.Pos, dir: ZDir): ZDoor {
        val zone = zones[getCell(pos).zoneIndex]
        for (door in zone.doors) {
            if (door.cellPosStart == pos && door.moveDirection === dir) {
                return door
            }
        }
        throw GException("No door found at $pos, $dir")
    }

    fun findVault(id: Int): ZDoor {
        var numIds = 0
        var ids = arrayOfNulls<Grid.Pos>(2)
        var color: GColor? = null
        zones.forEach { zone->
            zone.cells.forEach { pos->
                with (getCell(pos)) {
                    if (vaultId == id) {
                        color = this.vaultType.color
                        if (this.isVault) {
                            ids[1] = pos
                        } else {
                            ids[0] = pos
                        }
                        numIds ++
                    }
                }
            }
        }
        if (numIds != 2)
            throw GException("wrong number of positions for vaultId $id")
        return ZDoor(ids[0]!!, ids[1]!!, ZDir.DESCEND, color!!)
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

    fun canSeeCell(fromCell: Grid.Pos, toCell: Grid.Pos): Boolean {
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
            if (!cell.getWallFlag(dir).opened) {
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
    fun getShortestPathOptions(fromPos: Grid.Pos, toZoneIndex: Int): List<List<ZDir>> {
        if (grid[fromPos].zoneIndex == toZoneIndex) return emptyList()
        val toZone = zones[toZoneIndex]
        val allPaths: MutableList<List<ZDir>> = ArrayList()
        var maxDist = (grid.rows + grid.cols) * 2
        val visited: MutableSet<Grid.Pos> = HashSet()
        for (bCellPos in toZone.cells) {
            val paths = getShortestPathOptions(fromPos, bCellPos, visited, maxDist)
            for (l in paths) {
                maxDist = Math.min(maxDist, l.size)
            }
            allPaths.addAll(paths)
        }
        val it = allPaths.iterator()
        while (it.hasNext()) {
            if (it.next().size > maxDist) it.remove()
        }
        return allPaths
    }

    private fun getShortestPathOptions(fromCell: Grid.Pos, toCell: Grid.Pos, visited: MutableSet<Grid.Pos>, maxDist: Int): List<List<ZDir>> {
        val paths: MutableList<List<ZDir>> = ArrayList()
        searchPathsR(fromCell, toCell, intArrayOf(maxDist), LinkedList(), paths, visited)
        return paths
    }

    private fun searchPathsR(fromPos: Grid.Pos, toPos: Grid.Pos, maxDist: IntArray, curPath: LinkedList<ZDir>, paths: MutableList<List<ZDir>>, visited: MutableSet<Grid.Pos>) {
        if (fromPos == toPos) {
            if (curPath.size > 0) {
                paths.add(ArrayList(curPath))
                maxDist[0] = Math.min(maxDist[0], curPath.size)
            }
            return
        }
        if (curPath.size >= maxDist[0]) return
        if (visited.contains(fromPos)) return
        visited.add(fromPos)
        val fromCell = grid[fromPos]
        for (dir in valuesSorted(fromPos, toPos)) {
            if (fromCell.getWallFlag(dir).opened) {
                val nextPos = getAdjacent(fromPos, dir)
                if (visited.contains(nextPos)) continue

                // is the cell full?
                if (getCell(nextPos).isFull) continue
                curPath.addLast(dir)
                searchPathsR(nextPos, toPos, maxDist, curPath, paths, visited)
                curPath.removeLast()
            }
        }
        val fromZone = zones[fromCell.zoneIndex]
        for (door in fromZone.doors) {
            if (door.cellPosStart == fromPos && !door.isClosed(this)) {
                curPath.addLast(door.moveDirection)
                searchPathsR(door.cellPosEnd, toPos, maxDist, curPath, paths, visited)
                curPath.removeLast()
            }
        }
    }

    fun getCell(pos: Grid.Pos): ZCell {
        return grid[pos]
    }

    fun setObjective(pos: Grid.Pos, type: ZCellType) {
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
        with (door.otherSide) {
            getCell(cellPosStart).setWallFlag(moveDirection, flag)
        }
    }

    fun setDoorLocked(door: ZDoor) {
        addLockedDoor(door)
        addLockedDoor(door.otherSide)
    }

    private fun addLockedDoor(door: ZDoor) {
        val cell = getCell(door.cellPosStart)
        val zone = zones[cell.zoneIndex]
        if (zone.doors.contains(door)) throw AssertionError("Already have a door like that.")
        cell.setWallFlag(door.moveDirection, ZWallFlag.LOCKED)
        zone.doors.add(door)
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

    fun getMaxNoiseLevelZone(): ZZone? {
        var maxNoise = 0
        var maxZone: ZZone? = null
        for (z in zones) {
            if (z.noiseLevel > maxNoise) {
                maxZone = z
                maxNoise = z.noiseLevel
            } else if (z.noiseLevel == maxNoise) {
                maxZone = null // when multiple zones share same noise level, then neither are the max
            }
        }
        return maxZone
    }

    fun spawnActor(actor: ZActor<*>) : Boolean {
        val zone = zones[actor.occupiedZone]
        for (c in zone.cells.indices) {
            val pos = zone.cells[zone.nextCellAndIncrement]
            val cell = getCell(pos)
            if (cell.isFull)
                continue
            val quadrant = cell.findLowestPriorityOccupant()
            if (cell.getOccupant(quadrant) == null) {
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
    fun addActor(actor: ZActor<*>, zoneIndex: Int, cellPos: Grid.Pos?): Boolean {
        var cellPos:Grid.Pos? = cellPos
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
            var minPos: Grid.Pos? = null
            for (pos in zone.cells) {
                val cell = getCell(pos)
                val q = cell.findLowestPriorityOccupant()
                val priority = cell.getOccupant(q)?.priority?:0
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
        var targetPos: Grid.Pos? = null
        val fromZoneIndex = actor.occupiedZone
        val fromZone = zones[actor.occupiedZone]
        if (fromZoneIndex != toZoneIndex) {
            val toZone = zones[toZoneIndex]
            if (toZone.type === ZZoneType.VAULT) {
                // moving into a vault
                findDoor(actor.occupiedCell, ZDir.DESCEND).also {
                    targetPos = it.cellPosEnd
                }
            } else if (fromZone.type === ZZoneType.VAULT) {
                // moving out of a vault
                findDoor(actor.occupiedCell, ZDir.ASCEND).also {
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

    fun moveActor(actor: ZActor<*>, cellPos: Grid.Pos) {
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
        //actor.occupiedZone = -1;
        //actor.occupiedQuadrant = -1;
        //actor.occupiedCell = null;
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
        return getActorsInZone(zoneIdx).count { `object`: ZActor<*> -> `object` is ZZombie && `object`.isAlive }
    }

    fun getCharactersInZone(zoneIdx: Int): List<ZCharacter> {
        return getActorsInZone(zoneIdx).filterIsInstance<ZCharacter>()
    }

    fun getActorsInZone(zoneIndex: Int): List<ZActor<*>> {
        if (zoneIndex < 0)
            return emptyList()
        val actors: MutableList<ZActor<*>> = ArrayList()
        for (cellPos in zones[zoneIndex].cells) {
            getCell(cellPos).occupant.forEach {
                actors.add(it)
            }
        }
        return actors
    }

    fun getAllActors(): List<ZActor<*>> {
        val actors: MutableList<ZActor<*>> = ArrayList()
        for (cell in grid.cells) {
            cell.occupant.forEach { 
                actors.add(it)
            }
        }
        return actors
    }

    fun getAllZombies(): List<ZZombie> {
        return getAllActors().filterIsInstance<ZZombie>()
    }

    fun getAllCharacters(): List<ZCharacter> {
        return getAllActors().filterIsInstance<ZCharacter>()
    }

    private fun addActorToCell(actor: ZActor<*>, pos: Grid.Pos): Boolean {
        val cell = getCell(pos)
        var current: ZCellQuadrant? = actor.occupiedQuadrant
        if (current == null) {
            current = actor.getSpawnQuadrant()
        }
        if (current == null || cell.getOccupant(current) != null) {
            current = cell.findLowestPriorityOccupant()
        }
        cell.getOccupant(current)?.let {
            if (it.priority >= actor.priority)
                return false
        }
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
        getCell(actor.occupiedCell).setQuadrant(actor, actor.occupiedQuadrant)
    }

    fun getUndiscoveredIndoorZones(startPos: Grid.Pos, undiscovered: MutableSet<Int>) {
        val cell = getCell(startPos)
        if (cell.discovered) return
        cell.discovered = true
        val zone = zones[cell.zoneIndex]
        if (!zone.isSearchable) return
        undiscovered.add(cell.zoneIndex)
        for (dir in ZDir.values()) {
            if (cell.getWallFlag(dir).opened) getUndiscoveredIndoorZones(getAdjacent(startPos, dir), undiscovered)
        }
    }

    fun getCellsOfType(type: ZCellType?): List<ZCell> {
        val start: MutableList<ZCell> = ArrayList()
        for (cell in getCells()) {
            if (cell.isCellType(type!!)) {
                start.add(cell)
            }
        }
        return start
    }

    fun resetNoise() {
        for (zone in zones) {
            zone.noiseLevel = 0
            for (pos in zone.cells) {
                for (a in getCell(pos).occupant) {
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
        return getCell(actor.occupiedCell).getWallFlag(dir).opened
    }

    /**
     *
     * @return
     */
    fun getDimension(): GDimension {
        if (grid.isEmpty()) return GDimension(0f, 0f)
        val br: Vector2D = grid[grid.rows - 1, grid.cols - 1].bottomRight
        return GDimension(br.x, br.y)
    }

    /*
    @Omit
    private GRectangle zoomedRect = null;

    public GRectangle getZoomedRectangle() {
        return zoomedRect;
    }

    public GRectangle getZoomedRectangle(IVector2D center) {
        GDimension dim = getDimension();
        GDimension zoomed = new GDimension(dim.width-zoom, dim.height-zoom);

        GRectangle rect = new GRectangle(zoomed).withCenter(center);
        rect.x = Utils.clamp(rect.x, 0, dim.width-rect.w);
        rect.y = Utils.clamp(rect.y, 0, dim.height-rect.h);
        return zoomedRect = rect;
    }

    public void zoom(int amount) {
        int z = zoom + amount;
        if (z >= 0 && z < getMaxZoom()) {
            zoom = z;
        }
    }

    public int getZoom() {
        return zoom;
    }

    int getMaxZoom() {
        return Math.min(getRows(), getColumns());
    }*/
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
        return Utils.filter(zones) { `object`: ZZone -> `object`.type === type }
    }

    fun getSpawnZones(): List<ZZone> {
        return Utils.filter(zones) { `object`: ZZone -> isZoneSpawnable(`object`.zoneIndex) }
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

    fun <T : ZActor<*>> getActor(position: ZActorPosition): T {
        return grid[position.pos].getOccupant(position.quadrant) as T
    }
}