package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Player extends Reflector<Player> {
    static {
        addAllFields(Player.class);
    }

    int playerNum;
    Color color;
    final List<PieceType> captured = new ArrayList<>();
    boolean winner = false;

    void newGame() {
        captured.clear();
        winner = false;
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
    public final List<PieceType> getCaptured() {
        return Collections.unmodifiableList(captured);
    }

    public final void removeCaptured(PieceType pt) {
        for (int i=captured.size()-1; i>=0; i--) {
            if (captured.get(i) == pt) {
                captured.remove(i);
                break;
            }
        }
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

    /**
     *
     * @return
     */
    public final boolean isWinner() {
        return winner;
    }
}
