package marcos.games.hexes.core

import cc.lib.game.Utils
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector

open class Hexes : Reflector<Hexes>() {
	val version: String
		get() = "1.0"
	private val players = mutableListOf<Player>()
	var numPlayers = 0
		private set
	private var curPlayer = 0
	val board = Board()
	private var state = State.READY
	fun initPlayers(vararg players: Player) {
		Utils.println("init players")
		if (state != State.READY) throw RuntimeException("Already in a game")
		if (players.size < 2 || players.size > MAX_PLAYERS) throw RuntimeException("Invalid number of players '" + players.size + "'")
		numPlayers = 0
		for (p in players) {
			this.players[numPlayers++] = p
		}
	}

	fun newGame() {
		Utils.println("New Game")
		if (numPlayers < 2) throw RuntimeException("Not enough players")
		state = State.READY
		board.init()
		curPlayer = 0
		for (i in 0 until numPlayers) players[i].init()
	}

	@Omit
	private var playerChoice = -1
	fun runGame() {
		if (isGameOver) {
			state = State.READY
			return
		}
		if (state == State.READY) state = State.FIRST_MOVE
		val player = players[curPlayer]
		if (playerChoice < 0) {
			val choices = board.computeUnused()
			// reduce the choices
			for (pIndex in choices) {
				val p = board.getPiece(pIndex)
				for (i in 0..2) {
					val pp = board.getAdjacent(p, i)
					if (pp != null) {
						// TODO: 
					}
				}
			}
			board.setPieceChoices(choices)
			val pIndex = player.choosePiece(this, choices)
			if (choices.indexOf(pIndex) >= 0) {
				playerChoice = pIndex
				board.setPieceChoices(null)
			}
		} else {
			val choices = computeShapeChoices(playerChoice)
			val it = choices.iterator()
			while (it.hasNext()) {
				val s = it.next()
				if (player.getShapeCount(s) == 0) it.remove()
			}
			var shape: Shape? = null
			if (choices.size == 1) {
				shape = choices[0]
			} else if (choices.size > 1) {
				shape = player.chooseShape(this, choices.toTypedArray<Shape>())
			} else {
				cancel()
			}
			if (shape != null && playerChoice >= 0 && player.getShapeCount(shape) > 0) {
				val ptsBefore = board.computePlayerPoints(curPlayer + 1)
				board.setPiece(playerChoice, curPlayer + 1, shape)
				val ptsAfter = board.computePlayerPoints(curPlayer + 1)
				onPiecePlayed(playerChoice, ptsAfter - ptsBefore)
				board.shapeSearch(curPlayer + 1)
				playerChoice = -1
				player.score = board.computePlayerPoints(curPlayer + 1)
				curPlayer = (curPlayer + 1) % numPlayers
				if (curPlayer == 0) state = State.PLAYING
				player.decrementPiece(shape)
				if (isGameOver) {
					var winner = 0
					var winnerScore = players[0].score
					for (i in 1 until numPlayers) {
						if (players[i].score > winnerScore) {
							winner = i
							winnerScore = players[i].score
						}
					}
					onGameOver(winner + 1)
				} else if (state == State.PLAYING) {
					board.grow()
				}
			}
		}
	}

	private fun computeShapeChoices(pieceIndex: Int): ArrayList<Shape> {
		val p = board.getPiece(pieceIndex)
		val choices = ArrayList<Shape>()
		choices.add(Shape.DIAMOND)
		choices.add(Shape.TRIANGLE)
		choices.add(Shape.HEXAGON)
		for (i in 0..2) {
			val pp = board.getAdjacent(p, i)
			if (pp != null && pp.player > 0) {
				choices.remove(pp.type)
			}
		}
		return choices
	}

	val isGameOver: Boolean
		get() = players[curPlayer].countPieces() == 0

	fun getCurPlayer(): Player? {
		return if (curPlayer < 0) null else players[curPlayer]
	}

	/**
	 * Cancel the piece choice the player has made.  Only valid when player.chooseShape is processing (which must return
	 */
	fun cancel() {
		playerChoice = -1
	}

	/**
	 * Return player.  The first is 1
	 * @param num
	 * @return
	 */
	fun getPlayer(num: Int): Player {
		return players[num - 1]
	}

	protected open fun onGameOver(winner: Int) {}
	protected open fun onPiecePlayed(pieceIndex: Int, pts: Int) {}

	companion object {
		init {
			addAllFields(Hexes::class.java)
		}

		const val MAX_PLAYERS = 4
	}
}
