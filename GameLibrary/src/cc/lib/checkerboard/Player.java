package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Player extends Reflector<Player> {
    static {
        addAllFields(Player.class);
    }

    int playerNum;
    GColor color;
    final List<Piece> captured = new ArrayList<>();
    boolean forfeited = false;

    void newGame() {
        captured.clear();
        forfeited = false;
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
    public Move  chooseMoveForPiece(Game game, List<Move> moves) {
        return moves.get(Utils.rand() % moves.size());
    }

    /**
     *
     * @return
     */
    public final List<Piece> getCaptured() {
        return Collections.unmodifiableList(captured);
    }

    /**
     *
     * @return
     */
    public final GColor getColor() {
        return color;
    }

    /**
     *
     * @return
     */
    public final int getPlayerNum() {
        return playerNum;
    }

    /**
     *
     * @return
     */
    public final boolean isForfeited() {
        return forfeited;
    }
}
