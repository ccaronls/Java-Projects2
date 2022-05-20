package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import java.util.*

/**
 * Created by Chris Caron on 8/27/21.
 */
class WolfQuestImmortal : ZQuest(ZQuests.Immortal) {
	companion object {
		init {
			addAllFields(WolfQuestImmortal::class.java)
		}
	}

	var immortals: MutableList<ZSpawnArea> = ArrayList()
	var numStartImmortals = 0
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:re", "z1:t3", "z1:t3:re",                     "z2:i:ods:we", "z3", "z4:blspe"),
arrayOf("z5:t2:rn", "z1:t3:rs", "z1:t3:re:rs",              "z6:i:ws:de", "z7", "z8:i:dw:wn:ods:red"),
arrayOf("z5:t2:rs", "z9:t1:rs", "z10",                      "z11", "z12", "z13:i:ww"),
arrayOf("z14:spw", "z15", "z16",                            "z17:t2:rw:rn", "z18:t3:rn", "z18:t3:rn"),
arrayOf("z19:spw", "z20:v:wn:vd1:ws:we:ww", "z21:st",       "z22:t1:rw", "z18:t3:rw:rs", "z18:t3:rs"),
arrayOf("z23:spw", "z24", "z25",                            "z26", "z27", "z28"),
arrayOf("z29:i:dn:we:ods", "z30", "z31:i:dn:de:ods:ww:red", "z32:i:dn", "z32:i:wn:we", "z33"),
arrayOf("z34:i:ws:ode", "z35:i:ods:ode:wn", "z36:i:we:ods", "z32:i", "z32:i:red:we", "z40"),
arrayOf("z37:i:vd1", "z37:i:we", "z38:i:de",                "z32:i", "z32:i:we", "z39:blsps"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		var area: ZSpawnArea
		when (cmd) {
			"blsps" -> {
				setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.SOUTH, true, false, true).also { area = it })
				immortals.add(area)
			}
			"blspe" -> {
				setSpawnArea(grid[pos], ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.EAST, true, false, true).also { area = it })
				immortals.add(area)
			}
			else    -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("11R", 0, getQuadrant(0, 0)),
			ZTile("3V", 90, getQuadrant(0, 3)),
			ZTile("6R", 0, getQuadrant(3, 0)),
			ZTile("10V", 180, getQuadrant(3, 3)),
			ZTile("8V", 270, getQuadrant(6, 0)),
			ZTile("1V", 180, getQuadrant(6, 3))
		)

	override fun init(game: ZGame) {
		numStartImmortals = immortals.size
	}

	override fun onZombieSpawned(game: ZGame, zombie: ZZombie, zone: Int) {
		if (zombie.type == ZZombieType.Necromancer) {
			immortals.indexOfFirst { sp: ZSpawnArea ->
				zone == game.board.getCell(sp.cellPos).zoneIndex
			}.takeIf { it >= 0 }?.let { idx ->
				val area = immortals.removeAt(idx)
				area.icon = ZIcon.SPAWN_GREEN
				area.isCanBeRemovedFromBoard = true
				area.isEscapableForNecromancers = false
				area.isCanSpawnNecromancers = false
				game.spawnZombies(idx)
				return
			}
		}
		super.onZombieSpawned(game, zombie, zone)
	}

	fun getNumNecrosOnBoard(game: ZGame): Int {
		return game.board.getAllZombies().count { it.type === ZZombieType.Necromancer }
	}

	override fun getPercentComplete(game: ZGame): Int {
		val total = numStartObjectives + numStartImmortals + getNumNecrosOnBoard(game)
		val completed = numFoundObjectives + (numStartImmortals - immortals.size)
		return completed * 100 / total
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Collect all Objectives. Each objective gives a vault item.", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Purge the EVIL by removing the GREEN spawn areas. BLUE spawn areas become GREEN once a Necromancer is spawned.", String.format("%d of %d", numStartImmortals - immortals.size, numStartImmortals))
				.addRow("3.", "Random Vault weapon hidden in the Vault.", numFoundVaultItems > 0)
			)
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		game.giftRandomVaultArtifact(c)
	}
}