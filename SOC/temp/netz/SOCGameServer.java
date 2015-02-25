package cc.game.soc.netz;

import java.util.*;

import cc.game.soc.core.*;
import cc.lib.crypt.Cypher;
import cc.lib.net.*;

public class SOCGameServer extends SOC implements GameServer.Listener {

    final GameServer server;
    
    final int READ_TIMEOUT = 60000;
    final Cypher CYPHER = null;
    final String SERVER_VERSION = "SOCServer1.0";
    
    public SOCGameServer(int listenPort) throws Exception {
        server = new GameServer(this, listenPort, READ_TIMEOUT, SERVER_VERSION, CYPHER);
    }

    /**
     * 
     * @param playerNum range [1-numPlayers] inclusive
     * @return
     */
    public SOCNetPlayer getNetPlayer(int playerNum) {
        return (SOCNetPlayer)super.getPlayerByPlayerNum(playerNum);
    }
    
    public SOCNetPlayer getNetPlayer(ClientConnection conn) {
        for (int i=1; i<=getNumPlayers(); i++) {
            SOCNetPlayer p = getNetPlayer(i);
            if (p.connection == conn)
                return p;
        }
        for (int i=1; i<=getNumPlayers(); i++) {
            SOCNetPlayer p = getNetPlayer(i);
            if (p.connection == null) {
                p.connection = conn;
                return p;
            }
        }
        return null;
    }
    
    @Override
    public void onConnected(ClientConnection conn) {
        SOCNetPlayer p = getNetPlayer(conn);
        if (p == null) {
            conn.disconnect("Game Full");
        } else {
            server.broadcastMessage("Client " + conn.getName() + " connected");
            conn.sendCommand(SOCProtocol.svrUpdateBoard(getBoard()));
        }
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        SOCNetPlayer empty = null;
        for (int i=1; i<=this.getNumPlayers(); i++) {
            SOCNetPlayer p = this.getNetPlayer(i);
            if (p.connection == conn) {
                // resend last command
                p.resendLastCommand();
                return;
            }
        }
        conn.disconnect("Game Full");
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onClientCommand(ClientConnection conn, GameCommand command) {
        SOCProtocol.serverProcess(conn, command, this);
    }

    @Override
    public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected SOCPlayer instantiatePlayer(String className) throws Exception {
        // TODO Auto-generated method stub
        return super.instantiatePlayer(className);
    }

    @Override
    protected void onDistributeResources(SOCPlayer player, ResourceType type, int amount) {
        // TODO Auto-generated method stub
        super.onDistributeResources(player, type, amount);
    }

    @Override
    protected void onPlayerPointsChanged(SOCPlayer player, int changeAmount) {
        // TODO Auto-generated method stub
        super.onPlayerPointsChanged(player, changeAmount);
    }

    @Override
    protected void onDevelopmentCardPicked(SOCPlayer player, DevelopmentCardType card) {
        // TODO Auto-generated method stub
        super.onDevelopmentCardPicked(player, card);
    }

    @Override
    protected void onTakeOpponentCard(SOCPlayer taker, SOCPlayer giver, Object card) {
        // TODO Auto-generated method stub
        super.onTakeOpponentCard(taker, giver, card);
    }

    @Override
    protected void onTradeCompleted(SOCPlayer player, SOCTrade trade) {
        // TODO Auto-generated method stub
        super.onTradeCompleted(player, trade);
    }

    @Override
    protected void onLongestRoadPlayerUpdated(SOCPlayer oldPlayer, SOCPlayer newPlayer, int maxRoadLen) {
        // TODO Auto-generated method stub
        super.onLongestRoadPlayerUpdated(oldPlayer, newPlayer, maxRoadLen);
    }

    @Override
    protected void onPlayerRoadBlocked(SOCPlayer player, SOCEdge road) {
        // TODO Auto-generated method stub
        super.onPlayerRoadBlocked(player, road);
    }

    @Override
    protected void onLargestArmyPlayerUpdated(SOCPlayer oldPlayer, SOCPlayer newPlayer, int armySize) {
        // TODO Auto-generated method stub
        super.onLargestArmyPlayerUpdated(oldPlayer, newPlayer, armySize);
    }

    @Override
    protected void onMonopolyCardApplied(SOCPlayer taker, SOCPlayer giver, ResourceType type, int amount) {
        // TODO Auto-generated method stub
        super.onMonopolyCardApplied(taker, giver, type, amount);
    }

    @Override
    protected void onGameOver(SOCPlayer winner) {
        // TODO Auto-generated method stub
        super.onGameOver(winner);
    }

    
}
