package cc.lib.checkerboard

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.utils.GException

class UIPlayer : AIPlayer {
	companion object {
		init {
			addAllFields(UIPlayer::class.java)
		}
	}

	enum class Type {
		RANDOM,
		USER,
		AI
	}

	var type: Type

	@JvmOverloads
	constructor(type: Type = Type.USER) {
		this.type = type
	}

	constructor(type: Type, difficulty: Int) : super(difficulty) {
		this.type = type
	}

	override fun choosePieceToMove(game: Game, pieces: List<Piece>): Piece? {
		return when (type) {
			Type.RANDOM -> pieces[Utils.rand() % pieces.size]
			Type.USER -> (game as UIGame).choosePieceToMove(pieces)
			Type.AI -> super.choosePieceToMove(game, pieces)
		}
		throw GException("Unhandled case: $type")
	}

	override fun chooseMoveForPiece(game: Game, moves: List<Move>): Move? {
		return when (type) {
			Type.RANDOM -> moves[Utils.rand() % moves.size]
			Type.USER -> (game as UIGame).chooseMoveForPiece(moves)
			Type.AI -> super.chooseMoveForPiece(game, moves)
		}
		throw GException("Unhandled case: $type")
	}

	fun drawStatus(g: AGraphics, w: Float, h: Float) {
		when (type) {
			Type.AI -> if (isThinking) {
				g.color = color.color
				g.drawJustifiedStringOnBackground(w / 2, h / 2, Justify.CENTER, Justify.CENTER, "Thinking", GColor.TRANSLUSCENT_BLACK, 3f, 8f)
			}
		}
	}
}