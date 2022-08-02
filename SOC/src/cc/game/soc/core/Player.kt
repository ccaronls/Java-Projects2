package cc.game.soc.core

import cc.game.soc.core.DevelopmentArea
import cc.lib.game.Utils
import cc.lib.utils.Reflector
import java.util.*

/**
 *
 * @author Chris Caron
 */
abstract class Player(var playerNum:Int =-1) : Reflector<Player>() {
	companion object {
		init {
			addAllFields(Player::class.java)
		}
	}

	private val cards = Collections.synchronizedList(Vector<Card>())
	var roadLength = 0
	var points = 0
	var harborPoints = 0
	var numDiscoveredTerritories = 0
		private set
	private val cityDevelopment = IntArray(DevelopmentArea.values().size)
	var merchantFleetTradable: Card? = null

	/**
	 * Special case player type when Rules.isCatanForTwo enabled
	 *
	 * Neutral players don't get a turn, instead the other players take turns for them.
	 * When a player places a road, they do so also for the neutral player (for free)
	 * When a player places a settlement, they do so also for the neutral player if possible, a road otherwise
	 * When a player upgrades a settlement, the neutral player is unaffected
	 * When a player places a knight then a knight is also placed for the neutral player
	 * the neutral knights cannot be activated or have a bearing on the barbarian attack
	 * When a player promotes a knight then a neutral knight is also promoted, but not past a strong knight
	 * When a neutral player is present, then the other players can collect tokens by doing one of the following:
	 * - Place a settlement by the desert collects 2 tokens
	 * - Place a settlement on the coast yields 1 token
	 * - Exchange a used Soldier gets 1 token
	 * - Exchange a knight collects the level of the knight back in tokens
	 * The non-neutral players roll the dice twice and collect all resources from the 2 rolls. The die rolls must be different values.
	 *
	 * @return
	 */
	var isNeutralPlayer = false
		private set

	protected constructor(other: Player) : this() {
		cards.addAll(other.cards)
		playerNum = other.playerNum
		roadLength = other.roadLength
		points = other.points
		harborPoints = other.harborPoints
		numDiscoveredTerritories = other.numDiscoveredTerritories
		System.arraycopy(other.cityDevelopment, 0, cityDevelopment, 0, cityDevelopment.size)
		merchantFleetTradable = other.merchantFleetTradable
		isNeutralPlayer = other.isNeutralPlayer
	}

	/*
	@Override
    public final String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getName());
        synchronized (cards) {
            buf.append(" cards:").append(cards);
        }
        buf.append(" roadLen:").append(roadLength);
        buf.append(" points:").append(points);
        buf.append(" harbor pts:").append(harborPoints);
        buf.append(" num discovered territories:").append(numDiscoveredTerritories);
        for (DevelopmentArea area : DevelopmentArea.values()) {
        	if (cityDevelopment[area.ordinal()] > 0) {
        		buf.append(" " + area.name() + ":").append(cityDevelopment[area.ordinal()]);
        	}
        }
        if (merchantFleetTradable != null) {
        	buf.append(" merchantFleet:" + merchantFleetTradable);
        }
        return buf.toString();
    }*/
	/**
	 * set all un-usable cards to usable or usable cards to unusable
	 */
	fun setCardsUsable(type: CardType, usable: Boolean): List<Card> = cards.filter {
		it.cardType == type && it.isUsable == usable
	}.also {
		it.forEach { card ->
			card.cardStatus = if (usable) CardStatus.UNUSABLE else CardStatus.USABLE
		}
	}

	/**
	 *
	 * @param card
	 */
	fun addCard(card: Card) {
		cards.add(card)
		cards.sort()
	}

	/**
	 *
	 * @param type
	 */
	fun addCard(type: ICardType<*>) {
		cards.add(Card(type))
		cards.sort()
	}

	/**
	 *
	 * @param type
	 * @param num
	 */
	fun addCards(type: ICardType<*>, num: Int) {
		for (i in 0 until num) addCard(Card(type))
	}

	/**
	 *
	 * @param type
	 * @param num
	 */
	fun removeCards(type: ICardType<*>, num: Int) {
		for (i in 0 until num) removeCard(type)
	}

	/**
	 *
	 * @return
	 */
	val unusedProgressCards: List<Card>
		get() = getUnusedCards(CardType.Progress)

	/**
	 * Return list of unused resource and commodity cards
	 * @return
	 */
	val unusedBuildingCards: List<Card>
		get() = getUnusedCards(CardType.Resource).toMutableList().also {
			it.addAll(getUnusedCards(CardType.Commodity))
		}

	/**
	 *
	 * @param buildable
	 * @param multiple
	 */
	fun adjustResourcesForBuildable(buildable: BuildableType, multiple: Int) {
		for (t in ResourceType.values()) {
			incrementResource(t, buildable.getCost(t) * multiple)
		}
	}

	/**
	 *
	 */
	open fun reset() {
		synchronized(cards) { cards.clear() }
		roadLength = 0
		points = 0
		harborPoints = 0
		numDiscoveredTerritories = 0
		merchantFleetTradable = null
		Arrays.fill(cityDevelopment, 0)
	}

	/**
	 *
	 * @return
	 */
	fun getCards(): Iterable<Card> = cards.toList()

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getCards(type: CardType): List<Card> = cards.filter { it.cardType == type }

	fun getUniqueCards(type: CardType): List<Card> = cards.distinctBy { it.cardType }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsableCards(type: ICardType<*>) : List<Card> = cards.filter { it == type && it.isUsable }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsableCards(type: CardType): List<Card> = cards.filter { it.cardType == type && it.isUsable }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUnusedCards(type: CardType): List<Card> = cards.filter { it.cardType == type && !it.isUsed }

	/**
	 *
	 * @return
	 */
	val unusedCards: List<Card>
		get() = cards.filter { !it.isUsed }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getCardCount(type: CardType): Int = cards.count { it.cardType == type }

	val giftableCards: List<Card>
		get() {
			val giftable: MutableList<Card> = ArrayList()
			giftable.addAll(getCards(CardType.Commodity))
			giftable.addAll(getCards(CardType.Progress))
			giftable.addAll(getCards(CardType.Resource))
			giftable.addAll(getCards(CardType.Development))
			return giftable
		}

	/**
	 *
	 * @param type
	 * @param status
	 * @return
	 */
	fun getCardCount(type: CardType, status: CardStatus): Int = cards.count { it.cardType == type && it.cardStatus == status }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUnusableCardCount(type: CardType): Int = cards.count { it.cardType == type && !it.isUsable }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getCardCount(type: Card): Int = cards.count { it == type }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getCardCount(type: ICardType<*>): Int = cards.count{ it == type }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsedCardCount(type: ICardType<*>): Int = cards.count{ it == type && it.isUsed }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsableCardCount(type: ICardType<*>): Int = cards.count{ it == type && it.isUsable }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsableCardCount(type: CardType): Int = cards.count { it.cardType == type && it.isUsable }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUnusedCardCount(type: ICardType<*>): Int = cards.count { it == type && !it.isUsed }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUnusedCardCount(type: CardType): Int = cards.count { it.cardType == type && !it.isUsed }

	/**
	 *
	 * @return
	 */
	val unusedCardCount: Int
		get()= cards.count { !it.isUsed }

	/**
	 *
	 * @return
	 */
	fun removeRandomUnusedCard(): Card? = unusedCards.takeIf { it.isNotEmpty() }?.random().also {
		cards.remove(it)
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	fun removeRandomUnusedCard(type: CardType): Card? = getUnusedCards(type).takeIf{ it.isNotEmpty() }?.random().also {
		cards.remove(it)
	}

	/**
	 * Convenience method to get count of all resource cards in hand.
	 * @return
	 */
	val totalResourceCount: Int
		get() = getCardCount(CardType.Resource)

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getCard(type: ICardType<*>): Card? = cards.firstOrNull { it == type }

	/**
	 *
	 * @param type
	 * @return
	 */
	fun removeCard(type: ICardType<*>): Card? = cards.firstOrNull { it == type }?.also {
		cards.remove(it)
	}

	/**
	 *
	 * @param card
	 * @return
	 */
	fun removeCard(card: Card): Card? = if (cards.remove(card)) card else null

	/**
	 *
	 * @param type
	 * @return
	 */
	fun removeUsableCard(type: ICardType<*>): Card? = cards.firstOrNull { it == type && it.isUsable }?.also {
		cards.remove(it)
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	fun getUsableCard(type: ICardType<*>): Card? = cards.firstOrNull { it == type && it.isUsable }?.also {
		cards.remove(it)
	}

	/**
	 *
	 * @param type
	 * @param amount
	 */
	fun incrementResource(type: ICardType<*>, amount: Int) {
		if (amount < 0) removeCards(type, -amount) else addCards(type, amount)
	}

	/**
	 *
	 * @return
	 */
	fun canBuild(): Boolean {
		for (t in BuildableType.values()) if (canBuild(t)) return true
		return false
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	fun canBuild(type: BuildableType): Boolean {
		for (r in ResourceType.values()) if (getCardCount(r) < type.getCost(r)) return false
		return true
	}

	/**
	 *
	 * @param flag
	 * @return
	 */
	fun getTotalDevelopment(flag: Int): Int {
		return getCardCount(CardType.Development)
	}

	/**
	 *
	 * @return
	 */
	fun getArmySize(b: Board): Int {
		return getUsedCardCount(DevelopmentCardType.Soldier) + b.getNumRoutesOfType(playerNum, RouteType.WARSHIP)
	}

	/**
	 *
	 * @return
	 */
	val totalCardsLeftInHand: Int
		get() = cards.count { !it.isUsed }

	/**
	 *
	 * @param area
	 * @return
	 */
	fun getCityDevelopment(area: DevelopmentArea): Int {
		return cityDevelopment[area.ordinal]
	}

	/**
	 *
	 * @param area
	 * @param num
	 */
	fun setCityDevelopment(area: DevelopmentArea, num: Int) {
		cityDevelopment[area.ordinal] = num
	}

	/**
	 * Return the name of this player.  Default implementation just returns the player num.
	 * @return
	 */
	open val name: String
		get() = "Player $playerNum"

	/**
	 *
	 * @return
	 */
	fun hasFortress(): Boolean {
		return cityDevelopment[DevelopmentArea.Politics.ordinal] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY
	}

	/**
	 *
	 * @return
	 */
	fun hasTradingHouse(): Boolean {
		return cityDevelopment[DevelopmentArea.Trade.ordinal] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY
	}

	/**
	 *
	 * @return
	 */
	fun hasAqueduct(): Boolean {
		return cityDevelopment[DevelopmentArea.Science.ordinal] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY
	}

	/**
	 * Special victory points are points for longest road, largest army, printer, constitution
	 * @return
	 */
	val specialVictoryPoints: Int
		get() = getCards(CardType.SpecialVictory).sumBy {
			SpecialVictoryType.values()[it.typeOrdinal].points
		}

	/*
	 *
	 */
	fun incrementDiscoveredTerritory(amount: Int) {
		numDiscoveredTerritories += amount
	}

	fun setNeutral(neutral: Boolean) {
		isNeutralPlayer = neutral
	}

	/**
	 * Return a move or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 *
	 * @param soc
	 * @param moves
	 * @return
	 */
	abstract fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType?
	enum class VertexChoice {
		SETTLEMENT,
		CITY,
		CITY_WALL,
		NEW_KNIGHT,
		KNIGHT_TO_MOVE,
		KNIGHT_TO_ACTIVATE,
		KNIGHT_TO_PROMOTE,
		KNIGHT_MOVE_POSITION,
		KNIGHT_DISPLACED,
		KNIGHT_DESERTER,
		OPPONENT_KNIGHT_TO_DISPLACE,
		TRADE_METROPOLIS,
		POLITICS_METROPOLIS,
		SCIENCE_METROPOLIS,
		PIRATE_FORTRESS,
		OPPONENT_STRUCTURE_TO_ATTACK
	}

	/**
	 * @param soc
	 * @param vertexIndices
	 * @param mode
	 * @param knightToMove null unless mode is KNIGHT_DISPLACED or KNIGHT_TO_MOVE
	 * @return
	 */
	abstract fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightToMove: Int?): Int?
	enum class RouteChoice {
		ROAD,
		SHIP,
		UPGRADE_SHIP,
		SHIP_TO_MOVE,
		ROUTE_DIPLOMAT,  // player chooses from any open route.  If theirs, then they can move it, otherwise opponents are removed from the baord.
		OPPONENT_ROAD_TO_ATTACK,
		OPPONENT_SHIP_TO_ATTACK
		// player chooses an opponent ship adjacent to one of they're warships
	}

	abstract fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int?
	enum class RouteChoiceType {
		ROAD_CHOICE,
		SHIP_CHOICE
	}

	/**
	 *
	 * @param soc
	 * @return
	 */
	abstract fun chooseRouteType(soc: SOC): RouteChoiceType?
	enum class TileChoice {
		ROBBER,
		PIRATE,
		INVENTOR,
		MERCHANT
	}

	/**
	 *
	 * @param soc
	 * @param tileIndices
	 * @param mode
	 * @return
	 */
	abstract fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int?

	/**
	 * Return trade to make or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * This method can be canceled.
	 *
	 * @param soc
	 * @param trades
	 * @return
	 */
	abstract fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade?
	enum class PlayerChoice {
		PLAYER_TO_TAKE_CARD_FROM,
		PLAYER_TO_SPY_ON,
		PLAYER_FOR_DESERTION,
		PLAYER_TO_GIFT_CARD,
		PLAYER_TO_FORCE_HARBOR_TRADE
	}

	/**
	 * Return playerNum to take card from or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 *
	 * @param soc
	 * @param playerOptions
	 * @param mode
	 * @return
	 */
	abstract fun choosePlayer(soc: SOC, playerOptions: Collection<Int>, mode: PlayerChoice): Int?
	enum class CardChoice {
		RESOURCE_OR_COMMODITY,
		GIVEUP_CARD,  // cards are lost
		OPPONENT_CARD,  // choose from an opponents cards
		EXCHANGE_CARD,  // exchange for another players card
		FLEET_TRADE
		// any num of 2:1 trades of a single resource or commodity for 1 turn
	}

	/**
	 * Choose a card from a list
	 * @param soc
	 * @param cards
	 * @return
	 */
	abstract fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card?
	enum class EnumChoice {
		COMMODITY_MONOPOLY,
		RESOURCE_MONOPOLY,
		DRAW_PROGRESS_CARD,
		CRANE_CARD_DEVELOPEMENT
	}

	/**
	 *
	 * @param soc
	 * @param values
	 * @return
	 */
	abstract fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T?

	/**
	 * Players sets the dice to values of their choice each in range [1-6] inclusive
	 *
	 * @param soc
	 * @param dice array of dice to set
	 * @param num length of array to set
	 * @return true when player completed
	 */
	abstract fun setDice(soc: SOC, dice: List<Dice>, num: Int): Boolean
}