package cc.lib.zombicide.quests

import cc.lib.game.GColor
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ZZombieType

open class ZQuestTheCommandry(quests: ZQuests = ZQuests.The_Commandry) : ZQuest(quests) {
	companion object {
		init {
			addAllFields(ZQuestTheCommandry::class.java)
		}
	}

	lateinit var blueDoor: ZDoor
	lateinit var greenDoor: ZDoor
	private var blueDoorKeyZone = -1
	private var greenDoorKeyZone = -1

	override fun loadBoard(): ZBoard {
		val map = arrayOf(
			arrayOf(
				"z0:i:red:ods:de",
				"z1:spn",
				"z2:i:ww:de",
				"z3",
				"z4",
				"z5",
				"z6:i:dw:we:ods",
				"z7:spn",
				"z8:i:gvd1:ds:ww:red"
			),
			arrayOf(
				"z9:i:vd2:we",
				"z10",
				"z2:i:ww:we:ods",
				"z11",
				"z12:i:exit:bluedn:ww:ws:we",
				"z13",
				"z14:i:ww:we",
				"z15",
				"z16"
			),
			arrayOf(
				"z9:i:red:we:ods",
				"z17",
				"z18:i:ww:de:ods",
				"z19",
				"z20",
				"z21",
				"z14:i:dw:ods:de",
				"z22",
				"z23:i:wn:ww:red"
			),
			arrayOf(
				"z24:i:we::ods",
				"z25",
				"z26:i:ww:ws:ode",
				"z27:i:dn:ws",
				"z28:i:wn:greends:odw",
				"z28:i:dn:ws:ode",
				"z29:i:ws:we:odn",
				"z30",
				"z31:i:ww"
			),
			arrayOf(
				"z32:i:gvd3:we",
				"z33",
				"z34",
				"z35",
				"z36",
				"z37",
				"z38",
				"z39",
				"z31:i:ww:ods"
			),
			arrayOf(
				"z32:i:red:ws",
				"z32:i:wn:ws:ode",
				"z40:i:wn:ws:ode",
				"z41:i:wn:ws:de",
				"z42:sps:start:ws",
				"z43:i:dw:wn:ws:ode",
				"z44:i:wn:ws:ode",
				"z45:i:wn:ws:red",
				"z45:i:odn:ws:vd4"
			),
			arrayOf(
				"z46:v:gvd1:ww",
				"z46:v",
				"z46:v",
				"z46:v:gvd3:we",
				"z48:r",
				"z47:v:vd2",
				"z47:v",
				"z47:v",
				"z47:v:vd4"
			)
		)
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"greends" -> greenDoor = ZDoor(pos, ZDir.SOUTH, GColor.GREEN)
			"greende" -> greenDoor = ZDoor(pos, ZDir.EAST, GColor.GREEN)
			"bluedn" -> blueDoor = ZDoor(pos, ZDir.NORTH, GColor.BLUE)
			"bluede" -> blueDoor = ZDoor(pos, ZDir.EAST, GColor.BLUE)
			else      -> super.loadCmd(grid, pos, cmd)
		}
	}

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueDoorKeyZone) {
			game.addLogMessage(c.name() + " has unlocked the Blue Door")
			game.unlockDoor(blueDoor)
			blueDoorKeyZone = -1
		}
		if (c.occupiedZone == greenDoorKeyZone) {
			game.addLogMessage(c.name() + " has unlocked the Green Door")
			game.unlockDoor(greenDoor)
			greenDoorKeyZone = -1
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		return if (isAllPlayersInExit(game)) 100 else 0
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (numDeadPlayers(game) > 0) {
			"Not all players survived."
		} else super.getQuestFailedReason(game)
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("4R", 180, getQuadrant(0, 0)),
			ZTile("6R", 270, getQuadrant(0, 3)),
			ZTile("5R", 270, getQuadrant(0, 6)),
			ZTile("7R", 90, getQuadrant(3, 0)),
			ZTile("8R", 180, getQuadrant(3, 3)),
			ZTile("9R", 180, getQuadrant(3, 6)))

	override fun init(game: ZGame) {
		while (greenDoorKeyZone == blueDoorKeyZone) {
			greenDoorKeyZone = redObjectives.random()
			blueDoorKeyZone = redObjectives.random()
		}
		game.board.setDoorLocked(blueDoor)
		game.board.setDoorLocked(greenDoor)
	}

	override suspend fun handleSpawnForZone(game: ZGame, zoneIdx: Int): Boolean {
		if (zoneIdx == exitZone) {
			game.spawnZombies(1, ZZombieType.Abomination, zoneIdx)
			return true
		}
		return super.handleSpawnForZone(game, zoneIdx)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(
				Table().setNoBorder()
					.addRow(
						"1.",
						"Escape through the underpass",
						String.format(
							"%d of %d",
							game.board.getCharactersInZone(exitZone).size,
							game.allCharacters.size
						)
					)
					.addRow("2.", "Unlock the Green Door", greenDoorKeyZone < 0)
					.addRow("3.", "Unlock the Blue Door", blueDoorKeyZone < 0)
					.addRow("4.", "All Player must survive", numDeadPlayers(game) == 0)
					.addRow(
						"5.",
						"Exit is cleared of zombies",
						game.board.getNumZombiesInZone(exitZone) == 0
					)
			)
	}
}