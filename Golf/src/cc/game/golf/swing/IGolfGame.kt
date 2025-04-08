package cc.game.golf.swing;

import java.util.List;

import cc.game.golf.core.Card;
import cc.game.golf.core.Rules;
import cc.game.golf.core.State;

interface IGolfGame {
    
    /**
     * Get card representing top of deck
     * @return
     */
    Card getTopOfDeck();

    /**
     * Get card representing top of discard pile.  Can be null.
     * @return
     */
    Card getTopOfDiscardPile();

    int getCurrentPlayer();
    
    /**
     * Get current knocker or -1
     * @return
     */
    int getKnocker();

    /**
     * Get number of players
     * @return
     */
    int getNumPlayers();

    /**
     * Get the player who is facing the front
     * @return
     */
    int getFrontPlayer();

    /**
     * Get the rules.
     * @return
     */
    Rules getRules();

    /**
     * Get a players cards
     * @param player
     * @return
     */
    Card[][] getPlayerCards(int player);

    /**
     * Get the players name
     * @param player
     * @return
     */
    String getPlayerName(int player);

    /**
     * Get a players showing points 
     * @param player
     * @return
     */
    int getHandPoints(int player);

    /**
     * Get number of rounds played
     * @return
     */
    int getNumRounds();

    /**
     * Get the dealer.
     * @return
     */
    int getDealer();

    /**
     * Get the winner or -1
     * @return
     */
    int getWinner();

    /**
     * Get a players total game points.
     * @param player
     * @return
     */
    int getPlayerPoints(int player);

    /**
     * Get the deck
     * @return
     */
    List<Card> getDeck();

    /**
     * Get the current game state
     * @return
     */
    State getState();

    Card getPlayerCard(int player, int row, int col);

    boolean canResume();
    
    void updateRules();
    
    void quit();
    
    boolean isRunning();
    
    void resume() throws Exception;
    
    void startNewGame();
}
