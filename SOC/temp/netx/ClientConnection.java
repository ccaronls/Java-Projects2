package cc.game.soc.netx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Logger;

class ClientConnection implements Runnable {

    Logger log = Logger.getLogger("Pending Connection");    
	Socket socket;
	DataInputStream input;
	DataOutputStream output;
	String clientName;
	NetPlayer player;
	NetGame game;
	NetServer server;
	CommandQueue outQueue = new OutQueue();
	
	ClientConnection(Socket socket, NetServer server) throws IOException {
	    this.server = server;
	    this.socket = socket;
	    socket.setSoTimeout(NetClient.READ_TIMEOUT);
	    this.input = new DataInputStream(socket.getInputStream());
	    this.output = new DataOutputStream(socket.getOutputStream());
	}
	
	void close() {
	    log.info("close");
        outQueue.stop();
        try {
            output.flush();
        } catch (Exception e) {}
	    try {
	        output.close();
	    } catch (Exception e) {}
        try {
            input.close();
        } catch (Exception e) {}
	    Socket s = socket;
	    socket = null;
	    try {
	        s.close();
	    } catch (Exception e) {}
	    input = null;
	    output = null;
	}
	
	public void run() {

	    // handshake
	    try {
	        new Thread(outQueue).start();
	        //socket.setSoTimeout(5*1000);
	        Command connect = Command.read(input);
	        switch (connect.getType()) {
	            case CL_CONNECT:
	                clientName = server.generateNewClientName();
	                send(Command.newSrvrConnected(clientName));
	                server.clientConnected(this);
	                break;
	            case CL_RECONNECT:
	                clientName = Command.parseClReconnect(connect);
	                send(Command.newSrvrReconnected());
	                server.clientReconnected(this);
	                break;
	            default:
	                throw new Exception("Handshake fail: invalid connect command " + connect);
	        }
	        socket.setSoTimeout(0);
	        
	        log = Logger.getLogger("Client:" + clientName);
	        log.info("thread starting");
	    
	        while (socket != null) {
				
				final Command cmd = Command.read(input);
				log.debug("read cmd: " + cmd);
				if (cmd.getType().isBlocking()) {
				    new Thread(new Runnable() {
				        public void run() {
				            try {
				                processCommand(cmd);
				            } catch (Exception e) {
				                e.printStackTrace();
				                close();
				            }
				        }
				    }).start();
				} else {
				    processCommand(cmd);
				}
	        }
		} catch (Exception e) {
			e.printStackTrace();
			close();
		}
		
		log.info("thread exiting");
	}

    void processCommand(Command cmd) throws Exception {
        switch (cmd.getType()) {
            case CL_DISCONNECT:
                leaveGame();
                close();
                break;
            case CL_FORMSUBMIT:
                server.onFormSubmitted(Command.parseClFormSubmit(cmd), clientName);
                break;
            case CL_SET_SETTLEMENT_VERTEX:
                player.setResult(game.getBoard().getVertex(Command.parseClSetSettlementVertex(cmd))); 
                break;
            case CL_SET_ROAD_EDGE:
                player.setResult(game.getBoard().getEdge(Command.parseClSetRoadEdge(cmd))); 
                break;
            case CL_SET_CITY_VERTEX:
                player.setResult(game.getBoard().getVertex(Command.parseClSetCityVertex(cmd))); 
                break;
            case CL_SET_ROBBER_CELL:
                player.setResult(game.getBoard().getCell(Command.parseClChooseRobberCell(cmd))); 
                break;
            case CL_SET_MOVE:
                player.setResult(Command.parseClChooseMove(cmd)); 
                break;
            case CL_SPIN_DICE:
                game.spinDice(); player.setResult(new Boolean(true)); 
                break;
            case CL_SET_PLAYER_TO_TAKE_CARD_FROM:
                player.setResult(Command.parseClSetPlayerToTakeCardFrom(game, cmd)); 
                break;
            case CL_SET_CARD_TO_GIVE_UP:
                player.setResult(Command.parseClSetCardToGiveUp(cmd)); 
                break;
            case CL_SET_RESOURCE:
                player.setResult(Command.parseClSetResource(cmd)); 
                break;
            case CL_PING:
                send(Command.newSrvrPingResponse());
                break;
            default:
                throw new Exception("Unhandled command: " + cmd);
        }
        
    }

	class OutQueue extends CommandQueue {
	    protected void process(Command cmd) throws Exception {
	        log.info("write to socket: " + cmd);
	        cmd.write(output);
	    }
	    protected void onExit() {
	        log.info("onExit");
	        close();
	    }
	}
	
	void leaveGame() {
	    if (player != null) {
	        player.connection = null;
	        player = null;
	    }
	}

	void send(Command command) throws Exception {
	    outQueue.add(command);
	}

    public boolean isConnected() {
        return socket != null;
    }
}
