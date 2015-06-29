package cc.game.soc.core;

public interface ICardType<T> {
	CardType getCardType();
	
	int ordinal();
	
	String name();
	
	String helpText(Rules rules);
	
	T getData();
	
	CardStatus defaultStatus();
}
