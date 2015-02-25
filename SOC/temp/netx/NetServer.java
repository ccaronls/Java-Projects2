package cc.game.soc.netx;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Base class for SOC server implementations.  See SimpleServer for example
 * @author ccaron
 *
 */
public class NetServer {

    Logger log = Logger.getLogger("Server");
    ServerSocket listenSocket;
    
    protected NetServer(int listenPort) throws IOException {
        listenSocket = new ServerSocket(listenPort);
        new Thread(new ListenThread()).start();
    }
    
    public synchronized void stop() {
        try {
            ServerSocket s = listenSocket;
            listenSocket = null;
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private class ListenThread implements Runnable {
        public void run() {
            while (listenSocket != null) {
                try {
                    
                    Socket socket = listenSocket.accept();
                    new Thread(new ClientConnection(socket, NetServer.this)).start();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int numConnections = 0;
    private Map<String, ClientConnection> clients = Collections.synchronizedMap(new HashMap<String, ClientConnection>());
    
    public int getNumConnections() {
        return this.numConnections;
    }
    
    synchronized String generateNewClientName() {
        return "socclient" + (++numConnections);
    }

    synchronized void clientConnected(ClientConnection clientConnection) {
        clients.put(clientConnection.clientName, clientConnection);
        onClientConnection(clientConnection.clientName);
    }

    synchronized void clientReconnected(ClientConnection clientConnection) {
        clients.put(clientConnection.clientName, clientConnection);
        onClientReconnection(clientConnection.clientName);
    }

    public Set<String> getClientNames() {
        return Collections.unmodifiableSet(clients.keySet());
    }
    
    public void sendForm(ServerForm form, String clientName) throws Exception {
        log.debug("sendForm " + form.getId() + " to " + clientName);
        clients.get(clientName).send(Command.newSrvrForm(form));
    }

    /**
     * Try to attach a client to a game.  return true on succeess, false otherwise.
     * 
     * @param game
     * @param clientName
     * @param slot
     * @return
     *
    public synchronized boolean joinGame(NetGame game, String clientName, int slot) {
        try {
            ClientConnection conn = clients.get(clientName);
            game.getNetPlayer(slot).setConnection(conn);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }        
    }
    
    public void sendBoard(Board board, String clientName) throws Exception {
        clients.get(clientName).send(Command.newUpdateBoard(board));
    }
    
    /**
     * Get a new form instance.  Only servers can create forms.
     * @param id
     * @return
     */
    public ServerForm createForm(int id) {
        return new ServerForm(id);
    }
    
    /**
     * Called whenever a new client has successfully connected.
     * Default behavior does nothing
     * @param clientName
     */
    protected void onClientConnection(String clientName) {}
    
    /**
     * Called when a client that previously left has rejoined.
     * Default behavior does nothing
     * @param clientName
     */
    protected void onClientReconnection(String clientName) {}
    
    /**
     * Called when a client has disconnected.
     * Default behavior does nothing
     */
    protected void onClientDisconnected(String clientName) {}
    
    /**
     * Called when a client has submitted a form.
     * Default behavior does nothing
     * @param form
     * @param clientName
     */
    protected void onFormSubmitted(FormResponse form, String clientName) {}
}
