package cc.game.golf.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.*;
import cc.lib.net.*;

public class GolfServer extends Golf implements GameServer.Listener {

    GameServer server;
    int blankCardCounter = 0;

    public static void main(String [] args) {
        GolfServer gf = new GolfServer();
        try {
            gf.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Command> ");
                String line = reader.readLine();
                
                if (line == null)
                    break;
                
                line = line.trim();
                if (line.equals("quit") || line.equals("exit")) {
                    break;
                } else if (line.equals("help")) {
                    String help = 
                            "COMMANDS\n" +
                            "help             show this message\n" +
                            "exit|quit        stop server and exit\n" +
                            "";
                    
                    System.out.println(help);
                }
            }
        
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        gf.stop();
    }
    
    public GolfServer() {
    }

    public void start() throws IOException {
        if (server != null)
            throw new IOException("Server already running");
        int readTimeout = 5000;
        
        server = new GameServer(this, GolfProtocol.PORT, readTimeout, GolfProtocol.VERSION);
    }
    
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
    
    boolean gameRunning = false;
    private void startGameThread() {
        if (gameRunning)
            return;
        gameRunning = true;
        new Thread(new Runnable() {
            public void run() {
                server.logInfo("Game thread starting");
                while (server != null) {
                    try {
                        runGame();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                gameRunning = false;
                server.logInfo("Game thread stopping");
            }
        }).start();
    }

    private GameCommand getUpdateGameStateCommand() {
        GameCommand cmd = new GameCommand(GolfProtocol.SRVR_GAME_STATE);
        cmd.setArg("state", getState());
        cmd.setArg("curPlayer", getCurrentPlayer());
        cmd.setArg("numPlayers", getNumPlayers());
        cmd.setArg("winner", getWinner());
        cmd.setArg("knocker", getKnocker());
        cmd.setArg("dealer", getDealer());
        cmd.setArg("numRounds", getNumRounds());
        cmd.setArg("topOfDeck", getCardOrBlank(getTopOfDeck()));
        cmd.setArg("topOfDiscardPile", getCardOrBlank(getTopOfDiscardPile()));
        return cmd;
    }
    
    private Card getCardOrBlank(Card card) {
        if (card != null && card.isShowing())
            return card;
        return new Card(blankCardCounter++);
    }

    private GameCommand getUpdatePlayerStateCommand(int playerNum) {
        GameCommand cmd = new GameCommand(GolfProtocol.SRVR_PLAYER_STATE);
        cmd.setArg("player", playerNum);
        PlayerInfo info = new PlayerInfo();
        NetPlayer player = (NetPlayer)getPlayer(playerNum);
        info.name = player.connection.getName();
        info.handPoints = player.getHandPoints(this);
        info.points = player.getPoints();
        info.cards = player.getCards();
        for (int i=0; i<info.cards.length; i++) {
            for (int ii=0; ii<info.cards[i].length; ii++) {
                info.cards[i][ii] = getCardOrBlank(info.cards[i][ii]); // dont transmit face down cards
            }
        }
        cmd.setArg("info", info);
        return cmd;
    }
    
    @Override
    public void onConnected(ClientConnection conn) {
        if (getNumPlayers() == 0) {
            addPlayer(new NetPlayer(conn));
            addPlayer(new NetPlayer(null));
            addPlayer(new NetPlayer(null));
            addPlayer(new NetPlayer(null));
            startGameThread();
        } else {
            for (int i=0; i<getNumPlayers(); i++) {
                NetPlayer player = (NetPlayer)getPlayer(i);
                if (player.connection == null) {
                    player.connection = conn;
                } 
            }
        }
        conn.sendCommand(getUpdateGameStateCommand());
        for (int i=0; i<getNumPlayers(); i++)
            conn.sendCommand(getUpdatePlayerStateCommand(i));
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        conn.sendCommand(getUpdateGameStateCommand());
        for (int i=0; i<getNumPlayers(); i++)
            conn.sendCommand(getUpdatePlayerStateCommand(i));
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        // TODO Auto-generated method stub
        server.logInfo("Client " + conn.getName() + " disconnected");
    }

    private int numContinuesRemaining = 0;
    private Object continueLock = new Object();
    private void waitToContinue() {
        try {
            synchronized (this) {
                if (numContinuesRemaining == 0) {
                    for (ClientConnection cl : server.getConnectionValues())
                        if (cl.isConnected())
                            numContinuesRemaining++;
                }
            }
            synchronized (continueLock) {
                continueLock.wait(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    
    @Override
    public void onClientCommand(ClientConnection conn, GameCommand command) {
        if (command.getType() == GolfProtocol.CL_CONTINUE) {
            synchronized (this) {
                numContinuesRemaining -= 1;
            }
            if (numContinuesRemaining <= 0) {
                numContinuesRemaining = 0;
                synchronized (continueLock) {
                    continueLock.notify();
                }
            }
        }
    }

    @Override
    public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
        // TODO Auto-generated method stub
        server.logError("Unhandled form: " + id);
        
    }

    @Override
    protected void message(String format, Object... params) {
        server.broadcastMessage(String.format(format, params));
    }

    @Override
    protected void onKnock(int player) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_KNOCK).setArg("player", player));
    }

    @Override
    protected void onCardSwapped(int player, DrawType dtstack, Card drawn, Card swapped, int row, int col) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_CARD_SWAPPED).setArg("player", player).setArg("drawn", drawn).setArg("row", row).setArg("col", col));
        waitToContinue();
    }

    @Override
    protected void onCardDiscarded(int player, DrawType dtstack, Card swapped) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_CARD_DISCARDED).setArg("player", player).setArg("dtstack", dtstack).setArg("swapped", swapped));
        waitToContinue();
    }

    @Override
    protected void onDealCard(int player, Card card, int row, int col) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_CARD_DEALT).setArg("player", player).setArg("card", getCardOrBlank(null)).setArg("row", row).setArg("col", col));
        waitToContinue();
    }

    @Override
    protected void onCardTurnedOver(int player, Card card, int row, int col) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_CARD_TURNEDOVER).setArg("player", player).setArg("card", card).setArg("row", row).setArg("col", col));
        waitToContinue();
    }

    @Override
    protected void onDrawPileChoosen(int player, DrawType type) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_ON_DRAW_PILE_CHOOSEN).setArg("player", player).setArg("type", type).setArg("card", getTopOfDeck()));
        waitToContinue();
    }

    @Override
    protected void onChooseCardToSwap(int player, Card card, int row, int col) {
        server.broadcast(new GameCommand(GolfProtocol.SRVR_CHOOSE_CARD_TO_SWAP).setArg("player", player).setArg("card", card).setArg("row", row).setArg("col", col));
        waitToContinue();
    }
    
    
}
