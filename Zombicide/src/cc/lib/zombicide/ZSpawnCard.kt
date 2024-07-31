package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Reflector
import cc.lib.utils.Table
import cc.lib.utils.Table.Model
import cc.lib.utils.prettify
import cc.lib.utils.randomWeighted
import cc.lib.utils.wrap

@Keep
enum class ZDeckType {
	BLACK_PLAGUE,
	WOLFSBURF,
	GREEN_HOARD,
}

class ZSpawnCard private constructor(
	private val category: ZZombieCategory,
	private val easyCount: Int,
	private val mediumCount: Int,
	private val hardCount: Int,
	vararg actions: Action
) : Reflector<ZSpawnCard>() {
	companion object {

		val log = LoggerFactory.getLogger(ZSpawnCard::class.java)
		val NOTHING_IN_SIGHT = Action(ActionType.NOTHING_IN_SIGHT, 0, null)
		val DOUBLE_SPAWN = Action(ActionType.DOUBLE_SPAWN, 0, null)
		val EXTRA_ACTIVATION = Action(ActionType.EXTRA_ACTIVATION_STANDARD, 0, null)
		val NECROMANCER = Action(ActionType.SPAWN, 1, ZZombieType.Necromancer)
		val ORC_NECROMANCER = Action(ActionType.SPAWN, 1, ZZombieType.OrcNecromancer)
		val ENTER_THE_HOARD = Action(ActionType.ENTER_THE_HOARD, 0, null)

		val cards = arrayOf(
			ZSpawnCard(
				ZZombieCategory.STANDARD, 10, 8, 4,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 10, 5, 2,
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 3, ZZombieType.Walker),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 0, 1, 2,
				Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 10, 8, 6,
				Action(ActionType.SPAWN, 1, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 8, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 1, 2, 3,
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
				Action(ActionType.SPAWN, 6, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 8, 8, 8,
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 2, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 3, ZZombieType.Runner)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 4, 6, 6,
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 6, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 2, 6, 6,
				Action(ActionType.SPAWN, 1, ZZombieType.Runner),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 5, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 8, 6, 4,
				Action(ActionType.SPAWN, 3, ZZombieType.Walker),
				Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 4, ZZombieType.Walker),
				Action(ActionType.SPAWN, 6, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 8, 6, 4,
				Action(ActionType.SPAWN, 1, ZZombieType.Walker),
				Action(ActionType.SPAWN, 2, ZZombieType.Fatty),
				Action(ActionType.SPAWN, 2, ZZombieType.Runner),
				Action(ActionType.SPAWN, 8, ZZombieType.Walker)
			),
			ZSpawnCard(
				ZZombieCategory.STANDARD, 1, 2, 4,
				NECROMANCER, NECROMANCER, NECROMANCER, NECROMANCER
			),
			ZSpawnCard(
				ZZombieCategory.DOUBLE_SPAWN, 0, 1, 2,
				DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN
			),
			ZSpawnCard(
				ZZombieCategory.EXTRA_ACTIVATION, 0, 3, 6,
				NOTHING_IN_SIGHT, EXTRA_ACTIVATION, EXTRA_ACTIVATION, EXTRA_ACTIVATION
			),
			// ------------------ WOLFSBURG
			ZSpawnCard(
				ZZombieCategory.WOLFSBURG, 4, 2, 1,
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 2, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 3, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 4, ZZombieType.Wolfz)
			),
			ZSpawnCard(
				ZZombieCategory.WOLFSBURG, 3, 2, 1,
				Action(ActionType.SPAWN, 2, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 3, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 4, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 5, ZZombieType.Wolfz)
			),
			ZSpawnCard(
				ZZombieCategory.WOLFSBURG, 1, 2, 5,
				Action(ActionType.SPAWN, 3, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 4, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 5, ZZombieType.Wolfz),
				Action(ActionType.SPAWN, 6, ZZombieType.Wolfz)
			),
			ZSpawnCard(
				ZZombieCategory.WOLFSBURG, 1, 2, 4,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
				Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination)
			),
			// ------------------ GREEN HOARD
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD, 4, 3, 2,
				Action(ActionType.SPAWN, 1, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 2, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 3, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 4, ZZombieType.OrcWalker)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 5, 4, 3,
				Action(ActionType.SPAWN, 1, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 1, ZZombieType.OrcFatty),
				Action(ActionType.SPAWN, 4, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 5, ZZombieType.OrcWalker)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 5, 3, 2,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 2, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 3, ZZombieType.OrcWalker),
				Action(ActionType.SPAWN, 4, ZZombieType.OrcWalker)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 5, 3, 3,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 2, ZZombieType.OrcFatty),
				Action(ActionType.SPAWN, 3, ZZombieType.OrcFatty),
				Action(ActionType.SPAWN, 4, ZZombieType.OrcFatty)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 3, 4, 5,
				NOTHING_IN_SIGHT,
				ORC_NECROMANCER,
				ORC_NECROMANCER,
				ORC_NECROMANCER
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 4, 4, 4,
				Action(ActionType.ASSEMBLE_HOARD, 1, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 4, ZZombieType.OrcRunner),
				Action(ActionType.ASSEMBLE_HOARD, 3, ZZombieType.OrcFatty),
				Action(ActionType.ASSEMBLE_HOARD, 6, ZZombieType.OrcWalker)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 4, 4, 4,
				Action(ActionType.ASSEMBLE_HOARD, 1, ZZombieType.OrcFatty),
				Action(ActionType.ASSEMBLE_HOARD, 3, ZZombieType.OrcRunner),
				Action(ActionType.ASSEMBLE_HOARD, 6, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 6, ZZombieType.OrcRunner)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 4, 4, 4,
				Action(ActionType.ASSEMBLE_HOARD, 1, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 2, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 4, ZZombieType.OrcRunner),
				Action(ActionType.ASSEMBLE_HOARD, 7, ZZombieType.OrcRunner)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ASSEMBLE, 2, 3, 4,
				Action(ActionType.ASSEMBLE_HOARD, 1, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 2, ZZombieType.OrcWalker),
				Action(ActionType.ASSEMBLE_HOARD, 4, ZZombieType.OrcRunner),
				Action(ActionType.ASSEMBLE_HOARD, 7, ZZombieType.OrcRunner)
			),
			ZSpawnCard(
				ZZombieCategory.GREEN_HOARD_ENTER, 2, 3, 4,
				NOTHING_IN_SIGHT,
				Action(ActionType.ENTER_THE_HOARD),
				Action(ActionType.ENTER_THE_HOARD),
				Action(ActionType.ENTER_THE_HOARD),
			),
			ZSpawnCard(
				ZZombieCategory.SWAMPTROLL, 1, 2, 3,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 1, ZZombieType.SwampTroll),
				Action(ActionType.SPAWN, 1, ZZombieType.SwampTroll),
				Action(ActionType.SPAWN, 1, ZZombieType.SwampTroll)
			),
			ZSpawnCard(
				ZZombieCategory.RATKING, 1, 2, 3,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 1, ZZombieType.RatKing),
				Action(ActionType.SPAWN, 1, ZZombieType.RatKing),
				Action(ActionType.SPAWN, 1, ZZombieType.RatKing)
			),
			ZSpawnCard(
				ZZombieCategory.RAT_SWARMS, 5, 8, 12,
				Action(ActionType.SPAWN, 1, ZZombieType.Ratz),
				Action(ActionType.SPAWN, 1, ZZombieType.Ratz),
				Action(ActionType.SPAWN, 1, ZZombieType.Ratz),
				Action(ActionType.SPAWN, 1, ZZombieType.Ratz)
			),
			ZSpawnCard(
				ZZombieCategory.MURDER_CROWS, 5, 8, 12,
				Action(ActionType.SPAWN, 1, ZZombieType.Crowz),
				Action(ActionType.SPAWN, 1, ZZombieType.Crowz),
				Action(ActionType.SPAWN, 1, ZZombieType.Crowz),
				Action(ActionType.SPAWN, 1, ZZombieType.Crowz)
			),
			ZSpawnCard(
				ZZombieCategory.NECRO_DRAGON, 3, 7, 12,
				NOTHING_IN_SIGHT,
				Action(ActionType.SPAWN, 1, ZZombieType.NecromanticDragon),
				Action(ActionType.SPAWN, 1, ZZombieType.NecromanticDragon),
				Action(ActionType.SPAWN, 1, ZZombieType.NecromanticDragon)
			)

		)

		fun drawSpawnCard(wolfburg: Boolean, canSpawnNecromancers: Boolean, difficulty: ZDifficulty?): ZSpawnCard {
			val weights = IntArray(cards.size)
			for (i in cards.indices) {
				val card = cards[i]
				if (!wolfburg && card.category == ZZombieCategory.WOLFSBURG) continue
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

		val model = object : Model {
			override fun getCellColor(g: AGraphics, row: Int, col: Int): GColor {
				return ZColor.values().reversed()[row].color
			}

			override fun getBackgroundColor(): GColor = GColor.GRAY

			override fun getHeaderColor(g: AGraphics): GColor = GColor.BLACK

			override fun getCornerRadius(): Float = 10f

			override fun getBorderColor(g: AGraphics): GColor = GColor.BLACK

			override fun getHeaderJustify(col: Int): Justify = Justify.CENTER

			override fun getCellVerticalPadding(): Float = 10f
		}


		fun buildDeck(
			deckType: ZDeckType,
			difficulty: ZDifficulty,
			rules: ZRules
		): List<ZSpawnCard> {
			val deck = mutableListOf<ZSpawnCard>()
			cards.filter { it.category.isEnabled(rules) && it.category.decks.contains(deckType) }
				.forEach { card ->
					repeat(card.getCount(difficulty)) { deck.add(card) }
				}
			return deck.shuffled()
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
		EXTRA_ACTIVATION_WOLFSBURG,
		ASSEMBLE_HOARD, // Spawn and assemble
		ENTER_THE_HOARD
	}

	class Action(val action: ActionType=ActionType.NOTHING_IN_SIGHT, val count: Int = 0, val type: ZZombieType?=null) : Reflector<Action>() {
		override fun toString(): String {
			return if (count > 0) {
				String.format("%s %d X %s", action, count, type)
			} else action.prettify()
		}
	}

	constructor() : this(ZZombieCategory.STANDARD, 0, 0, 0)

	private val actions: Array<Action> = arrayOf(*actions)

	fun getAction(color: ZColor): Action {
		return actions[color.ordinal]
	}

	override fun toString(): String {
		return "ZSpawnCard{" +
			"category='" + category + '\'' +
			", easyCount=" + easyCount +
			", mediumCount=" + mediumCount +
			", hardCount=" + hardCount +
			", actions=" + actions.contentToString() +
			'}'
	}

	fun toTable(color: ZColor): Table = Table().setModel(model).also {
		it.addColumn(category.label.wrap(20), *actions.mapIndexed { index: Int, action: Action ->
			if (color.ordinal == index) {
				"> $action <"
			} else {
				"- $action -"
			}
		}.reversed().toTypedArray())
	}

	fun getCount(difficulty: ZDifficulty): Int = when (difficulty) {
		ZDifficulty.EASY -> easyCount
		ZDifficulty.MEDIUM -> mediumCount
		ZDifficulty.HARD -> hardCount
	}
}