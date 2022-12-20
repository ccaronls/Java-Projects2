package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class ZQuestTheShepherds : ZQuest(ZQuests.The_Shepherds) {
	companion object {
		init {
			addAllFields(ZQuestTheShepherds::class.java)
		}
	}

	private var greenSpawnZone = -1
	private var blueSpawnZone = -1
	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:gvd1:ws:ode", "z50:i:red:ws:de", "z1",            "z2", "z3:spn", "z4:i:ww:ods:red:ode",          "z5:i:we", "z6", "z7:i:ww:vd4"),
arrayOf("z8", "z9", "z10",                                      "z11:i:ww:wn:we:ods", "z12", "z13:i:ww:ds:we",  "z5:i:ods:we", "z14", "z7:i:ww:ds"),
arrayOf("z15", "z16:i:wn:ww:ws", "z16:i:wn:ode:ods",            "z17:i:de:ods:red", "z18", "z19",               "z20", "z21", "z22"),
arrayOf("z23", "z24:i:dw:ws:ode", "z25:i:ws:ode",               "z26:i:ws:we", "z27", "z28:i:ww:ws:dn:ode",     "z49:i:ws:we:dn:red", "z29", "z30:i:ww:ws:dn"),
arrayOf("z31:spw", "z32", "z33",                                "z34", "z35", "z36",                            "z37", "z38", "z39:spe"),
arrayOf("z40:i:red:wn:ode:ws", "z51:i:wn:ws:de", "z41:ws",      "z42:i:dw:wn:gvd3:we", "z43:st", "z44:i:dw:wn:vd2:ode", "z45:i:wn:ode", "z46:i:wn", "z46:i:red:wn"),
arrayOf("", "", "",                                             "z47:v:gvd3:ww:wn", "z47:v:wn", "z47:v:wn:we:gvd1", "z48:v:vd2:wn", "z48:v:wn", "z48:v:vd4:wn")))

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueSpawnZone) {
			game.spawnZombies(blueSpawnZone)
			blueSpawnZone = -1
		} else if (c.occupiedZone == greenSpawnZone) {
			game.spawnZombies(greenSpawnZone)
			greenSpawnZone = -1
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		return numFoundObjectives * 100 / numStartObjectives
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("1R", 90, getQuadrant(0, 0)),
			ZTile("2R", 180, getQuadrant(0, 3)),
			ZTile("9V", 270, getQuadrant(0, 6)),
			ZTile("3V", 0, getQuadrant(3, 0)),
			ZTile("4V", 270, getQuadrant(3, 3)),
			ZTile("5R", 180, getQuadrant(3, 6)))

	override fun init(game: ZGame) {
		require(redObjectives.size > 1)
		while (blueSpawnZone == greenSpawnZone) {
			blueSpawnZone = redObjectives.random()
			greenSpawnZone = redObjectives.random()
		}
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name).addRow(
			Table().setNoBorder()
				.addRow("Rescue the townsfolk by claiming\nall of the objectives.\nSome townsfolk are infected.", String.format("%d of %d", numFoundObjectives, numStartObjectives))
		)
	}
}