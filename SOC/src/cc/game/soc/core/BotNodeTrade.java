package cc.game.soc.core;

public class BotNodeTrade extends BotNode {

	private Trade trade;
	
	public BotNodeTrade() {}
	
	BotNodeTrade(Trade trade) {
		this.trade = trade;
	}

	@Override
	public Object getData() {
		return trade;
	}

	@Override
	public String getDescription() {
		return trade.toString();
	}
	
	
}
