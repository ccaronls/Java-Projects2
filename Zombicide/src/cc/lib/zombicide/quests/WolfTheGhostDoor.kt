package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 9/1/21.
 */
class WolfTheGhostDoor : ZQuest(ZQuests.The_Ghost_Door) {
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0", "z1:spn", "z2:i:ww:ode",                      "z10:i:vd1:we", "z11", "z12:i:ww:ode",              "z20:i:we:ods", "z21", "z22"),
arrayOf("z3:i:we:ods", "z4", "z2:i:ww:ws:we",               "z10:i:de:ws", "z13", "z12:i:dw:ws:we:red",         "z23:i:red:ws:we", "z24", "z25:i:dn:ww:ods"),
arrayOf("z5:i:red:we:ods", "z6", "z7",                      "z14", "z15", "z16",                                "z26", "z27", "z28:i:ww:red:ods"),
arrayOf("z30:i:ws:ode", "z31:i:wn:we:ds", "z32",            "z40:t3:rn:rw", "z40:t3:rn:re", "z41:t1:re",        "z50:i:dn:we:ods", "z51", "z52:i:ww:ws"),
arrayOf("z33", "z34", "z35",                                "z49:t3:rw:exit", "z40:t3", "z42:t2:re:rs",         "z53:i:we", "z54", "z55:spe"),
arrayOf("z36", "z37:i:wn:ww:ode:ws", "z38:i:wn:ods",        "z40:t3:rw:rs", "z40:t3:rs", "z40:t3:rs:re",        "z53:i:we:ods", "z56", "z57:i:wn:ww:ods:red"),
arrayOf("z60", "z61:i:ww:ws:ode", "z62:i:ds:de",            "z70:i:ws", "z70:i:ws:ode", "z71:i:ode:ws",         "z80:i:ws:we", "z81", "z82:ww:ds"),
arrayOf("z63", "z64", "z65",                                "z72:st", "z73", "z74",                             "z83", "z84", "z85"),
arrayOf("z66:i:wn:ode:red", "z67:i:dn:we", "z68:sps",       "z75:i:dw:vd1:wn:we", "z76:sps", "z77:i:ww:dn:ode", "z86:i:wn:we:red", "z87", "z88:i:dn:ww:red"))
		return load(map)
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("1R", 0, getQuadrant(0, 0)),
			ZTile("9V", 270, getQuadrant(0, 3)),
			ZTile("3V", 90, getQuadrant(0, 6)),
			ZTile("2R", 90, getQuadrant(3, 0)),
			ZTile("10R", 180, getQuadrant(3, 3)),
			ZTile("5R", 270, getQuadrant(3, 6)),
			ZTile("6V", 0, getQuadrant(6, 0)),
			ZTile("8R", 180, getQuadrant(6, 3)),
			ZTile("7V", 270, getQuadrant(6, 6))
		)

	override fun init(game: ZGame) {}
	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		c.addAvailableSkill(ZSkill.Inventory)
		game.chooseEquipmentFromSearchables()
	}

	private fun numSurvivorsAtDangerRED(game: ZGame): Int = game.allLivingCharacters.count { it.skillLevel.difficultyColor === ZColor.RED }
	private fun numREDinEXIT(game: ZGame): Int = game.allLivingCharacters.count { pl -> pl.skillLevel.difficultyColor === ZColor.RED && exitZone == pl.occupiedZone }

	override fun getPercentComplete(game: ZGame): Int {
		val needed = 2
		val total = if (numREDinEXIT(game) > 0) 1 else if (0 + numSurvivorsAtDangerRED(game) > 0) 1 else 0
		return total * 100 / needed
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name).addRow(Table().setNoBorder()
			.addRow("1.", "Get at least one survivor to RED danger level.", numSurvivorsAtDangerRED(game) > 0)
			.addRow("2.", "Get at least one survor at danger level RED to the EXIT. The EXIT but be clear on zombies.", numREDinEXIT(game) > 0)
			.addRow("3.", "Each OBJECTIVE grants " + getObjectiveExperience(0, 0) + " points and allow survivor to take an equipment card of their choice form the deck as well as reorganize their inventory for free", String.format("%d of %d", numRemainingObjectives, numStartObjectives))
		)
	}
}