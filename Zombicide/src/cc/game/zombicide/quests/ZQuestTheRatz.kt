package cc.game.zombicide.quests

import cc.game.zombicide.ZCell
import cc.game.zombicide.ZCellType
import cc.game.zombicide.ZGame
import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZQuests
import cc.game.zombicide.ZZombieType
import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.Table

class ZQuestTheRatz : ZQuest(ZQuests.The_Ratz) {

	private var numRatzToKill = 0
	private var numDoors = 0

	companion object {
		init {
			addAllFields(ZQuestTheRatz::class.java)
		}
	}

	override fun loadBoard() = load(
		arrayOf(
			arrayOf("z0:i:ds", "z1:i:ws", "z2:i:ws:ratz", "z3:i:de:ws", "z4:i"),
			arrayOf("z5:i:we", "z6:i:ws:ratz", "z7:i:ds:we", "z8:i:we:ratz", "z9:i"),
			arrayOf("z10:i:we:ratz", "z11:i:de", "z12:de:ds:st", "z13:i:ws:we", "z14:i:ratz"),
			arrayOf("z15:i:we", "z16:i:ds:we:ratz", "z17:i:ws", "z18:i:de:ws:ratz", "z19:i:ws"),
			arrayOf("z20:i:we", "z21:i", "z22:i:ratz", "z23:i", "z24:i")
		)
	)

	override fun loadCmd(grid: Grid<ZCell>, pos: Pos, cmd: String) {
		when (cmd) {
			"ratz" -> {
				numRatzToKill++
				grid[pos].setCellType(ZCellType.RATZ, true)
			}

			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		val doors = game.board.getDoors()
		val opened = doors.count { !it.isClosed(game.board) } / 2
		val ratzLeft = game.board.getAllZombies(ZZombieType.Ratz).size
		val tasks = numDoors + numRatzToKill
		val completed = opened + (numRatzToKill - ratzLeft)
		return completed * 100 / tasks
	}

	override fun init(game: ZGame) {
		numDoors = game.board.getDoors().size / 2 // need to ahlve the size since we get doubles
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		val doors = game.board.getDoors()
		val closed = doors.count { it.isClosed(game.board) } / 2
		val ratz = game.board.getAllZombies(ZZombieType.Ratz).size
		return Table(name)
			.addRow(
				Table().setNoBorder()
					.addRow("Open All Doors", String.format("%d of %d", (numDoors - closed) / 2, numDoors))
					.addRow("Kill all Ratz", String.format("%d of %d", (numRatzToKill - ratz), numRatzToKill))
			)
	}


}