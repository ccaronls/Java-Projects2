package cc.lib.zombicide.quests

import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZTile

/**
 * Created by Chris Caron on 12/29/23.
 */
class GreenHoardTheEvilShrine : ZQuest(ZQuests.The_Evil_Shrine) {

	companion object {
		init {
			addAllFields(GreenHoardTheEvilShrine::class.java)
		}
	}

	var numStartSpawnZones: Int = 0
	var numDestroyedSpanZones: Int = 0
	var numPlayers: Int = 0
	var blueObjectiveTaken = false
	var greenObjectiveTaken = false
	var redSpawnZoneDestroyed = false

	override fun loadBoard(): ZBoard = load(
		arrayOf(
			arrayOf("z0:he:hs", "z1:w:bluspn:he", "z2:ws", "z3:ws", "z4:st:we", "z5:i:ods"),
			arrayOf("z6:hs", "z7:w:we", "z8:i:we:ws", "z9:i:we:ws", "z10:de", "z11:i:blue:ods"),
			arrayOf("z12:he:catapult:ds", "z13:w:le", "z14:ds", "z15:ds", "z16:we", "z17:i:ods"),
			arrayOf("z18:i:red:we:ds", "z19:w:we", "z20:i:ws:ode", "z21:i:we:ws", "z22:we", "z23:i:ws"),
			arrayOf("z24:ds", "z25:w:ls:lw", "z26:w:ws", "z27:w:ws", "z28:w", "z29:grnspe:ws"),
			arrayOf("z30:i:red:de", "z31:sps:de", "z32:exit:i:ode", "z33:i:we", "z34:w:we", "z35:i:hoard")
		)
	)

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"bluspn" -> setSpawnArea(
				grid[pos],
				ZSpawnArea(
					cellPos = pos,
					icon = ZIcon.SPAWN_BLUE,
					dir = ZDir.NORTH,
					isCanBeDestroyedByCatapult = false
				)
			)

			"grnspe" -> setSpawnArea(
				grid[pos],
				ZSpawnArea(
					cellPos = pos,
					icon = ZIcon.SPAWN_GREEN,
					dir = ZDir.EAST,
					isCanBeDestroyedByCatapult = false
				)
			)

			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("20V", 270, ZTile.getQuadrant(0, 0)),
			ZTile("16R", 0, ZTile.getQuadrant(0, 3)),
			ZTile("18R", 90, ZTile.getQuadrant(3, 0)),
			ZTile("17R", 270, ZTile.getQuadrant(3, 3))
		)

	override fun init(game: ZGame) {
		numStartSpawnZones = game.board.getSpawnZones().size
	}

	override fun getPercentComplete(game: ZGame): Int {
		var total = numStartSpawnZones + numPlayers
		var current = numDestroyedSpanZones + numPlayersInExit(game)
		return current * 100 / total
	}

	override fun getObjectivesOverlay(game: ZGame): Table = Table(name).addRow(
		Table().setNoBorder()
			.addRow(
				"1.",
				"Prevent invasion in the vicinity. Destroy all spawn zones",
				"$numDestroyedSpanZones of $numStartSpawnZones"
			)
			.addRow(
				"2.",
				"Destroy the BLUE spawn zone by taking the BLUE objective",
				blueObjectiveTaken
			)
			.addRow(
				"3.",
				"Destroy the GREEN spawn zone by taking the GREEN objective.\nThe green objective is hidden among the RED objectives",
				greenObjectiveTaken
			)
			.addRow(
				"4.",
				"Use the Siege Engine to destroy the RED spawn zone",
				redSpawnZoneDestroyed
			)
			.addRow(
				"4.",
				"Adventure awaits. Reach the Exit with all survivors",
				"${numPlayersInExit(game)} of $numPlayers"
			)
	)
}