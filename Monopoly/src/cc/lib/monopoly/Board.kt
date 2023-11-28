package cc.lib.monopoly

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.math.Vector2D

class Board(  // dimension of whole board
	private val dim: Float) {
	val scale: Float
	private val borderDim // dimension of a corner square
		: Float
	private val cellDim // width of short side of a rect
		: Float

	fun getSqaureBounds(sq: Square): GRectangle {
		var index = sq.ordinal
		when (sq) {
			Square.FREE_PARKING -> return GRectangle(0f, 0f, borderDim, borderDim)
			Square.GOTO_JAIL -> return GRectangle(dim - borderDim, 0f, borderDim, borderDim)
			Square.GO -> return GRectangle(dim - borderDim, dim - borderDim, borderDim, borderDim)
			Square.VISITING_JAIL -> return GRectangle(0f, dim - borderDim, borderDim, borderDim)
			else -> Unit
		}
		if (index < Square.VISITING_JAIL.ordinal) {
			return GRectangle(dim - borderDim - cellDim * index, dim - borderDim, cellDim, borderDim)
		}
		if (index < Square.FREE_PARKING.ordinal) {
			index -= Square.VISITING_JAIL.ordinal
			return GRectangle(0f, dim - borderDim - cellDim * index, borderDim, cellDim)
		}
		if (index < Square.GOTO_JAIL.ordinal) {
			index -= Square.FREE_PARKING.ordinal
			return GRectangle(borderDim + (index - 1) * cellDim, 0f, cellDim, borderDim)
		}
		index -= Square.GOTO_JAIL.ordinal
		return GRectangle(dim - borderDim, borderDim + (index - 1) * cellDim, borderDim, cellDim)
	}

	/**
	 * Gives the center of the inner edge of the square unless sq is a corner then it gives the inner corner
	 * @param sq
	 * @return
	 */
	fun getInnerEdge(sq: Square): Vector2D {
		var index = sq.ordinal
		when (sq) {
			Square.FREE_PARKING -> return Vector2D(borderDim, borderDim)
			Square.GOTO_JAIL -> return Vector2D(dim - borderDim, borderDim)
			Square.GO -> return Vector2D(dim - borderDim, dim - borderDim)
			Square.VISITING_JAIL -> return Vector2D(borderDim, dim - borderDim)
			else -> Unit
		}
		if (index < Square.VISITING_JAIL.ordinal) {
			return Vector2D(dim - borderDim - cellDim * index + cellDim / 2, dim - borderDim)
		}
		if (index < Square.FREE_PARKING.ordinal) {
			index -= Square.VISITING_JAIL.ordinal
			return Vector2D(borderDim, dim - borderDim - cellDim * index + cellDim / 2)
		}
		if (index < Square.GOTO_JAIL.ordinal) {
			index -= Square.FREE_PARKING.ordinal
			return Vector2D(borderDim + (index - 1) * cellDim + cellDim / 2, borderDim)
		}
		index -= Square.GOTO_JAIL.ordinal
		return Vector2D(dim - borderDim, borderDim + (index - 1) * cellDim + cellDim / 2)
	}

	val pieceDimension: Float
		get() = cellDim / 2

	fun getPiecePlacement(player: Int, sq: Square): GRectangle {
		val dim = pieceDimension
		val rect = getSqaureBounds(sq)
		val cntr: Vector2D = rect.center
		if (sq == Square.VISITING_JAIL) {
			// special case to place around the outer edge
			when (player) {
				0 -> return GRectangle(rect.x, cntr.Y() - dim, dim, dim)
				1 -> return GRectangle(cntr.X() - dim, rect.y + rect.h - dim, dim, dim)
				2 -> return GRectangle(rect.x, cntr.y, dim, dim)
			}
			// bottom right
			return GRectangle(cntr.X(), rect.y + rect.h - dim, dim, dim)
		}
		when (player) {
			0 -> return GRectangle(cntr.X() - dim, cntr.Y() - dim, dim, dim)
			1 -> return GRectangle(cntr.X(), cntr.Y(), dim, dim)
			2 -> return GRectangle(cntr.X(), cntr.Y() - dim, dim, dim)
		}
		return GRectangle(cntr.X() - dim, cntr.Y(), dim, dim)
	}

	fun getPiecePlacementJail(playerNum: Int): GRectangle {
		val rect = getSqaureBounds(Square.VISITING_JAIL)
		var dim = rect.w / 4
		rect.x += dim
		rect.w -= dim
		rect.h -= dim
		dim = pieceDimension
		rect.w = dim
		rect.h = dim
		when (playerNum) {
			3 -> rect.y += dim
			1 -> {
				rect.x += dim
				rect.y += dim
			}
			2 -> rect.x += dim
		}
		return rect
	}

	enum class Position {
		CORNER_TL,
		TOP,
		CORNER_TR,
		RIGHT,
		CORNER_BR,
		BOTTOM,
		CORNER_BL,
		LEFT
	}

	// TODO: Make this apart of Square enum
	fun getSquarePosition(sq: Square): Position = when (sq) {
		Square.GO -> Position.CORNER_BR
		Square.MEDITERRANEAN_AVE, Square.COMM_CHEST1, Square.BALTIC_AVE, Square.INCOME_TAX, Square.READING_RAILROAD, Square.ORIENTAL_AVE, Square.CHANCE1, Square.VERMONT_AVE, Square.CONNECTICUT_AVE -> Position.BOTTOM
		Square.VISITING_JAIL -> Position.CORNER_BL
		Square.ST_CHARLES_PLACE, Square.ELECTRIC_COMPANY, Square.STATES_AVE, Square.VIRGINIA_AVE, Square.PENNSYLVANIA_RAILROAD, Square.ST_JAMES_PLACE, Square.COMM_CHEST2, Square.TENNESSEE_AVE, Square.NEW_YORK_AVE -> Position.LEFT
		Square.FREE_PARKING -> Position.CORNER_TL
		Square.KENTUCKY_AVE, Square.CHANCE2, Square.INDIANA_AVE, Square.ILLINOIS_AVE, Square.B_AND_O_RAILROAD, Square.ATLANTIC_AVE, Square.VENTNOR_AVE, Square.WATER_WORKS, Square.MARVIN_GARDINS -> Position.TOP
		Square.GOTO_JAIL -> Position.CORNER_TR
		Square.PACIFIC_AVE, Square.NORTH_CAROLINA_AVE, Square.COMM_CHEST3, Square.PENNSYLVANIA_AVE, Square.SHORT_LINE_RAILROAD, Square.CHANCE3, Square.PARK_PLACE, Square.LUXURY_TAX, Square.BOARDWALK -> Position.RIGHT
	}

	companion object {
		val WHITE = GColor.WHITE
		val BROWN = GColor.BROWN
		val LIGHT_BLUE = GColor(161, 216, 250)
		val PURPLE = GColor(207, 40, 137)
		val ORANGE = GColor(243, 133, 33)
		@JvmField
        val RED = GColor.RED
		val YELLOW = GColor.YELLOW
		@JvmField
        val GREEN = GColor.GREEN.darkened(.4f)
		val BLUE = GColor.BLUE
		@JvmField
        val CHANCE_ORANGE = GColor(-0x985e0)
		@JvmField
        val COMM_CHEST_BLUE = GColor(-0x792a0a)
		@JvmField
        val BOARD_COLOR = GColor(-0x2d1a2e)

		// values based on the board asset. rendered image will be scaled
		const val BOARD_DIMENSION = 1500f
		const val BOARD_CORNER_DIMENSION = 200f
		@JvmField
        val COMM_CHEST_RECT = arrayOf(
			Vector2D(265f, 447f),
			Vector2D(445f, 267f),
			Vector2D(560f, 382f),
			Vector2D(381f, 558f)
		)
		@JvmField
        val CHANCE_RECT = arrayOf(
			Vector2D(942f, 1125f),
			Vector2D(1122f, 943f),
			Vector2D(1236f, 1059f),
			Vector2D(1057f, 1239f)
		)
		val CENTER_RECT = arrayOf(
			Vector2D(1500f / 2 - 150, 1500f / 2 + 100),
			Vector2D(1500f / 2 + 150, 1500f / 2 + 100),
			Vector2D(1500f / 2 + 150, 1500f / 2 - 100),
			Vector2D(1500f / 2 - 150, 1500f / 2 - 100))
	}

	init {
		scale = dim / BOARD_DIMENSION
		borderDim = BOARD_CORNER_DIMENSION * scale
		cellDim = (dim - borderDim * 2) / 9
	}
}