package cc.game.soc.core;

public interface ICardType<T> {
	CardType getCardType();
	
	int ordinal();
	
	String name();
	
	String getHelpText(Rules rules, StringResource sr);
	
	T getData();
	
	CardStatus defaultStatus();

	String getName(StringResource sr);
}
