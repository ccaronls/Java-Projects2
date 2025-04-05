package cc.lib.dungeondice

import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.board.CustomBoard
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import java.util.Arrays
import java.util.LinkedList

class DBoard : CustomBoard<BVertex, BEdge, DCell>() {
	/**
	 * Find all cells player can move to given the die roll
	 *
	 * @param player
	 * @param dieRoll
	 * @return
	 */
	fun findMoves(player: DPlayer, dieRoll: Int): Array<DMove> {
		val moves: MutableList<DMove> = ArrayList()
		dfsFindPaths(player, player.cellIndex, player.backCellIndex, dieRoll, LinkedList(), moves)
		return moves.toTypedArray<DMove>()
	}

	private fun dfsFindPaths(player: DPlayer, rootCell: Int, backCell: Int, num: Int, path: LinkedList<Int>, moves: MutableList<DMove>) {
		path.addLast(rootCell)
		if (num == 0) moves.add(DMove(MoveType.MOVE_TO_CELL, player.playerNum, rootCell, path)) else {
			val root = getCell(rootCell)
			for (adjCell in root.getAdjCells()) {
				if (adjCell == backCell) continue
				val next = getCell(adjCell)
				if (next.cellType == CellType.ROOM) dfsFindPaths(player, adjCell, rootCell, 0, path, moves) else if (next.cellType == CellType.LOCKED_ROOM) {
					if (player.hasKey()) {
						dfsFindPaths(player, adjCell, rootCell, 0, path, moves)
					}
				} else {
					dfsFindPaths(player, adjCell, rootCell, num - 1, path, moves)
				}
			}
		}
		path.removeLast()
	}

	override fun newCell(pts: List<Int>): DCell {
		return DCell(pts, CellType.EMPTY)
	}

	override fun drawCells(g: AGraphics, scale: Float) {
		for (i in 0 until numCells) {
			val cell = getCell(i)
			g.color = cell.cellType.color
			renderCell(cell, g, 0.95f)
			g.drawTriangleFan()
			g.begin()
			if (cell.cellType == CellType.LOCKED_ROOM) {
				// draw black keyhole over cell
				val rect = getCellBoundingRect(i)
				g.pushMatrix()
				g.translate(rect.center)
				g.scale(Math.min(rect.width, rect.height) / 10)
				g.color = GColor.BLACK
				g.drawFilledCircle(0f, -0.5f, 1f)
				g.begin()
				g.vertex(0f, -0.5f)
				g.vertex(-1f, 1.5f)
				g.vertex(1f, 1.5f)
				g.drawTriangles()
				g.end()
				g.popMatrix()
			}
		}
	}

	val startCellIndex: Int
		get() = -1

	fun getCellsOfType(vararg types: CellType): List<Int> {
		val list: MutableList<Int> = ArrayList()
		if (types.size > 1) {
			Arrays.sort(types)
			for (i in 0 until numCells) {
				val cell = getCell(i)
				if (Arrays.binarySearch(types, cell.cellType) >= 0) {
					list.add(i)
				}
			}
		} else if (types.size > 0) {
			for (i in 0 until numCells) {
				val cell = getCell(i)
				if (cell.cellType == types[0]) {
					list.add(i)
				}
			}
		}
		return list
	}
}
