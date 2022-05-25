package cc.lib.checkerboard

import cc.lib.game.Utils

class CanadianDraughts : Draughts() {
	override fun init(game: Game): Array<Array<Piece>> {
		game.turn = if (Utils.flipCoin()) Game.FAR else Game.NEAR
		return initFromPieceTypes(arrayOf(
			Game.FAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.FAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.FAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.FAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.FAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NOP to arrayOf(PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.NEAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
			Game.NEAR to arrayOf(PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY),
			Game.NEAR to arrayOf(PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER, PieceType.EMPTY, PieceType.CHECKER),
		))
	}
}