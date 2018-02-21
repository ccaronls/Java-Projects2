package cc.game.kaiser.net;

import java.util.*;

import cc.game.kaiser.core.*;
import cc.lib.net.*;

/**
 * This server example hosts a game where 4 people join and then the game starts.
 * 
 * 
 * 
 * @author ccaron
 *
 */
public class KaiserServer extends Kaiser implements GameServer.Listener {
    
    private GameServer server = null;
    private Map<String, RemotePlayer> playerMap = new HashMap<String, RemotePlayer>();
    
    public void start(int port, String version) throws Exception {
        server = new GameServer(this, port, 5000, version, null, 4);
    }
    
    public GameServer getServer() {
        return server;
    }
    
    public void stop() {
        if (server != null) {
            GameServer s = server;
            server = null;
            s.stop();
        }
    }
    
    class GameRunner implements Runnable {
    
        @Override
        public void run() {
            while (getNumPlayers() == 4 && server != null && !isGameOver()) {
                try {
                    runGame();
                    switch (getState()) {
                        
                        case NEW_GAME:
                            for (int i=0; i<4; i++)
                                server.broadcast(KaiserCommand.getSetPlayerCommand(getPlayer(i).getName(), i));
                                
                            server.broadcast(KaiserCommand.getUpdateGameCommand(KaiserServer.this));
                            break;
                        case NEW_ROUND:
                            server.broadcastMessage("New Round " + getNumRounds());
                            server.broadcast(KaiserCommand.getUpdateGameCommand(KaiserServer.this));
                            break;
                        case DEAL:
                            //server.broadcastMessage("Dealing ...");
                            break;
                        case BID:
                        case TRICK:
                        case PROCESS_TRICK:
                        case RESET_TRICK:
                            break;
                        case PROCESS_ROUND:
                            //server.broadcast(new GameCommand(Common.CONFIRM).setMessage("End of round))
                        case GAME_OVER:
                            server.broadcast(KaiserCommand.getUpdateGameCommand(KaiserServer.this));
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    server.broadcastMessage("GAME HALTED DUE TO SERVER ERROR: " + e.getClass().getSimpleName() + " " +e.getMessage());
                    break;
                }
            }
        }
    }
    
    public int getNumPlayers() {
        int num = 0;
        for (int i=0; i<4; i++)
            if (this.getPlayer(i) != null)
                num++;
        return num;
    }
    
    @Override
    public void onConnected(ClientConnection conn) {
        int numPlayers = getNumPlayers();
        if (numPlayers < 4) {
            RemotePlayer player = new RemotePlayer(conn);
            this.setPlayer(numPlayers, player);
            numPlayers = getNumPlayers();
            server.broadcastMessage("Player " + numPlayers + " " + conn.getName() + " has joined the game.");
            playerMap.put(conn.getName(), player);
            if (numPlayers == 4)
                new Thread(new GameRunner()).start();
        } else {
            conn.disconnect("Game Full");
        }
        
    }
    
    @Override
    protected void message(String format, Object... params) {
        server.broadcastMessage(String.format(format, params));
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        int numPlayers = getNumPlayers();
        if (numPlayers < 4) {
            if (playerMap.containsKey(conn.getName())) {
                Player player = playerMap.get(conn.getName()); 
                this.setPlayer(numPlayers, player);
                numPlayers = getNumPlayers();
                server.broadcastMessage("Player " + numPlayers + " " + conn.getName() + " has re-joined the game.");
                if (numPlayers == 4)
                    new Thread(new GameRunner()).start();
            }
            //this.setPlayer(numConnected, new RemotePlayer(conn));
        } else {
            conn.disconnect("Game Full");
        }
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        if (playerMap.containsKey(conn.getName())) {
            RemotePlayer player = playerMap.get(conn.getName());
            this.setPlayer(player.getPlayerNum(), null);
            server.broadcastMessage(conn.getName() + " has left the game.");
        } 
    }

    @Override
    public void onClientCommand(ClientConnection conn, GameCommand command) {
        RemotePlayer player = getPlayer(conn);
        if (player != null) {
            try {
                KaiserCommand.serverDecode(player, this, command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    RemotePlayer getPlayer(ClientConnection conn) {
        return playerMap.get(conn.getName());
    }

    void logError(String msg) {
        System.err.println("ERROR: " + msg);
    }

    @Override
    public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
        // TODO Auto-generated method stub
        
    }
    
    
}
