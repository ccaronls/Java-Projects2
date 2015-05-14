package cc.game.soc.core;

public interface ICardType<T> {
	CardType getCardType();
	
	int ordinal();
	
	String name();
	
	String helpText();
	
	T getData();
}
