package cc.game.soc.core;

public enum CardType implements ILocalized{
	Resource("Resource", ResourceType.values()),				// resources for building
	Commodity("Commodity", CommodityType.values()),				// used to buy city upgrades
	Development("Development", DevelopmentCardType.values()),		// SOC
	Progress("Progress", ProgressCardType.values()),			// CAK
	SpecialVictory("", SpecialVictoryType.values()),	// CAK
	Event("Event", EventCardType.values()),				// No dice variation
	BarbarianAttackDevelopment("", BarbarianAttackDevelopmentCardType.values()),
	
	;

	CardType(String resourceId, Enum<?> [] typeValues) {
		this.resourceId = resourceId;
	    this.typeValues = typeValues;
	}

	final String resourceId;
	final Enum<?> [] typeValues;
	
	public ICardType<?> dereferenceOrdinal(int typeOrdinal) {
		return (ICardType<?>)typeValues[typeOrdinal];
	}
	
	/**
	 * Return number of unique values for this type
	 * @return
	 */
	public int getCount() {
		return typeValues.length;
	}


    @Override
    public String getName() {
	    return resourceId;
    }
}
