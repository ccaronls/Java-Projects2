package cc.android.checkerboard;

/**
 * Created by chriscaron on 10/10/17.
 */

public enum PieceType {
    EMPTY,
    PAWN,
    PAWN_IDLE, // this type of pawn has option to move forward 2 spaces
    PAWN_ENPASSANT, // this pawn is available for en-passant capture for 1 turn
    PAWN_TOSWAP, // This pawn is to be swapped for another piece
    BISHOP,
    KNIGHT,
    ROOK,
    ROOK_IDLE, // only an idle rook can castle
    QUEEN,
    CHECKED_KING, // chess only, flag the king as checked
    CHECKED_KING_IDLE, // a king that has not moved and so is
    UNCHECKED_KING, // chess only
    UNCHECKED_KING_IDLE, // only an unchecked idle king can castle
    KING, // checkers king, not chess
    CHECKER,
    UNAVAILABLE, // this means off board
    ;

};