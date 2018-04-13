package cc.game.soc.core;

public class BotNodeCard extends BotNode {

	private Card card;
	
	public BotNodeCard() {}
	
	BotNodeCard(Card card) {
		this.card = card;
	}
	
	BotNodeCard(ICardType<?> type) {
		this.card = new Card(type, CardStatus.USABLE);
	}

	@Override
	public Object getData() {
		return card;
	}

	@Override
	public String getDescription() {
		return card.toString();
	}

	
}
