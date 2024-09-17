package cc.lib.zombicide.quests

import cc.lib.game.GColor
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.ZBoard
import cc.lib.zombicide.ZCell
import cc.lib.zombicide.ZCellType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZMove
import cc.lib.zombicide.ZMove.Companion.newObjectiveMove
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZTile.Companion.getQuadrant

/**
 * Created by Chris Caron on 9/1/21.
 */
class WolfACoinForTheFerryman : ZQuest(ZQuests.A_Coin_For_The_Ferryman) {
	companion object {
		init {
			addAllFields(WolfACoinForTheFerryman::class.java)
		}
	}

	var lockedDoors: MutableList<ZDoor> = ArrayList()
	val isBlueKeyFound : Boolean
		get() = lockedDoors.isEmpty()
	lateinit var blueKeyPos: Grid.Pos
	override fun loadBoard(): ZBoard {
		val map = arrayOf(
					arrayOf("z0:i:ww:ws", "z1", "z2:i::ww::ws:de:red",      "z10", "z11", "z12",                                "z20", "z21:spn", "z22:i:ww"),
					arrayOf("z3", "z4", "z5",                               "z13", "z14:i:wn::lde:ldw:exit", "z15",             "z23:i:dn:ww:we:ods", "z24", "z22:i:ww:ws"),
					arrayOf("z6:i:wn:we:ods", "z7", "z8:i:wn:ww:de:ods",    "z16", "z17", "z18",                                "z25:i:dw:we:red", "z26", "z27"),
					arrayOf("z30:i:we:ws", "z31", "z32:i:ww",               "z40:t3:rn:rw", "z41:t3:blue:rn", "z42:t3:rn:re",   "z50:i:ds:ode", "z51:i:wn:we:ws", "z52"),
					arrayOf("z33", "z34", "z32:i:ww",                       "z43:t3:rw", "z44:t3", "z45:t3:re:rs",              "z53", "z54", "z55"),
					arrayOf("z35:i:wn:we:red", "z36", "z37:v:dw:wn:ws",     "z46:t3:rw:rs:re", "z47:t2:rs", "z48:t1:rs:st",     "z56", "z57:i:wn:dw:ws:ode", "z58:i:wn:ods"),
					arrayOf("z60:i:de:ods", "z61", "z62:i:ww:ws:ode",       "z70:i:ws", "z70:i:ws:ode", "z71:i:de:ws",          "z80", "z81:i:ww:ws:ode", "z82:i:ws"),
					arrayOf("z63:i:we", "z64", "z65",                       "z72", "z73", "z74",                                "z83", "z84", "z85"),
					arrayOf("z63:i:we", "z66:sps", "z67:i:ww:dn:ode",       "z75:i:wn:ode", "z76:i:wn", "z76:i:wn:red:ode",     "z86:i:dn:ode", "z87:i:wn:we", "z88"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"ldw" -> lockedDoors.add(ZDoor(pos, ZDir.WEST, GColor.BLUE))
			"lde" -> lockedDoors.add(ZDoor(pos, ZDir.EAST, GColor.BLUE))
			"blue" ->                 // blue key hidden until all other objectives taken
				blueKeyPos = pos
/*			"red" -> {
				if (numStartObjectives > 0)  // for DEBUG allow only 1
				else {
					super.loadCmd(grid, pos, "st")
				}
				super.loadCmd(grid, pos, cmd)
			}*/
			else   -> super.loadCmd(grid, pos, cmd)
		}
	}

	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("7V", 270, getQuadrant(0, 0)),
			ZTile("6R", 0, getQuadrant(0, 3)),
			ZTile("1R", 0, getQuadrant(0, 6)),
			ZTile("5R", 90, getQuadrant(3, 0)),
			ZTile("10R", 270, getQuadrant(3, 3)),
			ZTile("2R", 90, getQuadrant(3, 6)),
			ZTile("8R", 90, getQuadrant(6, 0)),
			ZTile("4R", 270, getQuadrant(6, 3)),
			ZTile("3V", 180, getQuadrant(6, 6))
		)

	override fun init(game: ZGame) {
		for (door in lockedDoors) game.board.setDoorLocked(door)
	}

	override suspend fun processObjective(game: ZGame, c: ZCharacter) {
		val zIdx = game.board.getCell(blueKeyPos).zoneIndex
		if (zIdx == c.occupiedZone) {
			game.addLogMessage("The PORTAL is unlocked!!")
			game.board.getZone(zIdx).isObjective = false
			for (door in lockedDoors) {
				game.unlockDoor(door)
			}
			lockedDoors.clear()
			return
		}

		super.processObjective(game, c)
		if (numRemainingObjectives == 0) {
			game.addLogMessage("The BLUE key is revealed!!!")
			game.board.setObjective(blueKeyPos, ZCellType.OBJECTIVE_BLUE)
		}
	}

	override suspend fun addMoves(game: ZGame, cur: ZCharacter, options: MutableCollection<ZMove>) {
		super.addMoves(game, cur, options)
		blueKeyPos.let {
			val idx = game.board.getCell(it).zoneIndex
			if (idx == cur.occupiedZone) {
				options.add(newObjectiveMove(idx))
			}
		}
	}

	fun isExitZoneOccupied(game: ZGame): Boolean {
		return numPlayersInExit(game) > 0
	}

	override fun getPercentComplete(game: ZGame): Int {
		val total = numStartObjectives + 2
		val completed = numFoundObjectives + (if (isExitZoneOccupied(game)) 1 else 0) + if (isBlueKeyFound) 1 else 0
		return completed * 100 / total
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Take all of the objective to reveal the BLUE key", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Take the BLUE key to unlock the BLUE doors and the escape portal", isBlueKeyFound)
				.addRow("3.", "Enter the portal and banish the Necromancers for good", isExitZoneOccupied(game))
			)
	}
}