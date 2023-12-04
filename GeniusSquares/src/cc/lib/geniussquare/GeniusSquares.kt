package cc.lib.geniussquare

import cc.lib.game.GColor
import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector
import cc.lib.utils.Grid
import cc.lib.utils.StopWatch

/*
PIECE_0(null, 0, 0, null), // this way the matrix value aligns with ordinal()
        PIECE_1x1(GColor.BLUE, 0, 0, new int [][][] {
                {{ 1 }}
        }), // matrices are row major
        PIECE_2x2(GColor.GREEN, 1, 0, new int [][][] {
                {{ 2, 2 }, { 2, 2 }}
        }),
        PIECE_1x2(GColor.BROWN, 0, 2, new int [][][] {
                {{ 3, 3 }}, {{ 3 }, { 3 }}
        }),
        PIECE_1x3(GColor.ORANGE, 0, 3, new int [][][] {
                {{ 4, 4, 4 }}, {{ 4 }, { 4 }, { 4 }}
        }),
        PIECE_1x4(GColor.GRAY, 0, 7, new int [][][] {
                {{ 5, 5, 5, 5 }},
                {{ 5 }, { 5 }, { 5 }, { 5 }}
        }),
        PIECE_EL(GColor.CYAN, 2, 0, new int [][][] {
                {{ 0, 6 }, { 0, 6 }, { 6, 6 }},
                {{ 6, 6 }, { 0, 6 }, { 0, 6 }},
                {{ 6, 6 }, { 6, 0 }, { 6, 0 }},
                {{ 6, 0 }, { 6, 0 }, { 6, 6 }},
                {{ 6, 0, 0 }, { 6, 6, 6 }},
                {{ 0, 0, 6 }, { 6, 6, 6 }},
                {{ 6, 6, 6 }, { 6, 0, 0 }},
                {{ 6, 6, 6 }, { 0, 0, 6 }}
        }),
        PIECE_BEND(GColor.MAGENTA, 2, 3, new int [][][] {
                {{ 0, 7 }, { 7, 7 }},
                {{ 7, 0 }, { 7, 7 }},
                {{ 7, 7 }, { 0, 7 }},
                {{ 7, 7 }, { 7, 0 }}}),
        PIECE_TEE(GColor.YELLOW, 0, 4, new int [][][] {
                {{ 8, 0 }, { 8, 8 }, { 8, 0 }}, // T
                {{ 0, 8 }, { 8, 8 }, { 0, 8 }}, // Upside down T
                {{ 8, 8, 8 }, { 0, 8, 0 }}, // sideways pointing right
                {{ 0, 8, 0 }, { 8, 8, 8 }}, // sideways pointing left
        }),

        PIECE_STEP(GColor.RED, 1, 5, new int [][][]  {{{ 0, 9, 9 }, { 9, 9, 0 }},
                {{ 9, 9, 0 }, { 0, 9, 9 }},
                {{ 9, 0 }, { 9, 9 }, { 0, 9 }},
                {{ 0, 9 }, { 9, 9 }, { 9, 0 }}}),
        PIECE_CHIT(new GColor(0xFFDBB584), 0, 0, new int [][][] {{{ 10 }}}),
        ;

 */

enum class PieceType(val color: GColor, val startX: Int, val startY: Int, val orientations: Array<Array<Array<Int>>>) {
	PIECE_0(GColor.TRANSPARENT, 0, 0, arrayOf(arrayOf(arrayOf(0)))),
	PIECE_1x1(GColor.BLUE, 0, 0, arrayOf(arrayOf(arrayOf(1)))),
	PIECE_2x2(GColor.GREEN, 1, 0, arrayOf(arrayOf(
		arrayOf(2, 2), arrayOf(2, 2)))), // BOX
	PIECE_1x2(GColor.BROWN, 0, 2, arrayOf(
		arrayOf(arrayOf(3, 3)), // horz
		arrayOf(arrayOf(3), arrayOf(3)))), // vert
	PIECE_1x3(GColor.ORANGE, 0, 3, arrayOf(
		arrayOf(arrayOf(4, 4, 4)), // horz
		arrayOf(arrayOf(4), arrayOf(4), arrayOf(4)))), // vert
	PIECE_1x4(GColor.GRAY, 0, 7, arrayOf(
		arrayOf(arrayOf(5, 5, 5, 5)), // horz
		arrayOf(arrayOf(5), arrayOf(5), arrayOf(5), arrayOf(5)))), // vert
	PIECE_EL(GColor.CYAN, 2, 0, arrayOf(
		arrayOf(arrayOf(0, 6), arrayOf(0, 6), arrayOf(6, 6)), // backward L
		arrayOf(arrayOf(6, 6), arrayOf(0, 6), arrayOf(0, 6)), // upside down backward L
		arrayOf(arrayOf(6, 6), arrayOf(6, 0), arrayOf(6, 0)), // upside down forward L
		arrayOf(arrayOf(6, 0), arrayOf(6, 0), arrayOf(6, 6)), // forward L
		arrayOf(arrayOf(6, 0, 0), arrayOf(6, 6, 6)), // backward L rotated CW 90 degrees
		arrayOf(arrayOf(0, 0, 6), arrayOf(6, 6, 6)), // L rotated CCW 90 degrees
		arrayOf(arrayOf(6, 6, 6), arrayOf(6, 0, 0)), // L rotated CW 90 degrees
		arrayOf(arrayOf(6, 6, 6), arrayOf(0, 0, 6)))), // backward L rotated CCW 90 degrees
	PIECE_BEND(GColor.MAGENTA, 2, 3, arrayOf(
		arrayOf(arrayOf(0, 7), arrayOf(7, 7)),  // . *
												// * *
		arrayOf(arrayOf(7, 0), arrayOf(7, 7)),  // * .
												// * *
		arrayOf(arrayOf(7, 7), arrayOf(0, 7)),  // * *
												// . *
		arrayOf(arrayOf(7, 7), arrayOf(7, 0)))),// * *
												// * .
	PIECE_TEE(GColor.YELLOW, 0, 4, arrayOf(
		arrayOf(arrayOf(8, 0), arrayOf(8, 8), arrayOf(8, 0)),
		arrayOf(arrayOf(0, 8), arrayOf(8, 8), arrayOf(0, 8)),
		arrayOf(arrayOf(8, 8, 8), arrayOf(0, 8, 0)),
		arrayOf(arrayOf(0, 8, 0), arrayOf(8, 8, 8)))),
	PIECE_STEP(GColor.RED, 1, 5, arrayOf(
		arrayOf(arrayOf(0, 9, 9), arrayOf(9, 9, 0)),
		arrayOf(arrayOf(9, 9, 0), arrayOf(0, 9, 9)),
		arrayOf(arrayOf(9, 0), arrayOf(9, 9), arrayOf(0, 9)), arrayOf(arrayOf(0, 9),
		arrayOf(9, 9), arrayOf(9, 0)))),
	PIECE_CHIT(GColor(219, 181, 132), 0, 0,
		arrayOf(arrayOf(arrayOf(10))));
}

class Piece @JvmOverloads constructor(val pieceType: PieceType = PieceType.PIECE_0) : Reflector<Piece>() {

	var index = 0
		private set
	val topLeft = MutableVector2D()
	val bottomRight = MutableVector2D()
	var dropped = false
	fun reset() {
		index = 0
		topLeft.set(pieceType.startX.toFloat(), pieceType.startY.toFloat())
		bottomRight.set(topLeft).addEq(width, height)
		dropped = false
	}

	val shape: Array<Array<Int>>
		get() = pieceType.orientations[index]

	fun increment(amt: Int) {
		setIndex((index + amt + pieceType.orientations.size) % pieceType.orientations.size)
	}

	fun setIndex(idx: Int) {
		index = idx
		val center: Vector2D = topLeft.add(bottomRight).scaledBy(0.5f)
		topLeft.set(center).subEq(width / 2, height / 2)
		bottomRight.set(topLeft).addEq(width, height)
	}

	val width: Float
		get() = shape[0].size.toFloat()
	val height: Float
		get() = shape.size.toFloat()
	var center: IVector2D?
		get() = topLeft.add(bottomRight).scaledBy(0.5f)
		set(cntr) {
			topLeft.set(cntr).subEq(width / 2, height / 2)
			bottomRight.set(cntr).addEq(width / 2, height / 2)
		}

	fun getTopLeft(): Vector2D {
		return topLeft
	}

	init {
		reset()
	}
}

open class GeniusSquares : Reflector<GeniusSquares?>() {
	companion object {
		// GeniusSquare. 6x6 board
		val log = LoggerFactory.getLogger(GeniusSquares::class.java)
		var BOARD_DIM_CELLS = 6
		var NUM_BLOCKERS = 7

		init {
			addAllFields(GeniusSquares::class.java)
			addAllFields(Piece::class.java)
		}
	}

	val pieces: MutableList<Piece> = ArrayList()
	var board: Grid<Int> = Grid<Int>(BOARD_DIM_CELLS, BOARD_DIM_CELLS, 0) // row major
	val timer = newStopWatch()
	var bestTime: Long = 0
	open fun newGame() {
		board.fill(0)
		var i = 0
		while (i < NUM_BLOCKERS) {
			val r = Utils.randRange(0, BOARD_DIM_CELLS - 1)
			val c = Utils.randRange(0, BOARD_DIM_CELLS - 1)
			if (board[r, c] != 0) {
				continue
			}
			board[r, c] = PieceType.PIECE_CHIT.ordinal
			i++
		}
		resetPieces()
		timer.start()
	}

	@Synchronized
	fun resetPieces() {
		pieces.clear()
		for (i in 1..9) {
			val p = Piece(PieceType.values()[i])
			pieces.add(p)
			liftPiece(p)
		}
	}

	fun canDropPiece(p: Piece, cellX: Int, cellY: Int): Boolean {
		if (cellX < 0 || cellY < 0) return false
		val shape = p.shape
		if (cellY + shape.size > BOARD_DIM_CELLS) return false
		if (cellX + shape[0].size > BOARD_DIM_CELLS) return false
		for (y in shape.indices) {
			for (x in shape[y].indices) {
				if (shape[y][x] != 0 && board[cellY + y, cellX + x] != 0)
					return false
			}
		}
		return true
	}

	/**
	 * search through all the possible orientations to fit the piece at cx, cy
	 * @param p
	 * @param cellX
	 * @param cellY
	 * @return 3 ints: orientation, y and x or null if not possible to fit
	 * if not result != null then can call dropPiece(p, result[0], result[1], result[2])
	 */
	fun findDropForPiece(p: Piece, cellX: Int, cellY: Int): Array<Int>? {
		for (s in p.pieceType.orientations.indices) {
			val shapeIndex = (p.index + s) % p.pieceType.orientations.size
			val shape = p.pieceType.orientations[shapeIndex]
			for (y in shape.indices) {
				for (x in shape[y].indices) {
					val tested = Array(shape.size) { Array(shape[0].size) { 0 } }
					if (searchDropPieceR(shape, x, y, cellX, cellY, tested)) {
						return arrayOf(shapeIndex, cellX - x, cellY - y)
					}
				}
			}
		}
		return null
	}

	/**
	 * This version will fit the piece using its current orientation only
	 *
	 * @param p
	 * @param cellX
	 * @param cellY
	 * @return
	 */
	fun findDropForPiece2(p: Piece, cellX: Int, cellY: Int): Array<Int>? {
		val shape = p.shape
		for (y in shape.indices) {
			for (x in shape[y].indices) {
				val tested = Array(shape.size) { Array(shape[0].size) { 0 } }
				if (searchDropPieceR(shape, x, y, cellX, cellY, tested)) {
					return arrayOf(p.index, cellX - x, cellY - y)
				}
			}
		}
		return null
	}

	private fun searchDropPieceR(shape: Array<Array<Int>>, px: Int, py: Int, cellX: Int, cellY: Int, tested: Array<Array<Int>>): Boolean {
		if (px < 0 || py < 0 || py >= shape.size || px >= shape[0].size) return true
		if (tested[py][px] != 0) return true
		tested[py][px] = 1
		if (cellX < 0 || cellY < 0 || cellX >= BOARD_DIM_CELLS || cellY >= BOARD_DIM_CELLS) return false
		return if (shape[py][px] != 0 && board[cellY, cellX] != 0) false else searchDropPieceR(shape, px - 1, py, cellX - 1, cellY, tested) &&
			searchDropPieceR(shape, px + 1, py, cellX + 1, cellY, tested) &&
			searchDropPieceR(shape, px, py - 1, cellX, cellY - 1, tested) &&
			searchDropPieceR(shape, px, py + 1, cellX, cellY + 1, tested)
	}

	@Synchronized
	fun dropPiece(p: Piece, cellX: Int, cellY: Int) {
		log.info("Dropping Piece")
		if (canDropPiece(p, cellX, cellY)) {
			//throw new cc.lib.utils.GException("Logic Error: Cannot drop piece");
			val shape = p.shape
			for (y in shape.indices) {
				for (x in shape[y].indices) {
					if (board[cellY + y, cellX + x] != 0)
						System.err.println("Logic Error: should not be able to drop piece")
					if (shape[y][x] != 0)
						board[cellY + y, cellX + x] = shape[y][x]
				}
			}
			p.dropped = true
		} else {
			log.error("Cannot drop piece at: $cellX, $cellY")
		}
	}

	@Synchronized
	fun liftPiece(p: Piece) {
		log.info("Lifting Piece")
		for (y in 0 until BOARD_DIM_CELLS) {
			for (x in 0 until BOARD_DIM_CELLS) {
				if (board[y, x] == p.pieceType.ordinal) board[y, x] = 0
			}
		}
		p.dropped = false
	}

	val isCompleted: Boolean
		get() {
			for (y in 0 until BOARD_DIM_CELLS) {
				for (x in 0 until BOARD_DIM_CELLS) {
					if (board[y, x] == 0) return false
				}
			}
			timer.pause()
			if (bestTime == 0L || timer.time < bestTime) {
				bestTime = timer.time
			}
			return true
		}

	fun pauseTimer() {
		synchronized(timer) { timer.pause() }
	}

	fun resumeTimer() {
		synchronized(timer) { timer.unpause() }
	}

	open fun newStopWatch() : StopWatch {
		return StopWatch()
	}
}