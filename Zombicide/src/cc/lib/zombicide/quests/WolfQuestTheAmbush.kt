package cc.lib.zombicide.quests

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 8/24/21.
 */
class WolfQuestTheAmbush : ZQuest(ZQuests.The_Ambush) {
	companion object {
		init {
			addAllFields(WolfQuestTheAmbush::class.java)
		}
	}

	private lateinit var blueSpawnPos: Grid.Pos
	private var occupyZones: MutableList<Int> = ArrayList()
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:red:i:ds:we", "z1:spn", "z2:i:ww:ws:ode",           "z3:i:red:ds", "z3:i:ws:ode", "z4:i:ws:de",         "z5:spn", "z6:i:ww:ds:ode", "z7:i:red:ws"),
arrayOf("z8", "z9", "z10",                                      "z11", "z12:st", "z13",                             "z14", "z15", "z16:spe"),
arrayOf("z17:i:wn:ode", "z18:i:dn", "z18:i:wn:ode",             "z19:i:red:wn:we:ods", "z20", "z21:i:red:wn:ww:ode", "z26:i:wn:ode", "z22:i:dn:we", "z23"),
arrayOf("z24:t2:rn", "z25:t3:rn:occupy", "z25:t3:rn:re",        "z32:i:we", "z27", "z28:i:ww:odn",                  "z29:t2:rn:rw", "z29:t2:rn:re", "z30"),
arrayOf("z31:t1:re", "z25:t3:rs", "z25:t3:rs:re",               "z32:i:ds:we", "z33", "z28:i:dw:ws",                "z34:t1:rw:re", "z35:t3:occupy", "z35:t3:rn"),
arrayOf("z36:blspw", "z37", "z38",                              "z39", "z40", "z41",                                "z42", "z35:t3:rw", "z35:t3:"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"blspw" -> {
				blueSpawnPos = pos
				setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.WEST, false, true, true))
			}
			"spn" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.NORTH, true, false, false))
			"spe" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.EAST, true, false, false))
			"occupy" -> occupyZones.add(grid[pos].zoneIndex)
			else     -> super.loadCmd(grid, pos, cmd)
		}
	}

	private fun getOccupiedZones(game: ZGame): List<Int> {
		return game.allCharacters.map { it.occupiedZone }.toMutableList().also {
			it.retainAll(occupyZones)
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		val blueCell = game.board.getCell(blueSpawnPos)
		val total = numStartObjectives + occupyZones.size + 1
		val found = numFoundObjectives + getOccupiedZones(game).size + if (blueCell.numSpawns > 0) 0 else 1
		return found * 100 / total
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("8R", 0, getQuadrant(0, 0)),
			ZTile("5R", 0, getQuadrant(0, 3)),
			ZTile("3V", 180, getQuadrant(0, 6)),
			ZTile("10V", 180, getQuadrant(3, 0)),
			ZTile("9V", 270, getQuadrant(3, 3)),
			ZTile("11V", 90, getQuadrant(3, 6))
		)

	override fun init(game: ZGame) {}
	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (vaultItemsRemaining.size > 0) {
			game.giftEquipment(c, vaultItemsRemaining.removeAt(0))
		}
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.BASTARD_SWORD, ZWeaponType.EARTHQUAKE_HAMMER)

	override fun getMaxNumZombiesOfType(type: ZZombieType?): Int = when (type) {
		ZZombieType.Necromancer -> 3
		else -> super.getMaxNumZombiesOfType(type)
	}

	override fun drawQuest(board: ZBoard, g: AGraphics) {
		for (zIdx in occupyZones) {
			val zone = board.getZone(zIdx)
			val rect = zone.rectangle.scaledBy(.25f, .25f)
			g.color = GColor.GREEN //.withAlpha(.5f));
			g.drawLine(rect.topLeft, rect.bottomRight, 10f)
			g.drawLine(rect.topRight, rect.bottomLeft, 10f)
			if (board.getActorsInZone(zIdx).count { it is ZCharacter } > 0) {
				g.drawCircle(rect.center, rect.radius, 10f)
			}
		}
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (game.allLivingCharacters.size < 2) "Not enough players alive to complete quest" else super.getQuestFailedReason(game)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val blueCell = game.board.getCell(blueSpawnPos)
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Collect all Objectives. Each objective gives a vault item.", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Eliminate the BLUE spawn zone using normal necromancer rules.", blueCell.numSpawns == 0)
				.addRow("3.", "Occupy each tower with at least one player", String.format("%d of %d", getOccupiedZones(game).size, occupyZones.size))
				.addRow("4.", "RED spawn zones can spawn Necromancers but they cannot be removed.")
			)
	}
}