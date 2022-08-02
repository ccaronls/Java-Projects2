package cc.lib.zombicide.quests

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZTile.Companion.getQuadrant
import cc.lib.zombicide.ui.UIZombicide

class ZQuestDeadTrail : ZQuest(ZQuests.Dead_Trail) {
	companion object {
		const val NUM_VAULT_ITEMS = 2

		init {
			addAllFields(ZQuestDeadTrail::class.java)
		}
	}

	var blueKeyZone = -1
	var greenKeyZone = -1
	var violetVault1: ZDoor? = null
	var violetVault2: ZDoor? = null
	var goldVault: ZDoor? = null
	override val tiles: Array<ZTile>
		get() = arrayOf(
			ZTile("5V", 0, getQuadrant(0, 0)),
			ZTile("1R", 90, getQuadrant(0, 3)),
			ZTile("2R", 0, getQuadrant(0, 6)),
			ZTile("3V", 90, getQuadrant(3, 0)),
			ZTile("6V", 0, getQuadrant(3, 3)),
			ZTile("4R", 90, getQuadrant(3, 6)))

	override fun loadBoard(): ZBoard {
		val map = arrayOf(
arrayOf("z0:i:de:ods", "z1:violet_vd1", "z2:i:dw:ods:ode",      "z3:i:red:ws:ode", "z4:i:de:ws", "z5",                  "z6:start", "z7", "z8:i:dw:ods"),
arrayOf("z9:i:ods", "z9:i:wn:ws", "z9:i:we:ods",                "z10", "z11", "z12",                                    "z13:i:wn:ww:we:ods", "z14", "z15:i:red:ww:ws"),
arrayOf("z16:i:we:ods", "z17", "z18:i:ww:ds:de",                "z19", "z20:i:dw:wn:ws", "z20:i:wn:ode:ods",            "z21:i:we:ods", "z22", "z23:spe"),
arrayOf("z24:i:we:ods", "z25", "z26",                           "z27", "z28:i:ww:ws:ode", "z29:i:gold_vd2:red:ws:ode", "z30:i:ds", "z30:i:wn:ws:ode", "z31:i:dn:ws"),
arrayOf("z32:i:ws:we:red", "z33", "z34:i:dn:ww:we:ods",         "z35", "z36", "z37",                                    "z38", "z39", "z40:violet_vd3:spe"),
arrayOf("z41:ws:exit", "z42:sps:ws", "z43:ww:i:ws:ode",         "z44:i:red:wn:ws:ode", "z45:i:ws:de:wn", "z46:sps:ws", "z47:i:dw:wn:ws:ode", "z48:i:wn:ws", "z48:i:red:wn:ws"),
arrayOf("", "", "",                                             "", "z49:v:vd1:ww", "z49:v",                            "z49:v:vd3:we", "z50:v:gvd2", "z50:v"))
		return load(map)
	}

	override fun loadCmd(grid: Grid<ZCell>, pos: Grid.Pos, cmd: String) {
		when (cmd) {
			"violet_vd1" -> super.loadCmd(grid, pos, "vd1")
			"violet_vd3" -> super.loadCmd(grid, pos, "vd3")
			"gold_vd2" -> super.loadCmd(grid, pos, "gvd2")
			else         -> super.loadCmd(grid, pos, cmd)
		}
	}

	override fun init(game: ZGame) {
		assert(redObjectives.size > 1)
		violetVault1 = game.board.findVault(1)
		violetVault2 = game.board.findVault(3)
		goldVault = game.board.findVault(2)
		while (greenKeyZone == blueKeyZone) {
			greenKeyZone = redObjectives.random()
			blueKeyZone = redObjectives.random()
		}
		game.lockDoor(violetVault1!!)
		game.lockDoor(violetVault2!!)
		game.lockDoor(goldVault!!)
	}

	override fun processObjective(game: ZGame, c: ZCharacter) {
		super.processObjective(game, c)
		if (c.occupiedZone == blueKeyZone) {
			game.addLogMessage(c.name() + " has found the BLUE key. Violet Vault doos UNLOCKED!")
			game.unlockDoor(violetVault1!!)
			game.unlockDoor(violetVault2!!)
			blueKeyZone = -1
		} else if (c.occupiedZone == greenKeyZone) {
			game.addLogMessage(c.name() + " has found the GREEN key.")
			greenKeyZone = -1
		}
		if (game.isDoorLocked(goldVault!!) && blueKeyZone < 0 && greenKeyZone < 0) {
			game.addLogMessage("Gold Vault door UNLOCKED")
			game.unlockDoor(goldVault!!)
		}
	}

	override fun getPercentComplete(game: ZGame): Int {
		val numTasks = numStartObjectives + NUM_VAULT_ITEMS + 1
		var numComplete = numFoundObjectives
		numComplete += numFoundVaultItems
		if (isAllPlayersInExit(game)) numComplete++
		return numComplete * 100 / numTasks
	}

	override fun getQuestFailedReason(game: ZGame): String? {
		return if (numDeadPlayers(game) > 0) {
			"Not all players survived"
		} else super.getQuestFailedReason(game)
	}

	override fun getObjectivesOverlay(game: ZGame): Table {
		return Table(name)
			.addRow(Table().setNoBorder()
				.addRow("1.", "Take all Objectives", String.format("%d of %d", numFoundObjectives, numStartObjectives))
				.addRow("2.", "Key to Violet Vault is hidden among the RED objectives", if (blueKeyZone == -1) "Found" else "Not Found")
				.addRow("3.", "Key to Gold Vault is hidden among the RED objectives", if (greenKeyZone == -1) "Found" else "Not Found")
				.addRow("4.", "Take all vault artifacts", String.format("%d of %d", numFoundVaultItems, NUM_VAULT_ITEMS))
				.addRow("5.", "Get all survivors to the exit zone")
			)
	}

	override fun drawQuest(game: UIZombicide, g: AGraphics) {
		if (true) {
			return
		}
		if (blueKeyZone >= 0) {
			val z = game.board.getZone(blueKeyZone)
			for (p in z.getCells()) {
				val cell = game.board.getCell(p)
				if (cell.isCellType(ZCellType.OBJECTIVE_RED)) {
					val redX = GRectangle(cell).scaledBy(.2f, .2f)
					g.color = GColor.BLUE
					g.drawLine(redX.topLeft, redX.bottomRight, 5f)
					g.drawLine(redX.topRight, redX.bottomLeft, 5f)
				}
			}
		}
		if (greenKeyZone >= 0) {
			val z = game.board.getZone(greenKeyZone)
			for (p in z.getCells()) {
				val cell = game.board.getCell(p)
				if (cell.isCellType(ZCellType.OBJECTIVE_RED)) {
					val redX = GRectangle(cell).scaledBy(.2f, .2f)
					g.color = GColor.GREEN
					g.drawLine(redX.topLeft, redX.bottomRight, 5f)
					g.drawLine(redX.topRight, redX.bottomLeft, 5f)
				}
			}
		}
	}
}