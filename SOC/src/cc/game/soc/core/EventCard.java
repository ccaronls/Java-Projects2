package cc.game.soc.core;

public class EventCard extends Card {

    static {
        addAllFields(EventCard.class);
    }

	private int production=-1;

    public EventCard() {}

	public EventCard(EventCardType t, int production) {
		super(t);
		this.production = production;
	}

	public final int getProduction() {
		return production;
	}

	public EventCardType getType() {
		return EventCardType.values()[getTypeOrdinal()];
	}
	
}
