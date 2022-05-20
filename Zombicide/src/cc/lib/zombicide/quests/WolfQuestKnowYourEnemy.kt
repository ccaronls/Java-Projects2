package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class WolfQuestKnowYourEnemy : ZQuest(ZQuests.Know_Your_Enemy) {
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:we", "z1:spn", "z3:i:ww",                     "z4:t1:rw", "z44:t2", "z5:red:v:vd1:ww:ws"),
arrayOf("z0:i:red:de:ws", "z6", "z3:we:ds",                 "z7", "z8:t3:rw", "z8:t3"),
arrayOf("z9:exit", "z10", "z11",                            "z12", "z8:t3:rs:rw", "z13:i:ww:wn:ws"),
arrayOf("z14:i:dn:red:we:ods", "z15", "z16:i:dn:de:ww:red", "z17", "z18", "z19:spe"),
arrayOf("z20:i:we", "z21", "z16:i:ww:we:ods",               "z22", "z23:i:dn:ww:we:ws:red", "z24"),
arrayOf("z20:i:ds:we", "z25", "z26:i:ww:de:ods",            "z27", "z28", "z29"),
arrayOf("z30:spw", "z31:st", "z32:i:ww",                    "z33:t2:rw:rn", "z34:t3:rn", "z34:t3:rn"),
arrayOf("z35:i:wn:de:ods", "z36", "z32:i:ww:ws:we:v:vd1",   "z37:t1", "z34:t3:rw:rs", "z34:t3:rs"),
arrayOf("z38:i:odn:we", "z39", "z40",                       "z41", "z42", "z43"))
		return load(map)
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjectives + game.allCharacters.size
		var numCompleted = numFoundObjectives
		for (c in game.board.getAllCharacters()) {
			if (c.occupiedZone == exitZone) numCompleted++
		}
		var percentCompleted = numCompleted * 100 / numTasks
		if (game.board.getZombiesInZone(exitZone).size > 0) percentCompleted--
		return percentCompleted
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("9V", 270, getQuadrant(0, 0)),
			ZTile("11R", 90, getQuadrant(0, 3)),
			ZTile("4R", 180, getQuadrant(3, 0)),
			ZTile("6R", 0, getQuadrant(3, 3)),
			ZTile("1R", 0, getQuadrant(6, 0)),
			ZTile("10V", 180, getQuadrant(6, 3)))

	override fun init(game: ZGame) {}
	override fun getObjectivesOverlay(game: ZGame): Table {
		val totalChars = game.allCharacters.size
		val numInZone = numPlayersInExitEvent(game)
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("", "Use the Towers for cover to execute ranged attacks on enemies")
				.addRow("1.", "Collect all Objectives", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "A Random Artifact is in the Vault - Go get it!", numFoundVaultItems > 0)
				.addRow("3.", "Get all players into the EXIT zone.", String.format("%d of %d", numInZone, totalChars))
				.addRow("4.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
				.addRow("5.", "All Players must survive.")
			)
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (numDeadPlayers(game) > 0) {
			"Not all players survived."
		} else super.getQuestFailedReason(game)
	}
}