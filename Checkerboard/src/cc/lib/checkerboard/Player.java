package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Player extends Reflector<Player> {
    static {
        addAllFields(Player.class);
    }

    int playerNum;
    Color color;

    void newGame() {

    }

    /**
     * Override to customize behavior. Default behavior is random.
     *
     * @param game
     * @param pieces
     * @return
     */
    public Piece choosePieceToMove(Game game, List<Piece> pieces) {
        return pieces.get(Utils.rand() % pieces.size());
    }

    /**
     * Override to customize behavior. Default behavior is random.
     *
     * @param game
     * @param moves
     * @return
     */
    public Move chooseMoveForPiece(Game game, List<Move> moves) {
        return moves.get(Utils.rand() % moves.size());
    }

    /**
     *
     * @return
     */
    public final Color getColor() {
        return color;
    }

    /**
     *
     * @return
     */
    public final int getPlayerNum() {
        return playerNum;
    }

}
