package cc.game.soc.core

enum class DiceEvent(val _nameId: String) : ILocalized {
	AdvanceBarbarianShip("Advance Barbarian Ship"),
	ScienceCard("Science Card"),
	TradeCard("Trade Card"),
	PoliticsCard("Politics Card");

	override fun getNameId(): String = _nameId

	companion object {
		/**
		 * Return thre event for a die num roll.  Valid range is [1-6] inclusive
		 * @param num
		 * @return
		 */
        @JvmStatic
        fun fromDieNum(num: Int): DiceEvent {
			when (num) {
				1, 2, 3 -> return AdvanceBarbarianShip
				4 -> return PoliticsCard
				5 -> return ScienceCard
				6 -> return TradeCard
			}
			throw AssertionError()
		}
	}
}