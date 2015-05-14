package cc.game.soc.core;

public enum CardType {
	Resource(ResourceType.values()),				// resources for building
	Commodity(CommodityType.values()),				// used to buy city upgrades
	Development(DevelopmentCardType.values()),		// SOC
	Progress(ProgressCardType.values()),			// CAK
	SpecialVictory(SpecialVictoryType.values()),	// CAK
	Event(EventCardType.values()),				// No dice variation
	;

	CardType(Enum<?> [] typeValues) {
		this.typeValues = typeValues;
	}
	
	final Enum<?> [] typeValues;
	
	public ICardType dereferenceOrdinal(int typeOrdinal) {
		return (ICardType)typeValues[typeOrdinal];
	}
}
