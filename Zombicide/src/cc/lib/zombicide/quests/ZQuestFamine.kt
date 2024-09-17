package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZItemType
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ZWallFlag
import cc.lib.zombicide.ZZoneType

class ZQuestFamine : ZQuest(ZQuests.Famine) {
	companion object {
		const val NUM_OF_EACH_QUEST_ITEM = 2

		init {
			addAllFields(ZQuestFamine::class.java)
		}
	}

	var numApplesFound = 0
	var numSaltedMeatFound = 0
	var numWaterFound = 0
	var blueKeyZone = -1
	var lockedVaults: MutableList<ZDoor> = ArrayList()
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:ws", "z0:i:ws:ode", "z1:i:red:ds:ode",            "z2:i:ws:ode", "z3:i:ws:we", "z4",          "z5", "z6:spn", "z7:i:ww:ods"),
arrayOf("z8:spw", "z9", "z10",                                  "z11", "z12", "z13",                        "z14:i:ww:wn:we", "z15", "z16:i:dw:ws:red:"),
arrayOf("z17:i:lvd1:wn:ode:ods", "z18:i:wn:ws", "z18:i:wn:de:ods", "z19", "z20:i:ww:wn:ws:ode",             "z21:i:wn:ds:ode", "z14:i:de:ods", "z22", "z23"),
arrayOf("z24:i:ws:we", "z25", "z26:i:ws:ww:de",                 "z27", "z28", "z29",                        "z30:i:dw:ws:we", "z31", "z32:i:dn:ww:"),
arrayOf("z33:spw", "z34", "z35",                                "z36", "z37:i:lvd2:wn:we:ws:dw", "z38",     "z39", "z40", "z32:i:red:ods:ww"),
arrayOf("z41:i:wn:ws:ode", "z42:i:red:wn:ws", "z42:i:wn:ws:de", "z43:ws", "z44:st:ws", "z45:ws",            "z46:i:wn:we:dw:ws", "z47:spe:ws", "z48:i:ww:ws"),
arrayOf("", "", "",                                             "z49:v:gvd1:ww", "z49:v", "z49:v:gvd2:we",  "", "", ""))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		val cell = grid[pos]
		when (cmd) {
			"lvd1" -> {
				super.loadCmd(grid, pos, "gvd1")
				setCellWall(grid, pos, ZDir.DESCEND, ZWallFlag.LOCKED)
			}
			"lvd2" -> {
				super.loadCmd(grid, pos, "gvd2")
				setCellWall(grid, pos, ZDir.DESCEND, ZWallFlag.LOCKED)
			}
			else   -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		lockedVaults.add(game.board.findVault(1))
		lockedVaults.add(game.board.findVault(2))
		blueKeyZone = redObjectives.random()
	}

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueKeyZone) {
			game.addLogMessage("Blue key found. Vault unlocked")
			for (door in lockedVaults) {
				game.unlockDoor(door)
			}
			blueKeyZone = -1
		}
		// give a random quest object
		remainingQuestItems.takeIf { it.isNotEmpty() }?.let {
			game.giftEquipment(c, it.random())
		}
	}

	val remainingQuestItems: List<ZEquipment<*>>
		get() {
			val l: MutableList<ZEquipment<*>> = ArrayList()
			for (i in numApplesFound until NUM_OF_EACH_QUEST_ITEM) {
				l.add(ZItemType.APPLES.create())
			}
			for (i in numSaltedMeatFound until NUM_OF_EACH_QUEST_ITEM) {
				l.add(ZItemType.SALTED_MEAT.create())
			}
			for (i in numWaterFound until NUM_OF_EACH_QUEST_ITEM) {
				l.add(ZItemType.WATER.create())
			}
			return l
		}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = 7
		var numCompleted = (Math.min(numApplesFound, NUM_OF_EACH_QUEST_ITEM)
			+ Math.min(numSaltedMeatFound, NUM_OF_EACH_QUEST_ITEM)
			+ Math.min(numWaterFound, NUM_OF_EACH_QUEST_ITEM))
		if (isAllLockedInVault(game)) {
			numCompleted++
		}
		return numCompleted * 100 / numTasks
	}

	fun isAllLockedInVault(game: ZGame): Boolean {
		var vaultZone = -1
		for (c in game.board.getAllCharacters()) {
			if (!c.isAlive) continue
			val zone = game.board.getZone(c.occupiedZone)
			vaultZone = if (zone.type !== ZZoneType.VAULT) {
				return false
			} else {
				c.occupiedZone
			}
		}
		if (vaultZone >= 0) {
			if (game.board.getZombiesInZone(vaultZone).size > 0) return false
			val zone = game.board.getZone(vaultZone)
			for (door in zone.doors) {
				if (!door.isClosed(game.board)) return false
			}
		}
		return true
	}

	override suspend fun onEquipmentFound(game: ZGame, equip: ZEquipment<*>) {
		if (equip.type === ZItemType.APPLES) {
			numApplesFound++
		} else if (equip.type === ZItemType.SALTED_MEAT) {
			numSaltedMeatFound++
		} else if (equip.type === ZItemType.WATER) {
			numWaterFound++
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("3R", 270, getQuadrant(0, 0)),
			ZTile("2R", 270, getQuadrant(0, 3)),
			ZTile("1R", 180, getQuadrant(0, 6)),
			ZTile("8R", 0, getQuadrant(3, 0)),
			ZTile("6R", 270, getQuadrant(3, 3)),
			ZTile("5R", 90, getQuadrant(3, 6)))

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Find BLUE key hidden among RED\nobjectives and unlocks the Vault", blueKeyZone < 0)
				.addRow("2.", "Find 2 Apples", String.format("%d of %d", numApplesFound, NUM_OF_EACH_QUEST_ITEM))
				.addRow("3.", "Find 2 Water", String.format("%d of %d", numWaterFound, NUM_OF_EACH_QUEST_ITEM))
				.addRow("4.", "Find 2 Salted Meat", String.format("%d of %d", numSaltedMeatFound, NUM_OF_EACH_QUEST_ITEM))
				.addRow("5.", "Lock yourselves in the Vault with no zombies.", isAllLockedInVault(game))
			)
	}

	override fun processLootDeck(items: MutableList<ZEquipment<*>>) {
		items.addAll(listOf(
			ZItemType.APPLES.create(),
			ZItemType.APPLES.create(),
			ZItemType.WATER.create(),
			ZItemType.WATER.create(),
			ZItemType.SALTED_MEAT.create(),
			ZItemType.SALTED_MEAT.create()
		))
	}
}