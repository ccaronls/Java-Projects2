package cc.game.golf.core;

public enum State {

    INIT, // initial state.  Adds players here.
    SHUFFLE,
    DEAL,
    TURN_OVER_CARDS,
    TURN,
    PLAY,
    DISCARD_OR_PLAY,
    SETUP_DISCARD_PILE, 
    PROCESS_ROUND,
    END_ROUND, 
    GAME_OVER,
}
