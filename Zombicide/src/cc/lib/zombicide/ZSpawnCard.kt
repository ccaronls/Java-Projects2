package cc.lib.zombicide

import cc.lib.logger.LoggerFactory
import cc.lib.utils.Reflector
import cc.lib.utils.randomWeighted
import java.util.*

class ZSpawnCard private constructor(val name: String, private val wolfburg: Boolean, private val easyCount: Int, private val mediumCount: Int, private val hardCount: Int, vararg actions: Action) : Reflector<ZSpawnCard>() {
	companion object {
		var log = LoggerFactory.getLogger(ZSpawnCard::class.java)
		var NOTHING_IN_SIGHT = Action(ActionType.NOTHING_IN_SIGHT, 0, null)
		var DOUBLE_SPAWN = Action(ActionType.DOUBLE_SPAWN, 0, null)
		var EXTRA_ACTIVATION = Action(ActionType.EXTRA_ACTIVATION_STANDARD, 0, null)
		var NECROMANCER = Action(ActionType.SPAWN, 1, ZZombieType.Necromancer)
		val cards = arrayOf(
			ZSpawnCard("Standard Zombie Invasion", false, 6, 4, 2,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty)),
			ZSpawnCard("Standard Zombie Invasion", false, 10, 5, 2,
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 3, ZZombieType.Walker),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker)),
			ZSpawnCard("Standard Zombie Invasion", false, 0, 0, 1,
				Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty)),
			ZSpawnCard("Standard Zombie Invasion", false, 8, 6, 4,
				Action(ActionType.SPAWN, 1, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 8, ZZombieType.Walker)),
			ZSpawnCard("Standard Zombie Invasion", false, 1, 2, 3,
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
				Action(ActionType.SPAWN, 2, ZZombieType.Walker)),
			ZSpawnCard("Standard Zombie Invasion", false, 4, 4, 4,
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 3, ZZombieType.Runner)),
			ZSpawnCard("Standard Zombie Invasion", false, 2, 4, 6,
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 6, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner)),
			ZSpawnCard("Standard Zombie Invasion", false, 4, 4, 4,
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker)),
			ZSpawnCard("Standard Zombie Invasion", false, 8, 6, 4,
				Action(ActionType.SPAWN, 3, ZZombieType.Walker),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 6, ZZombieType.Walker)),
			ZSpawnCard("Necromancer!", false, 1, 2, 4,
				NECROMANCER, NECROMANCER, NECROMANCER, NECROMANCER),
			ZSpawnCard("Double Spawn!", false, 0, 1, 2,
				DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN),
			ZSpawnCard("Extra Activation!", false, 0, 3, 6,
				NOTHING_IN_SIGHT, EXTRA_ACTIVATION, EXTRA_ACTIVATION, EXTRA_ACTIVATION),
			ZSpawnCard("Zombie Wolfz Invasion", true, 1, 2, 3,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination)),
			ZSpawnCard("Zombie Wolfz Invasion", true, 4, 6, 10,
				Action(ActionType.SPAWN, 2, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 3, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 4, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 5, ZZombieType.Wolfz)))

		fun drawSpawnCard(wolfburg: Boolean, canSpawnNecromancers: Boolean, difficulty: ZDifficulty?): ZSpawnCard {
			val weights = IntArray(cards.size)
			for (i in cards.indices) {
				val card = cards[i]
				if (!wolfburg && card.wolfburg) continue
				if (cards[i].actions[0] === NECROMANCER && !canSpawnNecromancers) continue  // dont spawn necromancers in spawn zones, only in buildings
				when (difficulty) {
					ZDifficulty.EASY -> weights[i] = card.easyCount
					ZDifficulty.MEDIUM -> weights[i] = card.mediumCount
					ZDifficulty.HARD -> weights[i] = card.hardCount
					else -> Unit
				}
			}
			for (i in weights.indices) {
				if (weights[i] == 0) continue
				log.debug("%2d %s", weights[i], cards[i].actions.contentToString())
			}
			val cardIdx = weights.randomWeighted()
			return cards[cardIdx]
		}

		init {
			addAllFields(ZSpawnCard::class.java)
			addAllFields(Action::class.java)
		}
	}

	enum class ActionType {
		NOTHING_IN_SIGHT,
		SPAWN,
		DOUBLE_SPAWN,
		EXTRA_ACTIVATION_STANDARD,
		EXTRA_ACTIVATION_NECROMANCER,
		EXTRA_ACTIVATION_WOLFSBURG
	}

	class Action(val action: ActionType=ActionType.NOTHING_IN_SIGHT, val count: Int = 0, val type: ZZombieType?=null) : Reflector<Action>() {
		override fun toString(): String {
			return if (count > 0) {
				String.format("%s %d X %s", action, count, type)
			} else action.toString()
		}
	}

	constructor() : this("", false, 0, 0, 0)

	private val actions: Array<Action> = arrayOf(*actions)

	fun getAction(color: ZColor): Action {
		return actions[color.ordinal]
	}

	override fun toString(): String {
		return "ZSpawnCard{" +
			"name='" + name + '\'' +
			", wolfzburg=" + wolfburg +
			", easyCount=" + easyCount +
			", mediumCount=" + mediumCount +
			", hardCount=" + hardCount +
			", actions=" + Arrays.toString(actions) +
			'}'
	}

}