package cc.game.soc.netx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import cc.game.soc.core.*;

/**
 * GUI implementations integrate with this class to interact with SOC Server
 * 
 * @author ccaron
 *
 */
public abstract class NetClient {

    final static int READ_TIMEOUT = 10000;
    final static int PING_FREQUENCY = 5000;
    
    enum State {
        DISCONNECTED,
        DISCONNECTING,
        CONNECTING,
        CONNECTED
    }
    
    private SOC soc;
	private SOCPlayer player;
	private String host;
	private int port;
	private String clientName;
	private State state = State.DISCONNECTED;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private CommandQueue inQueue;
    private CommandQueue inQueueBlocking;
	
	public NetClient() {
	}
	
	State getState() {
	    return this.state;
	}
	
	/**
	 * 
	 * @param soc
	 * @param player
	 */
	public void initialize(SOC soc, SOCPlayer player) {
	    this.soc = soc;
	    this.player = player;
	}

	/**
	 * Async connect to a SOC server
	 * 
	 * @param host
	 * @param port
	 * @throws Exception if socket cannot be established or IO error on sending
	 */
	public void connect(String host, int port) throws Exception {
		try {
		    state = State.CONNECTING;
			socket = new Socket(host, port);
			socket.setSoTimeout(10000);
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
            start();
			send(Command.newClConnect());
			this.host = host;
			this.port = port;
		} catch (Exception e) {
			close();
			throw e;
		}
	}
	
	/**
	 * Print a debug.  default behavior prints to stdout
	 * @param msg
	 */
	protected void printDebug(String msg) {
	    System.out.println(msg);
	}
	
	/**
	 * Cannot when we have successfully connected.  @see connect
	 */
	protected abstract void onConnected();
	
	/**
	 * Called when a new board has arrived
	 * @param board
	 */
	protected abstract void onNewBoard(SOCBoard board);

	/**
	 * Called when a form has arrived
	 * @param form
	 */
	protected abstract void onForm(ClientForm form);
	
	/**
	 * Attempt to reconnect using prior credentials.
	 * @throws Exception
	 */
	public void reconnect() throws Exception {
	    if (isConnected())
	        throw new Exception("Client still running");
	    if (host == null || clientName == null)
	        throw new Exception("Cannot reconnect from invalid prior connection");
	    try {
	        state = State.CONNECTING;
	        socket = new Socket(host, port);
            socket.setSoTimeout(10000);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            start();
            send(Command.newClReconnect(clientName));            
	    } catch (Exception e) {
	        close();
	        throw e;
	    }
	    
	    
	}

	/**
	 * Called when we have successfully re-connected.  @see reconnect
	 */
	protected abstract void onReconnected();

	/**
	 * gracefully disconnect from the server.  onDisconnected will NOT be called.
	 */
	public void disconnect() {
		if (isConnected()) {
		    state = State.DISCONNECTING;
		    try {
		        send(Command.newClDisconnect());
		        Thread.sleep(500);
		        close();
		    } catch (Exception e) {
		        e.printStackTrace();
		        close();
		    }
		}
	}
	/**
	 * Called when the server has disconnected us due to some error
	 * @param reason
	 */
	protected abstract void onDisconnected(String reason);
	
	/**
	 * 
	 * @return
	 */
	public SOC getSOC() {
		return soc;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return state == State.CONNECTED;
	}
	private void close() {
		try {
			input.close();
		} catch (Exception e) {}
		input = null;
		try {
			output.flush();
		} catch (Exception e) {}
		try {
			output.close();
		} catch (Exception e) {}
		output = null;
		try {
			Socket s = socket;
			socket = null;
			s.close();
		} catch (Exception e) {}
		if (inQueue != null) {
		    inQueue.stop();
		    inQueue = null;
		}
        if (inQueueBlocking != null) {
            inQueueBlocking.stop();
            inQueueBlocking = null;
        }
		
		state = State.DISCONNECTED;
	}
	private void start() {
	    inQueue = new InQueue();
	    new Thread(inQueue).start();
	    inQueueBlocking = new InQueue();
	    new Thread(inQueueBlocking).start();
		new Thread(new ServerConnection()).start();
	}
	
	private void startPingThread() {
		new Thread(new Runnable() { // Ping Thread
		   public void run() {
		       printDebug("Ping thread starting");
		       while (isConnected()) {
		           try {
		               Thread.sleep(PING_FREQUENCY);
		               send(Command.newClPing());		               
		           } catch (Exception e) {
		               e.printStackTrace();
		           }
		       }
		       printDebug("Ping thread ending");
		   }
		}).start();
	}
	
	private class ServerConnection implements Runnable {
	    
		public void run() {
		    printDebug("client thread starting");
			try {
				while (socket != null) {
				    Command cmd = Command.read(input);
				    if (cmd.getType().isBlocking())
				        inQueueBlocking.add(cmd);
				    else
				        inQueue.add(cmd);
				}
			} catch (Exception e) {
			    if (state != State.DISCONNECTING) {
    				e.printStackTrace();
    				onDisconnected("Connection lost: " + e.getClass().getSimpleName() + " " + e.getMessage());
			    }
			}
			close();
			printDebug("client thread exiting");
		}
		
	}

	synchronized void send(Command cmd) throws Exception {
		if (output != null) {
			cmd.write(output);
			output.flush();
		}
	}
	
	class InQueue extends CommandQueue {
	    
        protected void process(Command cmd) throws Exception {

			switch (cmd.getType()) {
			case SRVR_CONNECTED:
			    clientName = Command.parseSrvrConnected(cmd);
			    printDebug("Connected clientName=" + clientName);
			    state = State.CONNECTED;
			    startPingThread();
			    onConnected();
                break;
                
			case SRVR_RECONNECTED:
                state = State.CONNECTED;
                startPingThread();
			    onReconnected();
			    break;
			    
            case SRVR_ERROR:
                close();
                onDisconnected(Command.parseSrvrError(cmd));
                break;
                
            case SRVR_DISCONNECTED:
                close();
                onDisconnected(Command.parseSrvrDisconnected(cmd));
                break;
			    
			case SRVR_CHOOSE_SETTLEMENT_VERTEX:
			    SOCVertex v = player.chooseSettlementVertex(soc, Command.parseSrvrChooseSettlementVertex(cmd));
				send(Command.newClSetSettlementVertex(soc.getBoard().getVertexIndex(v)));
				break;
				
			case SRVR_CHOOSE_ROAD_EDGE:
			    SOCEdge e = player.chooseRoadEdge(soc, Command.parseSrvrChooseRoadEdge(cmd));
				send(Command.newClSetRoadEdge(soc.getBoard().getEdgeIndex(e)));
				break;
				
			case SRVR_CHOOSE_CITY_VERTEX:
			    v = player.chooseCityVertex(soc, Command.parseSrvrChooseCityVertex(cmd));
				send(Command.newClSetCityVertex(soc.getBoard().getVertexIndex(v)));
				break;
				
			case SRVR_CHOOSE_ROBBER_CELL:
			    SOCCell c = player.chooseRobberCell(soc, Command.parseSrvrChooseRobberCell(cmd));
				send(Command.newClChooseRobberCell(soc.getBoard().getCellIndex(c)));
				break;
				
			case SRVR_CHOOSE_MOVE:
				send(Command.newClChooseMove(player.chooseMove(soc, Command.parseSrvrChooseMove(cmd))));
				break;
				
			case SRVR_ROLL_DICE:
				send(Command.newClRollDice(player.rollDice(soc)));
				break;
				
			case SRVR_CHOOSE_TRADE_OPTIONS:
			    send(Command.newClSetTrade(player.chooseTradeOption(soc, Command.parseSrvrChooseTradeOption(cmd))));
				break;
				
			case SRVR_CHOOSE_PLAYER_TO_TAKE_CARD_FROM:
				send(Command.newClSetPlayertoTakeCardFrom(player.choosePlayerNumToTakeCardFrom(soc, Command.parseSrvrChoosePlayerToTakeCardFrom(soc, cmd))));
				break;
				
			case SRVR_CHOOSE_CARD_TO_GIVE_UP:
				send(Command.newClSetCardToGiveUp(player.chooseCardToGiveUp(soc, Command.parseSrvrChooseCardToGiveUp(cmd), 0)));
				break;
				
			case SRVR_CHOOSE_RESOURCE:
				send(Command.newClSetResource(player.chooseResource(soc)));
				break;
				
			case SRVR_BOARD:
			    //readBoard(cmd.getString("board"));
			    Command.parseUpdateBoard(soc.getBoard(), cmd);
			    break;
			    
			case SRVR_FORM:
			    onForm(Command.parseSrvrForm(cmd, NetClient.this));
			    break;
			    
			case SRVR_PING_RESPONSE:
			    // this is just to keep us from hitting the read timeout
			    break;
			}
	    }
        @Override
        protected void onExit() {
            // TODO Auto-generated method stub
            
        }
	}

    protected abstract void onFormSubbmited();
}
