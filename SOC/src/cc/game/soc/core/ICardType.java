package cc.game.soc.core;

public interface ICardType<T> extends ILocalized {
	CardType getCardType();
	
	int ordinal();
	
	String name();
	
	String getHelpText(Rules rules);
	
	T getData();
	
	CardStatus defaultStatus();
}
