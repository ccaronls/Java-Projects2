package cc.game.zombicide.quests

import cc.game.zombicide.ZCell
import cc.game.zombicide.ZCellEnvironment
import cc.game.zombicide.ZGame
import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZQuests
import cc.game.zombicide.ZTile
import cc.game.zombicide.ZZombieType
import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.Table

class ZQuestTheRatz : ZQuest(ZQuests.The_Ratz) {

	companion object {
		init {
			addAllFields(ZQuestTheRatz::class.java)
		}
	}

	override fun loadBoard() = load(
		arrayOf(
			arrayOf("z0:ds", "z1:ws", "z2:ws:ratz", "z3:de:ws", "z4"),
			arrayOf("z5:we", "z6:ws:ratz", "z7:ds:we", "z8:we:ratz", "z9"),
			arrayOf("z10:we:ratz", "z11:de", "z12:de:ds:st", "z13:ws:we", "z14:ratz"),
			arrayOf("z15:we", "z16:ds:we:ratz", "z17:ws", "z18:de:ws:ratz", "z19:ws"),
			arrayOf("z20:we", "z21", "z22:ratz", "z23", "z24")
		)
	)

	val ratz = HashSet<Pos>()

	override fun loadCmd(grid: Grid<ZCell>, pos: Pos, cmd: String) {
		grid[pos].environment = ZCellEnvironment.BUILDING
		when (cmd) {
			"ratz" -> {
				ratz.add(pos)
			}

			else -> super.loadCmd(grid, pos, cmd)
		}
	}

	override suspend fun handleSpawnForZone(game: ZGame, zoneIdx: Int): Boolean {
		game.board.getZone(zoneIdx).getCells().forEach {
			if (ratz.contains(it)) {
				game.spawnZombies(1, ZZombieType.Ratz, zoneIdx)
			}
		}
		return true
	}

	override fun getPercentComplete(game: ZGame): Int {
		val doors = game.board.getDoors()
		val closed = doors.count { it.isClosed(game.board) }
		val ratz = game.board.getAllZombies(ZZombieType.Ratz).size
		return doors.size - closed - ratz
	}

	override val tiles: Array<ZTile> = emptyArray()

	override fun init(game: ZGame) {}
	override fun getObjectivesOverlay(game: ZGame): Table {
		val doors = game.board.getDoors()
		val closed = doors.count { it.isClosed(game.board) }
		val ratz = closed == doors.size && game.board.getAllZombies(ZZombieType.Ratz).size == 0
		return Table(name)
			.addRow(
				Table().setNoBorder()
					.addRow("Open All Doors", String.format("%d of %d", (doors.size - closed) / 2, doors.size / 2))
					.addRow("Kill all Ratz", if (ratz) "No" else "Yes")
			)
	}


}