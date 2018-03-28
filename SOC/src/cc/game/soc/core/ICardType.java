package cc.game.soc.core;

public interface ICardType<T> extends ILocalized {
	CardType getCardType();
	
	int ordinal();
	
	String name();
	
	String getHelpText(Rules rules, StringResource sr);
	
	T getData();
	
	CardStatus defaultStatus();
}
