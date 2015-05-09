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
}
