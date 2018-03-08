package cc.lib.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;

/**
 * A Game server is a server that handles normal connection/handshaking and maintains
 * a set of ClientConnections.  The base version should be minimal with only those
 * methods to listen for connections accept and handshake with the client and maintain
 * the connections.  
 * 
 * The Base Game Server Provides:
 * - Handshaking with new connections
 * - Managing Connection timeouts and reconnects.
 * - Managing older client versions.
 * - Managing Protocol Encryption  (TODO)
 * 
 * Override protected methods to create custom behaviors
 * 
 * @author ccaron
 *
 */
public class GameServer {

    /**
     *
     */
    public interface Listener {
        /**
         *
         * @param conn
         */
        void onConnected(ClientConnection conn);

        /**
         *
         * @param conn
         */
        void onReconnection(ClientConnection conn);
        /*

         */
        void onClientDisconnected(ClientConnection conn);

        /**
         *
         * @param conn
         * @param command
         */
        void onClientCommand(ClientConnection conn, GameCommand command);

        /**
         *
         * @param conn
         * @param id
         * @param params
         */
        void onFormSubmited(ClientConnection conn, int id, Map<String,String> params);
    }
    
    // keep sorted by alphabetical order 
    private final Map<String,ClientConnection> clients = new LinkedHashMap<>();
    private SocketListener socketListener;
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private final int clientReadTimeout;
    private final String mVersion;
    private final Cypher cypher;
    private final int maxConnections;
    private final int port;
    
    final static int ENCRYPTION_CHUNK_SIZE = 256;

    public String toString() {
        String r = "GameServer:" + mVersion;
        if (clients.size() > 0)
            r += " connected clients:" + clients.size();
        return r;
    }

    /**
     * Create a server and start listening.  When cypher is not null, then then server will only
     * allow encrypted clients.
     * 
     * @param listenPort port to listen on for new connections
     * @param clientReadTimeout timeout in milliseconds before a client disconnect
     * @param serverVersion version of this service to use to check compatibility with clients
     * @param cypher used to encrypt the dialog. can be null.
     * @param maxConnections max number of clients to allow to be connected
     * @throws IOException 
     * @throws Exception
     */
    public GameServer(int listenPort, int clientReadTimeout, String serverVersion, Cypher cypher, int maxConnections) {
        this.clientReadTimeout = clientReadTimeout;
        this.port = listenPort;
        this.maxConnections = maxConnections;
        this.mVersion = serverVersion.toString(); // null check
        this.cypher = cypher;
    }

    /**
     * Start listening for connections
     * @throws IOException
     */
    public void listen() throws IOException {
        ServerSocket socket = new ServerSocket(port);
        new Thread(socketListener = new SocketListener(socket)).start();
    }

    /**
     *
     * @return
     */
    public final boolean isRunning() {
        return socketListener != null;
    }

    /**
     *
     * @param l
     */
    public final void addListener(Listener l) {
        listeners.add(l);
    }

    /**
     *
     * @param l
     */
    public final void removeListener(Listener l) {
        listeners.remove(l);
    }

    final Iterable<Listener> getListeners() {
        return listeners;
    }

    /**
     * Disconnect all clients and stop listening.  Will block until all clients have closed their sockets.
     */
    public final void stop() {
        logInfo("GameServer: Stopping server: " + this);
        if (socketListener != null) {
            socketListener.stop();
            synchronized (clients) {
                for (ClientConnection c : clients.values()) {
                    c.disconnect("Server Stopping");
                }
            }
            clients.clear();
            socketListener = null;
        }
    }
    
    /**
     * Get iterable over all connection ids
     * @return
     */
    public final Iterable<String> getConnectionKeys() {
        return clients.keySet();
    }
    
    /**
     * Get iterable over all connection values
     * @return
     */
    public final Iterable<ClientConnection> getConnectionValues() {
        return clients.values();
    }
    
    /**
     * Get a specific connection by its id.
     * @param id
     * @return
     */
    public final ClientConnection getClientConnection(String id) {
        return clients.get(id);
    }

    public final ClientConnection getConnection(int index) {
        Iterator<ClientConnection> it = clients.values().iterator();
        while (index-- > 0 && it.hasNext()) {
            it.next();
        }
        return it.next();
    }

    /**
     * 
     * @return
     */
    public final int getNumClients() {
        return clients.size();                
    }
    
    /**
     * Broadcast a command to all connected clients
     * @param cmd
     */
    public final void broadcast(GameCommand cmd) {
        synchronized (clients) {
            for (ClientConnection c : clients.values()) {
                if (c.isConnected())
                    try {
                        c.sendCommand(cmd);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logError("ERROR Sending to client '" + c.getName() + "' " + e.getClass() + " " + e.getMessage());
                    }
            }
        }
    }

    /**
     * Broadcast a command to all connected clients
     * @param message
     */
    public final void broadcastMessage(String message) {
        broadcast(new GameCommand(GameCommandType.SVR_MESSAGE).setMessage(message));
    }

    private class SocketListener implements Runnable {

        ServerSocket socket;
        boolean running;
        
        SocketListener(ServerSocket socket) {
            this.socket = socket;
            running = true;
        }
        
        void stop() {
            running = false;
            try {
                socket.close();
            } catch (Exception e) {}
        }
        
        public void run() {
            try {
            
                logInfo("GameServer: Thread started listening for connections");
                while (running) {
                    Socket client = socket.accept();
                    new Thread(new HandshakeThread(client)).start();
                }                
            } catch (SocketException e) {
                logWarn("GameServer: Thread Exiting since socket is closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void close(Socket socket, InputStream in, OutputStream out) {
        try {
            if (out== null)
                out = socket.getOutputStream();
            out.flush();
            out.close();
        } catch (Exception ex) {}                
        try {
            if (in == null)
                in = socket.getInputStream();
            in.close();
        } catch (Exception ex) {}
        try {
            socket.close();
        } catch (Exception ex) {}   
    }
    
    private class HandshakeThread implements Runnable {
        final Socket socket;
        
        HandshakeThread(Socket socket) throws Exception {
            this.socket = socket;
            socket.setSoTimeout(clientReadTimeout+5000); // give a few seconds latency 
        }

        public void run() {
            InputStream in = null;
            OutputStream out = null;
            try {

                logInfo("GameServer: Start handshake with new connection");
                if (cypher != null) {
                    in = new EncryptionInputStream(socket.getInputStream(), cypher);
                    out = new EncryptionOutputStream(socket.getOutputStream(), cypher);
                } else {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                }
                
                GameCommand cmd = GameCommand.parse(in);
                String name = cmd.getName();
                String clientVersion = cmd.getVersion();
                if (clientVersion == null) {
                    new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: Missing required 'version' attribute").write(out);
                    throw new ProtocolException("Broken Protocol.  Expected clientVersion field in cmd: " + cmd);
                }
                
                clientVersionCompatibilityTest(clientVersion, mVersion);
                ClientConnection conn = null;
                
                synchronized (clients) {
                    if (cmd.getType() == GameCommandType.CL_CONNECT) {
                        if (clients.containsKey(name)) {
                            //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
                            throw new ProtocolException("client with name already exists");
                        }
                        if (clients.size() >= maxConnections) {
                            throw new java.net.ProtocolException("Max client connections reached");
                        }
                        conn = new ClientConnection(GameServer.this, name);
                        conn.connect(socket, in, out);
                        clients.put(name, conn);
                    } else if (cmd.getType() == GameCommandType.CL_RECONNECT) {
                        if (!clients.containsKey(name)) {
                            //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: No record of client with the name '" + name + "' was found").write(out);
                            throw new ProtocolException("Unknown client connection '" + name + "'");
                        } 
    
                        conn = clients.get(name);
                        if (conn.isConnected()) {
                            //new GameCommand(GameCommandType.SVR_MESSAGE).setMessage("ERROR: A client with the name '" + name + "' is already connected").write(out);
                            throw new ProtocolException("Client '"  + name + "' is already connected");
                        }
                        conn.connect(socket, in , out);
                        //new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                        //logDebug("GameServer: Client " + name + " connected");
                        //serverListener.onReconnection(conn);
                    } else {
                        throw new ProtocolException("Handshake failed: Invalid client command: " + cmd);
                    }
                }
                
                new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                logDebug("GameServer: Client " + name + " connected");
                if (cmd.getType() == GameCommandType.CL_CONNECT) {
                    for (Listener l : listeners)
                        l.onConnected(conn);
                } else {
                    for (Listener l : listeners)
                        l.onReconnection(conn);
                }
                
                //new GameCommand(GameCommandType.SVR_CONNECTED).setArg("keepAlive", clientReadTimeout).write(out);
                //logDebug("GameServer: Client " + name + " connected");

                // send the client the main menu
            } catch (ProtocolException e) {
                try {
                    new GameCommand(GameCommandType.SVR_DISCONNECTED).setMessage(e.getMessage()).write(out);
                } catch (Exception ex) {}
                close(socket, in, out);
            } catch (Exception e) {
                logError(e);
                close(socket, null, null);
            }
        }
    }
    
    /**
     * Override to do custom debug logging.  Default impl writes to stdout and prefix with DEBUG
     * @param msg
     */
    public void logDebug(String msg) {
        System.out.println("DEBUG:" + msg);
    }

    /**
     * Override this method to perform any custom version compatibility test.
     * If the clientVersion is compatible, do nothing.  Otherwise throw a 
     * descriptive message. Default implementation throws an exception unless 
     * clientVersion is exact match for @see getVersion.
     * 
     * @param clientVersion
     * @throws Exception
     */
    protected void clientVersionCompatibilityTest(String clientVersion, String serverVersion) throws ProtocolException {
        if (!clientVersion.equals(mVersion))
            throw new ProtocolException("Incompatible client version '" + clientVersion + "'");
    }

    /**
     * Override to perform custom logging.  Default writes to stdout a prefix with INFO
     * @param msg
     */
    public void logInfo(String msg) {
        System.out.println("INFO :" + msg);
    }
    
    /**
     * Override to perform custom logging.  Default writes to stdout a prefix with WARN
     * @param msg
     */
    public void logWarn(String msg) {
        System.out.println("WARN :" + msg);
    }

    /**
     * Override to perform custom error logging.  default writes to stderr
     * @param msg
     */
    public void logError(String msg) {
        System.err.println("ERROR:" + msg);
    }

    /**
     * Override to perform custom error logging.  default writes to stderr
     * @param e
     */
    public void logError(Exception e) {
        System.err.println("ERROR:" + e.getClass().getSimpleName() + " " + e.getMessage());
    }
    
}
