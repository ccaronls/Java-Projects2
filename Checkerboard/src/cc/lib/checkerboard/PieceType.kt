package cc.lib.checkerboard


/**
 * Created by chriscaron on 10/10/17.
 */
enum class PieceType(castleWith: Boolean, abbrev: String, points: Int, value: Int, flag: Int) {
	BLOCKED("BL", 0, 0, 0),
	EMPTY("EM", 0, 0, 0),
	PAWN("Pn", 1, 10, 2),
	PAWN_IDLE("PI", 1, 9, 2),  // this type of pawn has option to move forward 2 spaces
	PAWN_ENPASSANT("PE", 2, 11, 2),  // this pawn is available for en-passant capture for 1 turn
	PAWN_TOSWAP("PS", 1, 10, 2),  // This pawn is to be swapped for another piece
	BISHOP("Bi", 3, 30, 8),  // Do right and left facing knights like (KNIGHT_R/L)
	KNIGHT_R("Kn", 3, 31, 16),
	KNIGHT_L("Kn", 3, 31, 16),
	ROOK("Ro", 5, 50, 4),
	ROOK_IDLE(true, "RI", 5, 51, 4),  // only an idle rook can castle
	QUEEN("Qu", 8, 80, 4 + 8),
	CHECKED_KING("Kc", 0, 1, 1),  // chess only, flag the king as checked
	CHECKED_KING_IDLE("KC", 1, 2, 1),  // a king that has not moved
	UNCHECKED_KING("Ki", 9, 3, 1),  // chess only
	UNCHECKED_KING_IDLE("KI", 10, 4, 1),  // only an unchecked idle king can castle
	DRAGON_R("Dr", 5, 70, 32),  // moves like queen but only 3 spaces
	DRAGON_L("Dr", 5, 70, 32),  // moves like queen but only 3 spaces
	DRAGON_IDLE_R(true, "DI", 7, 69, 32),  // moves like queen but only 3 spaces
	DRAGON_IDLE_L(true, "DI", 7, 71, 32),  // moves like queen but only 3 spaces
	KING("Ck", 5, 10, 64),  // checkers king, not chess
	FLYING_KING("CK", 10, 20, 64),
	CHECKER("Cm", 1, 1, 64),  // checkers, king move along the diagonals
	DAMA_MAN("Dm", 1, 1, 64),  // dama pieces move horz and vertically
	DAMA_KING("Dk", 5, 5, 64),
	CHIP_4WAY("C4", 1, 1, 64);

	// used for KingsCourt - a piece that can move in all four directions
    @JvmField
    val abbrev: String
	@JvmField
    val value: Int
	@JvmField
    val points: Int
	@JvmField
    val flag: Int
	val canCastleWith: Boolean
	val isFlying: Boolean
		get() {
			when (this) {
				FLYING_KING, DAMA_KING -> return true
			}
			return false
		}

	constructor(abbrev: String, points: Int, value: Int, flag: Int) : this(false, abbrev, points, value, flag) {}

	fun drawFlipped(): Boolean {
		when (this) {
			KNIGHT_R, DRAGON_IDLE_L, DRAGON_L -> return true
		}
		return false
	}

	/**
	 * Returns the logic type from the subtypes. Like all PAWN Variations lead to PAWN.
	 * All Rook variations lead to ROOK
	 * All King variations lead to KING
	 * All Checker variatiosn lead to CHECKER
	 * etc.
	 * @return the logical type
	 */
	val displayType: PieceType
		get() {
			when (this) {
				PAWN, PAWN_IDLE, PAWN_ENPASSANT, PAWN_TOSWAP -> return PAWN
				ROOK, ROOK_IDLE -> return ROOK
				DRAGON_R, DRAGON_IDLE_R, DRAGON_L, DRAGON_IDLE_L -> return DRAGON_R
				CHECKED_KING, CHECKED_KING_IDLE, UNCHECKED_KING, UNCHECKED_KING_IDLE, KING, FLYING_KING, DAMA_KING -> return KING
				CHECKER, DAMA_MAN, CHIP_4WAY -> return CHECKER
			}
			return this
		}
	val nonIdled: PieceType
		get() {
			when (this) {
				PAWN_IDLE -> return PAWN
				ROOK_IDLE -> return ROOK
				CHECKED_KING_IDLE -> return CHECKED_KING
				UNCHECKED_KING_IDLE -> return UNCHECKED_KING
				DRAGON_IDLE_R -> return DRAGON_R
				DRAGON_IDLE_L -> return DRAGON_L
			}
			throw AssertionError("Unhandled case $this")
		}
	val idled: PieceType
		get() {
			when (this) {
				PAWN -> return PAWN_IDLE
				ROOK -> return ROOK_IDLE
				CHECKED_KING -> return CHECKED_KING_IDLE
				UNCHECKED_KING -> return UNCHECKED_KING_IDLE
				DRAGON_R -> return DRAGON_IDLE_R
				DRAGON_L -> return DRAGON_IDLE_L
			}
			throw AssertionError("Unhandled case $this")
		}
	val isChecked: Boolean
		get() {
			when (this) {
				CHECKED_KING, CHECKED_KING_IDLE -> return true
			}
			return false
		}

	companion object {
		const val FLAG_KING = 1 // all piece types that move like a king
		const val FLAG_PAWN = 2 // all piece types that move like a pawn
		const val FLAG_ROOK_OR_QUEEN = 4 // all piece types that move along horizonal or vertical
		const val FLAG_BISHOP_OR_QUEEN = 8 // all piece types that can move along the diagonals
		const val FLAG_KNIGHT = 16 // all piece types that move like a knight
		const val FLAG_DRAGON = 32 // dragon chess. Moves like Queen but only 3 spaces
		const val FLAG_CHECKER = 64
	}

	init {
		cc.lib.utils.assert(abbrev.length == 2, "Abbrev must be 2 chars")
		this.abbrev = abbrev
		this.value = value
		this.points = points
		this.flag = flag
		this.canCastleWith = castleWith
	}
}