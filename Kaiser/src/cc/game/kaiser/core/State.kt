package cc.game.kaiser.core;

/**
 * The current state of the game.  state is updated on each call to run, although not guaranteed to change every time.
 * 
 * @author ccaron
 *
 */
public enum State
{
    /** initial state */
    NEW_GAME,               
    /** reset hands, bids increment start player */
    NEW_ROUND,              
    /** deal the cards */
    DEAL,
    /** take bids */
    BID,                    
    /** each player play's their card */
    TRICK,                  
    /** determine trick winner */
    PROCESS_TRICK,          
    /** reset before back to TRICK */
    RESET_TRICK,            
    /** all cards have been played, check for kaiser, assign points */
    PROCESS_ROUND,          
    /** one of the teams has won */
    GAME_OVER,              
};
