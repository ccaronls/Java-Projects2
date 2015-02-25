package cc.game.golf.core;

/**
 * Enum to define the types of moves a player can make
 * 
 * @author chriscaron
 *
 */
public enum DrawType {
    DTStack, // draw a card form the stack
    DTDiscardPile, //draw the top card form the discard pile
    DTWaiting, // dont draw any card
}
