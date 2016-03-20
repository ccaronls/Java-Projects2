package cc.game.soc.core;

import cc.lib.game.IVector2D;
import cc.lib.math.Vector2D;

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
		return "E(" + index + ") " + route.toString();
	}

	public IVector2D getBoardPosition(Board b) {
    	return b.getRouteMidpoint(route);
    }

}
