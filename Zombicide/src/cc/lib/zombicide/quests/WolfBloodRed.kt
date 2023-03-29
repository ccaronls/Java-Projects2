package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 9/1/21.
 */
class WolfBloodRed : ZQuest(ZQuests.Blood_Red) {
	companion object {
		init {
			addAllFields(WolfBloodRed::class.java)
		}
	}

	private var greenObjZone = -1
	private var blueObjZone = -1
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:ws:ode:red", "z1:i:ws:de", "z2",                      "z3", "z4", "z5",                           "z6:xspn", "z7:i:dw:ws", "z8:i:odw:ws:red"),
arrayOf("z9:xspw", "z10", "z11",                                    "z12:i:ww:wn:we", "z13", "z14:i:ww:wn:we",  "z15", "z16", "z17"),
arrayOf("z18", "z19:i:red:wn:ww:ws:ode", "z20:i:red:wn:ws:ode",     "z12:i:ds:we", "z21", "z14:i:ds:ww:ode",    "z22:i:wn:red:ode", "z23:i:red:wn:de", "z50"),
arrayOf("z24", "z25:t1:rn", "z26:t2:rn:re",                         "z27", "z28", "z29",                        "z30:t3:rn:rw:vd2", "z30:t3:rn:re", "z31:xspe"),
arrayOf("z32", "z33:t3:rw:rn", "z33:t3:re",                         "z34", "z35:i:wn:we:ww:gvd1",               "z36", "z30:t3:rw", "z30:t3:re", "z46:t1"),
arrayOf("z47:xsps", "z33:t3:rw", "z33:t3:re:gvd2",                  "z37", "z35:i:ww:we:ws:vd1:red", "z38",     "z39:st", "z40:t2:rw", "z40:t2"),
arrayOf("z41:v:vd1:wn:ww", "z41:v:wn", "z41:v:wn:vd2:we",           "z42", "z43", "z44",                        "z45:v:gvd1:ww:wn", "z45:v:wn", "z45:v:wn:we:gvd2"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		// setup spawns so they cannot be removed if necro killed
		when (cmd) {
			"xspn" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.NORTH, false, true, false))
			"xsps" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.SOUTH, false, true, false))
			"xspw" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.WEST, false, true, false))
			"xspe" -> setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.EAST, false, true, false))
			else   -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("2R", 270, getQuadrant(0, 0)),
			ZTile("9V", 90, getQuadrant(0, 3)),
			ZTile("3V", 0, getQuadrant(0, 6)),
			ZTile("10V", 270, getQuadrant(3, 0)),
			ZTile("6X", 0, getQuadrant(3, 3, 3, 4)),
			ZTile("11R", 270, getQuadrant(3, 6))
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
		if (c.occupiedZone == greenObjZone) {
			greenObjZone = -1
			game.giftRandomVaultArtifact(c)
		} else if (c.occupiedZone == blueObjZone) {
			blueObjZone = -1
			game.giftRandomVaultArtifact(c)
		}
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.EARTHQUAKE_HAMMER, ZWeaponType.DRAGON_FIRE_BLADE)

	override fun getPercentComplete(game: ZGame): Int {
		val ultraRed = ZSkillLevel(ZColor.RED, 1)
		val numPlayers = game.allCharacters.size
		val numAtUltraRed =game.allCharacters.count { it.character.skillLevel >= ultraRed }
		val total = numPlayers + numStartObjectives
		val completed = numAtUltraRed + numFoundObjectives
		return completed * 100 / total
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (game.board.getAllCharacters().count { it.isDead } > 0) {
			"Not all players survived."
		} else super.getQuestFailedReason(game)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val ultraRed = ZSkillLevel(ZColor.RED, 1)
		val numPlayers = game.allCharacters.size
		val numAtUltraRed = game.allCharacters.count { pl -> pl.character.skillLevel >= ultraRed }
		return Table(name).addRow(Table().setNoBorder()
			.addRow("1.", "Collect all objectives. Some of the objectives give a random vault item", String.format("%d of %d", numFoundObjectives, numStartObjectives))
			.addRow("2.", "Get all survivors to ultra RED danger level", String.format("%d of %d", numAtUltraRed, numPlayers))
			.addRow("3.", "Only spawns from Necromancers can be removed", "")
		)
	}
}