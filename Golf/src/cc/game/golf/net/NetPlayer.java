package cc.game.golf.net;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.*;
import cc.game.golf.net.GolfProtocol.InputState;
import cc.lib.net.*;

public class NetPlayer extends PlayerBot implements ClientConnection.Listener {

    ClientConnection connection;
    Object response;
    InputState inputState = InputState.NONE; 

    NetPlayer(ClientConnection connection) {
        this.connection = connection;
        connection.setListener(this);
    }
    
    private boolean isConnected() {
        return connection != null && connection.isConnected();
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getResponse() {
        try {
            synchronized (this) {
                wait(5000);
            } 
        } catch (Exception e) {}
        return (T)response;
    }
    
    @Override
    protected DrawType chooseDrawPile(Golf golf) {
        if (!isConnected())
            return super.chooseDrawPile(golf);

        inputState = InputState.CHOOSE_DRAW_PILE;
        connection.sendCommand(new GameCommand(GolfProtocol.SRVR_CHOOSE_DRAW_PILE));

        return getResponse();
    }

    @Override
    protected Card chooseDiscardOrPlay(Golf golf, Card drawCard) {
        if (!isConnected())
            return super.chooseDiscardOrPlay(golf, drawCard);
        
        inputState = InputState.CHOOSE_DISCARD_OR_PLAY;
        connection.sendCommand(new GameCommand(GolfProtocol.SRVR_CHOOSE_DISCARD_OR_PLAY).setArg("card", drawCard));
        return getResponse();
    }

    @Override
    protected Card chooseCardToSwap(Golf golf, Card drawCard) {
        if (!isConnected())
            return super.chooseCardToSwap(golf, drawCard);
        inputState = InputState.CHOOSE_CARD_TO_SWAP;
        connection.sendCommand(new GameCommand(GolfProtocol.SRVR_CHOOSE_CARD_TO_SWAP).setArg("card", drawCard));
        return getResponse();
    }

    @Override
    protected int turnOverCard(Golf golf, int row) {
        if (!isConnected())
            return super.turnOverCard(golf, row);
        inputState = InputState.CHOOSE_CARD_TO_TURN_OVER;
        connection.sendCommand(new GameCommand(GolfProtocol.SRVR_CHOOSE_CARD_TO_TURN_OVER).setArg("row", row));
        return getResponse();
    }
    @Override
    public void onCommand(GameCommand cmd) {
        try {
            if (cmd.getType() == GolfProtocol.CL_CHOOSE_DRAW_PILE && inputState == InputState.CHOOSE_DRAW_PILE) {
                response = DrawType.valueOf(cmd.getArg("type"));
            } else if (cmd.getType() == GolfProtocol.CL_CHOOSE_DISCARD_OR_PLAY && inputState == InputState.CHOOSE_DISCARD_OR_PLAY) {
                Card c = new Card();
                c.deserialize(cmd.getArg("card"));
                response = c;
            } else if (cmd.getType() == GolfProtocol.CL_CHOOSE_CARD_TO_TURN_OVER && inputState == InputState.CHOOSE_CARD_TO_TURN_OVER) {
                Card c = new Card();
                c.deserialize(cmd.getArg("card"));
                response = c;
            } else if (cmd.getType() == GolfProtocol.CL_CHOOSE_CARD_TO_SWAP && inputState == InputState.CHOOSE_CARD_TO_SWAP) {
                Card c = new Card();
                c.deserialize(cmd.getArg("card"));
                response = c;
            } else {
                connection.getServer().logWarn("Unhandled message: " + cmd.getType());
            }
            synchronized (this) {
                notify();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    public void onDisconnected(String reason) {
        // TODO Auto-generated method stub
        
    }
    
    
    
}
