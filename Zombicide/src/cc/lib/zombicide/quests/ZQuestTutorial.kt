package cc.lib.zombicide.quests

import cc.lib.game.GColor
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCellType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZEquipmentType
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZItemType
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ZWallFlag
import cc.lib.zombicide.ZWeaponType

class ZQuestTutorial : ZQuest(ZQuests.Tutorial) {
	companion object {
		init {
			addAllFields(ZQuestTutorial::class.java)
		}
	}

	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i", "z0:i", "z1:dw:fatty", "z2:i:ws:we", "z3:green:greende:ww", "z4:i:ws:exit"),
arrayOf("z5:spw:wn:ws", "z6:bluedn:we:walker", "z7:ds:we", "z8:red:wn:ws", "z9", "z10:red:wn:ws"),
arrayOf("z11:blue:i:wn:ws:ode", "z12:start:ws:odw:we", "z13:i:ws:dn:runner", "z13:i:wn:we:ws:gvd1", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:gvd2"),
arrayOf("", "", "", "z16:v:wn:ww:gvd1", "z16:v:wn", "z16:v:wn:gvd2")))

	lateinit var blueDoor: ZDoor
	lateinit var greenDoor: ZDoor
	var greenSpawnZone = -1
	var blueKeyZone = -1
	var greenKeyZone = -1

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		val cell = grid[pos]
		val zoneIndex = cell.zoneIndex
		when (cmd) {
			"green" -> greenSpawnZone = zoneIndex
			"blue" -> {
				blueKeyZone = zoneIndex
				cell.setCellType(ZCellType.OBJECTIVE_BLUE, true)
				addObjective(ZCellType.OBJECTIVE_BLUE, zoneIndex)
			}
			"bluedn" -> blueDoor = ZDoor(pos, ZDir.NORTH, GColor.BLUE)
			"greende" -> greenDoor = ZDoor(pos, ZDir.EAST, GColor.GREEN)
			else      -> super.loadCmd(grid, pos, cmd)
		}
	}

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueKeyZone) {
			game.unlockDoor(blueDoor)
			game.addLogMessage(c.name() + " has unlocked the BLUE door")
			blueKeyZone = -1
		} else if (c.occupiedZone == greenKeyZone) {
			game.unlockDoor(greenDoor)
			game.board.setSpawnZone(greenSpawnZone, ZIcon.SPAWN_GREEN, false, true, true)
			game.spawnZombies(greenSpawnZone)
			game.addLogMessage(c.name() + " has unlocked the GREEN door")
			game.addLogMessage(c.name() + " has created a new spawn zone!")
			greenKeyZone = -1
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("9R", 90, getQuadrant(0, 0)),
			ZTile("4V", 90, getQuadrant(0, 3))
		)

	override fun init(game: ZGame) {
		greenKeyZone = redObjectives.random()
		game.board.setDoorLocked(blueDoor)
		game.board.setDoorLocked(greenDoor)
		redObjectives.add(blueKeyZone) // call this after putting the greenKeyRandomly amongst the red objectives
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZItemType.DRAGON_BILE, ZItemType.TORCH, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.INFERNO)

	public override fun getInitVaultItems(vaultZone: Int): List<ZEquipment<*>> {
		return allVaultOptions.map { type -> type.create().also { it.vaultItem = true }}
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Unlock the BLUE Door.", game.board.getDoor(blueDoor) !== ZWallFlag.LOCKED)
				.addRow("2.", "Unlock the GREEN Door. GREEN key hidden among RED objectives.", game.board.getDoor(greenDoor) !== ZWallFlag.LOCKED)
				.addRow("3.", String.format("Collect all Objectives for %d EXP Each", getObjectiveExperience(0, 0)), String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("4.", "Get all players into the EXIT zone.", isAllPlayersInExit(game))
				.addRow("5.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
				.addRow("6.", "All Players must survive.")
			)
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjectives + game.allCharacters.size
		var numCompleted = numFoundObjectives
		for (c in game.board.getAllCharacters()) {
			if (c.occupiedZone == exitZone) numCompleted++
		}
		var percentCompleted = numCompleted * 100 / numTasks
		if (game.board.getZombiesInZone(exitZone).isNotEmpty()) percentCompleted--
		return percentCompleted
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (numDeadPlayers(game) > 0) {
			"Not all players survived."
		} else super.getQuestFailedReason(game)
	}
}