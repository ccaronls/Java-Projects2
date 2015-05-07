package cc.game.soc.core;

public class BotNodeVertex extends BotNode {

	static {
		addAllFields(BotNodeVertex.class);
	}
	
	private Vertex vertex;
	private int index;
	
	public BotNodeVertex() {}
	
	BotNodeVertex(Vertex v, int index) {
		this.vertex = v;
		this.index = index;
	}

	@Override
	public Object getData() {
		return vertex;
	}

	@Override
	public String getDescription() {
		String s = "V(" + index + ") " + vertex.getType().name();
		if (vertex.canPlaceStructure())
			s += " STRUC";
		if (vertex.isAdjacentToLand())
			s += " LAND";
		if (vertex.isAdjacentToWater())
			s += " WATER";
		return s;
	}
	
}
