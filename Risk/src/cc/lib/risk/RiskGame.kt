package cc.lib.risk

import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.utils.*
import java.util.*
import kotlin.Pair

/**
 * Created by Chris Caron on 9/13/21.
 */
open class RiskGame : Reflector<RiskGame>() {
	companion object {
		val log = LoggerFactory.getLogger(RiskGame::class.java)
		const val MIN_TROOPS_PER_TURN = 3

		init {
			addAllFields(RiskGame::class.java)
		}
	}

	var players: MutableList<RiskPlayer> = ArrayList()
	var board = RiskBoard()
	var currentPlayer = 0
	var state = State.INIT
	fun clear() {
		players.clear()
		board.reset()
		currentPlayer = 0
		state = State.INIT
	}

	@Synchronized
	fun runGame() {
		log.debug("runGame ENTER")
		log.verbose("Run game state: %s", state)
		if (players.size < 2) throw GException("Need at least 2 players")
		if (state != State.INIT && winner != null) {
			state = State.GAME_OVER
		}
		val cur = getCurrentPlayer()
		var territories = board.getTerritories(cur!!.army)
		val actions: MutableList<Action> = ArrayList()
		var stageable: List<Int>? = null
		when (state) {
			State.INIT                                                                          -> {
				val terr = LinkedList<RiskCell>()
				for (cell in board.cells) {
					cell.occupier = (null)
					cell.numArmies = (0)
					terr.add(cell)
				}
				terr.shuffle()
				var numEach = terr.size / players.size
				var infantryEach = 0
				if (players.size == 2) {
					// special case where we have a neutral player
					numEach = terr.size / 3
				}
				infantryEach = (board.numCells + 10) / 10 * 10 - 5 * players.size
				for (pl in players) {
					pl.setArmiesToPlace(infantryEach)
				}
				if (players.size > 2) {
					state = State.CHOOSE_TERRITORIES
					return
				}
				var i = 0
				while (i < numEach) {
					for (pl in players) {
						val cell = terr.removeFirst()
						cell.occupier = (pl.army)
						cell.numArmies = (1)
						pl.decrementArmy()
						assertTrue(pl.getArmiesToPlace() > 0)
					}
					i++
				}
				while (terr.size > 0) {
					val cell = terr.removeFirst()
					cell.occupier = (Army.NEUTRAL)
					cell.numArmies = (1)
				}
				state = State.PLACE_ARMY1
			}
			State.CHOOSE_TERRITORIES                                                            -> {
				val unclaimed = Array(board.numCells) { it }.filter { 
					with (board.getCell(it)) {
						occupier == null && numArmies == 0
					}
				}
				if (unclaimed.isEmpty()) {
					state = State.PLACE_ARMY
				} else {
					cur.pickTerritoryToClaim(this, unclaimed)?.let { picked ->
						assertTrue(unclaimed.contains(picked))
						val cell = board.getCell(picked)
						assertTrue(cell.occupier == null)
						cell.occupier = (cur.army)
						cell.numArmies = (1)
						cur.decrementArmy()
						nextPlayer()
					}
				}
			}
			State.PLACE_ARMY1, State.PLACE_ARMY2, State.PLACE_ARMY, State.BEGIN_TURN_PLACE_ARMY -> {
				if (cur.getArmiesToPlace() > 0) {
					val startArmiesToPlace: Int = cur.initialArmiesToPlace
					val remainingArmiesToPlace = startArmiesToPlace - cur.getArmiesToPlace()
					cur.pickTerritoryForArmy(this, territories, remainingArmiesToPlace, startArmiesToPlace)?.let { picked ->
						assertTrue(territories.contains(picked))
						onPlaceArmy(cur.army, picked)
						val cell = board.getCell(picked)
						cell.numArmies++
						cur.decrementArmy()
						when (state) {
							State.PLACE_ARMY1           -> state = State.PLACE_ARMY2
							State.PLACE_ARMY2           -> state = State.PLACE_NEUTRAL
							State.PLACE_ARMY            -> nextPlayer()
							State.BEGIN_TURN_PLACE_ARMY -> {
							}
							else                        -> throw GException("Unhandled case: $state")
						}
					}
				} else if (state == State.BEGIN_TURN_PLACE_ARMY) {
					state = State.CHOOSE_MOVE
				} else {
					if (players.count { player: RiskPlayer -> player.getArmiesToPlace() > 0 } == 0) {
						state = State.BEGIN_TURN
					} else {
						nextPlayer()
					}
				}
			}
			State.PLACE_NEUTRAL                                                                 -> {
				territories = board.getTerritories(Army.NEUTRAL)
				cur.pickTerritoryForNeutralArmy(this, territories)?.let { picked ->
					assertTrue(territories.contains(picked))
					val cell = board.getCell(picked)
					onPlaceArmy(Army.NEUTRAL, picked)
					cell.numArmies++
					state = State.PLACE_ARMY1
					nextPlayer()
				}
			}
			State.BEGIN_TURN                                                                    -> {
				if (territories.isEmpty()) {
					nextPlayer()
				} else {
					onBeginTurn(cur.army)
					val numArmies = computeTroopsPerTurn(cur.army)
					cur.setArmiesToPlace(numArmies)
					state = State.BEGIN_TURN_PLACE_ARMY
				}
			}
			State.CHOOSE_MOVE                                                                   -> {

				// See which territories can attack an adjacent
				stageable = territories.filter { idx ->
					board.getCell(idx).let { cell ->
						cell.numArmies > 1  && board.getConnectedCells(cell).firstOrNull { adj ->
							board.getCell(adj).occupier != cur.army
						} != null
					}
				}
				if (stageable.isNotEmpty()) {	
					actions.add(Action.ATTACK)
				}
				for (idx in territories) {
					val cell = board.getCell(idx)
					cell.movableTroops = cell.numArmies - 1
				}

				// See which territories can move into an adjacent
				val moveable = getMoveSourceOptions(territories, cur.army)
				if (moveable.isNotEmpty()) {
					actions.add(Action.MOVE)
				}
				if (actions.isNotEmpty())
					actions.add(Action.END)
				else {
					state = State.BEGIN_TURN
					nextPlayer()
					return
				}
				onBeginMove()
				cur.pickAction(this, actions, cur.army.toString() + " Choose your Move")?.let { action ->
					assertTrue(actions.contains(action))
					when (action) {
						Action.ATTACK -> {
							val start = cur.pickTerritoryToAttackFrom(this, stageable)
							if (start != null) {
								assertTrue(stageable!!.contains(start))
								onStartAttackTerritoryChosen(start)
								val cell = board.getCell(start)
								val options = board.getConnectedCells(cell).filter { idx: Int -> board.getCell(idx).occupier != cur.army }
								if (options.isEmpty()) throw GException("Invalid stagable")
								val end = cur.pickTerritoryToAttack(this, start, options)
								if (end != null) {
									assertTrue(options.contains(end))
									onEndAttackTerritoryChosen(start, end)
									performAttack(start, end)
								}
							}
						}
						Action.MOVE   -> {
							val start = cur.pickTerritoryToMoveFrom(this, moveable)
							if (start != null) {
								assertTrue(moveable.contains(start))
								onStartMoveTerritoryChosen(start)
								val cell = board.getCell(start)
								val options = board.getConnectedCells(cell).filter { idx: Int? -> board.getCell(idx!!).occupier == cur.army }
								if (options.isEmpty()) throw GException("Invalid movable")
								while (cell.movableTroops > 0) {
									val end = cur.pickTerritoryToMoveTo(this, start, options)
										?: break
									assertTrue(options.contains(end))
									onMoveTroops(start, end, 1)
									performMove(start, end)
									state = State.CHOOSE_MOVE_NO_ATTACK
								}
							}
						}
						Action.END    -> {
							state = State.BEGIN_TURN
							nextPlayer()
						}
					}
				}
			}
			State.CHOOSE_MOVE_NO_ATTACK                                                         -> {
				val moveable = getMoveSourceOptions(territories, cur.army)
				if (moveable.isNotEmpty()) {
					actions.add(Action.MOVE)
				}
				if (actions.size > 0) actions.add(Action.END) else {
					state = State.BEGIN_TURN
					nextPlayer()
					return
				}
				onBeginMove()
				val action = cur.pickAction(this, actions, cur.army.toString() + " Choose your Move")
				if (action != null) {
					assertTrue(actions.contains(action))
					when (action) {
						Action.ATTACK -> {
							assertTrue(stageable != null)
							val start = cur.pickTerritoryToAttackFrom(this, stageable!!)
							if (start != null) {
								assertTrue(stageable!!.contains(start))
								onStartAttackTerritoryChosen(start)
								val cell = board.getCell(start)
								val options = board.getConnectedCells(cell).filter { idx: Int? -> board.getCell(idx!!).occupier != cur.army }
								if (options.isEmpty()) throw GException("Invalid stagable")
								cur.pickTerritoryToAttack(this, start, options)?.let { end ->
									assertTrue(options.contains(end))
									onEndAttackTerritoryChosen(start, end)
									performAttack(start, end)
								}
							}
						}
						Action.MOVE   -> {
							val start = cur.pickTerritoryToMoveFrom(this, moveable)
							if (start != null) {
								assertTrue(moveable.contains(start))
								onStartMoveTerritoryChosen(start)
								val cell = board.getCell(start)
								val options = board.getConnectedCells(cell).filter { idx: Int -> board.getCell(idx).occupier == cur.army }
								if (options.isEmpty()) throw GException("Invalid movable")
								while (cell.movableTroops > 0) {
									val end = cur.pickTerritoryToMoveTo(this, start, options)
										?: break
									assertTrue(options.contains(end))
									onMoveTroops(start, end, 1)
									performMove(start, end)
									state = State.CHOOSE_MOVE_NO_ATTACK
								}
							}
						}
						Action.END    -> {
							state = State.BEGIN_TURN
							nextPlayer()
						}
					}
				}
			}
			State.GAME_OVER                                                                     -> {
				onGameOver(winner!!)
				state = State.DONE
			}
			State.DONE                                                                          -> {
			}
		}
		log.debug("runGame EXIT")
	}

	protected open fun onPlaceArmy(army: Army, cellIdx: Int) {
		log.debug("%s placing an army on %s", army, board.getCell(cellIdx).region)
	}

	protected open fun onGameOver(winner: Army) {}
	protected open fun onBeginMove() {}
	protected open fun onBeginTurn(army: Army) {}
	protected open fun onChooseMove(army: Army) {}
	protected open fun onStartAttackTerritoryChosen(cellIdx: Int) {}
	protected open fun onEndAttackTerritoryChosen(startIdx: Int, endIdx: Int) {}
	protected open fun onStartMoveTerritoryChosen(cellIdx: Int) {}
	protected open fun onMoveTroops(startIdx: Int, endIdx: Int, numTroops: Int) {}
	fun getMoveSourceOptions(territories: List<Int>, army: Army): List<Int> {
		return territories.filter { idx: Int ->
			board.getCell(idx).let { cell ->
				cell.movableTroops > 0 && board.getConnectedCells(cell).firstOrNull { adj ->
					board.getCell(adj).occupier == army
				} != null
			}
		}
	}

	fun getMovePairOptions(territories: List<Int>, army: Army): List<Pair<Int, Int>> {
		val pairs: MutableList<Pair<Int, Int>> = ArrayList()
		for (idx in territories) {
			val cell = board.getCell(idx)
			if (cell.movableTroops > 0) {
				for (adjIdx in board.getConnectedCells(cell)) {
					val adj = board.getCell(adjIdx)
					if (adj.occupier == cell.occupier) {
						pairs.add(Pair(idx, adjIdx))
					}
				}
			}
		}
		return pairs
	}

	fun computeTroopsPerTurn(army: Army): Int {
		var numArmies = board.getTerritories(army).size / 3
		for (r in Region.values()) {
			val cells = board.getTerritories(r)
			if (cells.count { c: Int -> board.getCell(c).occupier != army } == 0) {
				numArmies += r.extraArmies
				onMessage(army, "Gets ${r.extraArmies} extra armies for holding all of ${r.name}");
			}
		}
		return Math.max(MIN_TROOPS_PER_TURN, numArmies)
	}

	private fun performMove(startIdx: Int, endIdx: Int) {
		val start = board.getCell(startIdx)
		val end = board.getCell(endIdx)
		start.movableTroops--
		assertTrue(start.movableTroops >= 0)
		start.numArmies--
		assertTrue(start.numArmies > 0)
		end.numArmies++
	}

	private fun performAttack(startIdx: Int, endIdx: Int) {
		val start = board.getCell(startIdx)
		val end = board.getCell(endIdx)
		if (end.numArmies <= 0) throw GException("End has no armies!")
		val maxAttacking = start.numArmies - 1
		val options: MutableList<Action> = ArrayList()
		options.add(Action.ONE_ARMY)
		if (maxAttacking > 1) {
			options.add(Action.TWO_ARMIES)
		}
		if (maxAttacking > 2) {
			options.add(Action.THREE_ARMIES)
		}
		options.add(Action.CANCEL)
		val numToAttack = getCurrentPlayer()!!.pickAction(this, options, getCurrentPlayer()!!.army.toString() + " Choose Number of Armies to attack")
		if (numToAttack != null) {
			if (end.numArmies <= 0) throw GException("End has no armies!")
			val numAttacking: Int = numToAttack.armies
			if (numAttacking <= 0) return
			val attackingDice = IntArray(numAttacking)
			val defendingDice = IntArray(Math.min(2, end.numArmies))
			val result = BooleanArray(Math.min(attackingDice.size, defendingDice.size))
			rollDice(attackingDice)
			rollDice(defendingDice)
			var numToOccupy = attackingDice.size
			for (i in result.indices) {
				if (attackingDice[i] > defendingDice[i]) {
					result[i] = true
				} else {
					numToOccupy--
				}
			}
			onDiceRolled(start.occupier!!, attackingDice, end.occupier!!, defendingDice, result)
			var numStartArmiesLost = 0
			var numEndArmiesLost = 0
			for (i in result.indices) {
				if (result[i]) {
					numEndArmiesLost++
				} else {
					numStartArmiesLost++
				}
			}
			start.numArmies -= numStartArmiesLost
			end.numArmies -= numEndArmiesLost
			assertTrue(start.numArmies > 0)
			assertTrue(end.numArmies >= 0)
			onArmiesDestroyed(startIdx, numStartArmiesLost, endIdx, numEndArmiesLost)
			if (end.numArmies < 0) throw GException("Cannot have negative armies")
			if (end.numArmies == 0) {
				assertTrue(numToOccupy > 0)
				start.numArmies = (start.numArmies - numToOccupy)
				assertTrue(start.numArmies > 0)
				onMoveTroops(startIdx, endIdx, numToOccupy)
				end.occupier = (start.occupier)
				end.numArmies = (numToOccupy)
				if (winner == null && board.getTerritories(end.region).count { idx: Int -> board.getCell(idx).occupier != start.occupier } == 0) {
					onAttackerGainedRegion(end.occupier, end.region)
				} else {
					onAtatckerGainedTerritory(end.occupier, endIdx)
				}
			}
		}
	}

	protected open fun onDiceRolled(attacker: Army, attackingDice: IntArray, defender: Army, defendingDice: IntArray, result: BooleanArray) {
		val aDice: MutableList<Int?> = ArrayList()
		for (a in attackingDice) aDice.add(a)
		val dDice: MutableList<Int?> = ArrayList()
		for (d in defendingDice) dDice.add(d)
		val bResult: MutableList<String?> = ArrayList()
		for (b in result) bResult.add(if (b) "<--" else "-->")
		val table = Table()
			.addColumn(attacker.name, aDice)
			.addColumn("", bResult)
			.addColumn(defender.name, dDice)
		log.info("Result From Dice Roll\n$table")
	}

	protected open fun onArmiesDestroyed(attackerIdx: Int, attackersLost: Int, defenderIdx: Int, defendersLost: Int) {
		val attacker = board.getCell(attackerIdx)
		val defender = board.getCell(defenderIdx)
		log.info("%s has lost %d armies and %s has lost %d armies", attacker.occupier, attackersLost, defender.occupier, defendersLost)
	}

	protected fun onAtatckerGainedTerritory(attacker: Army?, cellIdx: Int) {
		log.info("%s: Gained Territory for %s", attacker, board.getCell(cellIdx).region)
	}

	protected open fun onAttackerGainedRegion(attacker: Army?, region: Region) {
		log.info("%s: Gained Region %s for +%d troops", attacker, region, region.extraArmies)
	}

	private fun rollDice(dice: IntArray) {
		for (i in dice.indices) {
			dice[i] = random(1 .. 6)
		}
		Arrays.sort(dice)
		dice.reverse()
	}

	protected fun onMessage(army: Army, msg: String?) {
		log.info("%s: %s", army, msg)
	}

	private fun nextPlayer() {
		currentPlayer = (currentPlayer + 1) % players.size
	}

	fun getCurrentPlayer(): RiskPlayer? {
		return if (currentPlayer >= 0 && currentPlayer < players.size) players[currentPlayer] else null
	}

	fun getPlayer(playerNum: Int): RiskPlayer {
		return players[playerNum]
	}

	val winner: Army?
			get() = board.getCell(0).occupier?.takeIf { army -> 
			board.cells.count { cell ->
				cell.occupier != army
			} == 0
		}

	fun getPlayerOrNull(army: Army): RiskPlayer? {
		for (rp in players) {
			if (rp.army == army) return rp
		}
		return null
	}

	fun getPlayer(army: Army): RiskPlayer {
		val pl = getPlayerOrNull(army)
		if (pl != null) return pl
		throw GException("No player for army: $army")
	}

	val numPlayers: Int
		get() = players.size

	fun addPlayer(player: RiskPlayer) {
		if (getPlayerOrNull(player.army) != null)
			throw GException("Player with army " + player.army + " already exists")
		players.add(player)
	}

	val isDone: Boolean
		get() = state == State.DONE

	val summary: Table
		get() {
			val header: MutableList<String> = ArrayList()
			header.add("Army")
			header.add("Troops")
			for (r in Region.values()) {
				header.add(prettify(r.name).replace(' ', '\n') + " +" + r.extraArmies)
			}
			val table = Table(header)
			val armies = players.map { p: RiskPlayer -> p.army }.toMutableList()
			if (armies.size < 3) armies.add(Army.NEUTRAL)
			for (army in armies) {
				val l = board.getTerritories(army)
				val row: MutableList<String?> = ArrayList()
				row.add(army.name)
				val troops = board.allTerritories.sumBy { t: RiskCell -> if (t.occupier == army) t.numArmies else 0 }
				if (army == Army.NEUTRAL)
					row.add(troops.toString())
				else if (l.isNotEmpty())
					row.add(String.format("%d +%d", troops, computeTroopsPerTurn(army)))
				else
					row.add("-----")
				for (r in Region.values()) {
					val l2 = board.getTerritories(r)
					val numOwned = l2.count { idx: Int -> l.contains(idx) }
					val percent = numOwned * 100 / l2.size
					row.add(String.format("%d%%", percent))
				}
				table.addRowList(row)
			}
			return table
		}
}