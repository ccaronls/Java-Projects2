package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.utils.assertTrue
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 8/24/21.
 */
class WolfQuestTheEvilTwins : ZQuest(ZQuests.The_Evil_Twins) {
	companion object {
		var NUM_TWINS = 2

		init {
			addAllFields(WolfQuestTheEvilTwins::class.java)
		}
	}

	var greenObjective = -1
	var blueObjective = -1
	override fun loadBoard(): ZBoard {
		val map = arrayOf(arrayOf("z0:i:gvd1", "z0:i:ode:ws", "z1:i:ws:ode", "z2:i:ws:de", "z3:spn", "z4:i:dw:ws:ode", "z5:i:ws:ode", "z6:i:vd1:ws:we", "z7"), arrayOf("z0:i:we:ods", "z8", "z9", "z10", "z11", "z12", "z13", "z14", "z15"), arrayOf("z16:i:red:we:ods", "z17", "z18:i:wn:ww:ds:ode", "z19:i:red:wn:we:ods", "z20", "z21:i:wn:ww:de:ods", "z22", "z23:i:wn:ws:dw:ode", "z24:i:wn"), arrayOf("z25:i:we:ods", "z26", "z27", "z28:i:dw:we:ods", "z29", "z30:i:ww:de:ods", "z31", "z32:t1:rn", "z33:t2:rn"), arrayOf("z34:i:ws:we", "z35", "z36:i:wn:ww:we:ods", "z37:i:ods", "z37:i:wn:ws", "z37:i:ods:we", "z38", "z39:t3:rw:rn", "z39:t3"), arrayOf("z40:spw", "z41", "z42:i:ww:ode", "z43:i:ds:we", "z44", "z45:i:ds:de:ww", "z46", "z39:t3:rw:rs", "z39:t3:rs"), arrayOf("z48", "z49:t1:rn", "z50:t2:rn:re", "z51", "z52", "z53", "z54", "z55", "z56"), arrayOf("z57:t3:rn", "z57:t3:rn", "z50:t2:re:rs", "z59", "z60:i:wn:ws:we:ww", "z61:st", "z62:i:wn:we:ww", "z63", "z64:i:dw:wn"), arrayOf("z57:t3:vd2:ws", "z57:t3:re:ws", "z65:sps:ws", "z66", "z67", "z68:exit", "z62:i:red:dw:we", "z69:sps", "z64:i:gvd2:ww"), arrayOf("", "", "", "z70:v:vd1:wn:ww", "z70:v:wn", "z70:v:wn:we:vd2", "z71:v:ww:wn:gvd1", "z71:v:wn", "z71:v:gvd2:wn:we"))
		return load(map)
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numThings = numStartObjectives + NUM_TWINS + 1
		val numFound = numFoundObjectives
		val numKilled: Int = game.getNumKills(ZZombieType.BlueTwin, ZZombieType.GreenTwin).coerceIn(0, NUM_TWINS)
		val allInZone = if (isAllPlayersInExit(game)) 1 else 0
		return (numFound + numKilled + allInZone) * 100 / numThings
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("7R", 180, getQuadrant(0, 0)),
			ZTile("4V", 90, getQuadrant(0, 3)),
			ZTile("2R", 90, getQuadrant(0, 6)),
			ZTile("3V", 90, getQuadrant(3, 0)),
			ZTile("5V", 180, getQuadrant(3, 3)),
			ZTile("10V", 270, getQuadrant(3, 6)),
			ZTile("11V", 180, getQuadrant(6, 0)),
			ZTile("6R", 0, getQuadrant(6, 3)),
			ZTile("9V", 90, getQuadrant(6, 6)))

	override fun init(game: ZGame) {
		assertTrue(redObjectives.size > 1)
		while (blueObjective == greenObjective) {
			blueObjective = redObjectives.random()
			greenObjective = redObjectives.random()
		}
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		game.giftRandomVaultArtifact(c)
		if (c.occupiedZone == blueObjective) {
			game.spawnZombies(1, ZZombieType.BlueTwin, blueObjective)
			blueObjective = -1
		} else if (c.occupiedZone == greenObjective) {
			game.spawnZombies(1, ZZombieType.GreenTwin, greenObjective)
			greenObjective = -1
		}
	}

	override fun getMaxNumZombiesOfType(type: ZZombieType?): Int {
		when (type) {
			ZZombieType.GreenTwin, ZZombieType.BlueTwin -> return 1
			ZZombieType.Abomination, ZZombieType.Wolfbomination -> return 0
		}
		return super.getMaxNumZombiesOfType(type)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val chars: List<ZCharacter> = game.allCharacters.map { c -> c.character }
		val totalChars = chars.size
		val numInExit: Int = numPlayersInExitEvent(game)
		val numAbominationsKilled = game.getNumKills(ZZombieType.BlueTwin, ZZombieType.GreenTwin)
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Find and eliminate the Evil Twin Abominations hidden among the RED objectives", String.format("%d of %d", numAbominationsKilled, NUM_TWINS))
				.addRow("2.", "Find BLUE Twin hidden among RED objectives.", blueObjective < 0)
				.addRow("3.", "Find GREEN Twin hidden among RED objectives.", greenObjective < 0)
				.addRow("4.", "Get all players into the EXIT zone.", String.format("%d of %d", numInExit, totalChars))
				.addRow("5.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
				.addRow("6.", "All Players must survive.")
			)
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (numDeadPlayers(game) > 0) {
			"Not all players survived."
		} else super.getQuestFailedReason(game)
	}
}