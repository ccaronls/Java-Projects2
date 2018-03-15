package cc.game.soc.core;

public class BotNodePlayer extends BotNode {

	static {
		addAllFields(BotNodePlayer.class);
	}
	
	private Player player;
	
	public BotNodePlayer() {}
	
	BotNodePlayer(Player player) {
		this.player = player;
	}

	@Override
	public Object getData() {
		return player.getPlayerNum();
	}

	@Override
	public String getDescription() {
		return player.getName();
	}
	
	
}
