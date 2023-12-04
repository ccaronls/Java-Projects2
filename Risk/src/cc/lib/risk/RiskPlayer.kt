package cc.lib.risk

import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.random
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 9/13/21.
 */
open class RiskPlayer(val army: Army = Army.NEUTRAL) : Reflector<RiskPlayer>() {
	companion object {
		val log = LoggerFactory.getLogger(RiskPlayer::class.java)

		init {
			addAllFields(RiskPlayer::class.java)
		}
	}

	private var armiesToPlace = 0
	var initialArmiesToPlace = 0
		private set

	@Omit
	private var bestMove: Pair<Int, Int>? = null

	fun getArmiesToPlace(): Int {
		return armiesToPlace
	}

	fun setArmiesToPlace(armies: Int) {
		initialArmiesToPlace = armies
		armiesToPlace = initialArmiesToPlace
	}

	fun decrementArmy() {
		armiesToPlace--
	}

	open fun pickTerritoryToClaim(game: RiskGame, options: List<Int>): Int? {
		//return Utils.randItem(options);
		return options.maxByOrNull {  
			val cell = game.board.getCell(it) 
			cell.region.extraArmies + cell.getAllConnectedCells().size
		}
	}

	open fun pickTerritoryForArmy(game: RiskGame, options: List<Int>, remaining: Int, start: Int): Int? {
		return options.maxByOrNull { idx ->
			val cell = game.board.getCell(idx)
			cell.numArmies++
			try {
				evaluateBoard(game.board, army).also {
					log.debug("pickTerritoryForArmy %s idx(%d) value(%d)", army, idx, it)
				}
			} finally {
				cell.numArmies--
			}
		}?.also {
			log.debug("pickTerritoryForArmy: choose cell: %d", it)
		}
	}

	open fun pickTerritoryForNeutralArmy(game: RiskGame, options: List<Int>): Int? {
		// place a neutral around opponents
		return options.random()
	}

	open fun pickTerritoryToAttackFrom(game: RiskGame, options: List<Int>): Int? {
		return options.maxByOrNull { game.board.getCell(it).numArmies }
	}

	open fun pickTerritoryToAttack(game: RiskGame, attackingCellIdx: Int, options: List<Int>): Int? {
		val attackingCell = game.board.getCell(attackingCellIdx)
		return options.maxByOrNull { attackingCell.numArmies - game.board.getCell(it).numArmies }
	}

	open fun pickTerritoryToMoveFrom(game: RiskGame, options: List<Int>): Int? {
		return bestMove?.first?:options.maxByOrNull { game.board.getCell(it).numArmies }
	}

	open fun pickTerritoryToMoveTo(game: RiskGame, sourceCellIdx: Int, options: List<Int>): Int? {
		return bestMove?.second?:options.maxByOrNull { -game.board.getCell(it).numArmies }
	}

	open fun pickAction(game: RiskGame, actions: List<Action>, msg: String): Action? {
		var best: Action? = null
		for (a in actions) {
			when (a) {
				Action.ATTACK                                           -> {

					// if we have any cells with 3 or more that are adjacent to something then choose yes
					if (0 < game.board.getTerritories(army).count { idx: Int ->
							val cell = game.board.getCell(idx)
							if (cell.numArmies < 3) return@count false
							for (adjIdx in cell.getAllConnectedCells()) {
								val adj = game.board.getCell(adjIdx)
								if (adj.occupier != army) {
									if (adj.numArmies < cell.numArmies) return@count true
								}
							}
							false
						}) {
						return a
					}
				}
				Action.MOVE                                             -> {

					// compute all move pairs
					val copy = game.board
					var value = evaluateBoard(copy, army)
					bestMove = null
					log.debug("chooseMove: %-10s value(%d)", "NONE", value)

					//copy.copyFrom(game.getBoard());
					val territories = game.board.getTerritories(army)
					for (p in game.getMovePairOptions(territories, army)) {
						copy.moveArmies(p.first, p.second, 1)
						val v = evaluateBoard(copy, army)
						log.debug("chooseMove: %-10s value(%d)", p.first.toString() + "->" + p.second, v)
						if (v > value) {
							bestMove = p
							value = v
						}
						copy.moveArmies(p.second, p.first, 1)
					}
					log.debug("chooseMove: best=%s", bestMove)
					if (bestMove != null) {
						return a
					}
				}
				Action.THREE_ARMIES, Action.TWO_ARMIES, Action.ONE_ARMY -> if (best == null || best.ordinal < a.ordinal) best = a
				Action.END                                              -> if (bestMove == null) return a
				Action.CANCEL                                           -> {
				}
			}
		}
		return best ?: actions.randomOrNull()
	}

	fun evaluateBoard(b: RiskBoard, army: Army): Long {
		var score = 0.0
		val terr = b.getTerritories(army)
		val enemies: MutableSet<Int> = HashSet()
		var numAdj = 0.0
		var adjDelta = 0.0
		for (idx in terr) {
			val cell = b.getCell(idx)
			for (adjIdx in cell.getAllConnectedCells()) {
				val adj = b.getCell(adjIdx)
				if (adj.occupier != army) {
					numAdj++
					adjDelta = (cell.numArmies - adj.numArmies).toDouble()
					enemies.add(adjIdx)
				}
			}
		}

		// make the distance from enemies to our territories a minimum
		if (enemies.size > 0) {
			var distance = 0.0
			for (idx in terr) {
				val cell = b.getCell(idx)
				for (adjIdx in enemies) {
					val enemy = b.getCell(adjIdx)
					val dist = b.getDistance(idx, adjIdx).toDouble()
					val delta = (cell.numArmies - enemy.numArmies).toDouble()
					if (delta > 1) {
						//delta = 1 + Math.log(delta);
					}
					if (dist > 0)
						distance += delta / dist
				}
			}
			score += distance
		}
		if (adjDelta < 1) {
			score += adjDelta
		} else if (numAdj > 0) {
			val factor = Math.log(adjDelta / numAdj)
			score += factor
		}
		return (score.roundToInt() * 100 + random(100)).toLong()
	}

	fun evaluateBoard0(b: RiskBoard, army: Army): Long {

		// things that are good:
		//   owning a continent
		//   having troops with low numbers surrounded by own troops with high numbers
		//   having more troops in territories adjacent to other armies
		//   we want to avoid having too many troops in one place so scale value logarithmically
		//   we want to minimize the delta between our armies and our opponents
		//   we want to maximize the number of cells we have advantage over
		var score: Long = 0
		val terr = b.getTerritories(army)
		var numEnemiesAdjacent = 0
		var adjacentEnemiesDelta = 0f
		for (idx in terr) {
			val cell = b.getCell(idx)
			score += (cell.region.extraArmies + cell.getAllConnectedCells().size).toLong()
			var numOwnArmiesAdj = 0
			var numEnemyArmiesAdj = 0
			for (adjIdx in cell.getAllConnectedCells()) {
				val c = b.getCell(adjIdx)
				if (c.occupier == cell.occupier) {
					numOwnArmiesAdj += c.numArmies
				} else {
					numEnemiesAdjacent++
					score += ((cell.numArmies - c.numArmies) / 2).toLong()
					adjacentEnemiesDelta += (cell.numArmies - c.numArmies).toFloat()
					numEnemyArmiesAdj += c.numArmies
				}
			}
			if (numEnemyArmiesAdj == 0) {
				score -= cell.numArmies.toLong() // if we are surrounded by only friendlies then less is more
			}
		}
		if (numEnemiesAdjacent > 0) {
			score += (5 * Math.round(adjacentEnemiesDelta / numEnemiesAdjacent.toFloat())).toLong()
		}
		return score //*100 + Utils.rand()%100;
	}
}