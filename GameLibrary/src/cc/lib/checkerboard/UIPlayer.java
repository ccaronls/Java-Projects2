package cc.lib.checkerboard;

import java.util.List;

public class UIPlayer extends Player {

    @Override
    public Piece choosePieceToMove(Game game, List<Piece> pieces) {
        return ((UIGame)game).choosePieceToMove(pieces);
    }

    @Override
    public Move chooseMoveForPiece(Game game, List<Move> moves) {
        return ((UIGame)game).chooseMoveForPiece(moves);
    }
}
