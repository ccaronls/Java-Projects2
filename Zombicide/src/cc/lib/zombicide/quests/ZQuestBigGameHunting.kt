package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.Table
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCellType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZEquipmentType
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZItemType
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ZWeaponType
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ZZone

class ZQuestBigGameHunting : ZQuest(ZQuests.Big_Game_Hunting) {
	companion object {
		init {
			addAllFields(ZQuestBigGameHunting::class.java)
		}
	}

	lateinit var blueObjCell: Pos
	var blueRevealZone = -1
	var skipKillAbomination = false

	override fun loadBoard() = load(
		arrayOf(
			arrayOf(
				"z0:i:wn:ww:ds:gvd1",
				"z29:i:wn:de:ws:red:odw",
				"z1:spn:wn:de",
				"z2:i:wn:ode:ws",
				"z3:i:wn:ode:ws",
				"z9:i:vd2:wn:we",
				"z28:v:wn:we:vd2"
			),
			arrayOf("z5:ww", "z6", "z7", "z8:we", "z9:i:ods", "z9:i:ods:we:red", "z28:v:we"),
			arrayOf(
				"z10:ww:we:start",
				"z11:i:ww:wn::ws:red:ode",
				"z12:i:wn:ds:ode",
				"z13:i:wn:red:ds:ode",
				"z14:i:ws:we:odn",
				"z15:i:ds:we:odn",
				"z28:v:we:ws:vd3"
			),
			arrayOf("z16:ww:ds", "z17", "z18", "z19", "z20", "z21:we:spe:dn:ds", "z27:v:we:gvd1"),
			arrayOf("z22:i:ww:we:vd3", "z23", "z24:i:wn:ww:we", "z25:i:wn", "z25:i:wn", "z25:i:dn:we", "z27:v:we"),
			arrayOf(
				"z22:i:ww:red:ws:de",
				"z26:ws:sps:de",
				"z24:i:red:ww:we:ws:dw",
				"z25:i:ws",
				"z25:i:blue:ws",
				"z25:i:ws:gvd4:we",
				"z27:v:we:ws:gvd4"
			)
		)
	)

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		// check for necro / abom in special spawn places
		game.board.getZone(c.occupiedZone).isObjective = false
		if (c.occupiedZone == blueRevealZone) {
			game.addLogMessage("The Laboratory objective is revealed!")
			val blueObjZone = game.board.getZone(blueObjCell)!!
			blueObjZone.isObjective = true
			game.board.getCell(blueObjCell).apply {
				setCellType(ZCellType.OBJECTIVE_BLUE, true)
				addObjective(ZCellType.OBJECTIVE_BLUE, zoneIndex)
				game.spawnZombies(1, ZZombieType.Necromancer, zoneIndex)
			}
			blueRevealZone = -1
		}
		if (redObjectives.size == 0 && game.getNumKills(ZZombieType.Abomination) == 0) {
			if (game.board.getAllZombies().count { it.type === ZZombieType.Abomination } === 0) {
				// spawn an abomination somewhere far form where all the characters are
				val spawnZones = game.board.getSpawnZones()
				if (spawnZones.isNotEmpty()) {
					val zone: ZZone = spawnZones.random()
					game.spawnZombies(1, ZZombieType.Abomination, zone.zoneIndex)
				} else {
					skipKillAbomination = true
				}
			}
		}
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Pos, cmd: String) {
		when (cmd) {
			"blue" -> {
				blueObjCell = pos
			}

			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		blueRevealZone = redObjectives.random()
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("2R", 90, getQuadrant(0, 0)),
			ZTile("8V", 180, getQuadrant(0, 3)),
			ZTile("9V", 90, getQuadrant(3, 0)),
			ZTile("1V", 90, getQuadrant(3, 3)))

	override fun getObjectivesOverlay(game: ZGame): Table {
		val exposeLaboratory = blueRevealZone < 0
		val necroKilled = game.getNumKills(ZZombieType.Necromancer) > 0
		val abomKilled = game.getNumKills(ZZombieType.Abomination) > 0
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Collect all objectives. One of the objectives\nexposes the laboratory objective.", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Find the Laboratory Objective", exposeLaboratory)
				.addRow("3.", "Kill at least 1 Necromancer.", necroKilled)
				.addRow("4.", "Kill at least 1 Abomination.", abomKilled)
				.addRow("5.", "Not all players need to survive.")
			)
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjectives + 2
		var numCompleted = numFoundObjectives
		if (skipKillAbomination || game.getNumKills(ZZombieType.Abomination) > 0) numCompleted++
		if (game.getNumKills(ZZombieType.Necromancer) > 0) numCompleted++
		return numCompleted * 100 / numTasks
	}

	override val allVaultOptions: List<ZEquipmentType>
		get() = listOf(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE)
}