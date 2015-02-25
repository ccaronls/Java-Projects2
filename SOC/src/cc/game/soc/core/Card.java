package cc.game.soc.core;

import cc.lib.utils.Reflector;

public final class Card extends Reflector<Card> implements Comparable <Card> {
	
	static {
		addAllFields(Card.class);
	}
	
	private final CardType type;
	private final int typeOrdinal;
	private CardStatus status;
	
	public Card() {
		this(null, 0, null);
	}
	
	/**
	 * 
	 * @param type
	 * @param flag
	 */
	public Card(ICardType type, CardStatus status) {
		this(type.getCardType(), type.ordinal(), status);
	}
	
	public Card(CardType type, int ordinal, CardStatus status) {
		this.type = type;
		this.status = status;
		this.typeOrdinal = ordinal;
	}
	
	@Override
    public String toString() {
    	return type.dereferenceOrdinal(typeOrdinal).name() + " " + status;
    }
    
    public String getName() {
    	switch (type) {
			case Commodity:
				return CommodityType.values()[typeOrdinal].name();
			case Development:
				return DevelopmentCardType.values()[typeOrdinal].name();
			case Progress:
				return ProgressCardType.values()[typeOrdinal].name();
			case Resource:
				return ResourceType.values()[typeOrdinal].name();
			case SpecialVictory:
				return SpecialVictoryType.values()[typeOrdinal].name();
    	}
    	throw new RuntimeException("Unhandled case");
    }
    
	public boolean isUsable() {
		return status == CardStatus.USABLE;
	}
	
	public void setUsable(boolean usable) {
		status = usable ? CardStatus.USABLE : CardStatus.UNUSABLE;
	}
	
	public boolean isUsed() {
		return status == CardStatus.USED;
	}
	
	public void setUsed(boolean used) {
		status = used ? CardStatus.USED : CardStatus.USABLE;
	}
	
	/**
	 * @return Returns the type.
	 */
	public CardType getCardType() {
		return type;
	}
	
	/**
	 * 
	 * @return
	 */
	public CardStatus getCardStatus() {
		return this.status;
	}
	
	/**
	 * An ordinal into the type of card this card represents (development or progress)
	 * @return
	 */
	public int getTypeOrdinal() {
		return typeOrdinal;
	}
	
	public boolean equals(ICardType card) {
		return (card.getCardType() == getCardType() && card.ordinal() == getTypeOrdinal());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Card))
			return false;
		Card card = (Card)obj;
		return type == card.type && typeOrdinal == card.typeOrdinal && status == card.status;
	}
	
	@Override
	public int compareTo(Card o) {
		if (type != o.type)
			return type.compareTo(o.type);
		if (typeOrdinal != o.typeOrdinal)
			return typeOrdinal - o.typeOrdinal;
		// we want usable cards to appear earliest in the list, then unusable, then used
		return status.compareTo(o.status);
	}
}
