package cc.game.soc.core;

import cc.lib.game.IVector2D;

public class BotNodeTile extends BotNode {

	private Tile tile;
	private int index;
	
	public BotNodeTile() {}
	
	BotNodeTile(Tile tile, int index) {
		this.tile = tile;
		this.index = index;
	}

	@Override
	public Object getData() {
		return index;
	}

	@Override
	public String getDescription() {
		return "T(" + index + ") " + tile.toString();
	}
	
	public IVector2D getBoardPosition(Board b) {
    	return tile;
    }

	
}
