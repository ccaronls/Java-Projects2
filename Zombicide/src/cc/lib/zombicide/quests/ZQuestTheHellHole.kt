package cc.lib.zombicide.quests

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

class ZQuestTheHellHole : ZQuest(ZQuests.The_Hell_Hole) {
	companion object {
		init {
			addAllFields(ZQuestTheHellHole::class.java)
		}
	}

	var hellHoleZone = -1
	val objSpawns: MutableList<Int> = ArrayList()
	var numStartObjSpawns = -1
	var hellholeBurnt = false
	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("7R", 180, getQuadrant(0, 0)),
			ZTile("9R", 180, getQuadrant(0, 3)),
			ZTile("8V", 90, getQuadrant(0, 6)),
			ZTile("2R", 0, getQuadrant(3, 0)),
			ZTile("1V", 270, getQuadrant(3, 3)),
			ZTile("6V", 270, getQuadrant(3, 6)))

	override fun loadBoard() = load(arrayOf(
arrayOf("z0:i:red:necro", "z0:i:ws:ode", "z1:i:ws:ode",             "z2:i:ws:de", "z3:st", "z4:i:dw:ode",       "z5:i:ods:ode", "z6:i:ode", "z7:i:red:ws:necro"),
arrayOf("z0:i:we:ods", "z8", "z9",                                  "z10", "z11", "z4:i:ds:we:ww",              "z12:i:ds:ode", "z6:i:ws:ode", "z13:i:ods"),
arrayOf("z14:i:ds:we", "z15", "z16:i:wn:ww:ode:ods",                "z17:i:wn:ws:ode", "z18:i:wn:ws", "z18:i:ds:ode", "z19:ods:i:we", "z20", "z21:i:ww:ds"),
arrayOf("z22:objspawnw", "z23", "z24:i:dw:ods:we",                  "z25:i:spn", "z25:i", "z25:i:spn:we",       "z26:i:ods:de", "z27", "z28:objspawne"),
arrayOf("z29:i:wn:de:ods:red", "z30", "z32:i:ww:ws:we:necro",       "z25:i:ws", "z25:i:hh:ds", "z25:i:we:ws",   "z33:i:ws:we:necro", "z34", "z35:i:red:wn:dw:ods"),
arrayOf("z36:i:we", "z37", "z38",                                   "z39", "z40", "z41",                        "z42", "z43", "z44:i:ww")))

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"hh" -> hellHoleZone = grid[pos].zoneIndex
			"objspawnw" -> {
				super.loadCmd(grid, pos, "spw")
				objSpawns.add(grid[pos].zoneIndex)
			}
			"objspawne" -> {
				super.loadCmd(grid, pos, "spe")
				objSpawns.add(grid[pos].zoneIndex)
			}
			else        -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		game.giftEquipment(c, ZItemType.DRAGON_BILE.create())
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjSpawns + 1
		var numCompleted = numStartObjSpawns - objSpawns.size
		if (hellholeBurnt) numCompleted++
		return numCompleted * 100 / numTasks
	}

	override fun init(game: ZGame) {
		numStartObjSpawns = objSpawns.size
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Destroy all spawn zones OUTSIDE the HELLHOLE (Red area). This requires waiting for Necromancers to spawn, killing them, then removing the spawns outside of the hellhole.", String.format("%d of %d", numStartObjSpawns - objSpawns.size, numStartObjSpawns))
				.addRow("2.", "Set the Hellhole ablaze using dragon bile AFTER the spawn objectives completed.", hellholeBurnt)
				.addRow("3.", "The RED Objectives give EXP and a Dragon Bile to the player that takes it.", String.format("%d Left", redObjectives.size))
			)
	}

	override fun onDragonBileExploded(c: ZCharacter, zoneIdx: Int) {
		if (zoneIdx == hellHoleZone && objSpawns.size == 0) {
			hellholeBurnt = true
		}
	}

	override fun drawQuest(board: ZBoard, g: AGraphics) {
		val hellhole = board.getZone(hellHoleZone)
		g.color = GColor.RED.withAlpha(.2f)
		hellhole.drawFilled(g)
	}

	override fun getMaxNumZombiesOfType(type: ZZombieType?): Int = when (type) {
		ZZombieType.Necromancer -> 6
		else -> super.getMaxNumZombiesOfType(type)
	}
}