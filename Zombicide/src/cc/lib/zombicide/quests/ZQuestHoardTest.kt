package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZZombieType

/**
 * Created by Chris Caron on 4/22/23.
 */
class ZQuestHoardTest : ZQuest(ZQuests.Hoard_Test) {

	override fun loadBoard(): ZBoard = load(arrayOf(
		arrayOf("z0:ww:wn:x", "z1:wn:x", "z2:wn:we:x"),
		arrayOf("z3:ww:x", "z4:start:ww:ws:we", "z5:we:x"),
		arrayOf("z6:ww:ws:x", "z7:ws:x", "z8:ws:we:x")
	))

	val spawns = mutableListOf<ZCell>()

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"x" -> spawns.add(grid.get(pos))
			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile> = arrayOf()

	override fun init(game: ZGame) {
		spawns.forEach {
			game.spawnZombies(6, ZZombieType.Walker, it.zoneIndex)
		}
	}

	override fun getPercentComplete(game: ZGame): Int = 0

	override fun getObjectivesOverlay(game: ZGame): Table = Table().addColumn("Zombie Hoard", "Test Zombie Movement")
}