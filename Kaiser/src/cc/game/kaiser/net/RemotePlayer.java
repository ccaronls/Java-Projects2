package cc.game.kaiser.net;

import cc.game.kaiser.core.*;
import cc.lib.net.*;

public class RemotePlayer extends Player {

    ClientConnection conn;
    Object response = null;
    
    public RemotePlayer(ClientConnection conn) {
        super(conn.getName());
        this.conn = conn;
    }

    @Override
    public Card playTrick(Kaiser kaiser, Card [] options) {
        conn.sendCommand(KaiserCommand.getPlayTrickCommand(options));
        waitForResponse();
        return response == null ? null : (Card)response;
    }

    @Override
    public Bid makeBid(Kaiser kaiser, Bid [] options) {
        conn.sendCommand(KaiserCommand.getMakeBidCommand(options));
        waitForResponse();
        try {
            return response == null ? Bid.NO_BID : (Bid)response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Bid.NO_BID;
    }
    
    

    @Override
    public void onWinsTrick(Kaiser k, Hand trick) {
        conn.getServer().broadcastMessage("Player " + getName() + " won the trick");
        //conn.sendMessage("You won the trick");
    }

    @Override
    public void onDealtCard(Kaiser k, Card c) {
        conn.sendCommand(KaiserCommand.getDealtCardCommand(c));
    }

    private void waitForResponse() {
        response = null;
        try {
            synchronized (this) {
                wait(5000);
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void setResponse(Object response) {
        this.response = response;
        synchronized (this) {
            notify();
        }
    }
    
}
