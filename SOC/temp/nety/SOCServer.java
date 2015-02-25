package cc.game.soc.nety;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

import org.apache.log4j.Logger;

import cc.game.soc.core.*;

public class SOCServer extends SOC implements Runnable {

    private final static Logger log = Logger.getLogger(SOCServer.class);

    private Map<String, Color> allColors = new LinkedHashMap<String, Color>();
    private Map<String, Color> availableColors = new LinkedHashMap<String, Color>();
    private boolean serverRunning = false;
    private Map<String, PlayerRemote> connections = new HashMap<String, PlayerRemote>();
    private int maxPlayers;
    private ServerSocket serverSocket;
    private int numClientNames = 0;
    private int inactivityTimeoutSeconds;
    private int joinTimeout;
    private int pingFreq = 1000;
    private Object connectMonitor = new Object();
    private Object confirmMonitor = new Object();
    private boolean gameRunning = false;
    private long firstConnection = 0;
    
    SOCServer(int maxPlayers, int inactivityTimeoutSeconds, int joinTimeout, int pingFreq) throws IOException {
    	this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
        this.joinTimeout = joinTimeout;
        this.maxPlayers = maxPlayers;
        this.pingFreq = pingFreq;
        serverSocket = new ServerSocket(ClientConnection.PORT);
    }
    
    void addAvailableColor(String name, Color color) {
    	allColors.put(name, color);
    }
    
    Map<String, Color> getAvailableColors() {
        return availableColors;
    }
    
    public void stop() {
    	log.debug("stopping ...");
        serverRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("STOPPED");
        synchronized (this) {
        	notifyAll();
        }
    }
    
    public void run() {
        // continuously listen for connections, even if we are full
        serverRunning = true;
        final long startTime = System.currentTimeMillis();
        while (serverRunning) {
            try {
            	log.debug("Accepting connections ...");/*
                if (joinTimeout > 0 && getNumConnected() > 0 && isWaitingForPlayersToJoin()) {
                    log.debug("Setting socket accept timeout to " + joinTimeout + " seconds");
                    serverSocket.setSoTimeout(joinTimeout*1000);
                } else {
                    serverSocket.setSoTimeout(0);
                }*/
            	if (getNumConnected() > 0)
            	    serverSocket.setSoTimeout(3000);
            	else
            	    serverSocket.setSoTimeout(0);
                Socket conn = serverSocket.accept();
                log.debug("Connection accepted");
                if (inactivityTimeoutSeconds > 0) {
                	log.debug("Setting read timeout to " + inactivityTimeoutSeconds + " seconds");
                	conn.setSoTimeout(inactivityTimeoutSeconds * 1000);
                }
                log.debug("Getting IO ...");
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                DataInputStream in = new DataInputStream(conn.getInputStream());
                log.debug("Processing ...");
                processNewConnection(conn, in , out);
            } catch (SocketTimeoutException e) {
                boolean expired = false;
                if (firstConnection > 0) {
                    long elapsed = System.currentTimeMillis() - firstConnection;
                    if (elapsed > joinTimeout)
                        expired = true;
                }
                if (!gameRunning && getNumConnected() > 0 && getNumConfirmed() > 0) {
                    log.debug("Join timeout expired");
                    log.info("populating remaining player slots with AI players");
                    addConnectedPlayers();
                    // add all the connected players
                    while (getNumPlayers() < maxPlayers) {
                        SOCPlayer dummy = new PlayerNetAI(this);
                        int num = getNumPlayers() + 1;
                        dummy.setPlayerNum(num);
                        addPlayer(dummy);
                        String nextColor = availableColors.keySet().iterator().next();
                        Color color = useColor(nextColor);
                        broadcastCommand(new Command(CommandType.CMD_SET_COLOR, Protocol.encodeColor(color)), dummy.getPlayerNum());
                    }
                }
                synchronized (connectMonitor) {
                    connectMonitor.notify();
                }
                synchronized (confirmMonitor) {
                    confirmMonitor.notify();
                }
            } catch (Exception e) {
            	e.printStackTrace();
                stop();
            } 
        }
        
        // disconnect everyone
    }
    
    private void addConnectedPlayers() {
        Iterator<PlayerRemote> it = connections.values().iterator();
        while (it.hasNext()) {
            addPlayer(it.next());            
        }
    }
    
    boolean isWaitingForPlayersToJoin() {
        return getNumPlayers() < maxPlayers;
    }
    
    boolean isRunning() {
    	return serverRunning;
    }

    boolean isWaitingForPlayerConfirmation() {
        return getNumConfirmed() < getNumConnected();
    }
    
    private String getUniqueClientName() {
    	numClientNames ++;
        return "socclient" + numClientNames;
    }
    
    private void processNewConnection(Socket conn, DataInputStream in, DataOutputStream out) throws IOException {
        Request req = null;
        
        try {
        	log.debug("Reading request ...");
            req = new Request(in);
            log.debug("Recieved request: " + req);
        } catch (Exception e) {
        	log.error("Failed to read request.  " + e.getMessage());
            try {
                in.close();
                out.close();
                conn.close();
            } catch (Exception ex) {
                
            }
            return;
        }
        Response rsp = null;
        boolean disconnect = true;
        if (!req.getVersion().equals(Protocol.VERSION)) {
        	rsp = new Response(req.getType(), ResponseStatus.RSP_ERR_INCOMPATIBLE_VERSION);
        } else if (req.getType() == RequestType.REQ_CONNECT) {
        	log.debug("Accepting CONNECT request");
            if (connections.containsKey(req.getClientName())) {
            	PlayerRemote player = connections.get(req.getClientName());
                if (!player.isConnected()) {
                	log.debug("Reconnecting '" + req.getClientName() + "' ");
                	player.setConnection(conn, in, out);
                    rsp = new Response(req.getType(), ResponseStatus.RSP_OK);
                    rsp.getCommands().add(new Command(CommandType.CMD_SET_BOARD, Protocol.encodeBoard(getBoard())));
                    disconnect = false;
                } else {
                	log.debug("Denying reconnect to Client '" + req.getClientName() + "' who is already connected");
                	rsp = new Response(req.getType(), ResponseStatus.RSP_ERR_KICK);
                }
            } else if (!gameRunning && getNumConnected() < maxPlayers) {
            	rsp = new Response(req.getType(), ResponseStatus.RSP_OK);
            	if (getNumConnected() == 0)
            	    firstConnection = System.currentTimeMillis();
            	String clientId = getUniqueClientName();
                log.debug("Adding new client '" + clientId + "'");
            	rsp.getCommands().add(new Command(CommandType.CMD_SET_CLIENT_NAME, clientId));
            	rsp.getCommands().add(new Command(CommandType.CMD_SET_PING_FREQ, pingFreq));
                rsp.getCommands().add(new Command(CommandType.CMD_SET_PLAYER_NUM, getNumConnected()+1));
                rsp.getCommands().add(new Command(CommandType.CMD_SET_NUM_PLAYERS, maxPlayers));
                rsp.getCommands().add(new Command(CommandType.CMD_SET_BOARD, Protocol.encodeBoard(getBoard())));
            	rsp.getCommands().add(new Command(CommandType.CMD_CHOOSE_COLOR, Protocol.encodeColors(availableColors)));
            	PlayerRemote newPlayer = new PlayerRemote(getNewDefaultPlayer(), this);
                int playerNum = getEmptySlot();
                connections.put(clientId, newPlayer);
                newPlayer.setPlayerNum(playerNum);
            	newPlayer.setConnection(conn, in, out);
            	disconnect = false;
            } else {
                rsp = new Response(req.getType(), ResponseStatus.RSP_ERROR_GAMEFULL);
            }
        } else {
        	rsp = new Response(req.getType(), ResponseStatus.RSP_ERR_INVALID_REQUEST);
        }
        rsp.write(out);
        if (disconnect) {
        	log.debug("Disconnecting connection ...");
            in.close();
            out.flush();
            out.close();
            conn.close();
            log.debug("done");
        } else {
            if (getNumConnected() == maxPlayers)
                addConnectedPlayers();
            else {
                log.info("Waiting for " + (maxPlayers - getNumConnected()) + " players");
            }
        }

        synchronized (connectMonitor) {
        	connectMonitor.notify();
        }

    }
    
    private int getEmptySlot() {
        int playerNum = connections.size()+1;
        Iterator<String> it = connections.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            PlayerRemote p = connections.get(key);
            if (!p.isConnected()) {
                log.debug("Removing disconnected client " + key);
                playerNum = p.getPlayerNum();
                connections.remove(key);
                break;
            }
        }
        
        return playerNum;
    }
    
    void waitForConection() {
    	try {
	    	synchronized (connectMonitor) {
	    		connectMonitor.wait();
	    	}
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }

    void waitForConfirmation() {
        try {
            synchronized (confirmMonitor) {
                confirmMonitor.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    void newConfirmation(int playerNum) {
        log.info("Player " + playerNum + " confirmed");
        synchronized (confirmMonitor) {
            confirmMonitor.notify();
        }
    }

    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#printinfo(java.lang.String)
     */
    @Override
    public void printinfo(int playerNum, String msg) {
        super.printinfo(playerNum, msg);
        broadcastCommand(new Command(CommandType.CMD_PRINTINFO, msg), playerNum);
    }
    
    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#initGame()
     */
    public void initGame() {
       super.initGame();
       broadcastCommand(new Command(CommandType.CMD_INITGAME), 0);
       gameRunning = true;
    }
    
    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#rollDice()
     */
    public void rollDice() {
    	super.rollDice(getRandom()); 
    	String diceString = String.valueOf(getDie1()) + "," + getDie2();
	    broadcastCommand(new Command(CommandType.CMD_SET_DICE, diceString), 0);
    }
    
    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#setLargestArmyPlayer(int)
     */
    public void setLargestArmyPlayer(int playerNum) {
        // TODO Auto-generated method stub
        super.setLargestArmyPlayer(playerNum);
        broadcastCommand(new Command(CommandType.CMD_SET_LARGEST_ARMY_PLAYER), getLargestArmyPlayerNum());
    }

    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#setLongestRoadPlayer(int)
     */
    public void setLongestRoadPlayer(int playerNum) {
        // TODO Auto-generated method stub
        super.setLongestRoadPlayer(playerNum);
        broadcastCommand(new Command(CommandType.CMD_SET_LONGEST_ROAD_PLAYER), getLongestRoadPlayerNum());
    }

    void broadcastCommand(Command cmd, int playerNum) {
    	cmd.setPlayerNum(playerNum);
    	Iterator<PlayerRemote> it = connections.values().iterator();
    	while (it.hasNext()) {
    		PlayerRemote p = it.next();
    		if (p.isConnected()) {
    			p.addCommand(cmd);
    		}
    	}
    }

    private int getNumConnected() {
        int numConnected = 0;
        Iterator<PlayerRemote> it = connections.values().iterator();
        while (it.hasNext()) {
            if (it.next().isConnected())
                numConnected++;
        }
        log.debug("getNumconnected returning : " + numConnected);
        return numConnected;
    }

    private int getNumConfirmed() {
        int numConfirmed = 0;
        Iterator<PlayerRemote> it = connections.values().iterator();
        while (it.hasNext()) {
            PlayerRemote p = it.next();
            if (p.isConnected() && p.isConfirmed())
                numConfirmed++;
        }
        return numConfirmed;
    }

    protected SOCPlayer getNewDefaultPlayer() {
        return new SOCPlayerRandom();
    }
    
    Color useColor(String name) {
        log.debug("useColor '" + name + "' actual: " + availableColors);
        String colors = null;
        Color color = null;
        synchronized (availableColors) {
        	if (availableColors.containsKey(name)) {
        		color = availableColors.remove(name);
        		colors = Protocol.encodeColors(availableColors);
            }
        }
        if (colors != null)
            broadcastCommand(new Command(CommandType.CMD_SET_AVAILABLE_COLORS, colors), 0);
        return color;
    }
    
    /*
     *  (non-Javadoc)
     * @see cc.game.soc.core.SOC#runGame()
     */
    public void runGame() {
       log.debug("Running game for player " + getCurPlayer());
       try {
           super.runGame();
       } catch (Throwable t) {
           t.printStackTrace();
           gameRunning = false;
       }
    }
    
    /*
     * Return true if there is a t least 1 player connected
     */
    boolean isGameRunning() {
        return gameRunning && getNumConnected() > 0;
    }
    
    void playerDisconnected(int playerNum) {
        synchronized (confirmMonitor) {
            confirmMonitor.notify();
        }    
        synchronized (connectMonitor) {
            connectMonitor.notify();
        }    
    }

    /**
     * 
     */
    public void reset() {
        super.reset();
        availableColors.putAll(allColors);
        gameRunning = false;
        firstConnection = 0;
    }
}
