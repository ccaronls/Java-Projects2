package cc.game.zombicide.quests

import cc.game.zombicide.ZBoard
import cc.game.zombicide.ZCell
import cc.game.zombicide.ZGame
import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZQuests
import cc.game.zombicide.ZTile
import cc.game.zombicide.ZZombieType
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.utils.launchIn

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
		launchIn {
			spawns.forEach {
				game.spawnZombies(6, ZZombieType.Walker, it.zoneIndex)
			}
		}
	}

	override fun getPercentComplete(game: ZGame): Int = 0

	override fun getObjectivesOverlay(game: ZGame): Table = Table().addColumn("Zombie Hoard", "Test Zombie Movement")
}