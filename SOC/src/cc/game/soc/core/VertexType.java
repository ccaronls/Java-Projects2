package cc.game.soc.core;

import cc.game.soc.android.R;

public enum VertexType {
	OPEN(R.string.vertex_type_open, 0, false, false),
	
	SETTLEMENT(R.string.vertex_type_settlement, 0, false, true), // produce
	OPEN_SETTLEMENT(R.string.vertex_type_open_settlement, 0, false, true), // used to setup 'pirate island' games
	CITY(R.string.vertex_type_city, 0, false, true),
	WALLED_CITY(R.string.vertex_type_walled_city, 0, false, true),
	
	METROPOLIS_TRADE(R.string.vertex_type_metro_trade, 0, false, true),
	METROPOLIS_POLITICS(R.string.vertex_type_metro_politics, 0, false, true),
	METROPOLIS_SCIENCE(R.string.vertex_type_metro_science, 0, false, true),
	
	BASIC_KNIGHT_ACTIVE(R.string.vertex_type_knight_basic_active, 1, true, false),
	STRONG_KNIGHT_ACTIVE(R.string.vertex_type_knight_strong_active, 2, true, false),
	MIGHTY_KNIGHT_ACTIVE(R.string.vertex_type_knight_mighty_active, 3, true, false),

	BASIC_KNIGHT_INACTIVE(R.string.vertex_type_knight_basic_inactive, 1, false, false),
	STRONG_KNIGHT_INACTIVE(R.string.vertex_type_knight_strong_inactive, 2, false, false),
	MIGHTY_KNIGHT_INACTIVE(R.string.vertex_type_knight_mighty_inactive, 3, false, false),

	PIRATE_FORTRESS(R.string.vertex_type_pirate_fortress, 0, false, true),
	;
	
	VertexType(int nameId, int knightLevel, boolean active, boolean isStructure) {
		this.nameId = nameId;
		this.knightLevel = knightLevel;
		this.active = active;
		this.isStructure = isStructure;
	}
	
	final int nameId;
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
				throw new SOCException("type not suitable to promote: " + name());
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
				throw new SOCException("type not suitable to demote: " + name());
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
				throw new SOCException("type not suitable to activeate: " + name());
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
				throw new SOCException("type not suitable to deactiveate: " + name());
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

	public final String getName(StringResource sr) {
		return sr.getString(nameId);
	}
}
