package cc.lib.game;

public interface IGame<M extends IMove> {
    void executeMove(M move);
    M undo();
    Iterable<M> getMoves();
    int getTurn();
}

