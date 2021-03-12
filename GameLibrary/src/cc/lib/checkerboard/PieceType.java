package cc.lib.checkerboard;

/**
 * Created by chriscaron on 10/10/17.
 */

public enum PieceType {
    BLOCKED("BL", 0, 0),
    EMPTY("EM", 0, 0),
    PAWN("Pn", 1, 2),
    PAWN_IDLE("PI", 1, 2), // this type of pawn has option to move forward 2 spaces
    PAWN_ENPASSANT("PE", 2, 2), // this pawn is available for en-passant capture for 1 turn
    PAWN_TOSWAP("PS", 100, 2), // This pawn is to be swapped for another piece
    BISHOP("Bi", 3, 8),
    KNIGHT("Kn", 3, 16),
    ROOK("Ro", 5, 4),
    ROOK_IDLE("RI", 5, 4), // only an idle rook can castle
    QUEEN("Qu", 8, 4 + 8),
    CHECKED_KING("Kc", 0, 1), // chess only, flag the king as checked
    CHECKED_KING_IDLE("KC", 1, 1), // a king that has not moved
    UNCHECKED_KING("Ki", 9, 1), // chess only
    UNCHECKED_KING_IDLE("KI", 10, 1), // only an unchecked idle king can castle
    DRAGON("Dr", 5, 32), // moves like queen but only 3 spaces
    DRAGON_IDLE("DI", 5, 32), // moves like queen but only 3 spaces

    KING("Ck", 5, 64), // checkers king, not chess
    FLYING_KING("CK", 10, 64),
    CHECKER("Cm", 1, 64), // checkers, king move along the diagonals
    DAMA_MAN("Dm", 1, 64), // dama pieces move horz and vertically
    DAMA_KING("Dk", 5, 64),

    CHIP_4WAY("C4", 1, 64), // used for KingsCourt - a piece that can move in all four directions
    ;

    public final String abbrev;
    final int value;
    final int flag;

    public boolean isFlying() {
        switch (this) {
            case FLYING_KING:
            case DAMA_KING:
                return true;
        }
        return false;
    }

    PieceType(String abbrev, int value, int flag) {
        if (abbrev.length() != 2)
            throw new AssertionError("Abbrev must be 2 chars");
        this.abbrev = abbrev;
        this.value = value;
        this.flag = flag;
    }

    /**
     * Returns the logic type from the subtypes. Like all PAWN Variations lead to PAWN.
     * All Rook variations lead to ROOK
     * All King variations lead to KING
     * All Checker variatiosn lead to CHECKER
     * etc.
     * @return the logical type
     */
    public PieceType getDisplayType() {
        switch (this) {
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                return PAWN;

            case ROOK:
            case ROOK_IDLE:
                return ROOK;

            case DRAGON:
            case DRAGON_IDLE:
                return DRAGON;

            case CHECKED_KING:
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                return KING;

            case CHECKER:
            case DAMA_MAN:
            case CHIP_4WAY:
                return CHECKER;

        }
        return this;
    }

    public final static int FLAG_KING = 1; // all piece types that move like a king
    public final static int FLAG_PAWN = 2; // all piece types that move like a pawn
    public final static int FLAG_ROOK_OR_QUEEN = 4; // all piece types that move along horizonal or vertical
    public final static int FLAG_BISHOP_OR_QUEEN = 8; // all piece types that can move along the diagonals
    public final static int FLAG_KNIGHT = 16; // all piece types that move like a knight
    public final static int FLAG_DRAGON = 32; // all piece types that move like a knight
    public final static int FLAG_CHECKER = 64;

}