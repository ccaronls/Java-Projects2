package cc.lib.game;

/**
 * Interface for a MiniMaxDescision Tree
 * @param <M>
 */
public interface IGame<M extends IMove> {
    /**
     * This will push of the game
     * @param move
     */
    void executeMove(M move);

    /**
     * This will pop state
     * @return
     */
    M undo();

    /**
     * Get all the moves available for this state
     * @return
     */
    Iterable<M> getMoves();

    /**
     * Get the current player turn
     * @return
     */
    int getTurn();

}

