package cc.lib.zombicide.quests

import cc.lib.kreflector.Reflector
import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.Table
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCellEnvironment
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZZombieType

class ZQuestTheRatz : ZQuest(ZQuests.The_Ratz) {

	companion object {
		init {
			Reflector.addAllFields(ZQuestTheRatz::class.java)
		}
	}

	override fun loadBoard() = load(
		arrayOf(
			arrayOf("z0:ds", "z1:ws", "z2:ws:ratz", "z4:de:ws", "z5"),
			arrayOf("z6:we", "z7:ws:ratz", "z8:ds:we", "z9:we:ratz", "z10"),
			arrayOf("z11:we:ratz", "z12:de", "z13:de:ds:st", "z14:ws:we", "z15:ratz"),
			arrayOf("z16:we", "z17:ds:we:ratz", "z18:ws", "z19:de:ws:ratz", "z20:ws"),
			arrayOf("z21:we", "z22", "z23:ratz", "z24", "z25")
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

	override fun handleSpawnForZone(game: ZGame, zoneIdx: Int): Boolean {
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