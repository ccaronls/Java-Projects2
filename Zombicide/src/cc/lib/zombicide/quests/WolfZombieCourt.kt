package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 9/1/21.
 */
class WolfZombieCourt : ZQuest(ZQuests.Zombie_Court) {
	companion object {
		init {
			addAllFields(WolfZombieCourt::class.java)
		}
	}

	var blueObjZone = -1
	var greenObjZone = -1
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:we:ws", "z1:t3", "z1:t3:re:gvd1",                     "z10:spn", "z11:i:ww:ws:ode", "z12:i:ds:de:red",            "z20", "z21:i:ds:ode:ww", "z22:i:red:ws"),
arrayOf("z2:t2", "z1:t3:rs", "z1:t3:rs:re",                         "z13", "z14", "z15",                                        "z23", "z24", "z25:spe"),
arrayOf("z3:t2:rs", "z4:t1:rs", "z5",                               "z16:i:dn:dw:ode:ods", "z17:i:wn:de:ws", "z18",             "z26:i:dw:wn:ode", "z27:i:wn:we:red", "z28"),
arrayOf("z30:i:vd1:ds:ode", "z31:i:red:we:ws", "z32",               "z40:i:dw:ws:red", "z40:i:ws:we", "z41",                    "z50:t3:rn:rw:vd2", "z50:t3:rn:re", "z51"),
arrayOf("z33", "z34", "z35:st",                                     "z42", "z43", "z44",                                        "z50:t3:rw", "z50:t3:rs:re", "z52"),
arrayOf("z36:spw:ws", "z37:i:ww:wn:red:ws", "z37:i:wn:ode:ws",      "z45:i:gvd2:dn", "z45:i:wn:we", "z46:sps",                  "z53:t2:rw", "z54:t1", "z55:red"),
arrayOf("", "", "",                                                 "z60:v:gvd1:ww:wn", "z60:v:wn", "z60:v:gvd2:ww:wn",         "z61:v:vd1:wn", "z61:v:wn", "z61:v:vd2:wn"))
		return load(map)
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("11R", 0, getQuadrant(0, 0)),
			ZTile("6V", 0, getQuadrant(0, 3)),
			ZTile("3V", 0, getQuadrant(0, 6)),
			ZTile("1R", 90, getQuadrant(3, 0)),
			ZTile("9V", 180, getQuadrant(3, 3)),
			ZTile("10V", 90, getQuadrant(3, 6))
		)

	override fun init(game: ZGame) {
		require(redObjectives.size > 1)
		while (blueObjZone == greenObjZone) {
			blueObjZone = redObjectives.random()
			greenObjZone = redObjectives.random()
		}
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		when (c.occupiedZone) {
			greenObjZone -> {
				game.addLogMessage(c.name() + " has found the PENDANT")
				greenObjZone = -1
			}
			blueObjZone  -> {
				game.addLogMessage(c.name() + " has found the CROWN")
				blueObjZone = -1
			}
			else         -> {
				game.spawnZombies(c.occupiedZone)
			}
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		return numFoundObjectives * 100 / numStartObjectives
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Collect all objectives", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Find the Crown", blueObjZone < 0)
				.addRow("3.", "Find the Pendant", greenObjZone < 0)
				.addRow("4.", "Find Vault Artifacts", numFoundVaultItems)
			)
	}
}