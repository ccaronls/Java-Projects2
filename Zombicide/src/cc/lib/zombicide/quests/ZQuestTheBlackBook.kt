package cc.lib.zombicide.quests

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class ZQuestTheBlackBook : ZQuest(ZQuests.The_Black_Book) {
	companion object {
		init {
			addAllFields(ZQuestTheBlackBook::class.java)
		}
	}

	// these are random amongst the red
	var blackBookZone = -1 // TODO: Draw as a black book
	var blueObjZone = -1
	var greenObjZone = -1
	var greenSpawnZone = -1
	lateinit var blueDoor: ZDoor
	lateinit var greenDoor: ZDoor
	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:we", "z1:green:de", "z2:i:ws:ode",            "z3:i:red:we:ws", "z4:spn", "z5:i:ww:we:vd1",           "z6:v:vd1"),
arrayOf("z0:i:we", "z7", "z8",                              "z9", "z10", "z5:i:ww:we:ods",                          "z6:v"),
arrayOf("z0:i:we:ods", "z11:st", "z12:i:wn:ww:blackbook:ods:ode", "z13:i:wn:we:ods:vd2", "z14", "z15:i:ww:we:ods",  "z6:v:vd2"),
arrayOf("z16:i:grds:we:gvd3", "z17", "z18:i:ww:ws:gvd4:ode", "z19:i:ws:we", "z20", "z21:i:blds:we:ww",              "z22:wn:v:gvd3"),
arrayOf("z23:spw", "z24", "z25",                            "z26", "z27", "z28:spe:we",                             "z22:v"),
arrayOf("z29:i:red:wn:de", "z30", "z31:i:dw:wn:ode",        "z32:i:red:wn:we", "z33", "z34:i:dw:red:wn:we",         "z22:v:gvd4")))

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		val cell = grid[pos]
		when (cmd) {
			"blackbook" -> {
				blackBookZone = cell.zoneIndex
				cell.setCellType(ZCellType.OBJECTIVE_BLACK, true)
				addObjective(ZCellType.OBJECTIVE_BLACK, cell.zoneIndex)
			}
			"green" -> greenSpawnZone = cell.zoneIndex
			"blds" -> blueDoor = ZDoor(pos, ZDir.SOUTH, GColor.BLUE)
			"grds" -> greenDoor = ZDoor(pos, ZDir.SOUTH, GColor.GREEN)
			else        -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		require(redObjectives.size > 1)
		while (blueObjZone == greenObjZone) {
			blueObjZone = redObjectives.random()
			greenObjZone = redObjectives.random()
		}
		// do this after the above so it does not get mixed in with other objectives. Effect would be player could never access
		redObjectives.add(blackBookZone)
		game.board.setDoorLocked(blueDoor)
		game.board.setDoorLocked(greenDoor)
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueObjZone) {
			game.unlockDoor(blueDoor)
		}
		if (c.occupiedZone == blackBookZone) {
			game.addLogMessage(c.name() + " has found the Black Book")
			blackBookZone = -1
		}
		if (c.occupiedZone == greenObjZone) {
			game.addLogMessage(c.name() + " has unlocked the Green Door. A New Spawn zone has appeared!")
			game.unlockDoor(greenDoor)
			game.board.setSpawnZone(greenSpawnZone, ZIcon.SPAWN_GREEN, false, false, true)
			game.spawnZombies(greenSpawnZone)
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = allVaultOptions.size + 1 + ZColor.RED.ordinal
		var numCompleted = if (blackBookZone < 0) 1 else 0
		numCompleted += numFoundVaultItems
		numCompleted += game.highestSkillLevel.difficultyColor.ordinal
		return numCompleted * 100 / numTasks
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("8R", 90, getQuadrant(0, 0)),
			ZTile("5R", 90, getQuadrant(0, 3)),
			ZTile("4V", 90, getQuadrant(3, 0)),
			ZTile("7V", 90, getQuadrant(3, 3)))

	override fun getObjectivesOverlay(game: ZGame): Table {
		val blackBookTaken = blackBookZone < 0
		val allVaultItems = allVaultOptions.size
		val numVaultItemsTaken = numFoundVaultItems
		val lvl = game.highestSkillLevel
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Unlock the GREEN Door. GREEN Key hidden among the RED objectives.", "", game.board.getDoor(greenDoor) !== ZWallFlag.LOCKED)
				.addRow("2.", "Unlock the BLUE Door. BLUE Key hidden among the RED objectives.", "", game.board.getDoor(blueDoor) !== ZWallFlag.LOCKED)
				.addRow("3.", "Steal the Black Book in central building.", "", blackBookTaken)
				.addRow("4.", "Claim all Vault artifacts.", String.format("%d of %d", numVaultItemsTaken, allVaultItems), numVaultItemsTaken == allVaultItems)
				.addRow("5.", "Get to RED Danger level with at least one survivor.", lvl, lvl.difficultyColor === ZColor.RED)
			)
	}

	override fun drawBlackObjective(game: ZGame, g: AGraphics, cell: ZCell, zone: ZZone) {
		g.drawImage(ZIcon.BLACKBOOK.imageIds[0], GRectangle(cell).scaledBy(.5f))
	}
}