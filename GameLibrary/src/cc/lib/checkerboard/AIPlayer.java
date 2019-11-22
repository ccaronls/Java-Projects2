package cc.lib.checkerboard;

import java.util.List;

import cc.lib.game.MiniMaxTree;

public class AIPlayer extends Player {

    @Override
    public Piece choosePieceToMove(Game game, List<Piece> pieces) {
        return super.choosePieceToMove(game, pieces);
    }

    @Override
    public Move chooseMoveForPiece(Game game, List<Move> moves) {
        return super.chooseMoveForPiece(game, moves);
    }
}
