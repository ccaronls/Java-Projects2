package cc.lib.checkerboard;

import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Base class for logical rules to govern a checkerboard style game.
 */
public abstract class Rules extends Reflector<Rules> {

    /**
     * setup pieces and choose side.
     * @param game
     */
    abstract void init(Game game);

    /**
     * Return a color for the side
     * @param side
     * @return
     */
    abstract Color getPlayerColor(int side);

    /**
     * Perform the logical move
     * @param game
     * @param move
     */
    abstract void executeMove(Game game, Move move);

    /**
     * return the playerNum >= 0 is there is a winner, < 0 otherwise.
     * @param game
     * @return
     */
    abstract int getWinner(Game game);

    /**
     * Return true if the current state of the game is a tie
     * @param game
     * @return
     */
    abstract boolean isDraw(Game game);

    /**
     * Return the heuristic value of the board for the current move
     * @param game
     * @param move
     * @return
     */
    public abstract long evaluate(Game game, Move move);

    /**
     * return a list of available moves
     * @param game
     * @return
     */
    abstract List<Move> computeMoves(Game game);

    /**
     * perform the inverse of executeMove. Use state stored in the move itself to speed the operation.
     * @param game
     * @param m
     */
    abstract void reverseMove(Game game, Move m);

    /**
     * Some text to display to the user on how to play.
     * @return
     */
    abstract String getInstructions();
}
