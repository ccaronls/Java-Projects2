package cc.game.soc.nety;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.game.soc.core.*;
import cc.game.soc.nety.IClient.ConnectionListener;

public abstract class Client implements Runnable {

    private final static Logger log = Logger.getLogger(Client.class);
    
	protected SOCPlayer player;
    protected SOC soc;
	private ClientConnection conn;
    private ClientInput input;
		
    //@Override
	public void initialize(SOCPlayer player, SOC soc) {
        this.player = player;
        this.soc = soc;
	}
    
    public void connect(String host) throws IOException {
        if (conn == null || !conn.isConnected()) {
            conn = new ClientConnection(host);
            new Thread(this).start();
            input = new ClientInput(player, this, conn, soc);
        }
    }

    private class ConnectorThread implements Runnable {
        
        private String host;
        private ConnectionListener listener;
        private boolean canceled;
        
        ConnectorThread(String host, IClient.ConnectionListener listener) {
            this.host = host;
            this.listener = listener;
        }
        public void run() {
            try {
                if (conn == null || !conn.isConnected()) {
                    conn = new ClientConnection(host);
                    if (!canceled) {
                        new Thread(Client.this).start();
                        input = new ClientInput(player, Client.this, conn, soc);
                        listener.onConnected();
                    } 
                }
            } catch (Exception e) {
                if (!canceled) {
                    listener.onConnectionError(e.getClass().getSimpleName() + " " + e.getMessage());
                }
            }
            connectorThread = null;
        }
        
        void cancel() {
            canceled = true;
        }
    }
    
    private ConnectorThread connectorThread;
    
	public void connectAsynchronous(String host, ConnectionListener listener) {
	    if (!isConnected() && connectorThread == null) {
	        connectorThread = new ConnectorThread(host, listener);
	        new Thread(connectorThread).start();
	    }
	    
	}
    
	public void cancelConnect() {
	    if (connectorThread != null) {
	        connectorThread.cancel();
	        connectorThread = null;
	    }
	}
	
    public boolean isConnected() {
        return conn != null && conn.isConnected();
    }
	
	public void run()
	{
		log.debug("Entering Client thread");
		while (conn != null && conn.isConnected()) {
			Command cmd = conn.getNextCommand(); // will block until a command is issued
            if (cmd == null) {
                log.error("client Connection Dropped");
                clientDropped();
                break;
            }
			try {
			    processCommand(cmd);
                clientChanged();
			} catch (ProtocolException e) {
				log.error("Broken Protocol: " + e.getMessage());
			} catch (Throwable e) {
			    e.printStackTrace();
            }
		}
		log.debug("Exiting Client thread");
	}
    
    public void disconnect() {
        if (conn != null) {
        	input.stop();
        	input = null;
            conn.disconnect();
            conn = null;
        }
    }
	
	boolean processCommand(Command cmd) throws ProtocolException {
		log.debug("Processing cmd [" + cmd + "]");
        switch (cmd.getType()) {
        case CMD_SET_CLIENT_NAME:
        	conn.setClientName(cmd.getData());
        	break;
        case CMD_SET_PING_FREQ:
        	conn.setPingFreq(Protocol.decodeInt(cmd.getData()));
        	break;
        case CMD_SET_PLAYER_NUM:
            player.setPlayerNum(Protocol.decodeInt(cmd.getData()));
            break;
        case CMD_SET_AVAILABLE_COLORS:
        	setColorChoices(Protocol.decodeColors(cmd.getData()));
        	break;
        case CMD_SET_BOARD:
        	Protocol.decodeBoard(soc.getBoard(), cmd.getData());
        	break;
        case CMD_SET_COLOR:
        	setColor(cmd.getPlayerNum(), Protocol.decodeColor(cmd.getData()));
        	break;
        case CMD_SET_ROAD:
            soc.getBoard().setPlayerForEdge(soc.getBoard().getEdge(Protocol.decodeInt(cmd.getData())), cmd.getPlayerNum());
        	break;
        case CMD_SET_SETTLEMENT:
        	soc.getBoard().getVertex(Protocol.decodeInt(cmd.getData())).setPlayer(cmd.getPlayerNum());
        	break;
        case CMD_SET_CITY:
            soc.getBoard().getVertex(Protocol.decodeInt(cmd.getData())).setIsCity(true);
            break;
        case CMD_SET_ROBBER:
        	soc.getBoard().setRobber(Protocol.decodeInt(cmd.getData()));
        	break;
        case CMD_SET_POINTS:
        	soc.getPlayerByPlayerNum(cmd.getPlayerNum()).setPoints(Protocol.decodeInt(cmd.getData()));
        	break;
        case CMD_SET_RESOURCE_COUNT:
        	Protocol.decodeResourceCount(soc.getPlayerByPlayerNum(cmd.getPlayerNum()), cmd.getData());
        	break;
        case CMD_SET_ROAD_LENGTH:
        	soc.getPlayerByPlayerNum(cmd.getPlayerNum()).setRoadLength(Protocol.decodeInt(cmd.getData()));
        	break;
        case CMD_SET_DICE:
        	Protocol.decodeDice(soc, cmd.getData());
        	break;
        case CMD_EDIT_DEVEL_CARD:
        	Protocol.decodeDevelCard(player, cmd.getData());
        	break;
        case CMD_SET_DEVEL_CARDS_USABLE:
        	player.setDevelopmentCardsUsable();
        	break;
        case CMD_PRINTINFO:
        	soc.printinfo(cmd.getPlayerNum(), cmd.getData());        	
        	break;
        case CMD_SET_NUM_PLAYERS:
            setNumPlayers(Protocol.decodeInt(cmd.getData()));
            break;
        case CMD_SET_LONGEST_ROAD_PLAYER:
            soc.setLongestRoadPlayer(cmd.getPlayerNum());
            break;
        case CMD_SET_LARGEST_ARMY_PLAYER:
            soc.setLargestArmyPlayer(cmd.getPlayerNum());
            break;
        case CMD_INITGAME:
            soc.initGame();
            break;
            // these are commands that wait for input from client
        default:
        	input.queueCommand(cmd);
            break;
            
        }
        
        return false;
	}
	
	public abstract void setColorChoices(Map<String, Color> choices);
	
	public abstract void setColor(int playerNum, Color color);
	
	public abstract String chooseColor();
    
    public abstract void setNumPlayers(int num);
    
    public abstract void clientDropped();
    
    public abstract void clientChanged();
    
    public boolean canCancel() {
        return input.canCancel();
    }
}
