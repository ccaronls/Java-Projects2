package cc.lib.checkerboard;

/**
 * Created by chriscaron on 10/10/17.
 */

public enum PieceType {
    EMPTY("EM", 0, 0),
    PAWN("Pn", 1, 2),
    PAWN_IDLE("Pn", 2, 2), // this type of pawn has option to move forward 2 spaces
    PAWN_ENPASSANT("Pn", 1, 2), // this pawn is available for en-passant capture for 1 turn
    PAWN_TOSWAP("Pn", 100, 2), // This pawn is to be swapped for another piece
    BISHOP("Bi", 3, 8),
    KNIGHT("Kn", 3, 16),
    ROOK("Ro", 5, 4),
    ROOK_IDLE("RI", 5, 4), // only an idle rook can castle
    QUEEN("Qu", 8, 4 + 8),
    CHECKED_KING("Kc", 0, 1), // chess only, flag the king as checked
    CHECKED_KING_IDLE("KC", 1, 1), // a king that has not moved
    UNCHECKED_KING("Ki", 9, 1), // chess only
    UNCHECKED_KING_IDLE("KI", 10, 1), // only an unchecked idle king can castle

    KING("Ck", 5, 0), // checkers king, not chess
    FLYING_KING("CK", 10, 0),
    CHECKER("Cm", 1, 0), // checkers, king move along the diagonals
    DAMA_MAN("Dm", 1, 0), // dama pieces move horz and vertically
    DAMA_KING("Dk", 5, 0),

    CHIP_4WAY("C4", 1, 0), // used for KingsCourt - a piece that can move in all four directions
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

    public final static int FLAG_KING = 1; // all piece types that move like a king
    public final static int FLAG_PAWN = 2; // all piece types that move like a pawn
    public final static int FLAG_ROOK_OR_QUEEN = 4; // all piece types that move along horizonal or vertical
    public final static int FLAG_BISHOP_OR_QUEEN = 8; // all piece types that can move along the diagonals
    public final static int FLAG_KNIGHT = 16; // all piece types that move like a knight

};