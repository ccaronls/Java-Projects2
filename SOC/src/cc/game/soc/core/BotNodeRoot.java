package cc.game.soc.core;

public class BotNodeRoot extends BotNode {

	private final String desc;
	
	BotNodeRoot(String desc) {
		this.desc = desc;
	}
	
	@Override
	public Object getData() {
		return null;
	}

	@Override
	public String getDescription() {
		return desc;
	}

	
}
