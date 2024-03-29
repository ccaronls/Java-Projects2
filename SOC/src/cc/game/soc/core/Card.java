package cc.game.soc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import cc.lib.utils.Reflector;

public class Card extends Reflector<Card> implements Comparable <Card> {
	
	static {
		addAllFields(Card.class);
	}
	
	private CardType type;
	private int typeOrdinal;
	private CardStatus status;

	@Override
	public void serialize(PrintWriter out) throws IOException {
		out.println(type+","+status+","+type.dereferenceOrdinal(typeOrdinal).name());
	}

	@Override
	protected void deserialize(BufferedReader in) throws Exception {
	    String [] parts = in.readLine().split(",");
		type = CardType.valueOf(parts[0]);
		status = CardStatus.valueOf(parts[1]);
		switch (type) {
			case Commodity:
				typeOrdinal = CommodityType.valueOf(parts[2]).ordinal();
				break;
			case Development:
				typeOrdinal = DevelopmentCardType.valueOf(parts[2]).ordinal();
				break;
			case Event:
				typeOrdinal = EventCardType.valueOf(parts[2]).ordinal();
				break;
			case Progress:
				typeOrdinal = ProgressCardType.valueOf(parts[2]).ordinal();
				break;
			case Resource:
				typeOrdinal = ResourceType.valueOf(parts[2]).ordinal();
				break;
			case SpecialVictory:
				typeOrdinal = SpecialVictoryType.valueOf(parts[2]).ordinal();
				break;
			default:
				throw new cc.lib.utils.GException("Unhandled case");
			
		}
	}

    @Override
    protected boolean isImmutable() {
        return true;
    }

    /**
	 * Needed for Reflector
	 */
	public Card() {
		this(null, 0, null);
	}
	
	/**
	 * 
	 * @param type
	 */
	public Card(ICardType<?> type) {
		this(type.getCardType(), type.ordinal(), CardStatus.USABLE);
	}
	
	/**
	 * 
	 * @param type
	 * @param status
	 */
	public Card(ICardType<?> type, CardStatus status) {
		this(type.getCardType(), type.ordinal(), status);
	}
	
	/**
	 * 
	 * @param type
	 * @param ordinal
	 * @param status
	 */
	public Card(CardType type, int ordinal, CardStatus status) {
		this.type = type;
		this.status = status;
		this.typeOrdinal = ordinal;
	}
	
	@Override
    public String toString() {
    	return type.dereferenceOrdinal(typeOrdinal).name() + " " + status;
    }

	/**
	 * 
	 * @return
	 */
    public final String getName(StringResource sr) {
    	return type.dereferenceOrdinal(typeOrdinal).getName(sr);
    }
    
    /**
     * 
     * @return
     */
	public final boolean isUsable() {
		return status == CardStatus.USABLE;
	}
	
	/**
	 * 
	 */
	public final void setUsable() {
		status = CardStatus.USABLE;
	}
	
	/**
	 * 
	 * @return
	 */
	public final boolean isUsed() {
		return status == CardStatus.USED;
	}
	
	/**
	 * 
	 */
	public final void setUsed() {
		status = CardStatus.USED;
	}
	
	/**
	 * 
	 */
	public final void setUnusable() {
		status = CardStatus.UNUSABLE;
	}
	
	/**
	 * @return Returns the type.
	 */
	public final CardType getCardType() {
		return type;
	}
	
	/**
	 * 
	 * @return
	 */
	public final CardStatus getCardStatus() {
		return this.status;
	}
	
	/**
	 * An ordinal into the type of card this card represents (development or progress)
	 * @return
	 */
	public final int getTypeOrdinal() {
		return typeOrdinal;
	}
	
	/**
	 * 
	 * @return
	 */
	public final String getHelpText(Rules rules, StringResource sr) {
		return type.dereferenceOrdinal(typeOrdinal).getHelpText(rules, sr);
	}
	
	/**
	 * 
	 * @return
	 */
	public Object getData() {
		return type.dereferenceOrdinal(typeOrdinal).getData();
	}
	
	/**
	 * 
	 * @param card
	 * @return
	 */
	public boolean equals(ICardType<?> card) {
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
