package cc.lib.checkerboard

import cc.lib.game.Utils

class Dama : Checkers() {
	override fun init(game: Game): Array<Array<Piece>> {
		game.turn = if (Utils.flipCoin()) Game.FAR else Game.NEAR
		return initFromPieceTypes(arrayOf(
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.FAR to arrayOf(PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN),
			Game.FAR to arrayOf(PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN),
			Game.NEAR to arrayOf(PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN, PieceType.DAMA_MAN),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
		))
	}

	override val isJumpsMandatory: Boolean
		get() = true

	override fun canMenJumpBackwards(): Boolean {
		return true
	}

	override fun canJumpSelf(): Boolean {
		return false
	}

	override val isFlyingKings: Boolean
		get() = true
}