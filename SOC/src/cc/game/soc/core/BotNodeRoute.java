package cc.game.soc.core;

public class BotNodeRoute extends BotNode {

	static {
		addAllFields(BotNodeRoute.class);
	}
	
	private Route route;
	private int index;
	
	public BotNodeRoute() {}
	
	BotNodeRoute(Route route, int index) {
		this.route = route;
		this.index = index;
	}
	
	@Override
	public Object getData() {
		return route;
	}

	@Override
	public String getDescription() {
		return "" + index + "  " + route.toString();
	}

	
}
