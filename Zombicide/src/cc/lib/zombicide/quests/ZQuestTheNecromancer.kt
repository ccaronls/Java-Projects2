package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*

class ZQuestTheNecromancer : ZQuest(ZQuests.The_Necromancer) {
	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:de:ds", "z1:walker", "z2", "z3:walker", "z4:i:dw:ds"),
arrayOf("z5", "z6:ws", "z7:wn:ww:we", "z8:ws", "z9"),
arrayOf("z10", "z11", "z12:v:necro:abom", "z13", "z14"),
arrayOf("z15", "z16:wn", "z17:we:ww", "z18:wn", "z19"),
arrayOf("z20:i:dn:de", "z21", "z22:st:sps", "z23", "z24:i:dn:dw")))

	override fun getPercentComplete(game: ZGame): Int {
		return if (game.getNumKills(ZZombieType.Necromancer) > 0) 100 else 0
	}

	override val tiles : Array<ZTile> = emptyArray()

	override fun init(game: ZGame) {}
	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("Kill the Necromancer")
			)
	}
}