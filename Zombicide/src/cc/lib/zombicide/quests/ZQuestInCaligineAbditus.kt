package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class ZQuestInCaligineAbditus : ZQuestTheCommandry(ZQuests.In_Caligine_Abditus) {
	companion object {
		init {
			addAllFields(ZQuestInCaligineAbditus::class.java)
		}
	}

	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:red:ws:ode", "z1:i:ds:we", "z2:spn",          "z4:i:dw:ws:gvd1", "z4:i:ws:greende", "z5:i:ws:bluede",         "z6:i:vd2:ws:we", "z7:spn", "z8:exit:ww"),
arrayOf("z9", "z10", "z11",                                  "z12", "z13", "z14",                                           "z15", "z16", "z8:ww:ods"),
arrayOf("z17", "z18:i:wn:ww:ws:ode", "z19:i:wn:ode:red:ods", "z20:i:wn:ode:ds", "z21:i:wn:ws", "z21:i:wn:ds:ode",           "z22:i:wn:we:ods", "z23", "z24:i:ww:odn:ods"),
arrayOf("z25", "z26:i:ww:ws:gvd3", "z26:i:ws:de",           "z27", "z28", "z29",                                            "z30:i:dw:ws:we", "z31", "z32:i:ww"),
arrayOf("z33", "z34", "z35",                                "z36", "z37:i:wn:ww:we:ws", "z38",                              "z39", "z40", "z32:i:ww:ods"),
arrayOf("z41:spw:ws", "z42:i:ww:wn:ws:red", "z42:i:ws:wn:de", "z43:ws", "z44:start:ws", "z45:ws",                           "z46:i:red:ws:wn:we:dw", "z47:ws:sps", "z48:i:vd4:ww:ws"),
arrayOf("", "", "",                                         "z49:v:gvd1:ww", "z49:v", "z49:v:gvd3:we",                      "z50:v:vd2", "z50:v", "z50:v:vd4"))
		return load(map)
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("2R", 270, getQuadrant(0, 0)),
			ZTile("3R", 270, getQuadrant(0, 3)),
			ZTile("5R", 90, getQuadrant(0, 6)),
			ZTile("9V", 0, getQuadrant(3, 0)),
			ZTile("6R", 270, getQuadrant(3, 3)),
			ZTile("8R", 270, getQuadrant(3, 6)))

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Reach EXIT will all survivors.")
				.addRow("2.", "Unlock the Green Door", game.board.getDoor(greenDoor) !== ZWallFlag.LOCKED)
				.addRow("3.", "Unlock the Blue Door", game.board.getDoor(blueDoor) !== ZWallFlag.LOCKED)
			)
	}
}