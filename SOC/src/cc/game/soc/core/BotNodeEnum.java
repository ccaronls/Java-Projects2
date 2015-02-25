package cc.game.soc.core;

public class BotNodeEnum extends BotNode {

	static {
		addAllFields(BotNodeEnum.class);
	}
	
	private Enum<?> enumData;
	
	public BotNodeEnum() {}
	
	BotNodeEnum(Enum<?> e) {
		this.enumData = e;
	}

	@Override
	public Object getData() {
		return enumData;
	}

	@Override
	public String getDescription() {
		return enumData.name();
	}
	
	
	
}
