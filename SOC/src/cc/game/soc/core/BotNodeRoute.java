package cc.game.soc.core;

import cc.lib.game.IVector2D;

public class BotNodeRoute extends BotNode {

	private Route route;
	private int index;
	
	public BotNodeRoute() {}
	
	BotNodeRoute(Route route, int index) {
		this.route = route;
		this.index = index;
	}
	
	@Override
	public Object getData() {
		return index;
	}

	@Override
	public String getDescription() {
		return "E(" + index + ") " + route.toString();
	}

	public IVector2D getBoardPosition(Board b) {
    	return b.getRouteMidpoint(route);
    }

}
