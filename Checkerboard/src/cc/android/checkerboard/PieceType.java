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
    QUEEN,
    CHECKED_KING, // chess only, flag the king as checked
    UNCHECKED_KING, // chess only
    KING, // checkers king, not chess
    CHECKER,
    UNAVAILABLE, // this means off board
    ;

};