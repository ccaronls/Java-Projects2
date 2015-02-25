package cc.game.soc.netx;

import java.util.*;

import org.apache.log4j.Logger;

/**
 * Example of how to use Server 
 * @author ccaron
 *
 */
public class SimpleServer extends NetServer {

    Logger log = Logger.getLogger("SimpleServer");
    
    public static void main(String [] args) {
        try {
            new SimpleServer(Integer.parseInt(args[0]));
        } catch (Exception e) {
            System.err.println("USAGE: SimpleServer <port>");            
            e.printStackTrace();
        }
    }
    
    // we will have 1 game and as players connect they join the game.
    // once 4 players join the game is full.  New connections are allowed
    // to spectate.  If a player leaves the game, then spectating players
    // will be joined in the order they connected.  The game is paused
    // indefinitly util there are 4 players
    NetGame game;
    
    SimpleServer(int listenPort) throws Exception {
        super(listenPort);
        game = new NetGame();
        game.getBoard().load("soc_def_board.txt");
        game.setNumPlayers(4);
        availableColors.put("RED", "255,0,0");
        availableColors.put("YELLOW", "0,255,255");
        availableColors.put("BLUE", "0,0,255");
        availableColors.put("GREEN", "0,255,0");
    }

    Map<String, String> availableColors = new HashMap<String, String>();
    
    final int HOME_PAGE_ID = 0;
    final int CHOOSE_COLOR_ID = 1;
    
    @Override
    public void onClientConnection(String clientName) {
        log.debug("onClientConnection " + clientName);
        int slot = game.findEmptySlot();
        if (slot == 0) {
            // give player the option to host a game
        } else if (slot < 4) {
            // give the player option to view or join the game
        } else {
            // player can view only
        }
        
        ServerForm form = createForm(HOME_PAGE_ID);
        form.addLabel("Multiplayer Menu");
        form.addSubmitButton("view", "View");
        if (game.findEmptySlot() > 0)
            form.addSubmitButton("join", "Join");
        try {
            sendForm(form, clientName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClientDisconnected(String clientName) {
        log.debug("onClientDisconnected " + clientName);
        // if this was a player that was in game, then pause the game
    }

    @Override
    public void onClientReconnection(String clientName) {
        log.debug("onClientReconnection " + clientName);
    }

    @Override
    protected void onFormSubmitted(FormResponse response, String clientName) {
        log.debug("onFormSubmitted from client=" + clientName + " action=" + response.getAction() + " response=" + response);
        try {
            switch (response.getId()) {
                case HOME_PAGE_ID:
                    if (response.getAction().equals("view")) {
                        
                    } else if (response.getAction().equals("join")) {
                        
                    } else if (response.getAction().equals("host")) {
                        
                    } else {
                        log.error("Unknown action '" + response.getAction() + "'");
                    }
                    break;
            }
            /*
            if (response.getAction().equals("view")) {
                sendBoard(game.getBoard(), clientName);
            } else if (response.getAction().equals("join")) {
                int slot = game.findEmptySlot();
                if (slot > 0) {
                    joinGame(game, clientName, slot);
                    ServerForm form = createForm(CHOOSE_COLOR_ID);
                    form.addLabel("Choose Color");
                    for (String c : availableColors.keySet()) {
                        form.addSubmitButton(c, c);
                    }
                    sendForm(form, clientName);
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
