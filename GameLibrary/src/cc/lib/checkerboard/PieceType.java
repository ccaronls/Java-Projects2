package cc.lib.checkerboard;

/**
 * Created by chriscaron on 10/10/17.
 */

public enum PieceType {
    EMPTY("EM"),
    PAWN("Pn"),
    PAWN_IDLE("Pn"), // this type of pawn has option to move forward 2 spaces
    PAWN_ENPASSANT("Pn"), // this pawn is available for en-passant capture for 1 turn
    PAWN_TOSWAP("Pn"), // This pawn is to be swapped for another piece
    BISHOP("Bi"),
    KNIGHT("Kn"),
    ROOK("Ro"),
    ROOK_IDLE("Ro"), // only an idle rook can castle
    QUEEN("Qu"),
    CHECKED_KING("Kc"), // chess only, flag the king as checked
    CHECKED_KING_IDLE("KC"), // a king that has not moved and so is
    UNCHECKED_KING("Ki"), // chess only
    UNCHECKED_KING_IDLE("KI"), // only an unchecked idle king can castle

    KING("CK"), // checkers king, not chess
    FLYING_KING("CK"),
    CHECKER("Ch"),
    DAMA_MAN("Da"), // dama pieces move horz and vertically
    DAMA_KING("DK"),
    ;

    public final String abbrev;

    public boolean isFlying() {
        switch (this) {
            case FLYING_KING:
            case DAMA_KING:
                return true;
        }
        return false;
    }

    PieceType(String abbrev) {
        if (abbrev.length() != 2)
            throw new AssertionError("Abbrev must be 2 chars");
        this.abbrev = abbrev;
    }

};