package cc.game.soc.core;

import cc.lib.utils.Reflector;

/**
 * 
 * @author Chris Caron
 * 
 */
public final class Trade extends Reflector<Trade> {
	
	static {
		addAllFields(Trade.class);
	}
	
	private Card type;
	private int	amount = 0;

	public Trade() {}

    public String toString() {
        return type + " X " + amount;
    }
    
    public Trade(Card card, int amount) {
		this.type = card;
		this.amount = amount;
	}
    
	public Trade(ICardType type, int amount) {
		this.type = new Card(type, CardStatus.USABLE);
		this.amount = amount;
	}

	public ICardType getType() {
	    return type.getCardType().dereferenceOrdinal(type.getTypeOrdinal());
	}
	
    public int getAmount() {
        return amount;
    }
    
}
