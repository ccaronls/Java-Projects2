package cc.game.golf.net;

import cc.game.golf.core.Card;
import cc.lib.net.*;

public class GolfProtocol {

    // Any significant changes to protocol must be associated with 
    // incrementing the VERSION
    public static final String VERSION = "Beta 0.0.1";
    public static final int    PORT = 10101;
    
    // broadcast events
    public static final GameCommandType SRVR_ON_KNOCK = new GameCommandType("srvrOnKnock");
    public static final GameCommandType SRVR_ON_CARD_SWAPPED = new GameCommandType("srvrOnCardSwapped");
    public static final GameCommandType SRVR_ON_CARD_DISCARDED = new GameCommandType("srvrOnCardDiscarded");
    public static final GameCommandType SRVR_ON_CARD_DEALT = new GameCommandType("srvrOnDealCard");
    public static final GameCommandType SRVR_ON_CARD_TURNEDOVER = new GameCommandType("srvrOnCardTurnedOver");
    public static final GameCommandType SRVR_ON_DRAW_PILE_CHOOSEN = new GameCommandType("srvrOnDrawPileChoosen");
    public static final GameCommandType SRVR_GAME_STATE = new GameCommandType("srvrUpdateGameState");
    public static final GameCommandType SRVR_PLAYER_STATE = new GameCommandType("srvrUpdatePlayerState");

    // client response to broadcast events
    public static final GameCommandType CL_CONTINUE = new GameCommandType("clContinue");

    // server requests for input to a particular player 
    public static final GameCommandType SRVR_CHOOSE_CARD_TO_SWAP = new GameCommandType("srvrChooseCardToSwap");
    public static final GameCommandType SRVR_CHOOSE_DRAW_PILE = new GameCommandType("srvrChooseDrawPile");
    public static final GameCommandType SRVR_CHOOSE_DISCARD_OR_PLAY = new GameCommandType("srvrChooseDiscardOrPlay");
    public static final GameCommandType SRVR_CHOOSE_CARD_TO_TURN_OVER = new GameCommandType("srvrChooseCardToTurnOver");

    // client responses to input requests
    public static final GameCommandType CL_CHOOSE_DRAW_PILE = new GameCommandType("clOnChooseDrawPile");
    public static final GameCommandType CL_CHOOSE_DISCARD_OR_PLAY = new GameCommandType("clOnChooseDiscardOrPlay");
    public static final GameCommandType CL_CHOOSE_CARD_TO_TURN_OVER = new GameCommandType("clOnChooseCardToTurnOver");
    public static final GameCommandType CL_CHOOSE_CARD_TO_SWAP = new GameCommandType("clOnChooseCardToSwap");
    
    
    public enum InputState {
        NONE,
        CHOOSE_CARD_TO_SWAP,
        CHOOSE_DRAW_PILE, 
        CHOOSE_DISCARD_OR_PLAY, 
        CHOOSE_CARD_TO_TURN_OVER,
    }    
    
    public static Card parseCard(String str) throws Exception {
        Card card = new Card();
        card.deserialize(str);
        return card;
    }
}
