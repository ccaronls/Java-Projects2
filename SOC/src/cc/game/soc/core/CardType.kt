package cc.game.soc.core

import cc.game.soc.core.ProgressCardType

enum class CardType(val resourceId: String, val typeValues: Array<Enum<*>>) : ILocalized {
	Resource("Resource", ResourceType.values() as Array<Enum<*>>),  // resources for building
	Commodity("Commodity", CommodityType.values() as Array<Enum<*>>),  // used to buy city upgrades
	Development("Development", DevelopmentCardType.values() as Array<Enum<*>>),  // SOC
	Progress("Progress", ProgressCardType.values() as Array<Enum<*>>),  // CAK
	SpecialVictory("", SpecialVictoryType.values() as Array<Enum<*>>),  // CAK
	Event("Event", EventCardType.values() as Array<Enum<*>>),  // No dice variation
	BarbarianAttackDevelopment("", BarbarianAttackDevelopmentCardType.values() as Array<Enum<*>>);

	fun dereferenceOrdinal(typeOrdinal: Int): ICardType<*> {
		return typeValues[typeOrdinal] as ICardType<*>
	}

	/**
	 * Return number of unique values for this type
	 * @return
	 */
	fun getCount(): Int {
		return typeValues.size
	}

	override fun getNameId(): String {
		return resourceId
	}
}