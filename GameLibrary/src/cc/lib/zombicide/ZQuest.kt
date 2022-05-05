package cc.lib.zombicide

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.utils.*
import cc.lib.zombicide.ui.UIZombicide
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
        for (et in allVaultOptions) {
            vaultItemsRemaining.add(et.create())
        }
        vaultItemsRemaining.shuffle()
    }

    private val objectives: MutableMap<ZCellType, ZObjective> = HashMap()
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
        get() = Utils.sumInt(objectives.values) { o: ZObjective -> o.objectives.size }

    /**
     *
     * @return
     */
    val numFoundObjectives: Int
        get() = Utils.sumInt(objectives.values) { o: ZObjective -> o.found.size }


    /**
     *
     * @return
     */
    val redObjectives: List<Int>
        get() = getObjectives(ZCellType.OBJECTIVE_RED)

    fun getObjectives(color: ZCellType): List<Int> {
        val obj = objectives[color] ?: return emptyList()
        return obj.objectives
    }

    val allObjectives: List<Int>
        get() = Utils.mergeLists(objectives.values) { o: ZObjective -> o.objectives }

    private fun addObjective(color: ZCellType, zoneIndex: Int) {
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

    fun getDirectionForEnvironment(env: Int): ZDir {
        when (env) {
            ZCell.ENV_VAULT -> return ZDir.ASCEND
        }
        return ZDir.DESCEND
    }

    protected fun setVaultDoor(cell: ZCell, grid: Grid<ZCell>, pos: Grid.Pos, type: ZCellType, vaultFlag: Int) {
        setVaultDoor(cell, grid, pos, type, vaultFlag, ZWallFlag.CLOSED)
    }

    protected fun setVaultDoor(cell: ZCell, grid: Grid<ZCell>, pos: Grid.Pos, type: ZCellType, vaultFlag: Int, wallFlag: ZWallFlag) {
        cell.setCellType(type, true)
        cell.vaultId = vaultFlag
        setCellWall(grid, pos, getDirectionForEnvironment(cell.environment), wallFlag)
    }

    protected fun setSpawnArea(cell: ZCell, area: ZSpawnArea?) {
        Utils.assertTrue(cell.numSpawns == 0)
        cell.spawns[cell.numSpawns++] = area
    }

    protected open fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
        val cell = grid[pos]
        when (cmd) {
            "i" -> cell.environment = ZCell.ENV_BUILDING
            "v" -> cell.environment = ZCell.ENV_VAULT
            "t1", "t2", "t3" -> {
                when (cmd.substring(1).toInt()) {
                    1    -> cell.scale = 1.05f
                    2    -> cell.scale = 1.1f
                    3    -> cell.scale = 1.15f
                    else -> throw GException("Unhandled case")
                }
                cell.environment = ZCell.ENV_TOWER
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
            "spn" -> setSpawnArea(cell, ZSpawnArea(pos, ZDir.NORTH))
            "sps" -> setSpawnArea(cell, ZSpawnArea(pos, ZDir.SOUTH))
            "spe" -> setSpawnArea(cell, ZSpawnArea(pos, ZDir.EAST))
            "spw" -> setSpawnArea(cell, ZSpawnArea(pos, ZDir.WEST))
            "st", "start" -> cell.setCellType(ZCellType.START, true)
            "exit" -> cell.setCellType(ZCellType.EXIT, true)
            "walker" -> cell.setCellType(ZCellType.WALKER, true)
            "runner" -> cell.setCellType(ZCellType.RUNNER, true)
            "fatty" -> cell.setCellType(ZCellType.FATTY, true)
            "necro" -> cell.setCellType(ZCellType.NECROMANCER, true)
            "abom", "abomination" -> cell.setCellType(ZCellType.ABOMINATION, true)
            "red" -> {
                addObjective(ZCellType.OBJECTIVE_RED, cell.zoneIndex)
                cell.setCellType(ZCellType.OBJECTIVE_RED, true)
            }
            "rn" -> setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.RAMPART)
            "rs" -> setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.RAMPART)
            "re" -> setCellWall(grid, pos, ZDir.EAST, ZWallFlag.RAMPART)
            "rw" -> setCellWall(grid, pos, ZDir.WEST, ZWallFlag.RAMPART)
            else                  -> throw RuntimeException("Invalid command '$cmd'")
        }
    }

    protected fun setCellWall(grid: Grid<ZCell>, pos: Grid.Pos, dir: ZDir, flag: ZWallFlag) {
        grid[pos].setWallFlag(dir, flag)
        val adj = dir.getAdjacent(pos)
        if (adj != null && grid.isOnGrid(adj)) grid[adj].setWallFlag(dir.opposite, flag)
    }

    fun load(map: Array<Array<String>>): ZBoard {
        val rows = map.size
        val cols: Int = map[0].size
        // make sure all cols the same length
        for (i in 1 until rows) {
            require(map[i].size == cols) { "Row $i is not same length as rest: $cols" }
        }
        val grid = Grid<ZCell>(rows, cols)
        val zoneMap: MutableMap<Int, ZZone> = HashMap()
        var maxZone = 0
        for (row in map.indices) {
            for (col in 0 until map[row].size) {
                grid[row, col] = ZCell(col.toFloat(), row.toFloat())
            }
        }
        for (row in map.indices) {
            require(map[0].size == map[row].size) { "Length of row $row differs" }
            for (col in 0 until map[row].size) {
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
                        zone.cells.add(Grid.Pos(row, col))
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
                    Utils.assertTrue(exitZone < 0, "Multiple EXIT zones not supported")
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
    open fun addMoves(game: ZGame, cur: ZCharacter, options: MutableList<ZMove>) {
        for (obj in allObjectives) {
            if (cur.occupiedZone == obj && game.board.getNumZombiesInZone(obj) == 0) options.add(ZMove.newObjectiveMove(obj))
        }
    }

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
        val obj = findObjectiveForZone(c.occupiedZone)
        if (obj!!.objectives.remove(c.occupiedZone as Any)) {
            obj.found.add(c.occupiedZone)
            game.addExperience(c, getObjectiveExperience(c.occupiedZone, numFoundObjectives))
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
        return if (game.allLivingCharacters.size == 0) {
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
        var list = vaultMap[vaultZone]
        if (list == null) {
            list = ArrayList()
            if (vaultItemsRemaining.size > 0) {
                val equip = vaultItemsRemaining.removeRandom()
                equip.vaultItem = true
                list.add(equip)
            }
            vaultMap[vaultZone] = list
        }
        return list
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

    open fun getMaxNumZombiesOfType(type: ZZombieType?): Int {
        when (type) {
            ZZombieType.GreenTwin, ZZombieType.BlueTwin -> return 0
            ZZombieType.Abomination, ZZombieType.Wolfbomination -> return 1
            ZZombieType.Necromancer -> return 2
            ZZombieType.Wolfz -> return 22
            ZZombieType.Walker -> return 35
            ZZombieType.Fatty, ZZombieType.Runner -> return 14
        }
        return 20
    }

    open fun onEquipmentFound(game: ZGame, equip: ZEquipment<*>) {
        //
    }

    protected fun isAllPlayersInExit(game: ZGame): Boolean {
        Utils.assertTrue(exitZone >= 0)
        return game.board.getNumZombiesInZone(exitZone) == 0 && Utils.count(game.board.getAllCharacters()) { `object`: ZCharacter -> `object`.occupiedZone != exitZone } <= 0
    }

    /**
     * Perform any processing to the searchable. Called once on quest init
     * @param items
     */
    open fun processLootDeck(items: List<ZEquipment<*>>) {}
    protected val numStartObjectives: Int
        get() = Utils.sumInt(objectives.values) { o: ZObjective -> o.found.size + o.objectives.size }

    open fun onDragonBileExploded(c: ZCharacter, zoneIdx: Int) {}
    open fun drawQuest(game: UIZombicide, g: AGraphics) {}
    fun onNecromancerEscaped(game: ZGame, z: ZZombie) {
        game.gameLost("Necromancer Escaped")
    }

    open fun onZombieSpawned(game: ZGame, zombie: ZZombie, zone: Int) {
        when (zombie.type) {
            ZZombieType.Necromancer -> {
                game.board.setSpawnZone(zone, ZIcon.SPAWN_GREEN, false, false, true)
                game.spawnZombies(zone)
            }
        }
    }

    /**
     * Return a spawn card or null if none left. Default behavior is infinite spawn cards
     *
     * @param game
     * @param targetZone
     * @param dangerLevel
     * @return
     */
    open fun drawSpawnCard(game: ZGame, targetZone: Int, dangerLevel: ZSkillLevel?): ZSpawnCard? {
        return ZSpawnCard.drawSpawnCard(quest.isWolfBurg, game.canZoneSpawnNecromancers(targetZone), game.getDifficulty())
    }

    fun isExitClearedOfZombies(game: ZGame): Boolean {
        return game.board.getNumZombiesInZone(exitZone) == 0
    }
}