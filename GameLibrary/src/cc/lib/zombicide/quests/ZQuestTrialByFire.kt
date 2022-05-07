package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZMove.Companion.newObjectiveMove
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import java.util.*

class ZQuestTrialByFire : ZQuest(ZQuests.Trial_by_Fire) {
	companion object {
		init {
			addAllFields(ZQuestTrialByFire::class.java)
		}
	}

	var blueObjZone = 0
	lateinit var blueObjTreasure: ZWeapon
	lateinit var lockedVault: ZDoor
	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:red:we:ods", "z1:i:ods:ode", "z2:i:ws:ode",       "z3:i:ws", "z3:i:ws:ode", "z4:i:ds:ode",            "z5:i:ws:we", "z6:spn", "z7:v:vd1:ww:ws"),
arrayOf("z8:i:ods", "z8:i:ws:we", "z9:ws",                      "z10:ws", "z11:ws", "z12:ws",                       "z13:ws", "z14", "z15:ws"),
arrayOf("z16:i:ode:ods", "z17:i:ws:ode", "z18:i:ods:ode",       "z19:i:ode:ds", "z20:i:ws", "z20:i:ds:ode",         "z21:i:we:ods", "z22", "z23:i:ww:ods"),
arrayOf("z24:i", "z24:i:ode:ws", "z68:i:de:ws",                 "z25", "z26", "z27",                                "z28:i:dw:ws:we", "z29", "z30:i:red:ww"),
arrayOf("z24:i:ods:we", "z31", "z32",                           "z33:st", "z34:i:wn:ws:we:ww", "z35",               "z36", "z37", "z30:i:ww:ods"),
arrayOf("z38:i:ds:we", "z39", "z40:i:de:wn:ods:ww",             "z41", "z42", "z43",                                "z44:i:dw:wn:lvd1:we:ods", "z45", "z46:i:odn:ww:ods"),
arrayOf("z47:spw", "z48", "z49:i:ww:ode:ods",                   "z50:i:dn", "z50:i:wn", "z50:i:dn",                 "z51:i:odn:we:ods", "z52", "z53:i:ww"),
arrayOf("z54:i:wn:de:ods", "z55", "z56:i:we:ws:ww",             "z50:i:ws", "z50:i:ws:abom", "z50:i:ws:we",         "z57:i:ws:we", "z58", "z53:i:dw:ws"),
arrayOf("z59:i:we:red", "z60", "z61",                           "z62", "z63", "z64",                                "z65", "z66", "z67:spe")))

	override fun getPercentComplete(game: ZGame): Int {
		return if (game.getNumKills(ZZombieType.Abomination) > 0) 100 else 0
	}

	//9V
	override val tiles: Array<ZTile>
		get() = arrayOf( //9V
			ZTile("8V", 0, getQuadrant(0, 0)),
			ZTile("3R", 270, getQuadrant(0, 3)),
			ZTile("4V", 90, getQuadrant(0, 6)),
			ZTile("7R", 180, getQuadrant(3, 0)),
			ZTile("6R", 270, getQuadrant(3, 3)),
			ZTile("5R", 90, getQuadrant(3, 6)),
			ZTile("2R", 0, getQuadrant(6, 0)),
			ZTile("1V", 270, getQuadrant(6, 3)),
			ZTile("9V", 270, getQuadrant(6, 6)))

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		val cell = grid[pos]
		when (cmd) {
			"lvd1" -> setVaultDoor(cell, grid, pos, ZCellType.VAULT_DOOR_VIOLET, 1, ZWallFlag.LOCKED)
			else   -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		lockedVault = game.board.findVault(1)
		blueObjZone = redObjectives.random()
		blueObjTreasure = listOf(ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.INFERNO).random().create()
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = ArrayList<ZEquipmentType>(Arrays.asList(ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE))

	override fun processObjective(game: ZGame, c: ZCharacter) {
		if (c.occupiedZone == blueObjZone) {
			game.giftEquipment(c, blueObjTreasure)
		}
		super.processObjective(game, c)
		if (redObjectives.size == 0 && game.board.getDoor(lockedVault!!) === ZWallFlag.LOCKED) {
			game.addLogMessage(c.name() + " has unlocked the Violet Door")
			game.unlockDoor(lockedVault!!)
		}
	}

	override fun addMoves(game: ZGame, cur: ZCharacter, options: MutableList<ZMove>) {
		super.addMoves(game, cur, options)
		if (cur.occupiedZone == blueObjZone && !redObjectives.contains(blueObjZone)) {
			options.add(newObjectiveMove(cur.occupiedZone))
		}
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val blueObjFound = blueObjZone < 0
		val numTaken = numFoundObjectives
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Kill the Abomination.", getPercentComplete(game) == 100)
				.addRow("2.", "Blue objective hidden among the red objectives gives a random artifact", blueObjFound)
				.addRow("3.", "All Dragon Bile hidden in the vault. Vault cannot be opened until all objectives taken.", String.format("%d of %d", numTaken, numStartObjectives))
			)
	}

	override fun processLootDeck(items: MutableList<ZEquipment<*>>) {
		items.removeAll { it.type == ZItemType.DRAGON_BILE }
	}
}