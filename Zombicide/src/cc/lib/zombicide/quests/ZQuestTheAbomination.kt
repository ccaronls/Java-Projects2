package cc.lib.zombicide.quests

import cc.lib.utils.Table
import cc.lib.zombicide.*

class ZQuestTheAbomination : ZQuest(ZQuests.The_Abomination) {
	companion object {
		init {
			addAllFields(ZQuestTheAbomination::class.java)
		}
	}

	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:red:st:ode:ds", "z1", "z2", "z3", "z4:i:red:st:odw:ds"),
arrayOf("z5", "z6", "z7:abom", "z8", "z9"),
arrayOf("z10:spw", "z11", "z12", "z13", "z14:spe"),
arrayOf("z15", "z16", "z17:t3:rn:re:rw", "z18", "z19"),
arrayOf("z20:i:red:st:dn:ode", "z21", "z22", "z23", "z24:i:red:st:dn:odw"))
		return load(map)
	}

	override fun getPercentComplete(game: ZGame): Int {
		return if (game.getNumKills(ZZombieType.Abomination) > 0) 100 else 0
	}

	override val tiles: Array<ZTile>
		get() = emptyArray()

	override fun init(game: ZGame) {}
	override fun getObjectiveExperience(zoneIdx: Int, nthFound: Int): Int {
		return 20
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (vaultItemsRemaining.size > 0) {
			game.giftEquipment(c, vaultItemsRemaining.removeAt(0))
		}
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.BASTARD_SWORD, ZWeaponType.EARTHQUAKE_HAMMER)

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("Kill the Abomination")
			)
	}
}