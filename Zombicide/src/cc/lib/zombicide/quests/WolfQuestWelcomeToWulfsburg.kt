package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class WolfQuestWelcomeToWulfsburg : ZQuest(ZQuests.Welcome_to_Wulfsberg) {
	companion object {
		init {
			addAllFields(WolfQuestWelcomeToWulfsburg::class.java)
		}
	}

	var blueKeyZone = 0
	var greenKeyZone = 0
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:exit:i:de:ws", "z1:spn", "z2:i:ww:ws:ode",                      "z3:i:ode:ds", "z4:i:ode:ods", "z5:i:ods"),
arrayOf("z6", "z7", "z8",                                                   "z9", "z10:i:ww:ods", "z10:i:ods"),
arrayOf("z11:i:wn:ode:ws", "z12:i:wn:ws", "z12:i:wn:ods:de",                "z13:i:wn:ode", "z14:i:we", "z15:i:odn"),
arrayOf("z16:i", "z16:i", "z16:i:we",                                       "z17:t2:rn", "z18:t3:rn", "z18:t3:rn"),
arrayOf("z16:i:ds", "z16:i:ws", "z16:i:we:ws",                              "z19:t1:re", "z18:t3:rs", "z18:t3:rs"),
arrayOf("z20:spw", "z21", "z22",                                            "z23", "z24", "z25:spe"),
arrayOf("z26", "z27:i:ww:wn:ws:ode", "z28:i:red:dn:de:ws",                  "z29", "z30:i:dw:red:wn:ws:ode", "z31:i:dn:ws"),
arrayOf("z32", "z33", "z34",                                                "z35:st", "z36", "z37"),
arrayOf("z38:i:wn:ode", "z39:i:dn:red:we", "z40:sps",                       "z41:i:dw:wn:red", "z42:i:wn:we", "z43"))
		return load(map)
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("5R", 180, getQuadrant(0, 0)),
			ZTile("8V", 180, getQuadrant(0, 3)),
			ZTile("1V", 270, getQuadrant(3, 0)),
			ZTile("10V", 180, getQuadrant(3, 3)),
			ZTile("6V", 0, getQuadrant(6, 0)),
			ZTile("3V", 180, getQuadrant(6, 3))
		)

	override fun init(game: ZGame) {
		blueKeyZone = redObjectives.random()
		do {
			greenKeyZone = redObjectives.random()
		} while (greenKeyZone == blueKeyZone)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val totalChars = game.allCharacters.size
		val numInZone = numPlayersInExit(game)
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("", "Use the Towers for cover to execute ranged attacks on enemies")
				.addRow("1.", "Collect all Objectives", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Find the BLUE objective hidden among RED objectives for a random vault item.", blueKeyZone < 0)
				.addRow("3.", "Find the GREEN objective hidden among RED objectives for a random vault item.", greenKeyZone < 0)
				.addRow("4.", "Get all players into the EXIT zone.", String.format("%d of %d", numInZone, totalChars))
				.addRow("5.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
				.addRow("6.", "All Players must survive.")
			)
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueKeyZone) {
			game.addLogMessage(c.getLabel() + " has found the BLUE key")
			game.giftRandomVaultArtifact(c)
			blueKeyZone = -1
		} else if (c.occupiedZone == greenKeyZone) {
			game.addLogMessage(c.getLabel() + " has found the GREEN key")
			game.giftRandomVaultArtifact(c)
			greenKeyZone = -1
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjectives + game.allCharacters.size
		var numCompleted = numFoundObjectives
		for (c in game.allCharacters) {
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