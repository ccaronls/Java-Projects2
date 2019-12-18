package cc.lib.checkerboard;

/**
 * Created by chriscaron on 10/10/17.
 */

public enum PieceType {
    EMPTY("EM", 0),
    PAWN("Pn", 1),
    PAWN_IDLE("Pn", 2), // this type of pawn has option to move forward 2 spaces
    PAWN_ENPASSANT("Pn", 1), // this pawn is available for en-passant capture for 1 turn
    PAWN_TOSWAP("Pn", 100), // This pawn is to be swapped for another piece
    BISHOP("Bi", 3),
    KNIGHT("Kn", 3),
    ROOK("Ro", 5),
    ROOK_IDLE("Ro", 5), // only an idle rook can castle
    QUEEN("Qu", 8),
    CHECKED_KING("Kc", 10), // chess only, flag the king as checked
    CHECKED_KING_IDLE("KC", 9), // a king that has not moved and so is
    UNCHECKED_KING("Ki", 1), // chess only
    UNCHECKED_KING_IDLE("KI", 0), // only an unchecked idle king can castle

    KING("CK", 5), // checkers king, not chess
    FLYING_KING("CK", 10),
    CHECKER("Ch", 1),
    DAMA_MAN("Da", 1), // dama pieces move horz and vertically
    DAMA_KING("DK", 5),
    ;

    public final String abbrev;
    final int value;

    public boolean isFlying() {
        switch (this) {
            case FLYING_KING:
            case DAMA_KING:
                return true;
        }
        return false;
    }

    PieceType(String abbrev, int value) {
        if (abbrev.length() != 2)
            throw new AssertionError("Abbrev must be 2 chars");
        this.abbrev = abbrev;
        this.value = value;
    }

};