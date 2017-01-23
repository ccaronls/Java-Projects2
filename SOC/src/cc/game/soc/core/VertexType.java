package cc.game.soc.core;

public enum VertexType {
	OPEN("Nothing", 0, false, false),
	
	SETTLEMENT("Settlement", 0, false, true), // produce 
	OPEN_SETTLEMENT("Settlement", 0, false, true), // used to setup 'pirate island' games
	CITY("City", 0, false, true),
	WALLED_CITY("Walled City", 0, false, true),
	
	METROPOLIS_TRADE("Trade Metropolis", 0, false, true),
	METROPOLIS_POLITICS("Politics Metropolis", 0, false, true),
	METROPOLIS_SCIENCE("Science Metropolis", 0, false, true),
	
	BASIC_KNIGHT_ACTIVE("Basic Knight (A)", 1, true, false),
	STRONG_KNIGHT_ACTIVE("Strong Knight (A)", 2, true, false),
	MIGHTY_KNIGHT_ACTIVE("Mighty Knight (A)", 3, true, false),

	BASIC_KNIGHT_INACTIVE("Basic Knight (IA)", 1, false, false),
	STRONG_KNIGHT_INACTIVE("Strong Knight (IA)", 2, false, false),
	MIGHTY_KNIGHT_INACTIVE("Mighty Knight (IA)", 3, false, false),

	PIRATE_FORTRESS("Pirate Fortress", 0, false, true),
	;
	
	VertexType(String niceName, int knightLevel, boolean active, boolean isStructure) {
		this.niceName = niceName;
		this.knightLevel = knightLevel;
		this.active = active;
		this.isStructure = isStructure;
	}
	
	final String niceName;
	final int knightLevel;
	final boolean active;
	final boolean isStructure;
	
	public int getKnightLevel() {
		return knightLevel;
	}
	
	public boolean isKnightActive() {
		return active;
	}
	
	public VertexType promotedType() {
		switch (this) {
			case BASIC_KNIGHT_ACTIVE:
				return STRONG_KNIGHT_ACTIVE;
			case BASIC_KNIGHT_INACTIVE:
				return STRONG_KNIGHT_INACTIVE;
			case STRONG_KNIGHT_ACTIVE:
				return MIGHTY_KNIGHT_ACTIVE;
			case STRONG_KNIGHT_INACTIVE:
				return MIGHTY_KNIGHT_INACTIVE;
			default:
				throw new RuntimeException("type not suitable to promote: " + name());
		}
	}

	public VertexType demotedType() {
		switch (this) {
			case STRONG_KNIGHT_ACTIVE:
				return BASIC_KNIGHT_ACTIVE;
			case STRONG_KNIGHT_INACTIVE:
				return BASIC_KNIGHT_INACTIVE;
			case MIGHTY_KNIGHT_ACTIVE:
				return STRONG_KNIGHT_ACTIVE;
			case MIGHTY_KNIGHT_INACTIVE:
				return STRONG_KNIGHT_INACTIVE;
			default:
				throw new RuntimeException("type not suitable to demote: " + name());
		}
	}

	public VertexType activatedType() {
		switch (this) {
			case BASIC_KNIGHT_INACTIVE:
				return BASIC_KNIGHT_ACTIVE;
			case MIGHTY_KNIGHT_INACTIVE:
				return MIGHTY_KNIGHT_ACTIVE;
			case STRONG_KNIGHT_INACTIVE:
				return STRONG_KNIGHT_ACTIVE;
			default:
				throw new RuntimeException("type not suitable to activeate: " + name());
		}
	}

	public VertexType deActivatedType() {
		switch (this) {
			case BASIC_KNIGHT_ACTIVE:
				return BASIC_KNIGHT_INACTIVE;
			case MIGHTY_KNIGHT_ACTIVE:
				return MIGHTY_KNIGHT_INACTIVE;
			case STRONG_KNIGHT_ACTIVE:
				return STRONG_KNIGHT_INACTIVE;
			default:
				throw new RuntimeException("type not suitable to deactiveate: " + name());
		}
	}

	public boolean isKnight() {
		switch (this) {
			case BASIC_KNIGHT_ACTIVE:
			case BASIC_KNIGHT_INACTIVE:
			case MIGHTY_KNIGHT_ACTIVE:
			case MIGHTY_KNIGHT_INACTIVE:
			case STRONG_KNIGHT_ACTIVE:
			case STRONG_KNIGHT_INACTIVE:
				return true;
			default:
				return false;
		}
	}
	
	public String getNiceName() {
		return niceName;
	}
}
