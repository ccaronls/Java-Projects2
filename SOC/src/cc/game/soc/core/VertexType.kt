package cc.game.soc.core

enum class VertexType(val _nameId: String, val knightLevel: Int, val isKnightActive: Boolean, val isStructure: Boolean) {
	OPEN("Open", 0, false, false),
	SETTLEMENT("Settlement", 0, false, true),  // produce
	OPEN_SETTLEMENT("Open Settlement", 0, false, true),  // used to setup 'pirate island' games
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
	PIRATE_FORTRESS("Pirate Fortress", 0, false, true);

	fun promotedType(): VertexType {
		return when (this) {
			BASIC_KNIGHT_ACTIVE -> STRONG_KNIGHT_ACTIVE
			BASIC_KNIGHT_INACTIVE -> STRONG_KNIGHT_INACTIVE
			STRONG_KNIGHT_ACTIVE -> MIGHTY_KNIGHT_ACTIVE
			STRONG_KNIGHT_INACTIVE -> MIGHTY_KNIGHT_INACTIVE
			else                   -> throw SOCException("type not suitable to promote: $name")
		}
	}

	fun demotedType(): VertexType {
		return when (this) {
			STRONG_KNIGHT_ACTIVE -> BASIC_KNIGHT_ACTIVE
			STRONG_KNIGHT_INACTIVE -> BASIC_KNIGHT_INACTIVE
			MIGHTY_KNIGHT_ACTIVE -> STRONG_KNIGHT_ACTIVE
			MIGHTY_KNIGHT_INACTIVE -> STRONG_KNIGHT_INACTIVE
			else                   -> throw SOCException("type not suitable to demote: $name")
		}
	}

	fun activatedType(): VertexType {
		return when (this) {
			BASIC_KNIGHT_INACTIVE -> BASIC_KNIGHT_ACTIVE
			MIGHTY_KNIGHT_INACTIVE -> MIGHTY_KNIGHT_ACTIVE
			STRONG_KNIGHT_INACTIVE -> STRONG_KNIGHT_ACTIVE
			else                   -> throw SOCException("type not suitable to activeate: $name")
		}
	}

	fun deActivatedType(): VertexType {
		return when (this) {
			BASIC_KNIGHT_ACTIVE -> BASIC_KNIGHT_INACTIVE
			MIGHTY_KNIGHT_ACTIVE -> MIGHTY_KNIGHT_INACTIVE
			STRONG_KNIGHT_ACTIVE -> STRONG_KNIGHT_INACTIVE
			else                 -> throw SOCException("type not suitable to deactiveate: $name")
		}
	}

	val isKnight: Boolean
		get() = when (this) {
			BASIC_KNIGHT_ACTIVE, BASIC_KNIGHT_INACTIVE, MIGHTY_KNIGHT_ACTIVE, MIGHTY_KNIGHT_INACTIVE, STRONG_KNIGHT_ACTIVE, STRONG_KNIGHT_INACTIVE -> true
			else                                                                                                                                   -> false
		}

	val harborPts: Int
		get() = when (this) {
			SETTLEMENT -> 1
			CITY -> 2
			else -> 0
		}
}