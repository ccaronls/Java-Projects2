package cc.lib.dungeondice

import cc.lib.dungeondice.DEnemy.EnemyType
import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Reflector

open class DDungeon : Reflector<DDungeon>() {
	enum class State {
		INIT,
		ROLL_DICE_TO_ADVANCE,
		ADVANCE,
		ROLL_DICE_FOR_RED_PRIZE,
		ROLL_DICE_FOR_BLUE_PRIZE,
		ROLL_DICE_FOR_FIGHT,
		ROLL_DICE_FOR_ROOM,
		ROLL_DICE_FOR_LOCKED_ROOM,
		CHOOSE_FIGHT_OR_FLEE,
		ROLL_DICE_FOR_ATTACK
	}

	internal enum class Prize {
		KEY,
		DMG_PLUS1,
		DMG_PLUS2,
		BMG_PLUS3,
		ATTACK_PLUS1,
		ATTACK_PLUS2,
		ATTACK_PLUS3,
		MAGIC_PLUS2,
		HP_PLUS_1,
		HP_PLUS_2
	}

	enum class DiceConfig {
		ONE_6x6,
		TWO_6x6,
		THREE_6x6
	}

	var board: DBoard? = null
	var players = mutableListOf<DPlayer>()
	var turn = 0
	var curEnemy = 0
	var state: State? = null
	var die1 = 0
	var die2 = 0
	var die3 = 0
	var diceConfig = DiceConfig.ONE_6x6
	val enemyList: MutableList<DEnemy> = ArrayList()
	val dieRoll: Int
		get() {
			when (diceConfig) {
				DiceConfig.ONE_6x6 -> return die1
				DiceConfig.TWO_6x6 -> return die1 + die2
				DiceConfig.THREE_6x6 -> return die1 + die2 + die3
				else -> Utils.unhandledCase(diceConfig)
			}
			return 0
		}

	fun setPlayer(index: Int, player: DPlayer) {
		if (state != State.INIT) throw RuntimeException("Cannot modify players while in game")
		players[index] = player
		player.playerNum = index
	}

	fun addPlayer(player: DPlayer) {
		if (state != State.INIT) throw RuntimeException("Cannot modify players while in game")
		players.add(player)
		player.playerNum = players.size
	}

	fun getCurPlayer(): DPlayer {
		return players[turn]
	}

	fun newGame() {
		if (players.size < 1) {
			throw RuntimeException("Must have at least 1 player")
		}
		state = State.INIT
		turn = 0
		die3 = 0
		die2 = die3
		die1 = die2
		enemyList.clear()
	}

	fun runGame() {
		when (state) {
			State.INIT -> {
				turn = 0
				curEnemy = -1
				enemyList.clear()
				for (p in players) {
					p.cellIndex = board!!.startCellIndex
				}
			}

			State.ROLL_DICE_TO_ADVANCE -> {
				diceConfig = DiceConfig.ONE_6x6
				if (getCurPlayer().rollDice()) {
					state = State.ADVANCE
				}
			}

			State.ADVANCE -> {
				val moves = board!!.findMoves(getCurPlayer(), dieRoll)
				val move = getCurPlayer().chooseMove(*moves)
				if (move != null) {
					getCurPlayer().backCellIndex = getCurPlayer().cellIndex
					getCurPlayer().cellIndex = move.index
					val cell = board!!.getCell(move.index)
					when (cell.cellType) {
						CellType.EMPTY -> {
							log.warn("Cell %d is EMPTY", move.index)
							nextPlayer()
						}

						CellType.WHITE, CellType.START -> nextPlayer()
						CellType.RED -> state = State.ROLL_DICE_FOR_RED_PRIZE
						CellType.GREEN -> {
							enemyList.add(EnemyType.SNAKE.newEnemy())
							state = State.ROLL_DICE_FOR_FIGHT
						}

						CellType.BLUE -> state = State.ROLL_DICE_FOR_BLUE_PRIZE
						CellType.BROWN -> {
							enemyList.add(EnemyType.RAT.newEnemy())
							state = State.ROLL_DICE_FOR_FIGHT
						}

						CellType.BLACK -> {
							enemyList.add(EnemyType.SPIDER.newEnemy())
							state = State.ROLL_DICE_FOR_FIGHT
						}

						CellType.ROOM -> state = State.ROLL_DICE_FOR_ROOM
						CellType.LOCKED_ROOM -> state = State.ROLL_DICE_FOR_LOCKED_ROOM
					}
				}
			}

			State.ROLL_DICE_FOR_FIGHT -> {
				diceConfig = DiceConfig.ONE_6x6
				if (getCurPlayer().rollDice()) {
					val enemy = enemyList[0]
					if (dieRoll <= enemy.type.chanceToFight) {
						state = State.CHOOSE_FIGHT_OR_FLEE
					} else {
						nextPlayer()
					}
				}
			}

			State.ROLL_DICE_FOR_BLUE_PRIZE -> {
				diceConfig = DiceConfig.ONE_6x6
				if (getCurPlayer().rollDice()) {
				}
			}

			State.ROLL_DICE_FOR_RED_PRIZE -> {
				diceConfig = DiceConfig.ONE_6x6
				if (getCurPlayer().rollDice()) {
				}
			}

			State.ROLL_DICE_FOR_ROOM -> {
				diceConfig = DiceConfig.TWO_6x6
				if (getCurPlayer().rollDice()) {
					val t = arrayOf(
						EnemyType.RAT,
						EnemyType.RAT,
						EnemyType.RAT,
						EnemyType.SNAKE,
						EnemyType.SNAKE,
						EnemyType.SPIDER)
					enemyList.add(t[die1 - 1].newEnemy())
					enemyList.add(t[die2 - 1].newEnemy())
				}
			}

			State.ROLL_DICE_FOR_LOCKED_ROOM -> {
				diceConfig = DiceConfig.THREE_6x6
				if (getCurPlayer().rollDice()) {
					val t = arrayOf(
						EnemyType.RAT,
						EnemyType.RAT,
						EnemyType.SNAKE,
						EnemyType.SNAKE,
						EnemyType.SPIDER,
						EnemyType.SPIDER)
					enemyList.add(t[die1 - 1].newEnemy())
					enemyList.add(t[die2 - 1].newEnemy())
					enemyList.add(t[die3 - 1].newEnemy())
				}
			}

			State.CHOOSE_FIGHT_OR_FLEE -> {
				val moves = arrayOf(
					DMove(MoveType.ATTACK, 0, 0, null),
					DMove(MoveType.FLEE, 0, 0, null)
				)
				val move = getCurPlayer().chooseMove(*moves)
				if (move != null) {
					when (move.type) {
						MoveType.ATTACK -> {
							state = State.ROLL_DICE_FOR_ATTACK
						}

						MoveType.FLEE -> {
							// the enemies get a shot in
							for (e in enemyList) {
								doAttack(e, getCurPlayer())
								doAttack(e, getCurPlayer())
							}
							if (!checkPlayerDead(getCurPlayer())) getCurPlayer().cellIndex = getCurPlayer().backCellIndex
							nextPlayer()
						}

						else -> error("unhandled case")
					}
				}
			}

			else -> error("unhandled case")
		}
	}

	private fun doAttack(attacker: DEntity, e: DEntity) {
		// attacker dexterity determines if a hit
		// attacker strength determines max damage
		// e def is amount reduced from damage
		// e att is amount added to damage
		if (Utils.rand() % 6 + 1 <= attacker.dex) {
			val damage = Utils.rand() % attacker.str + 1 + attacker.attack - e.defense
			if (damage > 0) {
				onDamage(e, damage)
				e.hp -= damage
			} else {
				onMiss(attacker)
			}
		} else {
			onMiss(attacker)
		}
	}

	private fun checkPlayerDead(p: DPlayer): Boolean {
		if (p.hp <= 0) {
			onPlayerDead(p)
			p.cellIndex = board!!.startCellIndex
			p.defense = 0
			p.attack = p.defense
			return true
		}
		return false
	}

	protected fun onPlayerDead(p: DPlayer) {
		log.info("Player %s has died. They return to beginning and lose there ATT and DEF")
	}

	protected fun onMiss(attacker: DEntity) {
		log.info("%d Missed!", attacker.name)
	}

	protected fun onDamage(e: DEntity, damage: Int) {
		log.info("%s took %d damage", e.name, damage)
	}

	private fun nextPlayer() {
		turn = (turn + 1) % players.size
		state = State.ROLL_DICE_TO_ADVANCE
	}

	protected fun rollDice() {
		when (diceConfig) {
			DiceConfig.THREE_6x6 -> {
				die3 = Utils.rand() % 6 + 1
				die2 = Utils.rand() % 6 + 1
				die1 = Utils.rand() % 6 + 1
			}

			DiceConfig.TWO_6x6 -> {
				die2 = Utils.rand() % 6 + 1
				die1 = Utils.rand() % 6 + 1
			}

			DiceConfig.ONE_6x6 -> die1 = Utils.rand() % 6 + 1
		}
	}

	fun draw(g: AGraphics) {
		board!!.drawCells(g, 1f)
		for (p in players) {
			drawPlayer(g, p)
		}
	}

	protected fun drawPlayer(g: AGraphics, p: DPlayer) {
		val cell = board!!.getCell(p.cellIndex)
		g.pushMatrix()
		g.translate(cell)
		g.color = p.getColor()
		val rect = board!!.getCellBoundingRect(p.cellIndex)
		val m = Math.min(rect.width, rect.height)
		rect.scale(m / 8, m / 8)
		g.setLineWidth(2f)
		g.drawCircle(0f, -1.5f, 0.5f)
		g.begin()
		g.vertexArray(arrayOf(floatArrayOf(0f, -1f), floatArrayOf(0f, .5f), floatArrayOf(-1f, -.5f), floatArrayOf(1f, -.5f), floatArrayOf(0f, .5f), floatArrayOf(-1f, 2f), floatArrayOf(0f, .5f), floatArrayOf(1f, 2f)))
		g.drawLines()
		g.popMatrix()
	}

	companion object {
		private val log = LoggerFactory.getLogger(DDungeon::class.java)

		init {
			addAllFields(DDungeon::class.java)
		}
	}
}
