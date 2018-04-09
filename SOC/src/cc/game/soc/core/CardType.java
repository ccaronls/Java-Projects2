package cc.game.soc.core;

import cc.game.soc.android.R;

public enum CardType implements ILocalized{
	Resource(0, ResourceType.values()),				// resources for building
	Commodity(0, CommodityType.values()),				// used to buy city upgrades
	Development(R.string.card_type_development, DevelopmentCardType.values()),		// SOC
	Progress(R.string.card_type_progress, ProgressCardType.values()),			// CAK
	SpecialVictory(0, SpecialVictoryType.values()),	// CAK
	Event(0, EventCardType.values()),				// No dice variation
	BarbarianAttackDevelopment(0, BarbarianAttackDevelopmentCardType.values()),
	
	;

	CardType(int resourceId, Enum<?> [] typeValues) {
		this.resourceId = resourceId;
	    this.typeValues = typeValues;
	}

	final int resourceId;
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
    public String getName(StringResource sr) {
	    if (resourceId == 0)
	        return "";
        return sr.getString(resourceId);
    }
}
