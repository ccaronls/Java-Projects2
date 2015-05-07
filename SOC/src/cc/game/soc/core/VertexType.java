package cc.game.soc.core;

public enum VertexType {
	OPEN(0, false, false),
	
	SETTLEMENT(0, false, true), // produce 
	CITY(0, false, true),
	WALLED_CITY(0, false, true),
	
	METROPOLIS_TRADE(0, false, true),
	METROPOLIS_POLITICS(0, false, true),
	METROPOLIS_SCIENCE(0, false, true),
	
	BASIC_KNIGHT_ACTIVE(1, true, false),
	STRONG_KNIGHT_ACTIVE(2, true, false),
	MIGHTY_KNIGHT_ACTIVE(3, true, false),

	BASIC_KNIGHT_INACTIVE(1, false, false),
	STRONG_KNIGHT_INACTIVE(2, false, false),
	MIGHTY_KNIGHT_INACTIVE(3, false, false),

	;
	
	VertexType(int knightLevel, boolean active, boolean isStructure) {
		this.knightLevel = knightLevel;
		this.active = active;
		this.isStructure = isStructure;
	}
	
	final int knightLevel;
	final boolean active;
	final boolean isStructure;
	
	public int getKnightLevel() {
		return knightLevel;
	}
	
	public boolean isKnightActive() {
		return active;
	}
	
}
