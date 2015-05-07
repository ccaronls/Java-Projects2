package cc.game.soc.core;

public class BotNodeTile extends BotNode {

	static {
		addAllFields(BotNodeTile.class);
	}
	
	private Tile tile;
	private int index;
	
	public BotNodeTile() {}
	
	BotNodeTile(Tile tile, int index) {
		this.tile = tile;
		this.index = index;
	}

	@Override
	public Object getData() {
		return tile;
	}

	@Override
	public String getDescription() {
		return "T(" + index + ") " + tile.toString();
	}
	
	
}
