package cc.game.soc.core

import cc.lib.reflector.RBufferedReader
import cc.lib.reflector.RPrintWriter
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import java.io.IOException

open class Card(var cardType: CardType, var typeOrdinal: Int, var cardStatus: CardStatus)
	: Reflector<Card>(), Comparable<Card> {
	companion object {
		init {
			addAllFields(Card::class.java)
		}
	}

	constructor(type: ICardType<*>) : this(type.cardType, type.ordinal, type.defaultStatus())

	constructor(type: ICardType<*>, status: CardStatus) : this(type.cardType, type.ordinal, status)

	constructor() : this(CardType.Resource, 0, CardStatus.UNUSABLE)

	@Throws(IOException::class)
	public override fun serialize(out: RPrintWriter) {
		out.println(cardType.toString() + "," + cardStatus + "," + cardType.dereferenceOrdinal(typeOrdinal).name)
	}

	@Throws(Exception::class)
	override fun deserialize(`in`: RBufferedReader) {
		val parts = `in`.readLine().split(",".toRegex()).toTypedArray()
		cardType = CardType.valueOf(parts[0])
		cardStatus = CardStatus.valueOf(parts[1])
		typeOrdinal = when (cardType) {
			CardType.Commodity -> CommodityType.valueOf(parts[2]).ordinal
			CardType.Development -> DevelopmentCardType.valueOf(parts[2]).ordinal
			CardType.Event -> EventCardType.valueOf(parts[2]).ordinal
			CardType.Progress -> ProgressCardType.valueOf(parts[2]).ordinal
			CardType.Resource -> ResourceType.valueOf(parts[2]).ordinal
			CardType.SpecialVictory -> SpecialVictoryType.valueOf(parts[2]).ordinal
			else -> throw GException("Unhandled case")
		}
	}

	override fun isImmutable(): Boolean {
		return true
	}

	override fun toString(): String {
		return cardType.dereferenceOrdinal(typeOrdinal).getNameId() + " " + cardStatus
	}

	/**
	 *
	 * @return
	 */
	val name: String
		get() = cardType.dereferenceOrdinal(typeOrdinal).getNameId()

	/**
	 *
	 * @return
	 */
	val isUsable: Boolean
		get() = cardStatus === CardStatus.USABLE

	/**
	 *
	 */
	fun setUsable() {
		cardStatus = CardStatus.USABLE
	}

	/**
	 *
	 * @return
	 */
	val isUsed: Boolean
		get() = cardStatus === CardStatus.USED

	/**
	 *
	 */
	fun setUsed() {
		cardStatus = CardStatus.USED
	}

	/**
	 *
	 */
	fun setUnusable() {
		cardStatus = CardStatus.UNUSABLE
	}

	/**
	 *
	 * @return
	 */
	fun getHelpText(rules: Rules): String? {
		return cardType.dereferenceOrdinal(typeOrdinal).getHelpText(rules)
	}

	/**
	 *
	 * @return
	 */
	val data: Any?
		get() = cardType.dereferenceOrdinal(typeOrdinal).getData()

	override fun equals(obj: Any?): Boolean {
		if (obj === this) return true
		if (obj is ICardType<*>) {
			return obj.cardType == cardType && obj.ordinal == typeOrdinal
		}
		if (obj is Card)
			return cardType == obj.cardType && typeOrdinal == obj.typeOrdinal && cardStatus === obj.cardStatus
		return false
	}

	override fun compareTo(o: Card): Int {
		if (cardType !== o.cardType) return cardType.compareTo(o.cardType)
		return if (typeOrdinal != o.typeOrdinal) typeOrdinal - o.typeOrdinal else cardStatus.compareTo(o.cardStatus)
		// we want usable cards to appear earliest in the list, then unusable, then used
	}
	/**
	 *
	 * @param cardType
	 * @param typeOrdinal
	 * @param cardStatus
	 */
}