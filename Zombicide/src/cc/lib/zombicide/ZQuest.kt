package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.utils.removeRandom
import java.util.*

abstract class ZQuest protected constructor(val quest: ZQuests) : Reflector<ZQuest>() {
    companion object {
        init {
            addAllFields(ZQuest::class.java)
        }
    }

    /**
     *
     * @return
     */
    var exitZone = -1
        private set
    private val vaultMap: MutableMap<Int, MutableList<ZEquipment<*>>> = HashMap()
    var vaultItemsRemaining: MutableList<ZEquipment<*>> = mutableListOf()

    /**
     *
     * @return
     */
    var numFoundVaultItems = 0
        private set

	init {
		vaultItemsRemaining.addAll(allVaultOptions.map { it.create() })
		vaultItemsRemaining.shuffle()
    }

    private val objectives: MutableMap<ZCellType, ZObjective> = HashMap()

	fun numPlayersInExit(game: ZGame): Int =
		game.board.getAllCharacters().count { it.occupiedZone == exitZone }

	fun numDeadPlayers(game: ZGame): Int = game.board.getAllCharacters().count { it.isDead }

    abstract fun loadBoard(): ZBoard

    /**
     *
     * @return
     */
    abstract val tiles: Array<ZTile>

    /**
     * Called once during INIT stage of game
     */
    abstract fun init(game: ZGame)

    /**
     * Return value between 0-100 for progress
     * 100 is assumed to be a game over win
     *
     * @param game
     * @return
     */
    abstract fun getPercentComplete(game: ZGame): Int

    /**
     * Return a table to be displayed when used want to view the objectives
     *
     * @param game
     * @return
     */
    abstract fun getObjectivesOverlay(game: ZGame): Table

    /**
     *
     * @return
     */
    val name: String
        get() = quest.displayName

    /**
     *
     * @return
     */
    val numRemainingObjectives: Int
        get() = objectives.values.sumBy { it.objectives.size }

    /**
     *
     * @return
     */
    val numFoundObjectives: Int
        get() = objectives.values.sumBy { it.found.size }


    /**
     *
     * @return
     */
    val redObjectives: MutableList<Int>
        get() = getObjectives(ZCellType.OBJECTIVE_RED).toMutableList()

    fun getObjectives(color: ZCellType): List<Int> {
        val obj = objectives[color] ?: return emptyList()
        return obj.objectives
    }

    val allObjectives: List<Int>
        get() = objectives.values.map { it.objectives }.flatten()

    fun addObjective(color: ZCellType, zoneIndex: Int) {
        var obj = objectives[color]
        if (obj == null) {
            obj = ZObjective()
            objectives[color] = obj
        }
        obj.objectives.add(zoneIndex)
    }

    /**
     *
     * @return
     */
    open val allVaultOptions: List<ZEquipmentType>
        get() = if (quest.isWolfBurg) {
            listOf(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.EARTHQUAKE_HAMMER, ZWeaponType.DRAGON_FIRE_BLADE)
        } else listOf(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW)

    protected fun setVaultDoor(cell: ZCell, grid: Grid<ZCell>, pos: Grid.Pos, type: ZCellType, vaultFlag: Int) {
        setVaultDoor(cell, grid, pos, type, vaultFlag, ZWallFlag.CLOSED)
    }

    protected fun setVaultDoor(cell: ZCell, grid: Grid<ZCell>, pos: Grid.Pos, type: ZCellType, vaultFlag: Int, wallFlag: ZWallFlag) {
        cell.setCellType(type, true)
        cell.vaultId = vaultFlag
	    setCellWall(grid, pos, cell.environment.getVaultDirection(), wallFlag)
    }

    protected fun setSpawnArea(cell: ZCell, area: ZSpawnArea?) {
        require(cell.numSpawns == 0)
        cell.spawns[cell.numSpawns++] = area
    }

    protected open fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
        val cell = grid[pos]
	    when (cmd) {
		    "i" -> cell.environment = ZCellEnvironment.BUILDING
		    "v" -> cell.environment = ZCellEnvironment.VAULT
		    "r" -> cell.setCellType(ZCellType.RUBBLE, true)
		    "w" -> {
			    cell.scale = .85f
			    cell.environment = ZCellEnvironment.WATER
		    }

		    "h", "hoard" -> cell.environment = ZCellEnvironment.HOARD
		    "t1", "t2", "t3" -> {
			    with(cmd.substring(1).toInt()) {
				    cell.scale = 1f + 0.5f * this
			    }
			    cell.environment = ZCellEnvironment.TOWER
		    }

		    "vd1" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_VIOLET, 1)
		    "vd2" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_VIOLET, 2)
		    "vd3" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_VIOLET, 3)
		    "vd4" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_VIOLET, 4)
		    "gvd1" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_GOLD, 1)
		    "gvd2" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_GOLD, 2)
		    "gvd3" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_GOLD, 3)
            "gvd4" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_GOLD, 4)
            "wn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.WALL)
            "ws" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.WALL)
		    "we" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.WALL)
		    "ww" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.WALL)
		    "dn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.CLOSED)
		    "ds" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.CLOSED)
		    "de" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.CLOSED)
		    "dw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.CLOSED)
		    "odn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.OPEN)
		    "ods" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.OPEN)
		    "ode" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.OPEN)
		    "odw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.OPEN)
		    "redspn", "spn" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.NORTH))
		    "redsps", "sps" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.SOUTH))
		    "redspe", "spe" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.EAST))
		    "redspw", "spw" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.WEST))
		    /*
			"bluspn" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.NORTH))
			"blusps" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.SOUTH))
			"bluspe" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.EAST))
			"bluspw" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.WEST))
			"grnspn" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.NORTH))
			"grnsps" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.SOUTH))
			"grnspe" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.EAST))
			"grnspw" -> setSpawnArea(cell, createSpawnAreas(pos, ZDir.WEST))*/
		    "st", "start" -> cell.setCellType(ZCellType.START, true)
		    "exit" -> cell.setCellType(ZCellType.EXIT, true)
		    "walker" -> cell.setCellType(ZCellType.WALKER, true)
		    "runner" -> cell.setCellType(ZCellType.RUNNER, true)
		    "fatty" -> cell.setCellType(ZCellType.FATTY, true)
		    "necro" -> cell.setCellType(ZCellType.NECROMANCER, true)
		    "abom", "abomination" -> cell.setCellType(ZCellType.ABOMINATION, true)
		    "ratz" -> cell.setCellType(ZCellType.RATZ, true)
		    "red" -> {
			    addObjective(ZCellType.OBJECTIVE_RED, cell.zoneIndex)
			    cell.setCellType(ZCellType.OBJECTIVE_RED, true)
		    }

		    "blue" -> {
			    addObjective(ZCellType.OBJECTIVE_BLUE, cell.zoneIndex)
			    cell.setCellType(ZCellType.OBJECTIVE_BLUE, true)
		    }

		    "green" -> {
			    addObjective(ZCellType.OBJECTIVE_GREEN, cell.zoneIndex)
			    cell.setCellType(ZCellType.OBJECTIVE_GREEN, true)
		    }

		    "rn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.RAMPART)
		    "rs" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.RAMPART)
		    "re" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.RAMPART)
		    "rw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.RAMPART)
		    "ln" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.LEDGE, false)
		    "ls" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.LEDGE, false)
		    "le" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.LEDGE, false)
		    "lw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.LEDGE, false)
		    "hn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.HEDGE)
		    "hs" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.HEDGE)
		    "he" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.HEDGE)
		    "hw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.HEDGE)
		    "catapult" -> cell.setCellType(ZCellType.CATAPULT, true)
		    else -> error("Invalid command '$cmd'")
	    }
    }

	open fun createSpawnAreas(pos: Grid.Pos, dir : ZDir) : ZSpawnArea {
		return ZSpawnArea(cellPos = pos, dir = dir, isEscapableForNecromancers = true)
	}

	protected fun setCellWall(grid: Grid<ZCell>, pos: Grid.Pos, dir: ZDir, flag: ZWallFlag, assignOpposite: Boolean = true) {
		grid[pos].setWallFlag(dir, flag)
		if (assignOpposite) dir.getAdjacent(pos)?.takeIf { grid.isOnGrid(it) }?.let {
			grid[it].setWallFlag(dir.opposite, flag.opposite)
		}
	}

    protected fun load(map: Array<Array<String>>): ZBoard {
	    val rows = map.size
	    val cols: Int = map[0].size
	    // make sure all cols the same length
	    for (i in 1 until rows) {
		    require(map[i].size == cols) { "Row $i is not same length as rest. Is ${map[i].size} expected $cols" }
	    }
	    val grid = Grid<ZCell>(rows, cols)
	    val zoneMap: MutableMap<Int, ZZone> = HashMap()
	    var maxZone = 0
	    for (row in map.indices) {
            for (col in map[row].indices) {
                grid[row, col] = ZCell(col.toFloat(), row.toFloat())
            }
        }
        for (row in map.indices) {
            require(map[0].size == map[row].size) { "Length of row $row differs" }
            for (col in map[row].indices) {
                val cell = grid[row, col]
                val parts = map[row][col].split(":").toTypedArray()
                var zone: ZZone? = null
                val pos = Grid.Pos(row, col)
                for (cmd in parts) {
                    if (cmd.isEmpty()) continue
                    if (cmd.startsWith("z")) {
                        val index = cmd.substring(1).toInt()
                        maxZone = Math.max(maxZone, index)
                        zone = zoneMap[index]
                        if (zone == null) {
                            zone = ZZone(index)
                            zoneMap[index] = zone
                        }
                        zone.addCell(Grid.Pos(row, col))
                        cell.zoneIndex = index
                        cell.setCellType(ZCellType.NONE, true)
                        continue
                    }
                    if (zone == null) {
                        throw GException("Problem with cmd: " + map[row][col])
                    }
                    loadCmd(grid, pos, cmd)
                    // make sure outer perimeter has walls
                }
                if (cell.isCellType(ZCellType.EXIT)) {
                    require(exitZone < 0) { "Multiple EXIT zones not supported" }
                    exitZone = cell.zoneIndex
                }
                if (row == 0) {
                    loadCmd(grid, pos, "wn")
                } else if (row == map.size - 1) {
                    loadCmd(grid, pos, "ws")
                }
                if (col == 0) {
                    loadCmd(grid, pos, "ww")
                } else if (col == map[0].size - 1) {
                    loadCmd(grid, pos, "we")
                }
            }
        }

        // do another pass ans make sure all the zone cells are adjacent
        //MergableVector<ZZone> zones = new MergableVector<>();
        val zones = Vector<ZZone>()
        zones.setSize(maxZone + 1)
        for ((key, value) in zoneMap) {
            zones[key] = value
        }
        // fill in null zones with empty ones
        for (i in zones.indices) {
            if (zones[i] == null) zones[i] = ZZone(i) else {
                zones[i]!!.checkSanity()
            }
        }
        return ZBoard(grid, zones)
    }

    /**
     *
     * @param game
     * @param cur
     * @param options
     */
    open fun addMoves(game: ZGame, cur: ZCharacter, options: MutableCollection<ZMove>) {
        allObjectives.filter {
	        game.board.getNumZombiesInZone(it) == 0
        }.firstOrNull {
	        it == cur.occupiedZone
        }?.takeIf {
        	canCharacterTakeObjective(game, cur, it)
        }?.let {
            options.add(ZMove.newObjectiveMove(it))
        }
    }

	open fun canCharacterTakeObjective(game: ZGame, cur: ZCharacter, zone: Int) : Boolean = true

    fun findObjectiveForZone(zoneIdx: Int): ZObjective? {
        for (obj in objectives.values) {
            if (obj.objectives.contains(zoneIdx)) return obj
        }
        return null
    }

    /**
     *
     * @param game
     * @param c
     */
    open fun processObjective(game: ZGame, c: ZCharacter) {
        findObjectiveForZone(c.occupiedZone)?.let { obj ->
	        if (obj.objectives.remove(c.occupiedZone as Any)) {
		        obj.found.add(c.occupiedZone)
		        game.addExperience(c, getObjectiveExperience(c.occupiedZone, numFoundObjectives))
	        }
        }
    }

    protected open fun getObjectiveExperience(zoneIdx: Int, nthFound: Int): Int {
        return 5
    }

    /**
     *
     * @return null if game not failed, otherwise a failed reason
     */
    open fun getQuestFailedReason(game: ZGame): String? {
        return if (game.allLivingCharacters.isEmpty()) {
            "All Players Killed"
        } else null
    }

    /**
     *
     * @param zone
     * @param equip
     */
    fun pickupItem(zone: Int, equip: ZEquipment<*>) {
        if (equip.vaultItem) {
            numFoundVaultItems++
            equip.vaultItem = false
        }
        getVaultItems(zone).remove(equip)
    }

    /**
     *
     * @param zone
     * @param equip
     */
    fun dropItem(zone: Int, equip: ZEquipment<*>) {
        getVaultItems(zone).add(equip)
    }

    /**
     *
     * @return
     */
    fun getVaultItems(vaultZone: Int): MutableList<ZEquipment<*>> {
	    return vaultMap.getOrPut(vaultZone) {
		    ArrayList<ZEquipment<*>>().also {
			    if (vaultItemsRemaining.size > 0) {
				    it.add(vaultItemsRemaining.removeRandom().also { item ->
					    item.vaultItem = true
				    })
			    }
		    }
	    }
    }

    /**
     * By default inits a vault with 1 item from the remaining items list
     *
     * @param vaultZone
     * @return
     */
    protected open fun getInitVaultItems(vaultZone: Int): List<ZEquipment<*>> {
	    val list: MutableList<ZEquipment<*>> = ArrayList()
	    if (vaultItemsRemaining.size > 0) {
		    val equip = vaultItemsRemaining.removeRandom()
		    equip.vaultItem = true
		    list.add(equip)
	    }
	    return list
    }

	open fun onEquipmentFound(game: ZGame, equip: ZEquipment<*>) {
		//
	}

	protected fun isAllPlayersInExit(game: ZGame): Boolean {
		require(exitZone >= 0)
		return game.board.getNumZombiesInZone(exitZone) == 0 && game.board.getAllCharacters().count { it.occupiedZone != exitZone } <= 0
	}

    /**
     * Perform any processing to the searchable. Called once on quest init
     * @param items
     */
    open fun processLootDeck(items: MutableList<ZEquipment<*>>) {}
    protected val numStartObjectives: Int
        get() = objectives.values.sumBy { it.found.size + it.objectives.size }

	open fun onDragonBileExploded(c: ZSurvivor, zoneIdx: Int) {}
	open fun drawQuest(board: ZBoard, g: AGraphics) {}
	fun onNecromancerEscaped(game: ZGame, z: ZZombie) {
		game.gameLost("Necromancer Escaped")
	}

    open fun onZombieSpawned(game: ZGame, zombie: ZZombie, zone: Int) {
	    when (zombie.type) {
		    ZZombieType.Necromancer -> {
			    game.board.setSpawnZone(zone, ZIcon.SPAWN_NECRO, false, false, true)
			    game.spawnZombies(zone)
		    }
		    else -> Unit
	    }
    }

	/**
	 * Return a spawn card or null if none left. Default behavior is infinite spawn cards
	 *
	 * @param game
	 * @param targetZone
	 * @param dangerLevel
	 * @return
	 *
	open fun drawSpawnCard(game: ZGame, targetZone: Int, dangerLevel: ZSkillLevel?): ZSpawnCard? {
	return ZSpawnCard.drawSpawnCard(quest.isWolfBurg, game.board.canZoneSpawnNecromancers(targetZone), game.getDifficulty())
	}*/

	fun isExitClearedOfZombies(game: ZGame): Boolean {
		return game.board.getNumZombiesInZone(exitZone) == 0
	}

	open fun buildDeck(difficulty: ZDifficulty, rules: ZRules): List<ZSpawnCard> =
		ZSpawnCard.buildDeck(quest.getDeckType(), difficulty, rules)

	open fun drawBlackObjective(board: ZBoard, g: AGraphics, cell: ZCell, zone: ZZone) {
		throw Exception("Unhandled method drawBlackObjective")
	}

	open fun onDoorOpened(game: ZGame, door: ZDoor, c: ZCharacter) {}

	open fun onSpawnZoneRemoved(game: ZGame, spawnArea: ZSpawnArea) {}

	open fun handleSpawnForZone(game: ZGame, zoneIdx: Int): Boolean = false
}